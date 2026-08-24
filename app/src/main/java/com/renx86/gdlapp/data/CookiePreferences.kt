package com.renx86.gdlapp.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stores cookie profile metadata: which domain the cookies are for,
 * the WebView User-Agent (must match during downloads!), and whether
 * cookies are enabled.
 */
@Singleton
class CookiePreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("gdl_cookie_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_COOKIE_DOMAIN = "cookie_domain"
        private const val KEY_COOKIE_USER_AGENT = "cookie_user_agent"
        private const val KEY_COOKIES_ENABLED = "cookies_enabled"
        const val COOKIE_FILENAME = "cookies.txt"
    }

    fun getCookieDomain(): String? = prefs.getString(KEY_COOKIE_DOMAIN, null)

    fun setCookieDomain(domain: String) {
        prefs.edit().putString(KEY_COOKIE_DOMAIN, domain).apply()
    }

    fun getUserAgent(): String? = prefs.getString(KEY_COOKIE_USER_AGENT, null)

    fun setUserAgent(userAgent: String) {
        prefs.edit().putString(KEY_COOKIE_USER_AGENT, userAgent).apply()
    }

    fun areCookiesEnabled(): Boolean = prefs.getBoolean(KEY_COOKIES_ENABLED, false)

    fun setCookiesEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_COOKIES_ENABLED, enabled).apply()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    /** Returns the path where cookies.txt should be stored */
    fun getCookieFilePath(): String {
        return java.io.File(context.filesDir, COOKIE_FILENAME).absolutePath
    }
}
