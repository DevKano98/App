package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = AccentPrimaryLight,
    onPrimary = CleanWhite,
    primaryContainer = MutedCanvas,
    onPrimaryContainer = TextPrimaryLight,
    secondary = TextSecondaryLight,
    onSecondary = CleanWhite,
    secondaryContainer = MutedCanvas,
    onSecondaryContainer = TextPrimaryLight,
    tertiary = TextTertiaryLight,
    background = CleanWhite,
    onBackground = TextPrimaryLight,
    surface = SurfaceCard,
    onSurface = TextPrimaryLight,
    surfaceVariant = MutedCanvas,
    onSurfaceVariant = TextSecondaryLight,
    outline = BorderLight,
    outlineVariant = BorderSubtle,
    error = DangerRed,
    onError = CleanWhite,
    errorContainer = DangerRedBg,
    onErrorContainer = DangerRed
)

private val DarkColorScheme = darkColorScheme(
    primary = AccentPrimaryDark,
    onPrimary = CanvasDark,
    primaryContainer = MutedCanvasDark,
    onPrimaryContainer = TextPrimaryDark,
    secondary = TextSecondaryDark,
    onSecondary = CanvasDark,
    secondaryContainer = MutedCanvasDark,
    onSecondaryContainer = TextPrimaryDark,
    tertiary = TextTertiaryDark,
    background = CanvasDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceCardDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = MutedCanvasDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = BorderDark,
    outlineVariant = BorderSubtleDark,
    error = DangerRed,
    onError = CanvasDark,
    errorContainer = Color(0xFF3B1E1E),
    onErrorContainer = Color(0xFFFFB4A9)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
