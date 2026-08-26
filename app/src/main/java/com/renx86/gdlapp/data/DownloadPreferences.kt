package com.renx86.gdlapp.data

import android.content.Context
import android.os.Environment
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stores and retrieves the user's chosen download directory.
 * Defaults to /storage/emulated/0/Download/GDL/
 */
@Singleton
class DownloadPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("gdl_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_DOWNLOAD_PATH = "download_path"
        private const val KEY_HIDDEN_FOLDER = "hidden_folder"

        fun getDefaultPath(isHidden: Boolean): String {
            return File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                if (isHidden) ".GDL" else "GDL"
            ).absolutePath
        }
    }

    fun isHiddenFolderEnabled(): Boolean {
        return prefs.getBoolean(KEY_HIDDEN_FOLDER, false)
    }

    fun setHiddenFolderEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HIDDEN_FOLDER, enabled).apply()
    }

    fun getDownloadPath(): String {
        val defaultPath = getDefaultPath(isHiddenFolderEnabled())
        val savedPath = prefs.getString(KEY_DOWNLOAD_PATH, null)
        
        // If the saved path matches the *other* default path (e.g. they toggled hidden but had the old default saved),
        // we should return the new default path.
        val otherDefault = getDefaultPath(!isHiddenFolderEnabled())
        if (savedPath == otherDefault) {
            return defaultPath
        }
        
        return savedPath ?: defaultPath
    }

    fun setDownloadPath(path: String?) {
        if (path == null) {
            prefs.edit().remove(KEY_DOWNLOAD_PATH).apply()
        } else {
            prefs.edit().putString(KEY_DOWNLOAD_PATH, path).apply()
        }
    }
}
