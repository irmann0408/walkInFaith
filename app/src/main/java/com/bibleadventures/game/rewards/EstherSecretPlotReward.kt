package com.bibleadventures.game.rewards

import com.bibleadventures.R
import com.bibleadventures.domain.model.Badge
import com.bibleadventures.domain.model.ChapterId
import com.bibleadventures.domain.model.ScriptureCard

/** Concrete reward content awarded on completing Esther: The Secret Plot chapter. */
object EstherSecretPlotReward {
    val badge = Badge(
        id = "WATCHFUL_EARS",
        titleRes = R.string.badge_watchful_ears_title,
        descriptionRes = R.string.badge_watchful_ears_description,
        iconRes = R.drawable.ic_badge_watchful_ears,
        chapterId = ChapterId.ESTHER_SECRET_PLOT,
    )

    val scriptureCard = ScriptureCard(
        id = "ESTHER_2_22",
        titleRes = R.string.scripture_esther_2_22_title,
        reference = "Esther 2:22",
        textRes = R.string.scripture_esther_2_22_text,
        chapterId = ChapterId.ESTHER_SECRET_PLOT,
    )
}
