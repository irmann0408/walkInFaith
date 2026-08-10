package com.bibleadventures.game.rewards

import com.bibleadventures.R
import com.bibleadventures.domain.model.Badge
import com.bibleadventures.domain.model.ChapterId
import com.bibleadventures.domain.model.ScriptureCard

/** Concrete reward content awarded on completing David and Goliath. */
object DavidGoliathReward {
    val badge = Badge(
        id = "BRAVE_HEART",
        titleRes = R.string.badge_brave_heart_title,
        descriptionRes = R.string.badge_brave_heart_description,
        iconRes = R.drawable.ic_badge_brave_heart,
        chapterId = ChapterId.DAVID_GOLIATH,
    )

    val scriptureCard = ScriptureCard(
        id = "1_SAMUEL_17_45",
        titleRes = R.string.scripture_1_samuel_17_45_title,
        reference = "1 Samuel 17:45",
        textRes = R.string.scripture_1_samuel_17_45_text,
        chapterId = ChapterId.DAVID_GOLIATH,
    )
}
