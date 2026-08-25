package com.renx86.gdlapp.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class NeoColors(
    val background: Color,
    val surface: Color,
    val border: Color,
    val text: Color,
    val textSecondary: Color,
    val yellow: Color,
    val pink: Color,
    val blue: Color,
    val green: Color,
    val orange: Color
)

val ClassicLightColors = NeoColors(
    background = Color(0xFFFFFDF5),
    surface = Color(0xFFFFFFFF),
    border = Color.Black,
    text = Color.Black,
    textSecondary = Color.DarkGray,
    yellow = Color(0xFFFFD800),
    pink = Color(0xFFFF8AE2),
    blue = Color(0xFF8AE2FF),
    green = Color(0xFF8AFF8A),
    orange = Color(0xFFFF9040)
)

val ClassicDarkColors = NeoColors(
    background = Color(0xFF1E1E2E), // Deep dark purple/gray
    surface = Color(0xFF2A2A3C),
    border = Color(0xFF000000), // Pure black borders contrast well with vibrant colors in dark mode
    text = Color(0xFFE0E0E0),
    textSecondary = Color(0xFFA0A0A0),
    yellow = Color(0xFFFFD800),
    pink = Color(0xFFFF8AE2),
    blue = Color(0xFF8AE2FF),
    green = Color(0xFF8AFF8A),
    orange = Color(0xFFFF9040)
)

val MonochromeLightColors = NeoColors(
    background = Color(0xFFE5E5E5),
    surface = Color(0xFFFFFFFF),
    border = Color.Black,
    text = Color.Black,
    textSecondary = Color.DarkGray,
    yellow = Color(0xFFFFFFFF), // White
    pink = Color(0xFFFFFFFF),
    blue = Color(0xFFFFFFFF),
    green = Color(0xFFFFFFFF),
    orange = Color(0xFFFFFFFF)
)

val MonochromeDarkColors = NeoColors(
    background = Color(0xFF111111),
    surface = Color(0xFF222222),
    border = Color(0xFF000000),
    text = Color(0xFFE0E0E0),
    textSecondary = Color(0xFFA0A0A0),
    yellow = Color(0xFF222222),
    pink = Color(0xFF222222),
    blue = Color(0xFF222222),
    green = Color(0xFF222222),
    orange = Color(0xFF222222)
)

val LocalNeoColors = staticCompositionLocalOf { ClassicLightColors }

object NeoTheme {
    val colors: NeoColors
        @Composable
        get() = LocalNeoColors.current
}
