package com.bibleadventures.game.rewards

import com.bibleadventures.R
import com.bibleadventures.domain.model.Badge
import com.bibleadventures.domain.model.ChapterId
import com.bibleadventures.domain.model.ScriptureCard

/** Concrete reward content awarded on completing the Daniel and the Lions chapter. */
object DanielReward {
    val badge = Badge(
        id = "FAITHFUL_HEART",
        titleRes = R.string.badge_faithful_heart_title,
        descriptionRes = R.string.badge_faithful_heart_description,
        iconRes = R.drawable.ic_badge_faithful_heart,
        chapterId = ChapterId.DANIEL,
    )

    val scriptureCard = ScriptureCard(
        id = "DANIEL_6_22",
        titleRes = R.string.scripture_daniel_6_22_title,
        reference = "Daniel 6:22",
        textRes = R.string.scripture_daniel_6_22_text,
        chapterId = ChapterId.DANIEL,
    )
}
