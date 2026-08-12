package com.bibleadventures.game.puzzles.stackbuild

/**
 * Whether a drop was close enough to count (a "gentle snap" radius check)
 * is screen-side geometry, not this engine's concern — same split this app
 * already uses for `dragsort` (screen hit-tests against category bounds,
 * engine only validates the resolved category). The screen only calls
 * [onItemPlaced] once it has already decided a drop counts; this engine
 * then decides whether it was the *right* item — placing anything other
 * than [StackBuildGameState.nextExpectedId] is never a failure, just
 * re-prompts (`WRONG_ORDER`) with no progress lost.
 */
object StackBuildGame {
    fun onItemPlaced(state: StackBuildGameState, itemId: String): StackBuildGameState {
        if (state.isComplete) return state
        return if (itemId == state.nextExpectedId) {
            val nextOrder = state.placedOrder + itemId
            val outcome = if (nextOrder.size == state.itemIds.size) StackBuildOutcome.COMPLETE else StackBuildOutcome.PLACED
            state.copy(placedOrder = nextOrder, lastOutcome = outcome)
        } else {
            state.copy(lastOutcome = StackBuildOutcome.WRONG_ORDER)
        }
    }
}
