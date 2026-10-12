package com.renx86.gdlapp.viewmodel

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.renx86.gdlapp.data.CompressionPreferences
import com.renx86.gdlapp.data.DownloadPreferences
import com.renx86.gdlapp.data.engine.EnginePreferences
import com.renx86.gdlapp.data.engine.EngineUpdateInfo
import com.renx86.gdlapp.data.engine.GalleryDlUpdateManager
import com.renx86.gdlapp.data.update.VersionComparator
import com.renx86.gdlapp.python.GalleryDlBridge
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

sealed interface EngineUpdateState {
    object Idle : EngineUpdateState
    object Checking : EngineUpdateState
    data class UpdateAvailable(val info: EngineUpdateInfo) : EngineUpdateState
    data class Downloading(val progress: Float) : EngineUpdateState
    object UpToDate : EngineUpdateState
    data class Success(val message: String) : EngineUpdateState
    data class Error(val message: String) : EngineUpdateState
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadPrefs: DownloadPreferences,
    private val compressionPrefs: CompressionPreferences,
    private val enginePrefs: EnginePreferences,
    private val engineUpdateManager: GalleryDlUpdateManager,
    private val bridge: GalleryDlBridge
) : ViewModel() {

    private val _compressionState = MutableStateFlow<CompressionProgressState>(CompressionProgressState.Idle)
    val compressionState: StateFlow<CompressionProgressState> = _compressionState.asStateFlow()

    private val _engineUpdateState = MutableStateFlow<EngineUpdateState>(EngineUpdateState.Idle)
    val engineUpdateState: StateFlow<EngineUpdateState> = _engineUpdateState.asStateFlow()

    private val _currentEngineVersion = MutableStateFlow("Loading...")
    val currentEngineVersion: StateFlow<String> = _currentEngineVersion.asStateFlow()

    private val _isCustomEngine = MutableStateFlow(false)
    val isCustomEngine: StateFlow<Boolean> = _isCustomEngine.asStateFlow()

    private val _autoCheckEngineUpdates = MutableStateFlow(enginePrefs.isAutoCheckEnabled())
    val autoCheckEngineUpdates: StateFlow<Boolean> = _autoCheckEngineUpdates.asStateFlow()

    init {
        refreshEngineInfo()
    }

    fun refreshEngineInfo() {
        viewModelScope.launch(Dispatchers.IO) {
            val version = bridge.getEngineVersion()
            _currentEngineVersion.value = version
            _isCustomEngine.value = enginePrefs.getCustomEnginePath() != null

            if (enginePrefs.isAutoCheckEnabled()) {
                val lastCheck = enginePrefs.getLastCheckTime()
                // Check if more than 24 hours have passed since last check
                if (System.currentTimeMillis() - lastCheck > 24 * 60 * 60 * 1000) {
                    checkEngineUpdate(silent = true)
                }
            }
        }
    }

    fun checkEngineUpdate(silent: Boolean = false) {
        if (_engineUpdateState.value is EngineUpdateState.Checking ||
            _engineUpdateState.value is EngineUpdateState.Downloading
        ) return

        if (!silent) {
            _engineUpdateState.value = EngineUpdateState.Checking
        }

        viewModelScope.launch(Dispatchers.IO) {
            val result = engineUpdateManager.checkLatest()
            if (result.isSuccess) {
                val info = result.getOrNull()
                if (info != null) {
                    val current = _currentEngineVersion.value
                    if (VersionComparator.isNewer(info.version, current)) {
                        _engineUpdateState.value = EngineUpdateState.UpdateAvailable(info)
                    } else {
                        if (!silent) {
                            _engineUpdateState.value = EngineUpdateState.UpToDate
                            delay(2500)
                            _engineUpdateState.value = EngineUpdateState.Idle
                        }
                    }
                } else if (!silent) {
                    _engineUpdateState.value = EngineUpdateState.Error("No wheel package found on PyPI")
                    delay(3000)
                    _engineUpdateState.value = EngineUpdateState.Idle
                }
            } else if (!silent) {
                val errorMsg = result.exceptionOrNull()?.message ?: "Check failed"
                _engineUpdateState.value = EngineUpdateState.Error(errorMsg)
                delay(3000)
                _engineUpdateState.value = EngineUpdateState.Idle
            }
        }
    }

    fun installEngineUpdate(info: EngineUpdateInfo) {
        if (_engineUpdateState.value is EngineUpdateState.Downloading) return
        _engineUpdateState.value = EngineUpdateState.Downloading(0f)

        viewModelScope.launch(Dispatchers.IO) {
            val result = engineUpdateManager.downloadAndInstall(info) { progress ->
                _engineUpdateState.value = EngineUpdateState.Downloading(progress)
            }

            if (result.isSuccess) {
                val newVer = result.getOrNull() ?: info.version
                _currentEngineVersion.value = newVer
                _isCustomEngine.value = true
                _engineUpdateState.value = EngineUpdateState.Success("gallery-dl updated to v$newVer!")
                delay(3000)
                _engineUpdateState.value = EngineUpdateState.Idle
            } else {
                val err = result.exceptionOrNull()?.message ?: "Installation failed"
                _engineUpdateState.value = EngineUpdateState.Error(err)
                delay(4000)
                _engineUpdateState.value = EngineUpdateState.Idle
            }
        }
    }

    fun resetEngineToBundled() {
        viewModelScope.launch(Dispatchers.IO) {
            val result = engineUpdateManager.resetToBundled()
            if (result.isSuccess) {
                val bundledVer = bridge.getEngineVersion()
                _currentEngineVersion.value = bundledVer
                _isCustomEngine.value = false
                _engineUpdateState.value = EngineUpdateState.Success("Reset to bundled v$bundledVer")
                delay(2500)
                _engineUpdateState.value = EngineUpdateState.Idle
            } else {
                val err = result.exceptionOrNull()?.message ?: "Reset failed"
                _engineUpdateState.value = EngineUpdateState.Error(err)
                delay(3000)
                _engineUpdateState.value = EngineUpdateState.Idle
            }
        }
    }

    fun setAutoCheckEngineUpdates(enabled: Boolean) {
        enginePrefs.setAutoCheckEnabled(enabled)
        _autoCheckEngineUpdates.value = enabled
    }

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
