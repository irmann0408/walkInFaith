package com.bibleadventures.game.puzzles.slingshot

/**
 * A plain 2D point/vector in the same 0..1 fractional coordinate space the
 * screen already uses for every other puzzle's geometry — not
 * `androidx.compose.ui.geometry.Offset`, since this package stays pure
 * Kotlin with no Compose/Android dependency.
 */
data class Vector2(val x: Float, val y: Float)

/** Never FAILED — a miss just means try again, and an escaped rat just means the next one comes, free, uncounted (spec section 9). */
enum class SlingshotOutcome { NONE, HIT, MISS, ESCAPED }

/**
 * Rats appear one at a time until [requiredHits] have actually been hit —
 * an escaped rat doesn't count against or toward that total, it's simply
 * free practice that costs nothing. [ratsSpawned] exists purely so the
 * screen can tell when the *current* rat's turn has ended (hit or
 * escaped) and a fresh one should start falling from the top again; it
 * plays no part in [isComplete].
 */
data class SlingshotGameState(
    val hits: Int = 0,
    val requiredHits: Int = 7,
    val ratsSpawned: Int = 0,
    val lastOutcome: SlingshotOutcome = SlingshotOutcome.NONE,
) {
    val isComplete: Boolean get() = hits >= requiredHits
}
