package com.renx86.gdlapp.viewmodel

import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.renx86.gdlapp.data.update.UpdateInfo
import com.renx86.gdlapp.data.update.UpdatePreferences
import com.renx86.gdlapp.data.update.UpdateRepository
import com.renx86.gdlapp.data.update.VersionComparator
import com.renx86.gdlapp.util.UpdateNotifier
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val info: UpdateInfo) : UpdateState
    data class Downloading(val info: UpdateInfo, val progress: Float, val read: Long, val total: Long) : UpdateState
    data class ReadyToInstall(val info: UpdateInfo, val apk: File) : UpdateState
    data class Error(val message: String, val info: UpdateInfo?) : UpdateState
}

@HiltViewModel
class UpdateViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: UpdateRepository,
    private val preferences: UpdatePreferences
) : ViewModel() {

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    private val _showSheet = MutableStateFlow(false)
    val showSheet: StateFlow<Boolean> = _showSheet.asStateFlow()

    private var downloadJob: Job? = null
    
    private val currentVersionName: String by lazy {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0.0.0"
        } catch (e: PackageManager.NameNotFoundException) {
            "0.0.0"
        }
    }

    init {
        // Run cleanup of old APKs asynchronously
        viewModelScope.launch {
            repository.cleanupOldApks(currentVersionName)
        }
    }

    private var launchCheckDone = false

    fun checkOnLaunch() {
        if (!preferences.isAutoCheckEnabled()) return
        // Once per ViewModel (i.e. per app launch) — survives rotation/recomposition
        if (launchCheckDone) return
        launchCheckDone = true

        viewModelScope.launch {
            preferences.setLastCheckTimestamp(System.currentTimeMillis())
            performCheck(silent = true)
        }
    }

    fun checkNow() {
        viewModelScope.launch {
            _showSheet.value = true
            performCheck(silent = false)
        }
    }

    private suspend fun performCheck(silent: Boolean) {
        if (_state.value is UpdateState.Downloading || _state.value is UpdateState.ReadyToInstall) {
            return
        }
        
        if (!silent) {
            _state.value = UpdateState.Checking
        }

        val latest = repository.fetchLatest()
        if (latest == null) {
            if (!silent) _state.value = UpdateState.Error("Failed to check for updates. GitHub might be unreachable.", null)
            return
        }

        if (VersionComparator.isNewer(latest.version, currentVersionName)) {
            val skipped = preferences.getSkippedVersion()
            if (skipped != latest.version) {
                _state.value = UpdateState.Available(latest)
                if (silent) {
                    UpdateNotifier.showUpdateAvailable(context, latest)
                }
            } else if (!silent) {
                // They manually checked, so ignore the skip
                _state.value = UpdateState.Available(latest)
            }
        } else {
            if (!silent) _state.value = UpdateState.UpToDate
        }
    }

    fun startDownload(info: UpdateInfo) {
        downloadJob?.cancel()
        downloadJob = viewModelScope.launch {
            _state.value = UpdateState.Downloading(info, 0f, 0L, info.apkSizeBytes)
            
            repository.downloadApk(info)
                .catch { e ->
                    android.util.Log.e("GDL_UPDATE", "APK download failed", e)
                    _state.value = UpdateState.Error("Download failed: ${e.message ?: e.javaClass.simpleName}", info)
                }
                .collect { progress ->
                    if (progress.readBytes >= progress.totalBytes) {
                        _state.value = UpdateState.ReadyToInstall(info, repository.getApkFile(info.version))
                    } else {
                        val pct = if (progress.totalBytes > 0) progress.readBytes.toFloat() / progress.totalBytes.toFloat() else 0f
                        _state.value = UpdateState.Downloading(info, pct, progress.readBytes, progress.totalBytes)
                    }
                }
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        val currentState = _state.value
        if (currentState is UpdateState.Downloading) {
            _state.value = UpdateState.Available(currentState.info)
        }
    }

    fun skipVersion(version: String) {
        preferences.setSkippedVersion(version)
        closeSheet()
    }

    fun openSheet() {
        _showSheet.value = true
    }

    fun closeSheet() {
        _showSheet.value = false
        // If we were up to date or errored, reset to idle when closing
        val s = _state.value
        if (s is UpdateState.UpToDate || s is UpdateState.Error || s is UpdateState.Checking) {
            _state.value = UpdateState.Idle
        }
    }
}
