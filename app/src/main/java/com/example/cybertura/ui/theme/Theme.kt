package com.example.cybertura.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CyberturaDarkRpgScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = DungeonBlack,
    primaryContainer = Color(0xFF003D36),
    onPrimaryContainer = Color(0xFF70F7D7),
    secondary = NeonPurple,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF3B1885),
    onSecondaryContainer = Color(0xFFE9DDFF),
    tertiary = NeonGold,
    onTertiary = DungeonBlack,
    tertiaryContainer = Color(0xFF4A3800),
    onTertiaryContainer = Color(0xFFFFE082),
    background = DungeonDark,
    onBackground = TextPrimary,
    surface = DungeonCard,
    onSurface = TextPrimary,
    surfaceVariant = DungeonCardHover,
    onSurfaceVariant = TextSecondary,
    outline = DungeonBorder,
    outlineVariant = Color(0xFF1F2A4A)
)

@Composable
fun CyberturaTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CyberturaDarkRpgScheme,
        typography = Typography,
        content = content
    )
}