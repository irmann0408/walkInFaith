package com.bibleadventures.game.puzzles.sequence

/** Never FAILED — tapping out of order just re-prompts, with all prior progress kept (spec section 9). */
enum class SequenceOutcome { NONE, POINT_CONNECTED, OUT_OF_ORDER, COMPLETE }

/**
 * A fixed set of points that must be connected in a specific order. No
 * position data — this engine is pure and chapter-agnostic, like
 * [com.bibleadventures.game.puzzles.gridmaze.GridMazeState]; where each
 * point renders on screen is content, not engine state.
 */
data class SequenceGameState(
    val pointIds: List<String>,
    val connectedIds: List<String> = emptyList(),
    val lastOutcome: SequenceOutcome = SequenceOutcome.NONE,
) {
    val isComplete: Boolean get() = connectedIds.size == pointIds.size
    val nextExpectedId: String? get() = pointIds.getOrNull(connectedIds.size)
}
