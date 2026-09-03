package com.bibleadventures.game.puzzles.slingshot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SlingshotGameTest {

    private val anchor = Vector2(0.5f, 0.9f)
    private val ratAbove = Vector2(0.5f, 0.2f)
    private val pullDown = Vector2(0f, 0.3f) // pulled down -> launches straight up
    private val pullUp = Vector2(0f, -0.3f) // pulled up, toward the rat -> launches away from it

    @Test
    fun `wouldHit and onStoneReleased agree on a straight hit`() {
        assertTrue(SlingshotGame.wouldHit(anchor, pullDown, ratAbove))

        val state = SlingshotGame.onStoneReleased(SlingshotGameState(), anchor, pullDown, ratAbove)
        assertEquals(SlingshotOutcome.HIT, state.lastOutcome)
    }

    @Test
    fun `pulling toward the rat launches away from it and misses`() {
        assertFalse(SlingshotGame.wouldHit(anchor, pullUp, ratAbove))

        val state = SlingshotGame.onStoneReleased(SlingshotGameState(), anchor, pullUp, ratAbove)
        assertEquals(SlingshotOutcome.MISS, state.lastOutcome)
        assertEquals(0, state.hits)
        assertEquals(0, state.ratsSpawned)
    }

    @Test
    fun `pulling southwest launches northeast`() {
        val rat = Vector2(0.8f, 0.6f) // up and to the right of the anchor
        val pull = Vector2(-0.3f, 0.3f) // pulled down-left (southwest)

        assertTrue(SlingshotGame.wouldHit(anchor, pull, rat))
    }

    @Test
    fun `a rat too far off the launch ray is a miss`() {
        assertFalse(SlingshotGame.wouldHit(anchor, pullDown, Vector2(0.9f, 0.2f)))
    }

    @Test
    fun `a rat just inside the tolerance counts as a hit, just outside does not`() {
        val justInside = Vector2(anchor.x + SlingshotGame.HIT_TOLERANCE - 0.01f, 0.2f)
        val justOutside = Vector2(anchor.x + SlingshotGame.HIT_TOLERANCE + 0.01f, 0.2f)

        assertTrue(SlingshotGame.wouldHit(anchor, pullDown, justInside))
        assertFalse(SlingshotGame.wouldHit(anchor, pullDown, justOutside))
    }

    @Test
    fun `a pull shorter than MIN_PULL_DISTANCE is not a real shot and changes nothing`() {
        val tinyPull = Vector2(0f, 0.01f)

        assertFalse(SlingshotGame.wouldHit(anchor, tinyPull, ratAbove))
        assertEquals(SlingshotGameState(), SlingshotGame.onStoneReleased(SlingshotGameState(), anchor, tinyPull, ratAbove))
    }

    @Test
    fun `a hit increases both hits and ratsSpawned, a miss increases neither`() {
        var state = SlingshotGameState()
        state = SlingshotGame.onStoneReleased(state, anchor, pullDown, ratAbove)
        assertEquals(1, state.hits)
        assertEquals(1, state.ratsSpawned)

        state = SlingshotGame.onStoneReleased(state, anchor, pullUp, ratAbove)
        assertEquals(SlingshotOutcome.MISS, state.lastOutcome)
        assertEquals(1, state.hits)
        assertEquals(1, state.ratsSpawned)
    }

    @Test
    fun `an escaped rat advances ratsSpawned without counting as a hit, and doesn't count toward completion`() {
        val state = SlingshotGame.onRatEscaped(SlingshotGameState(requiredHits = 1))

        assertEquals(SlingshotOutcome.ESCAPED, state.lastOutcome)
        assertEquals(0, state.hits)
        assertEquals(1, state.ratsSpawned)
        assertFalse(state.isComplete)
    }

    @Test
    fun `isComplete depends only on hits reaching requiredHits, escapes don't help or hurt`() {
        var state = SlingshotGameState(requiredHits = 2)
        state = SlingshotGame.onRatEscaped(state)
        state = SlingshotGame.onRatEscaped(state)
        state = SlingshotGame.onRatEscaped(state)
        assertEquals(3, state.ratsSpawned)
        assertFalse(state.isComplete)

        state = SlingshotGame.onStoneReleased(state, anchor, pullDown, ratAbove)
        assertFalse(state.isComplete)

        state = SlingshotGame.onStoneReleased(state, anchor, pullDown, ratAbove)
        assertEquals(2, state.hits)
        assertTrue(state.isComplete)
    }

    @Test
    fun `once complete, further stone releases and escapes are a no-op`() {
        var state = SlingshotGameState(requiredHits = 1)
        state = SlingshotGame.onStoneReleased(state, anchor, pullDown, ratAbove)
        val afterComplete = state
        assertTrue(afterComplete.isComplete)

        state = SlingshotGame.onStoneReleased(state, anchor, pullUp, ratAbove)
        assertEquals(afterComplete, state)

        state = SlingshotGame.onRatEscaped(state)
        assertEquals(afterComplete, state)
    }
}
