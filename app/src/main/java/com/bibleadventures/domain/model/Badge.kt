package com.bibleadventures.domain.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

/** Static badge definition. Persisted profiles reference badges by [id]. */
data class Badge(
    val id: String,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    @DrawableRes val iconRes: Int,
    val chapterId: ChapterId,
)
