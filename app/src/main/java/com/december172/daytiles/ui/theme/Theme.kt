package com.december172.daytiles.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = Color(0xFFFF8800), onPrimary = Color.White,
    primaryContainer = Color(0xFFFFF2E2), onPrimaryContainer = Color(0xFF653A00),
    secondary = Color(0xFF567DA9), onSecondary = Color.White,
    secondaryContainer = Color(0xFFEAF0F8), onSecondaryContainer = Color(0xFF304D72),
    tertiary = Color(0xFF8B83D8), tertiaryContainer = Color(0xFFF0EDFA),
    background = Color.White, onBackground = Color(0xFF333333),
    surface = Color.White, onSurface = Color(0xFF333333),
    surfaceVariant = Color(0xFFF5F5F6), onSurfaceVariant = Color(0xFF929292),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFFAFAFA),
    surfaceContainer = Color(0xFFF7F7F8), surfaceContainerHigh = Color(0xFFF0F0F2),
    surfaceContainerHighest = Color(0xFFEBEBED),
    outline = Color(0xFFBDBDC6), outlineVariant = Color(0xFFEEEEF0),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFA33E), onPrimary = Color(0xFF311B00),
    primaryContainer = Color(0xFF3A2B1C), onPrimaryContainer = Color(0xFFFFDDB7),
    secondary = Color(0xFF9FBCE0), secondaryContainer = Color(0xFF293A50),
    tertiary = Color(0xFFBCB3F0), tertiaryContainer = Color(0xFF353047),
    background = Color(0xFF171719), surface = Color(0xFF171719),
    surfaceVariant = Color(0xFF242427), onSurfaceVariant = Color(0xFFA6A6AC),
    surfaceContainer = Color(0xFF202023), surfaceContainerLow = Color(0xFF1D1D20),
    outlineVariant = Color(0xFF303034),
)

@Composable
fun DayTilesTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        shapes = Shapes(
            extraSmall = RoundedCornerShape(4.dp), small = RoundedCornerShape(8.dp),
            medium = RoundedCornerShape(12.dp), large = RoundedCornerShape(16.dp),
            extraLarge = RoundedCornerShape(20.dp),
        ),
        content = content,
    )
}
