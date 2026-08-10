package com.bibleadventures.domain.model

import androidx.annotation.StringRes

/**
 * Static definition of one Bible adventure chapter. Content lives behind
 * string resources so the "all strings in strings.xml" convention holds
 * even from a non-Composable static catalog (see [com.bibleadventures.game.stories.ChapterCatalog]).
 */
data class Chapter(
    val id: ChapterId,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    @StringRes val lessonRes: Int,
    val scriptureReference: String,
    val requiredChapter: ChapterId?,
)
