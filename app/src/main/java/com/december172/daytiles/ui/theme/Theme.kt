package com.december172.daytiles.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF765A47),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF3DFCF),
    onPrimaryContainer = Color(0xFF2B170B),
    secondary = Color(0xFF6C5D45),
    secondaryContainer = Color(0xFFF4E1BD),
    surface = Color(0xFFFFF8F3),
    surfaceVariant = Color(0xFFF1E6DD),
    background = Color(0xFFFFF8F3),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFE3BFA5),
    primaryContainer = Color(0xFF5B402F),
    secondary = Color(0xFFD8C3A0),
    secondaryContainer = Color(0xFF51462F),
    surface = Color(0xFF1A120E),
    surfaceVariant = Color(0xFF342A24),
    background = Color(0xFF1A120E),
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

