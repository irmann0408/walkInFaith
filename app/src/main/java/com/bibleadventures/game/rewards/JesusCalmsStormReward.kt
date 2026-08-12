package com.bibleadventures.game.rewards

import com.bibleadventures.R
import com.bibleadventures.domain.model.Badge
import com.bibleadventures.domain.model.ChapterId
import com.bibleadventures.domain.model.ScriptureCard

/** Concrete reward content awarded on completing the Jesus Calms the Storm chapter. */
object JesusCalmsStormReward {
    val badge = Badge(
        id = "UNSHAKEN_FAITH",
        titleRes = R.string.badge_unshaken_faith_title,
        descriptionRes = R.string.badge_unshaken_faith_description,
        iconRes = R.drawable.ic_badge_unshaken_faith,
        chapterId = ChapterId.JESUS_CALMS_STORM,
    )

    val scriptureCard = ScriptureCard(
        id = "MARK_4_39",
        titleRes = R.string.scripture_mark_4_39_title,
        reference = "Mark 4:39",
        textRes = R.string.scripture_mark_4_39_text,
        chapterId = ChapterId.JESUS_CALMS_STORM,
    )
}
