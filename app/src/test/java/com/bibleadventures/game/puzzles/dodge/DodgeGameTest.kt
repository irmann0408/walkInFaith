package com.bibleadventures.game.puzzles.dodge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DodgeGameTest {

    private val beats = listOf(
        DodgeBeat("beat_1", DodgeLane.LEFT),
        DodgeBeat("beat_2", DodgeLane.RIGHT),
        DodgeBeat("beat_3", DodgeLane.LEFT),
    )

    @Test
    fun `stepping into the hazard lane reports TRY_AGAIN and never FAILS`() {
        val state = DodgeGame.onLaneTapped(DodgeGameState(beats), tappedLane = DodgeLane.LEFT)

        assertEquals(DodgeOutcome.TRY_AGAIN, state.lastOutcome)
        assertEquals(0, state.currentBeatIndex)
        assertFalse(state.isComplete)
    }

    @Test
    fun `stepping to the safe lane reports DODGED and advances to the next beat`() {
        val state = DodgeGame.onLaneTapped(DodgeGameState(beats), tappedLane = DodgeLane.RIGHT)

        assertEquals(DodgeOutcome.DODGED, state.lastOutcome)
        assertEquals(1, state.currentBeatIndex)
        assertEquals(beats[1], state.currentBeat)
    }

    @Test
    fun `a wrong step does not block a later correct one`() {
        var state = DodgeGameState(beats)
        state = DodgeGame.onLaneTapped(state, tappedLane = DodgeLane.LEFT) // wrong, TRY_AGAIN
        state = DodgeGame.onLaneTapped(state, tappedLane = DodgeLane.RIGHT) // correct

        assertEquals(DodgeOutcome.DODGED, state.lastOutcome)
        assertEquals(1, state.currentBeatIndex)
    }

    @Test
    fun `tapping through every beat completes the game`() {
        var state = DodgeGameState(beats)
        state = DodgeGame.onLaneTapped(state, tappedLane = DodgeLane.RIGHT) // dodges beat_1 (LEFT)
        state = DodgeGame.onLaneTapped(state, tappedLane = DodgeLane.LEFT) // dodges beat_2 (RIGHT)
        state = DodgeGame.onLaneTapped(state, tappedLane = DodgeLane.RIGHT) // dodges beat_3 (LEFT)

        assertTrue(state.isComplete)
        assertEquals(null, state.currentBeat)
    }

    @Test
    fun `once complete, further taps are a no-op`() {
        var state = DodgeGameState(listOf(DodgeBeat("beat_1", DodgeLane.LEFT)))
        state = DodgeGame.onLaneTapped(state, tappedLane = DodgeLane.RIGHT)
        val afterComplete = state

        state = DodgeGame.onLaneTapped(state, tappedLane = DodgeLane.LEFT)

        assertEquals(afterComplete, state)
    }
}
