package com.bibleadventures.game.rewards

import com.bibleadventures.R
import com.bibleadventures.domain.model.Badge
import com.bibleadventures.domain.model.ChapterId
import com.bibleadventures.domain.model.ScriptureCard

/** Concrete reward content awarded on completing Esther: The Banquets & Rescue chapter. */
object EstherBanquetsRescueReward {
    val badge = Badge(
        id = "BOLD_VOICE",
        titleRes = R.string.badge_bold_voice_title,
        descriptionRes = R.string.badge_bold_voice_description,
        iconRes = R.drawable.ic_badge_bold_voice,
        chapterId = ChapterId.ESTHER_BANQUETS_RESCUE,
    )

    val scriptureCard = ScriptureCard(
        id = "ESTHER_7_3",
        titleRes = R.string.scripture_esther_7_3_title,
        reference = "Esther 7:3",
        textRes = R.string.scripture_esther_7_3_text,
        chapterId = ChapterId.ESTHER_BANQUETS_RESCUE,
    )
}
