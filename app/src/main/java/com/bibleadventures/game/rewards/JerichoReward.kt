package com.bibleadventures.game.rewards

import com.bibleadventures.R
import com.bibleadventures.domain.model.Badge
import com.bibleadventures.domain.model.ChapterId
import com.bibleadventures.domain.model.ScriptureCard

/** Concrete reward content awarded on completing the Battle of Jericho chapter. */
object JerichoReward {
    val badge = Badge(
        id = "FAITHFUL_STEPS",
        titleRes = R.string.badge_faithful_steps_title,
        descriptionRes = R.string.badge_faithful_steps_description,
        iconRes = R.drawable.ic_badge_faithful_steps,
        chapterId = ChapterId.JERICHO,
    )

    val scriptureCard = ScriptureCard(
        id = "JOSHUA_6_20",
        titleRes = R.string.scripture_joshua_6_20_title,
        reference = "Joshua 6:20",
        textRes = R.string.scripture_joshua_6_20_text,
        chapterId = ChapterId.JERICHO,
    )
}
