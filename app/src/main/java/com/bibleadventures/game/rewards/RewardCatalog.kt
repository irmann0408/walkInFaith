package com.bibleadventures.game.rewards

import com.bibleadventures.domain.model.Badge
import com.bibleadventures.domain.model.ScriptureCard

/**
 * Aggregates every chapter's reward content into flat lists for the Badges
 * and Scripture Cards galleries. Each future chapter's own `*Reward.kt`
 * object gets appended here by hand — no catalog-registration framework,
 * same reasoning as `ChapterCatalog`'s fixed list.
 */
object RewardCatalog {
    val badges: List<Badge> = listOf(NoahsArkReward.badge, DavidGoliathReward.badge, GoodSamaritanReward.badge, DanielReward.badge)
    val scriptureCards: List<ScriptureCard> = listOf(
        NoahsArkReward.scriptureCard,
        DavidGoliathReward.scriptureCard,
        GoodSamaritanReward.scriptureCard,
        DanielReward.scriptureCard,
    )
}
