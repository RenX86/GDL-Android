package com.renx86.gdlapp.python

import android.content.Context
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
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
    private val prefs: DownloadPreferences
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