package com.renx86.gdlapp.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class NeoColors(
    val background: Color,
    val surface: Color,
    val border: Color,
    val shadow: Color,
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
    shadow = Color.Black,
    text = Color.Black,
    textSecondary = Color.DarkGray,
    yellow = Color(0xFFFFD800),
    pink = Color(0xFFFF8AE2),
    blue = Color(0xFF8AE2FF),
    green = Color(0xFF8AFF8A),
    orange = Color(0xFFFF9040)
)

val ClassicDarkColors = NeoColors(
    background = Color(0xFF1A1A2E),
    surface = Color(0xFF25253A),
    border = Color(0xFF7A7A90),      // medium slate — defines edges without glowing
    shadow = Color(0xFF0D0D1A),      // very dark, subtle depth
    text = Color(0xFFD8D8D8),        // soft white, readable
    textSecondary = Color(0xFF8E8E9E),
    yellow = Color(0xFFF0CC00),      // bold gold — just a touch less neon
    pink = Color(0xFFF080D0),        // vibrant pink, slightly warmed
    blue = Color(0xFF80D0F0),        // bright sky blue
    green = Color(0xFF80F080),       // vivid green
    orange = Color(0xFFF08535)       // punchy orange
)

val MonochromeLightColors = NeoColors(
    background = Color(0xFFE5E5E5),
    surface = Color(0xFFFFFFFF),
    border = Color.Black,
    shadow = Color.Black,
    text = Color.Black,
    textSecondary = Color.DarkGray,
    yellow = Color(0xFFFFFFFF),
    pink = Color(0xFFFFFFFF),
    blue = Color(0xFFFFFFFF),
    green = Color(0xFFFFFFFF),
    orange = Color(0xFFFFFFFF)
)

val MonochromeDarkColors = NeoColors(
    background = Color(0xFF111111),
    surface = Color(0xFF1E1E1E),
    border = Color(0xFF5A5A5A),      // muted gray border
    shadow = Color(0xFF000000),
    text = Color(0xFFCDCDCD),
    textSecondary = Color(0xFF888888),
    yellow = Color(0xFF333333),
    pink = Color(0xFF333333),
    blue = Color(0xFF333333),
    green = Color(0xFF333333),
    orange = Color(0xFF333333)
)

val LocalNeoColors = staticCompositionLocalOf { ClassicLightColors }

object NeoTheme {
    val colors: NeoColors
        @Composable
        get() = LocalNeoColors.current
}
