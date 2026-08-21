package com.renx86.gdlapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.renx86.gdlapp.model.DownloadItem
import com.renx86.gdlapp.model.DownloadStatus
import com.renx86.gdlapp.python.GalleryDlBridge
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DownloadViewModel @Inject constructor(
    private val bridge: GalleryDlBridge
) : ViewModel() {

    private val _downloads = MutableStateFlow<List<DownloadItem>>(emptyList())
    val downloads: StateFlow<List<DownloadItem>> = _downloads.asStateFlow()

    fun enqueue(url: String) {
        val item = DownloadItem(url = url.trim())
        _downloads.update {it + item }
        processDownload(item)
    }

    private fun processDownload(item: DownloadItem) {
        viewModelScope.launch {
            // Mark AS Downloading
            updateItem(item.id) { it.copy(status = DownloadStatus.DOWNLOADING)}

            try {
                val result = bridge.download(item.url)
                val status = result.optString("status", "error")
                if (status == "ok") {
                    updateItem(item.id) {it.copy(status = DownloadStatus.DONE) }
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
        }
    }

    fun retry(itemId: String) {
        val item = _downloads.value.find { it.id == itemId } ?: return
        updateItem(itemId) { it.copy(status = DownloadStatus.QUEUED, error = "")}
    }

    fun removeItem(itemId: String) {
        _downloads.update { list -> list.filter { it.id != itemId} }
    }

    private fun updateItem(id: String, transform: (DownloadItem) -> DownloadItem) {
        _downloads.update { list ->
            list.map { if (it.id == id) transform(it) else it }
        }
    }
}