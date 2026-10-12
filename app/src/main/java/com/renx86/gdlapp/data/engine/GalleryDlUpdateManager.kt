package com.renx86.gdlapp.data.engine

import android.content.Context
import com.renx86.gdlapp.python.GalleryDlBridge
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GalleryDlUpdateManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val enginePrefs: EnginePreferences,
    private val bridge: GalleryDlBridge
) {

    companion object {
        private const val PYPI_URL = "https://pypi.org/pypi/gallery-dl/json"

        fun parsePyPiJson(jsonString: String): EngineUpdateInfo? {
            return try {
                val json = Json.parseToJsonElement(jsonString).jsonObject
                val infoObj = json["info"]?.jsonObject ?: return null
                val latestVersion = infoObj["version"]?.jsonPrimitive?.content ?: return null

                val urlsArray = json["urls"]?.jsonArray ?: return null
                var wheelUrl: String? = null
                var wheelSize = 0L
                var wheelSha256: String? = null
                var uploadTime = ""

                for (element in urlsArray) {
                    val item = element.jsonObject
                    val packageType = item["packagetype"]?.jsonPrimitive?.content
                    val filename = item["filename"]?.jsonPrimitive?.content ?: ""
                    if (packageType == "bdist_wheel" || filename.endsWith(".whl")) {
                        wheelUrl = item["url"]?.jsonPrimitive?.content
                        wheelSize = item["size"]?.jsonPrimitive?.longOrNull ?: 0L
                        val digests = item["digests"]?.jsonObject
                        wheelSha256 = digests?.get("sha256")?.jsonPrimitive?.content
                        uploadTime = item["upload_time"]?.jsonPrimitive?.content ?: ""
                        break
                    }
                }

                if (wheelUrl != null && wheelSha256 != null) {
                    EngineUpdateInfo(
                        version = latestVersion,
                        wheelUrl = wheelUrl,
                        sizeBytes = wheelSize,
                        sha256 = wheelSha256,
                        releaseDate = uploadTime
                    )
                } else null
            } catch (e: Exception) {
                null
            }
        }

        fun unzipWithSlipProtection(inputStream: InputStream, destinationDir: File) {
            val canonicalDest = destinationDir.canonicalPath
            ZipInputStream(inputStream).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val targetFile = File(destinationDir, entry.name)
                    val canonicalTarget = targetFile.canonicalPath

                    if (!canonicalTarget.startsWith(canonicalDest + File.separator) && canonicalTarget != canonicalDest) {
                        throw SecurityException("Zip Slip path traversal attempt: ${entry.name}")
                    }

                    if (entry.isDirectory) {
                        targetFile.mkdirs()
                    } else {
                        targetFile.parentFile?.mkdirs()
                        FileOutputStream(targetFile).use { out ->
                            zip.copyTo(out)
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        }
    }

    suspend fun checkLatest(): Result<EngineUpdateInfo?> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(PYPI_URL)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
                connectTimeout = 10_000
                readTimeout = 15_000
            }

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext Result.failure(Exception("HTTP error ${connection.responseCode} querying PyPI"))
            }

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val info = parsePyPiJson(body)
            enginePrefs.setLastCheckTime(System.currentTimeMillis())
            Result.success(info)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }

    suspend fun downloadAndInstall(
        info: EngineUpdateInfo,
        onProgress: (Float) -> Unit = {}
    ): Result<String> = withContext(Dispatchers.IO) {
        val tempWheelFile = File(context.cacheDir, "gallery_dl_${info.version}.whl.part")
        var connection: HttpURLConnection? = null

        try {
            // 1. Download .whl file with SHA-256 calculation
            val url = URL(info.wheelUrl)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                instanceFollowRedirects = true
                connectTimeout = 15_000
                readTimeout = 30_000
            }

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext Result.failure(Exception("HTTP error ${connection.responseCode} downloading wheel"))
            }

            val totalBytes = connection.contentLengthLong.takeIf { it > 0 } ?: info.sizeBytes
            val digest = MessageDigest.getInstance("SHA-256")
            var downloadedBytes = 0L

            connection.inputStream.use { input ->
                FileOutputStream(tempWheelFile).use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var read: Int
                    var lastProgressUpdate = 0L

                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        digest.update(buffer, 0, read)
                        downloadedBytes += read

                        val now = System.currentTimeMillis()
                        if (now - lastProgressUpdate > 100 && totalBytes > 0) {
                            onProgress(downloadedBytes.toFloat() / totalBytes.toFloat())
                            lastProgressUpdate = now
                        }
                    }
                }
            }

            // 2. Verify SHA-256 checksum
            val calculatedHash = digest.digest().joinToString("") { "%02x".format(it) }
            if (!calculatedHash.equals(info.sha256, ignoreCase = true)) {
                tempWheelFile.delete()
                return@withContext Result.failure(
                    SecurityException("Checksum mismatch: expected ${info.sha256}, got $calculatedHash")
                )
            }

            // 3. Extract wheel into app-private storage with Zip-Slip protection
            val targetDir = File(context.filesDir, "gdl_engine_${info.version}")
            if (targetDir.exists()) {
                targetDir.deleteRecursively()
            }
            targetDir.mkdirs()

            unzipWithSlipProtection(tempWheelFile.inputStream(), targetDir)

            // 4. Validate unpacked python module structure
            val initFile = File(targetDir, "gallery_dl/__init__.py")
            if (!initFile.exists()) {
                targetDir.deleteRecursively()
                return@withContext Result.failure(
                    IllegalStateException("Extracted files missing gallery_dl/__init__.py")
                )
            }

            // 5. Reload Python bridge with new custom engine path
            val reloadResult = bridge.reloadEngine(targetDir.absolutePath)
            if (reloadResult.isFailure) {
                targetDir.deleteRecursively()
                return@withContext Result.failure(
                    reloadResult.exceptionOrNull() ?: Exception("Failed to reload Python engine")
                )
            }

            // 6. Persist settings and clean up old versions
            val activeVersion = reloadResult.getOrNull() ?: info.version
            enginePrefs.setCustomEnginePath(targetDir.absolutePath)
            enginePrefs.setCustomEngineVersion(activeVersion)

            cleanupOldEngineDirs(keepDirName = targetDir.name)

            Result.success(activeVersion)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            connection?.disconnect()
            tempWheelFile.delete()
        }
    }

    suspend fun resetToBundled(): Result<String> = withContext(Dispatchers.IO) {
        try {
            enginePrefs.clearCustomEngine()
            cleanupOldEngineDirs(keepDirName = null)
            val reloadResult = bridge.reloadEngine(null)
            reloadResult
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun cleanupOldEngineDirs(keepDirName: String?) {
        val files = context.filesDir.listFiles() ?: return
        for (file in files) {
            if (file.isDirectory && file.name.startsWith("gdl_engine_")) {
                if (keepDirName == null || file.name != keepDirName) {
                    file.deleteRecursively()
                }
            }
        }
    }
}
