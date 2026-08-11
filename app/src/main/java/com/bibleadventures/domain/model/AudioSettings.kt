package com.bibleadventures.domain.model

import kotlinx.serialization.Serializable

/** Persisted audio preferences. All default to on — the friendliest default for a game. */
@Serializable
data class AudioSettings(
    val musicEnabled: Boolean = true,
    val soundEffectsEnabled: Boolean = true,
    val narrationEnabled: Boolean = true,
)
