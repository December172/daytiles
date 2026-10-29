package com.december172.daytiles.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Keep the orange identity while using paired Material color roles at every level.
private val LightColors = lightColorScheme(
    primary = Color(0xFF8B5000), onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDCBE), onPrimaryContainer = Color(0xFF2D1600),
    inversePrimary = Color(0xFFFFB870),
    secondary = Color(0xFF725A42), onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDDBF), onSecondaryContainer = Color(0xFF291806),
    tertiary = Color(0xFF66558E), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFEBDDFF), onTertiaryContainer = Color(0xFF211047),
    background = Color(0xFFFFF8F4), onBackground = Color(0xFF211A15),
    surface = Color(0xFFFFF8F4), onSurface = Color(0xFF211A15),
    surfaceVariant = Color(0xFFF2DFD0), onSurfaceVariant = Color(0xFF51443A),
    surfaceDim = Color(0xFFE5D8CE), surfaceBright = Color(0xFFFFF8F4),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFFFF1E7),
    surfaceContainer = Color(0xFFF9EBE1), surfaceContainerHigh = Color(0xFFF3E5DB),
    surfaceContainerHighest = Color(0xFFEDE0D5),
    inverseSurface = Color(0xFF372F29), inverseOnSurface = Color(0xFFFFEFE4),
    outline = Color(0xFF837469), outlineVariant = Color(0xFFD6C3B5),
    error = Color(0xFFBA1A1A), onError = Color.White,
    errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB870), onPrimary = Color(0xFF4A2800),
    primaryContainer = Color(0xFF6A3C00), onPrimaryContainer = Color(0xFFFFDCBE),
    inversePrimary = Color(0xFF8B5000),
    secondary = Color(0xFFE2C1A4), onSecondary = Color(0xFF402C18),
    secondaryContainer = Color(0xFF59422D), onSecondaryContainer = Color(0xFFFFDDBF),
    tertiary = Color(0xFFD0BDF7), onTertiary = Color(0xFF37265C),
    tertiaryContainer = Color(0xFF4E3D74), onTertiaryContainer = Color(0xFFEBDDFF),
    background = Color(0xFF19120D), onBackground = Color(0xFFEDE0D5),
    surface = Color(0xFF19120D), onSurface = Color(0xFFEDE0D5),
    surfaceVariant = Color(0xFF51443A), onSurfaceVariant = Color(0xFFD6C3B5),
    surfaceDim = Color(0xFF19120D), surfaceBright = Color(0xFF413731),
    surfaceContainerLowest = Color(0xFF130D08), surfaceContainerLow = Color(0xFF211A15),
    surfaceContainer = Color(0xFF261E19), surfaceContainerHigh = Color(0xFF312923),
    surfaceContainerHighest = Color(0xFF3C342E),
    inverseSurface = Color(0xFFEDE0D5), inverseOnSurface = Color(0xFF372F29),
    outline = Color(0xFF9E8E81), outlineVariant = Color(0xFF51443A),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
)

@Composable
fun DayTilesTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        shapes = Shapes(
            extraSmall = RoundedCornerShape(4.dp), small = RoundedCornerShape(8.dp),
            medium = RoundedCornerShape(12.dp), large = RoundedCornerShape(16.dp),
            extraLarge = RoundedCornerShape(28.dp),
        ),
        content = content,
    )
}
