package com.renx86.gdlapp.python

import android.content.Context
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.renx86.gdlapp.data.CookiePreferences
import com.renx86.gdlapp.data.DownloadPreferences
import com.renx86.gdlapp.data.CompressionPreferences
import com.renx86.gdlapp.util.WebPConverter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GalleryDlBridge @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: DownloadPreferences,
    private val cookiePrefs: CookiePreferences,
    private val compressionPrefs: CompressionPreferences
) {
    private var currentDownloadDir: String = ""

    private val module by lazy {
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(context))
        }
        Python.getInstance().getModule("gallery_dl_wrapper")
    }

    /**
     * Initialize (or re-initialize) gallery-dl with the current download path.
     * Called before every download to pick up any path changes from Settings.
     */
    private var isInitialized = false

    private fun ensureInitialized(tempCacheDir: String) {
        // We ALWAYS re-initialize with the new temp cache dir per download
        val filesDir = context.filesDir.absolutePath
        module.callAttr("initialize", filesDir, tempCacheDir)
        isInitialized = true

        applyCookies()
        applyDeduplication()
    }

    private fun applyDeduplication() {
        if (prefs.isDeduplicationEnabled()) {
            val archiveFile = File(context.filesDir, "archive.sqlite3")
            module.callAttr("set_archive", archiveFile.absolutePath)
        } else {
            // Explicitly clear archive so gallery-dl doesn't retain a stale setting
            module.callAttr("clear_archive")
        }
    }

    private fun applyCookies() {
        val cookieFile = File(context.filesDir, CookiePreferences.COOKIE_FILENAME)
        if (cookieFile.exists() && cookieFile.length() > 0) {
            module.callAttr("set_cookies", cookieFile.absolutePath)
            
            val userAgent = cookiePrefs.getUserAgent()
            if (!userAgent.isNullOrBlank()) {
                module.callAttr("set_user_agent", userAgent)
            }
        }
    }

    suspend fun getInfo(url: String): JSONObject = withContext(Dispatchers.IO) {
        val dummyCache = File(context.cacheDir, "gdl_dummy")
        dummyCache.mkdirs()
        ensureInitialized(dummyCache.absolutePath)
        
        val result = module.callAttr("get_info", url).toString()
        JSONObject(result)
    }

    suspend fun download(url: String, downloadId: String): JSONObject = withContext(Dispatchers.IO) {
        // STEP 1: Create a temporary cache directory for Python
        val tempCacheDir = File(context.cacheDir, "gdl_temp_$downloadId")
        tempCacheDir.mkdirs()

        ensureInitialized(tempCacheDir.absolutePath)

        // Run the python download!
        val result = module.callAttr("download", url).toString()
        val jsonResult = JSONObject(result)

        if (jsonResult.optString("status") == "ok") {
            // STEP 2: Optional WebP Auto-Conversion
            if (compressionPrefs.isAutoConvertEnabled()) {
                val quality = compressionPrefs.getWebpQuality()
                val keepOriginal = compressionPrefs.isKeepOriginalEnabled()
                tempCacheDir.walkTopDown().filter { it.isFile }.forEach { file ->
                    WebPConverter.convertFileToWebp(file, quality, keepOriginal)
                }
            }
            
            // STEP 3: Move from App Storage to User Chosen Directory (SAF)
            try {
                copyCacheToSafDestination(tempCacheDir)
            } catch (e: Exception) {
                return@withContext JSONObject().apply {
                    put("status", "error")
                    put("message", "Failed to move files to SAF destination: ${e.message}")
                }
            } finally {
                // STEP 4: Cleanup
                tempCacheDir.deleteRecursively()
            }
        } else {
            tempCacheDir.deleteRecursively()
        }

        jsonResult
    }

    private fun copyCacheToSafDestination(tempCacheDir: File) {
        val targetUriString = prefs.getDownloadPath()
        
        // If the user hasn't set a SAF folder yet (still absolute path or default)
        if (!targetUriString.startsWith("content://")) {
            // Fallback to direct copy (e.g. they are on Android 10 or using public Download folder natively)
            val destDir = File(targetUriString)
            destDir.mkdirs()
            tempCacheDir.copyRecursively(destDir, overwrite = true)
            return
        }

        val targetTreeUri = android.net.Uri.parse(targetUriString)
        val pickedDir = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, targetTreeUri)
            ?: throw IllegalStateException("Could not access selected folder")

        // Recursively walk the cache dir and copy every file
        tempCacheDir.walkTopDown().forEach { file ->
            if (file.isFile) {
                // We need to recreate the folder structure if gallery-dl created subfolders
                val relativePath = file.relativeTo(tempCacheDir).parent
                var currentDir = pickedDir
                
                if (relativePath != null && relativePath != "") {
                    val segments = relativePath.split(File.separatorChar)
                    for (segment in segments) {
                        val nextDir = currentDir.findFile(segment) ?: currentDir.createDirectory(segment)
                        currentDir = nextDir ?: throw IllegalStateException("Failed to create subfolder $segment")
                    }
                }

                // Check if file already exists in SAF, if so delete to overwrite
                val existingFile = currentDir.findFile(file.name)
                existingFile?.delete()

                val extension = file.extension.lowercase()
                val mimeType = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
                    ?: when (extension) {
                        "mp4", "m4v" -> "video/mp4"
                        "webm" -> "video/webm"
                        "mkv" -> "video/x-matroska"
                        "mov" -> "video/quicktime"
                        "jpg", "jpeg" -> "image/jpeg"
                        "png" -> "image/png"
                        "gif" -> "image/gif"
                        "webp" -> "image/webp"
                        else -> "application/octet-stream"
                    }

                val newDocFile = currentDir.createFile(mimeType, file.name)
                    ?: throw IllegalStateException("Failed to create file ${file.name}")

                context.contentResolver.openOutputStream(newDocFile.uri)?.use { outStream ->
                    file.inputStream().use { inStream ->
                        inStream.copyTo(outStream)
                    }
                }
            }
        }
    }
}