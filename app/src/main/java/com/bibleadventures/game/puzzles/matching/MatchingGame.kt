package com.bibleadventures.game.puzzles.matching

/**
 * Pure memory/concentration transition logic — no Compose/Android dependency,
 * so it's directly unit-testable and reusable by any future chapter that
 * needs a card-matching mini-game.
 *
 * Cards start face down. Tapping flips one face up; a second tap compares it
 * against the first. A mismatch stays face up (the player gets a real look,
 * and sees the "try another one" feedback) rather than hiding immediately —
 * there's no forced timer, so the next tap anywhere flips the mismatched
 * pair back down and starts a fresh selection at the player's own pace
 * (spec section 9: never FAILED, no time pressure).
 */
object MatchingGame {

    fun onItemTapped(state: MatchingGameState, tappedId: String): MatchingGameState {
        if (tappedId in state.matchedIds) return state

        val current = if (state.lastOutcome == MatchOutcome.TRY_AGAIN) {
            state.copy(selectedIds = emptyList(), lastOutcome = MatchOutcome.NONE)
        } else {
            state
        }

        if (tappedId in current.selectedIds) return current

        val selected = current.selectedIds + tappedId
        if (selected.size < 2) {
            return current.copy(selectedIds = selected, lastOutcome = MatchOutcome.NONE)
        }

        val first = current.items.first { it.id == selected[0] }
        val second = current.items.first { it.id == selected[1] }

        return if (first.pairKey == second.pairKey) {
            current.copy(
                selectedIds = emptyList(),
                matchedIds = current.matchedIds + selected,
                lastOutcome = MatchOutcome.CORRECT,
            )
        } else {
            current.copy(selectedIds = selected, lastOutcome = MatchOutcome.TRY_AGAIN)
        }
    }
}
