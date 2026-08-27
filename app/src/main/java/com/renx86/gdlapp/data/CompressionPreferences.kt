package com.renx86.gdlapp.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CompressionPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("gdl_compression_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_AUTO_CONVERT = "auto_convert_webp"
        private const val KEY_WEBP_QUALITY = "webp_quality"
        private const val KEY_KEEP_ORIGINAL = "keep_original_files"
    }

    fun isAutoConvertEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUTO_CONVERT, false)
    }

    fun setAutoConvertEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_CONVERT, enabled).apply()
    }

    fun isKeepOriginalEnabled(): Boolean {
        return prefs.getBoolean(KEY_KEEP_ORIGINAL, false)
    }

    fun setKeepOriginalEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_KEEP_ORIGINAL, enabled).apply()
    }

    fun getWebpQuality(): Int {
        return prefs.getInt(KEY_WEBP_QUALITY, 85)
    }

    fun setWebpQuality(quality: Int) {
        prefs.edit().putInt(KEY_WEBP_QUALITY, quality).apply()
    }
}
