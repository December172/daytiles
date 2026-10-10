package com.december172.daytiles.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFFFF7A1A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE6D2),
    onPrimaryContainer = Color(0xFF321300),
    secondary = Color(0xFF5C6F91),
    secondaryContainer = Color(0xFFDCE5F7),
    surface = Color(0xFFFFFBF8),
    surfaceVariant = Color(0xFFF5EEE9),
    background = Color(0xFFFFFBF8),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB77E),
    primaryContainer = Color(0xFF713500),
    secondary = Color(0xFFB7C7E8),
    secondaryContainer = Color(0xFF344563),
    surface = Color(0xFF19120E),
    surfaceVariant = Color(0xFF382C25),
    background = Color(0xFF19120E),
)

@Composable
fun DayTilesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
