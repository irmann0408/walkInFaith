package com.bibleadventures.game.rewards

import com.bibleadventures.R
import com.bibleadventures.domain.model.Badge
import com.bibleadventures.domain.model.ChapterId
import com.bibleadventures.domain.model.ScriptureCard

/** Concrete reward content awarded on completing Noah's Ark. */
object NoahsArkReward {
    val badge = Badge(
        id = "ARK_BUILDER",
        titleRes = R.string.badge_ark_builder_title,
        descriptionRes = R.string.badge_ark_builder_description,
        chapterId = ChapterId.NOAHS_ARK,
    )

    val scriptureCard = ScriptureCard(
        id = "GENESIS_6_22",
        titleRes = R.string.scripture_genesis_6_22_title,
        reference = "Genesis 6:22",
        textRes = R.string.scripture_genesis_6_22_text,
    )
}
