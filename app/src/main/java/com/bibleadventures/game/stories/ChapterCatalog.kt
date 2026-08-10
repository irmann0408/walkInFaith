package com.bibleadventures.game.stories

import com.bibleadventures.R
import com.bibleadventures.domain.model.Chapter
import com.bibleadventures.domain.model.ChapterId

/**
 * Static definition of every chapter, in play order. Only Noah's Ark has
 * real gameplay (Milestone 4) — the rest exist here so the World Map,
 * unlock rules, and progression logic have real content to work with
 * ahead of their own milestones (spec section 7: "future chapters should
 * be represented in the architecture").
 */
object ChapterCatalog {
    val all: List<Chapter> = listOf(
        Chapter(
            id = ChapterId.NOAHS_ARK,
            titleRes = R.string.chapter_noahs_ark_title,
            descriptionRes = R.string.chapter_noahs_ark_description,
            lessonRes = R.string.chapter_noahs_ark_lesson,
            scriptureReference = "Genesis 6:22",
            requiredChapter = null,
        ),
        Chapter(
            id = ChapterId.DAVID_GOLIATH,
            titleRes = R.string.chapter_david_goliath_title,
            descriptionRes = R.string.chapter_david_goliath_description,
            lessonRes = R.string.chapter_david_goliath_lesson,
            scriptureReference = "1 Samuel 17:45",
            requiredChapter = ChapterId.NOAHS_ARK,
        ),
        Chapter(
            id = ChapterId.GOOD_SAMARITAN,
            titleRes = R.string.chapter_good_samaritan_title,
            descriptionRes = R.string.chapter_good_samaritan_description,
            lessonRes = R.string.chapter_good_samaritan_lesson,
            scriptureReference = "Luke 10:33",
            requiredChapter = ChapterId.DAVID_GOLIATH,
        ),
        Chapter(
            id = ChapterId.FEEDING_5000,
            titleRes = R.string.chapter_feeding_5000_title,
            descriptionRes = R.string.chapter_feeding_5000_description,
            lessonRes = R.string.chapter_feeding_5000_lesson,
            scriptureReference = "John 6:11",
            requiredChapter = ChapterId.GOOD_SAMARITAN,
        ),
        Chapter(
            id = ChapterId.DANIEL,
            titleRes = R.string.chapter_daniel_title,
            descriptionRes = R.string.chapter_daniel_description,
            lessonRes = R.string.chapter_daniel_lesson,
            scriptureReference = "Daniel 6:22",
            requiredChapter = ChapterId.FEEDING_5000,
        ),
        Chapter(
            id = ChapterId.JESUS_CALMS_STORM,
            titleRes = R.string.chapter_jesus_calms_storm_title,
            descriptionRes = R.string.chapter_jesus_calms_storm_description,
            lessonRes = R.string.chapter_jesus_calms_storm_lesson,
            scriptureReference = "Mark 4:39",
            requiredChapter = ChapterId.DANIEL,
        ),
    )
}
