package com.nana.ui.main.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFA3D9C2),
    primaryContainer = Color(0xFF245A49),
    secondary = Color(0xFFD9BD84),
    background = Color(0xFF101B1C),
    surface = Color(0xFF172526),
    error = Color(0xFFFFB4AB)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF286451),
    primaryContainer = Color(0xFFB9E7D6),
    secondary = Color(0xFF9A6A2F),
    background = Color(0xFFF4F6F4),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF172633),
    error = Color(0xFFB3261E)
)

@Composable
fun NaNaTheme(
    darkTheme: Boolean = false, // Will be overridden by system
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
