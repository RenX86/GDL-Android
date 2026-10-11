package com.renx86.gdlapp.data.update

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

data class DownloadProgress(val readBytes: Long, val totalBytes: Long)

@Singleton
class UpdateRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }

    suspend fun fetchLatest(): UpdateInfo? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL("https://api.github.com/repos/RenX86/GDL-Android/releases/latest")
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().readText()
                val json = JSONObject(response)
                
                val version = json.getString("tag_name")
                val changelog = json.optString("body", "No changelog provided.")
                val htmlUrl = json.optString("html_url", "")

                var apkUrl = ""
                var apkSizeBytes = 0L

                val assets = json.optJSONArray("assets")
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.getString("name")
                        if (name.endsWith(".apk")) {
                            apkUrl = asset.getString("browser_download_url")
                            apkSizeBytes = asset.optLong("size", 0L)
                            break
                        }
                    }
                }

                if (apkUrl.isNotEmpty()) {
                    return@withContext UpdateInfo(version, apkUrl, apkSizeBytes, changelog, htmlUrl)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            connection?.disconnect()
        }
        null
    }

    fun downloadApk(info: UpdateInfo): Flow<DownloadProgress> = flow {
        val apkFile = getApkFile(info.version)
        if (apkFile.exists() && apkFile.length() > 0) {
            // Already downloaded
            emit(DownloadProgress(apkFile.length(), apkFile.length()))
            return@flow
        }

        val partFile = File(updatesDir, "GDL-${info.version}.part")
        var connection: HttpURLConnection? = null
        
        try {
            val url = URL(info.apkUrl)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.instanceFollowRedirects = true // GitHub redirects to objects.githubusercontent.com
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw Exception("HTTP Error ${connection.responseCode}")
            }

            // Content-Length may be missing (chunked); fall back to the size reported by GitHub
            val totalBytes = connection.contentLengthLong.takeIf { it > 0L } ?: info.apkSizeBytes
            if (totalBytes <= 0L) throw Exception("Unknown download size")

            var readBytes = 0L
            val buffer = ByteArray(8 * 1024)
            var lastEmitTime = 0L

            connection.inputStream.use { input ->
                FileOutputStream(partFile).use { output ->
                    while (currentCoroutineContext().isActive) {
                        val bytes = input.read(buffer)
                        if (bytes == -1) break
                        
                        output.write(buffer, 0, bytes)
                        readBytes += bytes

                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastEmitTime > 100) {
                            // Never report "complete" until the file has been finalized below
                            emit(DownloadProgress(readBytes.coerceAtMost(totalBytes - 1), totalBytes))
                            lastEmitTime = currentTime
                        }
                    }
                }
            }

            if (currentCoroutineContext().isActive && readBytes > 0) {
                if (partFile.length() != totalBytes) {
                    partFile.delete()
                    throw Exception("Downloaded file size mismatch (${partFile.length()} != $totalBytes)")
                }
                val archiveInfo = context.packageManager.getPackageArchiveInfo(partFile.absolutePath, 0)
                if (archiveInfo == null || archiveInfo.packageName != context.packageName) {
                    partFile.delete()
                    throw Exception("Downloaded file is not a valid APK for this app")
                }
                if (!partFile.renameTo(apkFile)) throw Exception("Could not save update file")
                emit(DownloadProgress(apkFile.length(), apkFile.length()))
            } else {
                partFile.delete()
            }
        } catch (e: Exception) {
            partFile.delete()
            throw e
        } finally {
            connection?.disconnect()
        }
    }.flowOn(Dispatchers.IO)

    fun getApkFile(version: String): File {
        return File(updatesDir, "GDL-$version.apk")
    }

    fun cleanupOldApks(currentVersion: String) {
        val files = updatesDir.listFiles() ?: return
        for (file in files) {
            if (file.name.startsWith("GDL-") && file.name.endsWith(".apk")) {
                val fileVersion = file.name.removePrefix("GDL-").removeSuffix(".apk")
                if (!VersionComparator.isNewer(fileVersion, currentVersion)) {
                    file.delete()
                }
            } else if (file.name.endsWith(".part")) {
                file.delete()
            }
        }
    }
}
