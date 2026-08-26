package com.renx86.gdlapp.viewmodel

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.renx86.gdlapp.data.db.DownloadHistoryDao
import com.renx86.gdlapp.data.db.DownloadHistoryEntity
import com.renx86.gdlapp.python.GalleryDlBridge
import com.renx86.gdlapp.service.DownloadService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class DownloadViewModel @Inject constructor(
    private val bridge: GalleryDlBridge,
    private val dao: DownloadHistoryDao,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    val downloads: StateFlow<List<DownloadHistoryEntity>> = dao.getAllHistory()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        viewModelScope.launch {
            downloads.collect { list ->
                val activeCount = list.count { it.status == "DOWNLOADING" }
                val queuedCount = list.count { it.status == "QUEUED" }
                
                if (activeCount > 0 || queuedCount > 0) {
                    val text = when {
                        activeCount > 0 -> "Downloading $activeCount item(s), $queuedCount queued"
                        else -> "$queuedCount item(s) queued"
                    }
                    try {
                        val intent = DownloadService.startIntent(appContext, text)
                        appContext.startForegroundService(intent)
                    } catch (e: Exception) {
                        Log.e("GDL_SERVICE", "Failed to update service", e)
                    }
                } else {
                    appContext.stopService(Intent(appContext, DownloadService::class.java))
                }
            }
        }
    }

    fun enqueue(url: String) {
        val item = DownloadHistoryEntity(
            id = UUID.randomUUID().toString(),
            url = url.trim(),
            timestamp = System.currentTimeMillis(),
            status = "QUEUED"
        )
        
        viewModelScope.launch(Dispatchers.IO) {
            dao.insert(item)
            processDownload(item)
        }
    }

    private fun processDownload(item: DownloadHistoryEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.updateStatus(item.id, "DOWNLOADING", null)

            try {
                val result = bridge.download(item.url, item.id)
                val status = result.optString("status", "error")
                if (status == "ok") {
                    dao.updateStatus(item.id, "COMPLETED", null)
                } else {
                    val msg = result.optString("message", "Unknown error")
                    dao.updateStatus(item.id, "FAILED", msg)
                }
            } catch (e: Exception) {
                dao.updateStatus(item.id, "FAILED", e.message ?: "Unknown error")
            }
        }
    }

    fun retry(itemId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val item = downloads.value.find { it.id == itemId } ?: return@launch
            val updated = item.copy(timestamp = System.currentTimeMillis(), status = "QUEUED", errorMessage = null)
            dao.update(updated)
            processDownload(updated)
        }
    }

    fun removeItem(itemId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteById(itemId)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            dao.clearAll()
        }
    }
}