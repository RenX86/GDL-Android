package com.renx86.gdlapp.data.engine

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EnginePreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("gdl_engine_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_CUSTOM_ENGINE_PATH = "custom_engine_path"
        private const val KEY_CUSTOM_ENGINE_VERSION = "custom_engine_version"
        private const val KEY_AUTO_CHECK_UPDATES = "auto_check_engine_updates"
        private const val KEY_LAST_CHECK_TIME = "last_engine_check_time"
    }

    fun getCustomEnginePath(): String? {
        return prefs.getString(KEY_CUSTOM_ENGINE_PATH, null)
    }

    fun setCustomEnginePath(path: String?) {
        prefs.edit().putString(KEY_CUSTOM_ENGINE_PATH, path).apply()
    }

    fun getCustomEngineVersion(): String? {
        return prefs.getString(KEY_CUSTOM_ENGINE_VERSION, null)
    }

    fun setCustomEngineVersion(version: String?) {
        prefs.edit().putString(KEY_CUSTOM_ENGINE_VERSION, version).apply()
    }

    fun isAutoCheckEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUTO_CHECK_UPDATES, true)
    }

    fun setAutoCheckEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_CHECK_UPDATES, enabled).apply()
    }

    fun getLastCheckTime(): Long {
        return prefs.getLong(KEY_LAST_CHECK_TIME, 0L)
    }

    fun setLastCheckTime(time: Long) {
        prefs.edit().putLong(KEY_LAST_CHECK_TIME, time).apply()
    }

    fun clearCustomEngine() {
        prefs.edit()
            .remove(KEY_CUSTOM_ENGINE_PATH)
            .remove(KEY_CUSTOM_ENGINE_VERSION)
            .apply()
    }
}
