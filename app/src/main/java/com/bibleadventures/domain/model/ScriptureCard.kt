package com.bibleadventures.domain.model

import androidx.annotation.StringRes

/** Static scripture card definition. Persisted profiles reference cards by [id]. */
data class ScriptureCard(
    val id: String,
    @StringRes val titleRes: Int,
    val reference: String,
    @StringRes val textRes: Int,
    val chapterId: ChapterId,
)
