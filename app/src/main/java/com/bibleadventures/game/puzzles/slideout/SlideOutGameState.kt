package com.bibleadventures.game.puzzles.slideout

enum class SlideDirection { UP, DOWN, LEFT, RIGHT }

/** Never FAILED — a stuck latch is BLOCKED, not lost progress; every attempt is fully retriable (spec section 9). */
enum class SlideOutOutcome { NONE, RELEASED, BLOCKED, COMPLETE }

data class CellPosition(val row: Int, val col: Int)

/** A single-cell latch with one fixed exit direction — tapping it either flies it off the board or leaves it exactly where it was. */
data class LatchBlock(val id: String, val position: CellPosition, val direction: SlideDirection)

/**
 * An "Arrow Block: Slide Out" board: every remaining latch has one fixed
 * exit direction, and [SlideOutGame.onBlockTapped] either removes it
 * (clear path to the board edge) or leaves it exactly where it was
 * (something else is in the way). Deliberately simpler than
 * `game/puzzles/roadblock`'s Rush-Hour-style engine — no
 * orientation/length concept, since every latch here is a single cell and
 * either fully exits or doesn't move at all.
 */
data class SlideOutGameState(
    val rows: Int,
    val cols: Int,
    val blocks: List<LatchBlock>,
    val lastOutcome: SlideOutOutcome = SlideOutOutcome.NONE,
    /** Which latch just got stuck, for the screen's shake/red-flash reaction and a specific live-region message — cleared on any successful release. */
    val lastBlockedId: String? = null,
) {
    val isComplete: Boolean get() = blocks.isEmpty()
}
