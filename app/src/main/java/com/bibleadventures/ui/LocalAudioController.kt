package com.bibleadventures.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.bibleadventures.audio.AudioController
import com.bibleadventures.audio.NoOpAudioController

/**
 * Reaches [AudioController] from any composable (including shared ones like
 * [com.bibleadventures.ui.components.StoryBeatScreen]) without threading an
 * `AudioController` parameter through every screen/ViewModel signature.
 * Provided once at the app root in `MainActivity`; the [NoOpAudioController]
 * default only applies to `@Preview` composables that never install a real
 * provider.
 */
val LocalAudioController = staticCompositionLocalOf<AudioController> { NoOpAudioController() }
