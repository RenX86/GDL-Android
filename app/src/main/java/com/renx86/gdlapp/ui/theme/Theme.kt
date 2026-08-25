package com.renx86.gdlapp.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.renx86.gdlapp.data.ThemePreferences
import com.renx86.gdlapp.data.ThemeMode
import com.renx86.gdlapp.data.ThemeStyle
import androidx.compose.runtime.CompositionLocalProvider

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

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

    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalNeoColors provides neoColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}