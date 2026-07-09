package com.catchuppos.app.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val CatchUpColorScheme = darkColorScheme(
    primary = OrangeAccent,
    onPrimary = TextWhite,
    primaryContainer = OrangeDark,
    onPrimaryContainer = TextWhite,
    secondary = OrangeMuted,
    onSecondary = TextWhite,
    tertiary = StatusGreen,
    onTertiary = TextWhite,
    background = DarkBackground,
    onBackground = TextWhite,
    surface = DarkSurface,
    onSurface = TextWhite,
    surfaceVariant = DarkCard,
    onSurfaceVariant = TextMuted,
    outline = DarkBorder,
    outlineVariant = InputBorder,
    error = MutedRed,
    onError = TextWhite
)

@Composable
fun CatchUpTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CatchUpColorScheme,
        typography = Typography,
        content = content
    )
}
