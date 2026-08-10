package com.bibleadventures.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val BibleAdventuresColorScheme = lightColorScheme(
    primary = SunGold,
    secondary = SkyBlue,
    tertiary = LeafGreen,
    background = CreamBackground,
    surface = CardSurface,
    onPrimary = TextPrimary,
    onSecondary = TextPrimary,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    error = CoralPink,
)

/**
 * App-wide Material 3 theme. The MVP intentionally uses one bright, warm
 * palette rather than light/dark variants — a dark theme reads as
 * unfriendly for this audience.
 */
@Composable
fun BibleAdventuresTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BibleAdventuresColorScheme,
        typography = BibleAdventuresTypography,
        content = content,
    )
}
