package com.renx86.gdlapp.data.update

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdatePreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("update_prefs", Context.MODE_PRIVATE)

    fun getLastCheckTimestamp(): Long = prefs.getLong("last_check_time", 0L)
    
    fun setLastCheckTimestamp(timeMillis: Long) {
        prefs.edit().putLong("last_check_time", timeMillis).apply()
    }

    fun getSkippedVersion(): String? = prefs.getString("skipped_version", null)
    
    fun setSkippedVersion(version: String?) {
        prefs.edit().putString("skipped_version", version).apply()
    }

    fun isAutoCheckEnabled(): Boolean = prefs.getBoolean("auto_check_enabled", true)
    
    fun setAutoCheckEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("auto_check_enabled", enabled).apply()
    }
}
