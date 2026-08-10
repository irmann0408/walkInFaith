package com.bibleadventures.game.puzzles.dragsort

/**
 * Pure drop-target logic — no Compose/Android dependency. The UI layer
 * (spec section 9: "Use drag-and-drop") owns the actual pointer gesture
 * handling and calls this once it knows which category an item was
 * dropped on.
 */
object DragSortGame {

    fun onItemDroppedOnCategory(
        state: DragSortGameState,
        itemId: String,
        categoryKey: String,
    ): DragSortGameState {
        if (itemId in state.placedItems) return state.copy(lastOutcome = SortOutcome.NONE)

        val item = state.items.first { it.id == itemId }
        return if (item.categoryKey == categoryKey) {
            state.copy(
                placedItems = state.placedItems + (itemId to categoryKey),
                lastOutcome = SortOutcome.CORRECT,
            )
        } else {
            // Item is not added to placedItems, so the UI animates it back to origin.
            state.copy(lastOutcome = SortOutcome.TRY_AGAIN)
        }
    }
}
