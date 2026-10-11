package com.renx86.gdlapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import com.renx86.gdlapp.data.ThemeMode
import com.renx86.gdlapp.data.ThemePreferences
import com.renx86.gdlapp.data.ThemeStyle

@Composable
fun GDLAndroidTheme(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val prefs = ThemePreferences(context)
    val themeMode = prefs.getThemeMode()
    val themeStyle = prefs.getThemeStyle()

    val isDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val neoColors = when (themeStyle) {
        ThemeStyle.CLASSIC -> if (isDark) ClassicDarkColors else ClassicLightColors
        ThemeStyle.MONOCHROME -> if (isDark) MonochromeDarkColors else MonochromeLightColors
    }

    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = neoColors.primary,
            secondary = neoColors.secondary,
            background = neoColors.background,
            surface = neoColors.surface,
            onPrimary = neoColors.text,
            onSecondary = neoColors.text,
            onBackground = neoColors.text,
            onSurface = neoColors.text
        )
    } else {
        lightColorScheme(
            primary = neoColors.primary,
            secondary = neoColors.secondary,
            background = neoColors.background,
            surface = neoColors.surface,
            onPrimary = neoColors.text,
            onSecondary = neoColors.text,
            onBackground = neoColors.text,
            onSurface = neoColors.text
        )
    }

    CompositionLocalProvider(LocalNeoColors provides neoColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}