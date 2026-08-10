package com.bibleadventures.game.puzzles.dodge

/**
 * Pure transition logic for the discrete lane-dodge mini-game — no
 * Compose/Android dependency, directly unit-testable. Stepping into the
 * hazard's lane is never a failure state: it just re-shows the same beat
 * with TRY_AGAIN feedback, retriable immediately at the player's own pace.
 */
object DodgeGame {

    fun onLaneTapped(state: DodgeGameState, tappedLane: DodgeLane): DodgeGameState {
        val beat = state.currentBeat ?: return state
        return if (tappedLane != beat.hazardLane) {
            state.copy(currentBeatIndex = state.currentBeatIndex + 1, lastOutcome = DodgeOutcome.DODGED)
        } else {
            state.copy(lastOutcome = DodgeOutcome.TRY_AGAIN)
        }
    }
}
