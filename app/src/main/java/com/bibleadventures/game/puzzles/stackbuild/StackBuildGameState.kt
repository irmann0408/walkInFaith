package com.bibleadventures.game.puzzles.stackbuild

/**
 * Drag any remaining item, in whatever order the player picks, onto one
 * growing pile — [placedOrder] is the strict, append-only record of that
 * stacking order (index = level). Unlike [com.bibleadventures.game.puzzles.dragsort.DragSortGameState]
 * (sorts into multiple categories) or [com.bibleadventures.game.puzzles.sequence.SequenceGameState]
 * (order is required/enforced), placement here is order-*independent* —
 * only the act of stacking one at a time is tracked.
 */
data class StackBuildGameState(
    val itemIds: List<String>,
    val placedOrder: List<String> = emptyList(),
) {
    val isComplete: Boolean get() = placedOrder.size == itemIds.size
    val remainingIds: List<String> get() = itemIds.filterNot { it in placedOrder }
}
