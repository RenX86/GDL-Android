package com.renx86.gdlapp.viewmodel

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.renx86.gdlapp.model.DownloadItem
import com.renx86.gdlapp.model.DownloadStatus
import com.renx86.gdlapp.python.GalleryDlBridge
import com.renx86.gdlapp.service.DownloadService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DownloadViewModel @Inject constructor(
    private val bridge: GalleryDlBridge,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _downloads = MutableStateFlow<List<DownloadItem>>(emptyList())
    val downloads: StateFlow<List<DownloadItem>> = _downloads.asStateFlow()

    fun enqueue(url: String) {
        val item = DownloadItem(url = url.trim())
        _downloads.update { it + item }

        // Start the foreground service IMMEDIATELY so Android keeps us alive
        startService()

        processDownload(item)
    }

    private fun processDownload(item: DownloadItem) {
        viewModelScope.launch {
            // Mark as downloading and update notification
            updateItem(item.id) { it.copy(status = DownloadStatus.DOWNLOADING) }
            updateServiceNotification()

            try {
                val result = bridge.download(item.url, item.id)
                val status = result.optString("status", "error")
                if (status == "ok") {
                    updateItem(item.id) { it.copy(status = DownloadStatus.DONE) }
                } else {
                    val msg = result.optString("message", "Unknown error")
                    updateItem(item.id) {
                        it.copy(status = DownloadStatus.FAILED, error = msg)
                    }
                }
            } catch (e: Exception) {
                updateItem(item.id) {
                    it.copy(status = DownloadStatus.FAILED, error = e.message ?: "Unknown error")
                }
            }

            // After each download completes, check if we should stop the service
            stopServiceIfIdle()
        }
    }

    fun retry(itemId: String) {
        val item = _downloads.value.find { it.id == itemId } ?: return
        updateItem(itemId) { it.copy(status = DownloadStatus.QUEUED, error = "") }
        startService()
        processDownload(item.copy(status = DownloadStatus.QUEUED))
    }

    fun removeItem(itemId: String) {
        _downloads.update { list -> list.filter { it.id != itemId } }
        stopServiceIfIdle()
    }

    private fun updateItem(id: String, transform: (DownloadItem) -> DownloadItem) {
        _downloads.update { list ->
            list.map { if (it.id == id) transform(it) else it }
        }
    }

    // ---- Service control ----

    private fun getStatusText(): String {
        val list = _downloads.value
        val active = list.count { it.status == DownloadStatus.DOWNLOADING }
        val queued = list.count { it.status == DownloadStatus.QUEUED }
        return when {
            active > 0 -> "Downloading $active item(s), $queued queued"
            queued > 0 -> "$queued item(s) queued"
            else -> "Finishing up..."
        }
    }

    private fun startService() {
        try {
            val text = getStatusText()
            Log.d("GDL_SERVICE", "Starting foreground service: $text")
            val intent = DownloadService.startIntent(appContext, text)
            appContext.startForegroundService(intent)
            Log.d("GDL_SERVICE", "startForegroundService called successfully")
        } catch (e: Exception) {
            Log.e("GDL_SERVICE", "Failed to start service", e)
        }
    }

    private fun updateServiceNotification() {
        try {
            val text = getStatusText()
            Log.d("GDL_SERVICE", "Updating notification: $text")
            val intent = DownloadService.startIntent(appContext, text)
            appContext.startForegroundService(intent)
        } catch (e: Exception) {
            Log.e("GDL_SERVICE", "Failed to update service", e)
        }
    }

    private fun stopServiceIfIdle() {
        val hasActive = _downloads.value.any {
            it.status == DownloadStatus.QUEUED || it.status == DownloadStatus.DOWNLOADING
        }
        if (!hasActive) {
            Log.d("GDL_SERVICE", "No active downloads, stopping service")
            appContext.stopService(Intent(appContext, DownloadService::class.java))
        }
    }
}