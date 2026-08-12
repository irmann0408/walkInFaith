package com.bibleadventures.game.puzzles.slingshot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SlingshotGameTest {

    private val shieldMin = 0.35f
    private val shieldMax = 0.65f

    @Test
    fun `a release within tolerance and inside the shield reports HIT and increases hits`() {
        val state = SlingshotGame.onStoneReleased(
            SlingshotGameState(),
            aimedPosition = 0.5f,
            markPosition = 0.55f,
            shieldMinFraction = shieldMin,
            shieldMaxFraction = shieldMax,
        )

        assertEquals(SlingshotOutcome.HIT, state.lastOutcome)
        assertEquals(1, state.hits)
        assertFalse(state.isComplete)
    }

    @Test
    fun `a release outside tolerance reports MISS and never FAILS`() {
        val state = SlingshotGame.onStoneReleased(
            SlingshotGameState(),
            aimedPosition = 0.1f,
            markPosition = 0.8f,
            shieldMinFraction = shieldMin,
            shieldMaxFraction = shieldMax,
        )

        assertEquals(SlingshotOutcome.MISS, state.lastOutcome)
        assertEquals(0, state.hits)
        assertFalse(state.isComplete)
    }

    @Test
    fun `a release that matches the mark exactly but the mark is outside the shield reports MISS`() {
        val state = SlingshotGame.onStoneReleased(
            SlingshotGameState(),
            aimedPosition = 0.15f,
            markPosition = 0.15f,
            shieldMinFraction = shieldMin,
            shieldMaxFraction = shieldMax,
        )

        assertEquals(SlingshotOutcome.MISS, state.lastOutcome)
        assertEquals(0, state.hits)
    }

    @Test
    fun `a miss between hits does not reset progress`() {
        var state = SlingshotGameState()
        state = SlingshotGame.onStoneReleased(state, aimedPosition = 0.5f, markPosition = 0.5f, shieldMinFraction = shieldMin, shieldMaxFraction = shieldMax)
        assertEquals(1, state.hits)

        state = SlingshotGame.onStoneReleased(state, aimedPosition = 0.1f, markPosition = 0.8f, shieldMinFraction = shieldMin, shieldMaxFraction = shieldMax)

        assertEquals(SlingshotOutcome.MISS, state.lastOutcome)
        assertEquals(1, state.hits)
        assertFalse(state.isComplete)
    }

    @Test
    fun `isComplete only flips true once requiredHits is reached, not after 1 or 2`() {
        var state = SlingshotGameState(requiredHits = 3)
        state = SlingshotGame.onStoneReleased(state, aimedPosition = 0.5f, markPosition = 0.5f, shieldMinFraction = shieldMin, shieldMaxFraction = shieldMax)
        assertFalse(state.isComplete)

        state = SlingshotGame.onStoneReleased(state, aimedPosition = 0.5f, markPosition = 0.5f, shieldMinFraction = shieldMin, shieldMaxFraction = shieldMax)
        assertFalse(state.isComplete)

        state = SlingshotGame.onStoneReleased(state, aimedPosition = 0.5f, markPosition = 0.5f, shieldMinFraction = shieldMin, shieldMaxFraction = shieldMax)
        assertEquals(3, state.hits)
        assertTrue(state.isComplete)
    }

    @Test
    fun `a release just inside the tolerance counts as a hit, just outside does not`() {
        val justInside = SlingshotGame.onStoneReleased(
            SlingshotGameState(),
            aimedPosition = 0.5f,
            markPosition = 0.5f + SlingshotGame.HIT_TOLERANCE - 0.01f,
            shieldMinFraction = shieldMin,
            shieldMaxFraction = shieldMax,
        )
        val justOutside = SlingshotGame.onStoneReleased(
            SlingshotGameState(),
            aimedPosition = 0.5f,
            markPosition = 0.5f + SlingshotGame.HIT_TOLERANCE + 0.01f,
            shieldMinFraction = shieldMin,
            shieldMaxFraction = shieldMax,
        )

        assertEquals(SlingshotOutcome.HIT, justInside.lastOutcome)
        assertEquals(SlingshotOutcome.MISS, justOutside.lastOutcome)
    }

    @Test
    fun `a mark just inside the shield counts as a hit, just outside does not`() {
        val justInside = SlingshotGame.onStoneReleased(
            SlingshotGameState(),
            aimedPosition = shieldMin,
            markPosition = shieldMin,
            shieldMinFraction = shieldMin,
            shieldMaxFraction = shieldMax,
        )
        val justOutside = SlingshotGame.onStoneReleased(
            SlingshotGameState(),
            aimedPosition = shieldMin - 0.02f,
            markPosition = shieldMin - 0.02f,
            shieldMinFraction = shieldMin,
            shieldMaxFraction = shieldMax,
        )

        assertEquals(SlingshotOutcome.HIT, justInside.lastOutcome)
        assertEquals(SlingshotOutcome.MISS, justOutside.lastOutcome)
    }

    @Test
    fun `once complete, further releases are a no-op`() {
        var state = SlingshotGameState(requiredHits = 1)
        state = SlingshotGame.onStoneReleased(state, aimedPosition = 0.5f, markPosition = 0.5f, shieldMinFraction = shieldMin, shieldMaxFraction = shieldMax)
        val afterComplete = state
        assertTrue(afterComplete.isComplete)

        state = SlingshotGame.onStoneReleased(state, aimedPosition = 0.1f, markPosition = 0.9f, shieldMinFraction = shieldMin, shieldMaxFraction = shieldMax)

        assertEquals(afterComplete, state)
    }
}
