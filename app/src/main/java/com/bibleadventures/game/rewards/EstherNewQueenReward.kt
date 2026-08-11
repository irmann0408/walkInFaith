package com.bibleadventures.game.rewards

import com.bibleadventures.R
import com.bibleadventures.domain.model.Badge
import com.bibleadventures.domain.model.ChapterId
import com.bibleadventures.domain.model.ScriptureCard

/** Concrete reward content awarded on completing Esther: The New Queen chapter. */
object EstherNewQueenReward {
    val badge = Badge(
        id = "HUMBLE_TRUST",
        titleRes = R.string.badge_humble_trust_title,
        descriptionRes = R.string.badge_humble_trust_description,
        iconRes = R.drawable.ic_badge_humble_trust,
        chapterId = ChapterId.ESTHER_NEW_QUEEN,
    )

    val scriptureCard = ScriptureCard(
        id = "ESTHER_2_20",
        titleRes = R.string.scripture_esther_2_20_title,
        reference = "Esther 2:20",
        textRes = R.string.scripture_esther_2_20_text,
        chapterId = ChapterId.ESTHER_NEW_QUEEN,
    )
}
