package com.renx86.gdlapp.python

import android.content.Context
import android.os.Environment
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GalleryDlBridge @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val module by lazy {
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(context))
        }
        val py = Python.getInstance()
        val mod = py.getModule("gallery_dl_wrapper")

        val filesDir = context.filesDir.absolutePath
        val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!.absolutePath

        mod.callAttr("initialize", filesDir, downloadDir)
        mod
    }

    suspend fun getInfo(url: String): JSONObject = withContext(Dispatchers.IO) {
        val result = module.callAttr("get_info", url).toString()
        JSONObject(result)
    }

    suspend fun download(url: String): JSONObject = withContext(Dispatchers.IO) {
        val result = module.callAttr("download", url).toString()
        JSONObject(result)
    }
}