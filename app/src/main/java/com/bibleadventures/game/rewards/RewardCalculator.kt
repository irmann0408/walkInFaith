package com.bibleadventures.game.rewards

/**
 * MVP reward is always full stars on completion — mistakes made along the
 * way inside a mini-game are never punished (spec section 10). Kept as a
 * function rather than a constant so a future chapter can plug in
 * different scoring without touching call sites.
 */
object RewardCalculator {
    const val MAX_STARS = 3

    fun calculateStars(chapterCompleted: Boolean): Int = if (chapterCompleted) MAX_STARS else 0
}
