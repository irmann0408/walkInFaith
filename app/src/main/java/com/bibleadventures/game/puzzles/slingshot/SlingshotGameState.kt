package com.bibleadventures.game.puzzles.slingshot

/** Never FAILED — a miss just means try again (spec section 9). */
enum class SlingshotOutcome { NONE, HIT, MISS }

/** Since this is practice, one hit isn't the point — [requiredHits] real hits are, a miss between them never resets [hits]. */
data class SlingshotGameState(
    val hits: Int = 0,
    val requiredHits: Int = 3,
    val lastOutcome: SlingshotOutcome = SlingshotOutcome.NONE,
) {
    val isComplete: Boolean get() = hits >= requiredHits
}
