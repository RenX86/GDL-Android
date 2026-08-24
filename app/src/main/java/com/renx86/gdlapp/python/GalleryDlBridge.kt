package com.renx86.gdlapp.python

import android.content.Context
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.renx86.gdlapp.data.CookiePreferences
import com.renx86.gdlapp.data.DownloadPreferences
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
    private val cookiePrefs: CookiePreferences
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
    private fun ensureInitialized() {
        val targetDir = prefs.getDownloadPath()

        // Create the directory if it doesn't exist
        File(targetDir).mkdirs()

        // Only re-initialize if the path changed
        if (targetDir != currentDownloadDir) {
            val filesDir = context.filesDir.absolutePath
            module.callAttr("initialize", filesDir, targetDir)
            currentDownloadDir = targetDir
        }

        // Apply cookies if enabled
        applyCookies()
    }

    /**
     * If cookies.txt exists (saved via WebView login or manual paste),
     * automatically tell gallery-dl to use them. No manual toggle needed.
     */
    private fun applyCookies() {
        val cookieFile = File(context.filesDir, CookiePreferences.COOKIE_FILENAME)
        if (cookieFile.exists() && cookieFile.length() > 0) {
            module.callAttr("set_cookies", cookieFile.absolutePath)

            // Also match the User-Agent if one was saved from the WebView
            val userAgent = cookiePrefs.getUserAgent()
            if (!userAgent.isNullOrBlank()) {
                module.callAttr("set_user_agent", userAgent)
            }
        }
    }

    suspend fun getInfo(url: String): JSONObject = withContext(Dispatchers.IO) {
        ensureInitialized()
        val result = module.callAttr("get_info", url).toString()
        JSONObject(result)
    }

    suspend fun download(url: String): JSONObject = withContext(Dispatchers.IO) {
        ensureInitialized()
        val result = module.callAttr("download", url).toString()
        JSONObject(result)
    }
}