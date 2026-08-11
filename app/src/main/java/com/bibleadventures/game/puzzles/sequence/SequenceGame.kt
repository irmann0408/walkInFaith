package com.bibleadventures.game.puzzles.sequence

/**
 * Pure transition logic for the connect-in-order mini-game — no
 * Compose/Android dependency, directly unit-testable. Tapping a point out of
 * order is never a failure state: it never undoes prior progress, just
 * flags OUT_OF_ORDER so the UI can re-prompt for the correct next point.
 */
object SequenceGame {

    fun onPointTapped(state: SequenceGameState, tappedId: String): SequenceGameState {
        if (state.isComplete) return state

        return when {
            tappedId in state.connectedIds -> state
            tappedId == state.nextExpectedId -> {
                val connected = state.connectedIds + tappedId
                val outcome = if (connected.size == state.pointIds.size) {
                    SequenceOutcome.COMPLETE
                } else {
                    SequenceOutcome.POINT_CONNECTED
                }
                state.copy(connectedIds = connected, lastOutcome = outcome)
            }
            else -> state.copy(lastOutcome = SequenceOutcome.OUT_OF_ORDER)
        }
    }
}
