package com.bibleadventures.game.rewards

import com.bibleadventures.R
import com.bibleadventures.domain.model.Badge
import com.bibleadventures.domain.model.ChapterId
import com.bibleadventures.domain.model.ScriptureCard

/** Concrete reward content awarded on completing the Feeding the 5,000 chapter. */
object Feeding5000Reward {
    val badge = Badge(
        id = "GENEROUS_HEART",
        titleRes = R.string.badge_generous_heart_title,
        descriptionRes = R.string.badge_generous_heart_description,
        iconRes = R.drawable.ic_badge_generous_heart,
        chapterId = ChapterId.FEEDING_5000,
    )

    val scriptureCard = ScriptureCard(
        id = "JOHN_6_11",
        titleRes = R.string.scripture_john_6_11_title,
        reference = "John 6:11",
        textRes = R.string.scripture_john_6_11_text,
        chapterId = ChapterId.FEEDING_5000,
    )
}
