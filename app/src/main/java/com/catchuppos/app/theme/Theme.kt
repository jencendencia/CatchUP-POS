package com.catchuppos.app.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

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
    // Auto-adjust font size to the screen: every sp value in the app is
    // multiplied by a width-based scale, so text gets bigger on large
    // tablets and stays unchanged on phones. System font-size settings
    // are still honoured on top of this.
    val density = LocalDensity.current
    val textScale = rememberTextScale()
    val scaledDensity = remember(density, textScale) {
        Density(density = density.density, fontScale = density.fontScale * textScale)
    }

    CompositionLocalProvider(LocalDensity provides scaledDensity) {
        MaterialTheme(
            colorScheme = CatchUpColorScheme,
            typography = Typography,
            content = content
        )
    }
}
