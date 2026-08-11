package com.bibleadventures.game.rewards

import com.bibleadventures.R
import com.bibleadventures.domain.model.Badge
import com.bibleadventures.domain.model.ChapterId
import com.bibleadventures.domain.model.ScriptureCard

/** Concrete reward content awarded on completing Esther: The Threat chapter. */
object EstherThreatReward {
    val badge = Badge(
        id = "FAITHFUL_MESSENGER",
        titleRes = R.string.badge_faithful_messenger_title,
        descriptionRes = R.string.badge_faithful_messenger_description,
        iconRes = R.drawable.ic_badge_faithful_messenger,
        chapterId = ChapterId.ESTHER_THREAT,
    )

    val scriptureCard = ScriptureCard(
        id = "ESTHER_4_3",
        titleRes = R.string.scripture_esther_4_3_title,
        reference = "Esther 4:3",
        textRes = R.string.scripture_esther_4_3_text,
        chapterId = ChapterId.ESTHER_THREAT,
    )
}
