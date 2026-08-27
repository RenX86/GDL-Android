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
        private const val KEY_LOGGED_SITES = "logged_sites"
        private const val KEY_COOKIE_USER_AGENT = "cookie_user_agent"
        const val COOKIE_FILENAME = "cookies.txt"
    }

    fun getLoggedSites(): Set<String> {
        return prefs.getStringSet(KEY_LOGGED_SITES, emptySet()) ?: emptySet()
    }

    fun addLoggedSite(domain: String) {
        val current = getLoggedSites().toMutableSet()
        current.add(domain)
        prefs.edit().putStringSet(KEY_LOGGED_SITES, current).apply()
    }
    
    fun removeLoggedSite(domain: String) {
        val current = getLoggedSites().toMutableSet()
        current.remove(domain)
        prefs.edit().putStringSet(KEY_LOGGED_SITES, current).apply()
    }

    fun getUserAgent(): String? = prefs.getString(KEY_COOKIE_USER_AGENT, null)

    fun setUserAgent(userAgent: String) {
        prefs.edit().putString(KEY_COOKIE_USER_AGENT, userAgent).apply()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    /** Returns the path where cookies.txt should be stored */
    fun getCookieFilePath(): String {
        return java.io.File(context.filesDir, COOKIE_FILENAME).absolutePath
    }
}
