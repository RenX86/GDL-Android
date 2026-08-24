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

        // Public Downloads/GDL/ — survives app uninstall
        val DEFAULT_PATH: String = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "GDL"
        ).absolutePath
    }

    fun getDownloadPath(): String {
        return prefs.getString(KEY_DOWNLOAD_PATH, DEFAULT_PATH) ?: DEFAULT_PATH
    }

    fun setDownloadPath(path: String) {
        prefs.edit().putString(KEY_DOWNLOAD_PATH, path).apply()
    }
}
