package com.bibleadventures.game.rewards

import com.bibleadventures.R
import com.bibleadventures.domain.model.Badge
import com.bibleadventures.domain.model.ChapterId
import com.bibleadventures.domain.model.ScriptureCard

/** Concrete reward content awarded on completing the Esther's Rescue of Her People chapter. */
object EstherReward {
    val badge = Badge(
        id = "COURAGEOUS_HEART",
        titleRes = R.string.badge_courageous_heart_title,
        descriptionRes = R.string.badge_courageous_heart_description,
        iconRes = R.drawable.ic_badge_courageous_heart,
        chapterId = ChapterId.ESTHER,
    )

    val scriptureCard = ScriptureCard(
        id = "ESTHER_4_14",
        titleRes = R.string.scripture_esther_4_14_title,
        reference = "Esther 4:14",
        textRes = R.string.scripture_esther_4_14_text,
        chapterId = ChapterId.ESTHER,
    )
}
