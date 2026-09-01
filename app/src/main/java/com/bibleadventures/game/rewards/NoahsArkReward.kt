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
        iconRes = R.drawable.ic_badge_ark_builder,
        chapterId = ChapterId.NOAHS_ARK,
    )

    val scriptureCard = ScriptureCard(
        id = "GENESIS_9_13",
        titleRes = R.string.scripture_genesis_9_13_title,
        reference = "Genesis 9:13",
        textRes = R.string.scripture_genesis_9_13_text,
        chapterId = ChapterId.NOAHS_ARK,
    )
}
