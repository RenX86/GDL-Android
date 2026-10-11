package com.renx86.gdlapp.viewmodel

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.renx86.gdlapp.data.db.DownloadHistoryDao
import com.renx86.gdlapp.data.db.DownloadHistoryEntity
import com.renx86.gdlapp.data.db.DownloadStatus
import com.renx86.gdlapp.python.GalleryDlBridge
import com.renx86.gdlapp.service.DownloadService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

@HiltViewModel
class DownloadViewModel @Inject constructor(
    private val bridge: GalleryDlBridge,
    private val dao: DownloadHistoryDao,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val activeJobs = ConcurrentHashMap<String, Job>()

    val downloads: StateFlow<List<DownloadHistoryEntity>> = dao.getAllHistory()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        viewModelScope.launch {
            downloads.collect { list ->
                val activeCount = list.count { it.status == DownloadStatus.DOWNLOADING }
                val queuedCount = list.count { it.status == DownloadStatus.QUEUED }
                
                if (activeCount > 0 || queuedCount > 0) {
                    val text = when {
                        activeCount > 0 -> "Downloading $activeCount item(s), $queuedCount queued"
                        else -> "$queuedCount item(s) queued"
                    }
                    try {
                        val intent = DownloadService.startIntent(appContext, text)
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            appContext.startForegroundService(intent)
                        } else {
                            appContext.startService(intent)
                        }
                    } catch (e: Exception) {
                        Log.e("GDL_SERVICE", "Failed to update service", e)
                    }
                } else {
                    try {
                        appContext.stopService(Intent(appContext, DownloadService::class.java))
                    } catch (e: Exception) {
                        Log.e("GDL_SERVICE", "Failed to stop service", e)
                    }
                }
            }
        }
    }

    fun enqueue(url: String) {
        val item = DownloadHistoryEntity(
            id = UUID.randomUUID().toString(),
            url = url.trim(),
            timestamp = System.currentTimeMillis(),
            status = DownloadStatus.QUEUED
        )
        
        val job = viewModelScope.launch(Dispatchers.IO) {
            dao.insert(item)
            processDownload(item)
        }
        job.invokeOnCompletion { activeJobs.remove(item.id) }
        activeJobs[item.id] = job
    }

    private suspend fun processDownload(item: DownloadHistoryEntity) {
        dao.updateStatus(item.id, DownloadStatus.DOWNLOADING, null)

        try {
            val result = bridge.download(item.url, item.id)
            val status = result.optString("status", "error")
            if (status == "ok") {
                dao.updateStatus(item.id, DownloadStatus.COMPLETED, null)
            } else {
                val msg = result.optString("message", "Unknown error")
                dao.updateStatus(item.id, DownloadStatus.FAILED, msg)
            }
        } catch (e: Exception) {
            if (e is CancellationException) {
                withContext(NonCancellable) {
                    dao.updateStatus(item.id, DownloadStatus.CANCELLED, "Cancelled by user")
                }
                throw e
            }
            dao.updateStatus(item.id, DownloadStatus.FAILED, e.message ?: "Unknown error")
        } finally {
            activeJobs.remove(item.id)
        }
    }

    fun cancelDownload(itemId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            activeJobs.remove(itemId)?.cancel()
            withContext(NonCancellable) {
                dao.updateStatus(itemId, DownloadStatus.CANCELLED, "Cancelled by user")
            }
        }
    }

    fun retry(itemId: String) {
        val job = viewModelScope.launch(Dispatchers.IO) {
            val item = downloads.value.find { it.id == itemId } ?: return@launch
            val updated = item.copy(timestamp = System.currentTimeMillis(), status = DownloadStatus.QUEUED, errorMessage = null)
            dao.update(updated)
            processDownload(updated)
        }
        job.invokeOnCompletion { activeJobs.remove(itemId) }
        activeJobs[itemId] = job
    }

    fun removeItem(itemId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            activeJobs.remove(itemId)?.cancel()
            withContext(NonCancellable) {
                dao.deleteById(itemId)
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            dao.clearCompleted()
        }
    }
}