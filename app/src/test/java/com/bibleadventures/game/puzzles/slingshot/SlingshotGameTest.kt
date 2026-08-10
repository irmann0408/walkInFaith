package com.bibleadventures.game.puzzles.slingshot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SlingshotGameTest {

    @Test
    fun `a release within tolerance reports HIT and completes the game`() {
        val state = SlingshotGame.onStoneReleased(SlingshotGameState(), aimedPosition = 0.5f, markPosition = 0.55f)

        assertEquals(SlingshotOutcome.HIT, state.lastOutcome)
        assertTrue(state.isHit)
        assertTrue(state.isComplete)
    }

    @Test
    fun `a release outside tolerance reports MISS and never FAILS`() {
        val state = SlingshotGame.onStoneReleased(SlingshotGameState(), aimedPosition = 0.1f, markPosition = 0.8f)

        assertEquals(SlingshotOutcome.MISS, state.lastOutcome)
        assertFalse(state.isHit)
        assertFalse(state.isComplete)
    }

    @Test
    fun `a miss does not block a later hit`() {
        var state = SlingshotGameState()
        state = SlingshotGame.onStoneReleased(state, aimedPosition = 0.1f, markPosition = 0.8f)
        state = SlingshotGame.onStoneReleased(state, aimedPosition = 0.5f, markPosition = 0.5f)

        assertEquals(SlingshotOutcome.HIT, state.lastOutcome)
        assertTrue(state.isComplete)
    }

    @Test
    fun `a release just inside the tolerance counts as a hit, just outside does not`() {
        val justInside = SlingshotGame.onStoneReleased(
            SlingshotGameState(),
            aimedPosition = 0.5f,
            markPosition = 0.5f + SlingshotGame.HIT_TOLERANCE - 0.01f,
        )
        val justOutside = SlingshotGame.onStoneReleased(
            SlingshotGameState(),
            aimedPosition = 0.5f,
            markPosition = 0.5f + SlingshotGame.HIT_TOLERANCE + 0.01f,
        )

        assertEquals(SlingshotOutcome.HIT, justInside.lastOutcome)
        assertEquals(SlingshotOutcome.MISS, justOutside.lastOutcome)
    }

    @Test
    fun `once hit, further releases are a no-op`() {
        var state = SlingshotGame.onStoneReleased(SlingshotGameState(), aimedPosition = 0.5f, markPosition = 0.5f)
        val afterHit = state

        state = SlingshotGame.onStoneReleased(state, aimedPosition = 0.1f, markPosition = 0.9f)

        assertEquals(afterHit, state)
    }
}
