package com.bibleadventures.game.puzzles.slingshot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SlingshotGameTest {

    private val shieldMin = 0.35f
    private val shieldMax = 0.65f

    @Test
    fun `a release within tolerance and inside the shield reports HIT and completes the game`() {
        val state = SlingshotGame.onStoneReleased(
            SlingshotGameState(),
            aimedPosition = 0.5f,
            markPosition = 0.55f,
            shieldMinFraction = shieldMin,
            shieldMaxFraction = shieldMax,
        )

        assertEquals(SlingshotOutcome.HIT, state.lastOutcome)
        assertTrue(state.isHit)
        assertTrue(state.isComplete)
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
        assertFalse(state.isHit)
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
        assertFalse(state.isHit)
    }

    @Test
    fun `a miss does not block a later hit`() {
        var state = SlingshotGameState()
        state = SlingshotGame.onStoneReleased(state, aimedPosition = 0.1f, markPosition = 0.8f, shieldMinFraction = shieldMin, shieldMaxFraction = shieldMax)
        state = SlingshotGame.onStoneReleased(state, aimedPosition = 0.5f, markPosition = 0.5f, shieldMinFraction = shieldMin, shieldMaxFraction = shieldMax)

        assertEquals(SlingshotOutcome.HIT, state.lastOutcome)
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
    fun `once hit, further releases are a no-op`() {
        var state = SlingshotGame.onStoneReleased(SlingshotGameState(), aimedPosition = 0.5f, markPosition = 0.5f, shieldMinFraction = shieldMin, shieldMaxFraction = shieldMax)
        val afterHit = state

        state = SlingshotGame.onStoneReleased(state, aimedPosition = 0.1f, markPosition = 0.9f, shieldMinFraction = shieldMin, shieldMaxFraction = shieldMax)

        assertEquals(afterHit, state)
    }
}
