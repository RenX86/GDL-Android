package com.renx86.gdlapp.viewmodel

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.renx86.gdlapp.data.CompressionPreferences
import com.renx86.gdlapp.data.DownloadPreferences
import com.renx86.gdlapp.util.WebPConverter
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

sealed interface CompressionProgressState {
    object Idle : CompressionProgressState
    data class Running(val progress: Float, val statusText: String) : CompressionProgressState
    data class Finished(val message: String) : CompressionProgressState
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadPrefs: DownloadPreferences,
    private val compressionPrefs: CompressionPreferences
) : ViewModel() {

    private val _compressionState = MutableStateFlow<CompressionProgressState>(CompressionProgressState.Idle)
    val compressionState: StateFlow<CompressionProgressState> = _compressionState.asStateFlow()

    fun resetCompressionState() {
        _compressionState.value = CompressionProgressState.Idle
    }

    fun compressExistingLibrary() {
        if (_compressionState.value is CompressionProgressState.Running) return

        viewModelScope.launch(Dispatchers.IO) {
            _compressionState.value = CompressionProgressState.Running(0f, "Scanning library...")
            val downloadPath = downloadPrefs.getDownloadPath()
            val quality = compressionPrefs.getWebpQuality()
            val keepOriginal = compressionPrefs.isKeepOriginalEnabled()

            try {
                if (downloadPath.startsWith("content://")) {
                    val treeUri = Uri.parse(downloadPath)
                    val pickedDir = DocumentFile.fromTreeUri(context, treeUri)
                    if (pickedDir == null || !pickedDir.exists()) {
                        _compressionState.value = CompressionProgressState.Finished("Selected folder is not accessible.")
                        return@launch
                    }

                    val allDocFiles = mutableListOf<DocumentFile>()
                    fun walk(dir: DocumentFile) {
                        for (child in dir.listFiles()) {
                            if (child.isDirectory) {
                                walk(child)
                            } else {
                                val ext = child.name?.substringAfterLast('.', "")?.lowercase()
                                if (ext in listOf("jpg", "jpeg", "png")) {
                                    allDocFiles.add(child)
                                }
                            }
                        }
                    }
                    walk(pickedDir)

                    if (allDocFiles.isEmpty()) {
                        _compressionState.value = CompressionProgressState.Finished("No JPG/PNG files found.")
                        delay(2500)
                        _compressionState.value = CompressionProgressState.Idle
                        return@launch
                    }

                    var converted = 0
                    for (docFile in allDocFiles) {
                        _compressionState.value = CompressionProgressState.Running(
                            converted.toFloat() / allDocFiles.size,
                            "Converting ${docFile.name.orEmpty()}..."
                        )
                        try {
                            val tempFile = File(context.cacheDir, docFile.name ?: "temp.jpg")
                            context.contentResolver.openInputStream(docFile.uri)?.use { inStream ->
                                tempFile.outputStream().use { outStream -> inStream.copyTo(outStream) }
                            }
                            val webpFile = WebPConverter.convertFileToWebp(tempFile, quality, keepOriginal)
                            if (webpFile.exists() && webpFile.name.endsWith(".webp")) {
                                val parent = docFile.parentFile
                                if (!keepOriginal) {
                                    docFile.delete()
                                }
                                val newDoc = parent?.createFile("image/webp", webpFile.name)
                                if (newDoc != null) {
                                    context.contentResolver.openOutputStream(newDoc.uri)?.use { out ->
                                        webpFile.inputStream().use { input -> input.copyTo(out) }
                                    }
                                }
                            }
                            if (webpFile.exists()) webpFile.delete()
                            if (tempFile.exists()) tempFile.delete()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        converted++
                    }

                    _compressionState.value = CompressionProgressState.Finished("Done! Converted $converted files.")
                    delay(3000)
                    _compressionState.value = CompressionProgressState.Idle
                } else {
                    val dir = File(downloadPath)
                    if (!dir.exists()) {
                        _compressionState.value = CompressionProgressState.Finished("Directory does not exist.")
                        return@launch
                    }

                    val javaFiles = dir.walkTopDown().filter {
                        it.isFile && it.extension.lowercase() in listOf("jpg", "jpeg", "png")
                    }.toList()

                    if (javaFiles.isEmpty()) {
                        _compressionState.value = CompressionProgressState.Finished("No JPG/PNG files found.")
                        delay(2500)
                        _compressionState.value = CompressionProgressState.Idle
                        return@launch
                    }

                    var converted = 0
                    for (file in javaFiles) {
                        _compressionState.value = CompressionProgressState.Running(
                            converted.toFloat() / javaFiles.size,
                            "Converting ${file.name}..."
                        )
                        WebPConverter.convertFileToWebp(file, quality, keepOriginal)
                        converted++
                    }

                    _compressionState.value = CompressionProgressState.Finished("Done! Converted $converted files.")
                    delay(3000)
                    _compressionState.value = CompressionProgressState.Idle
                }
            } catch (e: Exception) {
                _compressionState.value = CompressionProgressState.Finished("Error: ${e.message}")
                delay(3000)
                _compressionState.value = CompressionProgressState.Idle
            }
        }
    }
}
