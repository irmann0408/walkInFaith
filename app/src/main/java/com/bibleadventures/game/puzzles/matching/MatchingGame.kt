package com.bibleadventures.game.puzzles.matching

/**
 * Pure tap-to-match transition logic — no Compose/Android dependency, so
 * it's directly unit-testable and reusable by any future chapter that
 * needs a pairs-matching mini-game.
 */
object MatchingGame {

    fun onItemTapped(state: MatchingGameState, tappedId: String): MatchingGameState {
        if (tappedId in state.matchedIds || tappedId == state.selectedId) {
            return state.copy(lastOutcome = MatchOutcome.NONE)
        }

        val selectedId = state.selectedId
            ?: return state.copy(selectedId = tappedId, lastOutcome = MatchOutcome.NONE)

        val selectedItem = state.items.first { it.id == selectedId }
        val tappedItem = state.items.first { it.id == tappedId }

        return if (selectedItem.pairKey == tappedItem.pairKey) {
            state.copy(
                selectedId = null,
                matchedIds = state.matchedIds + selectedId + tappedId,
                lastOutcome = MatchOutcome.CORRECT,
            )
        } else {
            state.copy(selectedId = null, lastOutcome = MatchOutcome.TRY_AGAIN)
        }
    }
}
