package com.renx86.gdlapp.data

import android.content.Context
import android.content.SharedPreferences

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

enum class ThemeStyle {
    CLASSIC, MONOCHROME
}

class ThemePreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)

    fun getThemeMode(): ThemeMode {
        val saved = prefs.getString("theme_mode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        return try { ThemeMode.valueOf(saved) } catch (e: Exception) { ThemeMode.SYSTEM }
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString("theme_mode", mode.name).apply()
    }

    fun getThemeStyle(): ThemeStyle {
        val saved = prefs.getString("theme_style", ThemeStyle.CLASSIC.name) ?: ThemeStyle.CLASSIC.name
        return try { ThemeStyle.valueOf(saved) } catch (e: Exception) { ThemeStyle.CLASSIC }
    }

    fun setThemeStyle(style: ThemeStyle) {
        prefs.edit().putString("theme_style", style.name).apply()
    }
}
