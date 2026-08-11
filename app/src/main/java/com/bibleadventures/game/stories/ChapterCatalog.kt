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
            id = ChapterId.DANIEL,
            titleRes = R.string.chapter_daniel_title,
            descriptionRes = R.string.chapter_daniel_description,
            lessonRes = R.string.chapter_daniel_lesson,
            scriptureReference = "Daniel 6:22",
            requiredChapter = ChapterId.GOOD_SAMARITAN,
        ),
        Chapter(
            id = ChapterId.ESTHER_NEW_QUEEN,
            titleRes = R.string.chapter_esther_new_queen_title,
            descriptionRes = R.string.chapter_esther_new_queen_description,
            lessonRes = R.string.chapter_esther_new_queen_lesson,
            scriptureReference = "Esther 2:20",
            requiredChapter = ChapterId.DANIEL,
        ),
        Chapter(
            id = ChapterId.ESTHER_SECRET_PLOT,
            titleRes = R.string.chapter_esther_secret_plot_title,
            descriptionRes = R.string.chapter_esther_secret_plot_description,
            lessonRes = R.string.chapter_esther_secret_plot_lesson,
            scriptureReference = "Esther 2:22",
            requiredChapter = ChapterId.ESTHER_NEW_QUEEN,
        ),
        Chapter(
            id = ChapterId.ESTHER_THREAT,
            titleRes = R.string.chapter_esther_threat_title,
            descriptionRes = R.string.chapter_esther_threat_description,
            lessonRes = R.string.chapter_esther_threat_lesson,
            scriptureReference = "Esther 4:3",
            requiredChapter = ChapterId.ESTHER_SECRET_PLOT,
        ),
        Chapter(
            id = ChapterId.ESTHER_BRAVE_APPROACH,
            titleRes = R.string.chapter_esther_brave_approach_title,
            descriptionRes = R.string.chapter_esther_brave_approach_description,
            lessonRes = R.string.chapter_esther_brave_approach_lesson,
            scriptureReference = "Esther 4:14",
            requiredChapter = ChapterId.ESTHER_THREAT,
        ),
        Chapter(
            id = ChapterId.ESTHER_BANQUETS_RESCUE,
            titleRes = R.string.chapter_esther_banquets_rescue_title,
            descriptionRes = R.string.chapter_esther_banquets_rescue_description,
            lessonRes = R.string.chapter_esther_banquets_rescue_lesson,
            scriptureReference = "Esther 7:3",
            requiredChapter = ChapterId.ESTHER_BRAVE_APPROACH,
        ),
        Chapter(
            id = ChapterId.JERICHO,
            titleRes = R.string.chapter_jericho_title,
            descriptionRes = R.string.chapter_jericho_description,
            lessonRes = R.string.chapter_jericho_lesson,
            scriptureReference = "Joshua 6:20",
            requiredChapter = ChapterId.ESTHER_BANQUETS_RESCUE,
        ),
        Chapter(
            id = ChapterId.FEEDING_5000,
            titleRes = R.string.chapter_feeding_5000_title,
            descriptionRes = R.string.chapter_feeding_5000_description,
            lessonRes = R.string.chapter_feeding_5000_lesson,
            scriptureReference = "John 6:11",
            requiredChapter = ChapterId.JERICHO,
        ),
        Chapter(
            id = ChapterId.JESUS_CALMS_STORM,
            titleRes = R.string.chapter_jesus_calms_storm_title,
            descriptionRes = R.string.chapter_jesus_calms_storm_description,
            lessonRes = R.string.chapter_jesus_calms_storm_lesson,
            scriptureReference = "Mark 4:39",
            requiredChapter = ChapterId.FEEDING_5000,
        ),
    )
}
