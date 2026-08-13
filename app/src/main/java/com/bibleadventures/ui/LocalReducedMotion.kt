package com.bibleadventures.ui

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Reaches the player's Reduced Motion preference from any composable
 * without threading a `Boolean` parameter through every screen/ViewModel
 * signature — a cross-cutting UI concern, same reasoning as
 * [LocalAudioController]. Provided once at the app root in `MainActivity`;
 * the `false` default only applies to `@Preview` composables that never
 * install a real provider. Only read at the small set of purely decorative
 * animation call sites (drag-snap pulses, lane slides, etc.) — this app's
 * `withFrameNanos`-driven real-time mini-games are gameplay-critical
 * timing, not decoration, and are deliberately never gated by this flag.
 */
val LocalReducedMotion = staticCompositionLocalOf { false }
