package com.bibleadventures.game.puzzles.hiddenobject

/**
 * Pure found-item logic. The UI layer is responsible for rendering tap
 * targets at least 48dp regardless of the icon's visual size, to avoid
 * frustrating pixel-hunting (spec section 9).
 */
object HiddenObjectGame {

    fun onItemTapped(state: HiddenObjectGameState, itemId: String): HiddenObjectGameState {
        if (itemId in state.foundIds) return state
        return state.copy(foundIds = state.foundIds + itemId)
    }
}
