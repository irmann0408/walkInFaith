package com.bibleadventures.game.rewards

import com.bibleadventures.R
import com.bibleadventures.domain.model.Badge
import com.bibleadventures.domain.model.ChapterId
import com.bibleadventures.domain.model.ScriptureCard

/**
 * Concrete reward content awarded on completing Esther's Rescue of Her
 * People — one badge for the whole chapter, plus every scripture verse
 * earned along the way by its 5 mini-puzzles.
 */
object EstherReward {
    val badge = Badge(
        id = "COURAGEOUS_HEART",
        titleRes = R.string.badge_courageous_heart_title,
        descriptionRes = R.string.badge_courageous_heart_description,
        iconRes = R.drawable.ic_badge_courageous_heart,
        chapterId = ChapterId.ESTHER,
    )

    val scriptureCards: List<ScriptureCard> = listOf(
        ScriptureCard(
            id = "ESTHER_2_20",
            titleRes = R.string.scripture_esther_2_20_title,
            reference = "Esther 2:20",
            textRes = R.string.scripture_esther_2_20_text,
            chapterId = ChapterId.ESTHER,
        ),
        ScriptureCard(
            id = "ESTHER_2_22",
            titleRes = R.string.scripture_esther_2_22_title,
            reference = "Esther 2:22",
            textRes = R.string.scripture_esther_2_22_text,
            chapterId = ChapterId.ESTHER,
        ),
        ScriptureCard(
            id = "ESTHER_4_3",
            titleRes = R.string.scripture_esther_4_3_title,
            reference = "Esther 4:3",
            textRes = R.string.scripture_esther_4_3_text,
            chapterId = ChapterId.ESTHER,
        ),
        ScriptureCard(
            id = "ESTHER_4_14",
            titleRes = R.string.scripture_esther_4_14_title,
            reference = "Esther 4:14",
            textRes = R.string.scripture_esther_4_14_text,
            chapterId = ChapterId.ESTHER,
        ),
        ScriptureCard(
            id = "ESTHER_7_3",
            titleRes = R.string.scripture_esther_7_3_title,
            reference = "Esther 7:3",
            textRes = R.string.scripture_esther_7_3_text,
            chapterId = ChapterId.ESTHER,
        ),
    )
}
