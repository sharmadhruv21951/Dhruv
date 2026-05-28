package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = SpotifyGreen,
    onPrimary = DeepBlack,
    secondary = SpotifyLightGreen,
    onSecondary = DeepBlack,
    tertiary = DarkGreenGlow,
    background = BlackBg,
    onBackground = TextWhite,
    surface = GraySurface,
    onSurface = TextWhite,
    surfaceVariant = LightGraySurface,
    onSurfaceVariant = TextMuted,
    outline = TextMuted
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force Dark mode for signature Spotify look
    dynamicColor: Boolean = false, // Disable dynamic colors to keep Spotify aesthetic branded
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
