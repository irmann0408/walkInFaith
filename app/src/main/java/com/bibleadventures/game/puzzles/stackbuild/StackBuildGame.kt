package com.bibleadventures.game.puzzles.stackbuild

/**
 * Whether a drop was close enough to count (a "gentle snap" radius check)
 * is screen-side geometry, not this engine's concern — same split this app
 * already uses for `dragsort` (screen hit-tests against category bounds,
 * engine only validates the resolved category). The screen only calls
 * [onItemPlaced] once it has already decided a drop counts.
 */
object StackBuildGame {
    fun onItemPlaced(state: StackBuildGameState, itemId: String): StackBuildGameState {
        if (state.isComplete || itemId in state.placedOrder || itemId !in state.itemIds) return state
        return state.copy(placedOrder = state.placedOrder + itemId)
    }
}
