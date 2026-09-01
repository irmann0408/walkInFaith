package com.bibleadventures.game.puzzles.stackbuild

/** Never FAILED — placing the wrong next item just re-prompts, all prior progress kept. */
enum class StackBuildOutcome { NONE, PLACED, WRONG_ORDER, COMPLETE }

/**
 * Drag items onto a growing pile **in the exact order given by [itemIds]**
 * — [placedOrder] is the strict, append-only record of what's been placed
 * so far (index = level). Unlike [com.bibleadventures.game.puzzles.groupfill.GroupFillGameState]
 * (drops into any of several bins as long as the running sum fits, order-independent) or
 * [com.bibleadventures.game.puzzles.sequence.SequenceGameState] (tap-based
 * ordered targets), this is drag-based with an enforced order: whoever
 * calls [StackBuildGame.onItemPlaced] decides whether a drop counts at all
 * (screen-side geometry, e.g. a snap radius); this engine only decides
 * whether it's the *right* next item.
 */
data class StackBuildGameState(
    val itemIds: List<String>,
    val placedOrder: List<String> = emptyList(),
    val lastOutcome: StackBuildOutcome = StackBuildOutcome.NONE,
) {
    val isComplete: Boolean get() = placedOrder.size == itemIds.size
    val remainingIds: List<String> get() = itemIds.filterNot { it in placedOrder }
    val nextExpectedId: String? get() = itemIds.getOrNull(placedOrder.size)
}
