package com.bibleadventures.game.rewards

import com.bibleadventures.R
import com.bibleadventures.domain.model.Badge
import com.bibleadventures.domain.model.ChapterId
import com.bibleadventures.domain.model.ScriptureCard

/** Concrete reward content awarded on completing the Good Samaritan chapter. */
object GoodSamaritanReward {
    val badge = Badge(
        id = "GOOD_NEIGHBOR",
        titleRes = R.string.badge_good_neighbor_title,
        descriptionRes = R.string.badge_good_neighbor_description,
        iconRes = R.drawable.ic_badge_good_neighbor,
        chapterId = ChapterId.GOOD_SAMARITAN,
    )

    val scriptureCard = ScriptureCard(
        id = "LUKE_10_33",
        titleRes = R.string.scripture_luke_10_33_title,
        reference = "Luke 10:33",
        textRes = R.string.scripture_luke_10_33_text,
        chapterId = ChapterId.GOOD_SAMARITAN,
    )
}
