package com.bibleadventures.game.puzzles.slingshot

/** Never FAILED — a miss just means try again (spec section 9). */
enum class SlingshotOutcome { NONE, HIT, MISS }

data class SlingshotGameState(
    val lastOutcome: SlingshotOutcome = SlingshotOutcome.NONE,
    val isHit: Boolean = false,
) {
    val isComplete: Boolean get() = isHit
}
