package com.bibleadventures.game.puzzles.matching

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private val lionA = MatchItem("lion_a", iconRes = 1, contentDescriptionRes = 1, pairKey = "lion")
private val lionB = MatchItem("lion_b", iconRes = 1, contentDescriptionRes = 1, pairKey = "lion")
private val birdA = MatchItem("bird_a", iconRes = 2, contentDescriptionRes = 2, pairKey = "bird")
private val birdB = MatchItem("bird_b", iconRes = 2, contentDescriptionRes = 2, pairKey = "bird")

private fun initialState() = MatchingGameState(items = listOf(lionA, lionB, birdA, birdB))

class MatchingGameTest {

    @Test
    fun `all cards start face down`() {
        val state = initialState()

        assertFalse(state.isFaceUp("lion_a"))
        assertFalse(state.isFaceUp("bird_b"))
    }

    @Test
    fun `tapping the first item flips it face up without a match outcome`() {
        val state = MatchingGame.onItemTapped(initialState(), "lion_a")

        assertEquals(listOf("lion_a"), state.selectedIds)
        assertTrue(state.isFaceUp("lion_a"))
        assertEquals(MatchOutcome.NONE, state.lastOutcome)
        assertTrue(state.matchedIds.isEmpty())
    }

    @Test
    fun `tapping a matching pair marks both matched and reports CORRECT`() {
        var state = initialState()
        state = MatchingGame.onItemTapped(state, "lion_a")
        state = MatchingGame.onItemTapped(state, "lion_b")

        assertEquals(MatchOutcome.CORRECT, state.lastOutcome)
        assertEquals(setOf("lion_a", "lion_b"), state.matchedIds)
        assertTrue(state.selectedIds.isEmpty())
        assertTrue(state.isFaceUp("lion_a"))
        assertTrue(state.isFaceUp("lion_b"))
    }

    @Test
    fun `tapping a non-matching pair reports TRY_AGAIN, never FAILED, and keeps both face up`() {
        var state = initialState()
        state = MatchingGame.onItemTapped(state, "lion_a")
        state = MatchingGame.onItemTapped(state, "bird_a")

        assertEquals(MatchOutcome.TRY_AGAIN, state.lastOutcome)
        assertTrue(state.matchedIds.isEmpty())
        assertEquals(listOf("lion_a", "bird_a"), state.selectedIds)
        assertTrue(state.isFaceUp("lion_a"))
        assertTrue(state.isFaceUp("bird_a"))
    }

    @Test
    fun `the next tap after a mismatch flips the pair back face down and starts a fresh selection`() {
        var state = initialState()
        state = MatchingGame.onItemTapped(state, "lion_a")
        state = MatchingGame.onItemTapped(state, "bird_a")

        state = MatchingGame.onItemTapped(state, "lion_a")

        assertEquals(MatchOutcome.NONE, state.lastOutcome)
        assertEquals(listOf("lion_a"), state.selectedIds)
        assertFalse(state.isFaceUp("bird_a"))
    }

    @Test
    fun `a mismatch does not block progress on the next attempt`() {
        var state = initialState()
        state = MatchingGame.onItemTapped(state, "lion_a")
        state = MatchingGame.onItemTapped(state, "bird_a")
        state = MatchingGame.onItemTapped(state, "lion_a")
        state = MatchingGame.onItemTapped(state, "lion_b")

        assertEquals(MatchOutcome.CORRECT, state.lastOutcome)
        assertEquals(setOf("lion_a", "lion_b"), state.matchedIds)
    }

    @Test
    fun `tapping an already-matched item is a no-op`() {
        var state = initialState()
        state = MatchingGame.onItemTapped(state, "lion_a")
        state = MatchingGame.onItemTapped(state, "lion_b")
        val beforeTap = state

        state = MatchingGame.onItemTapped(state, "lion_a")

        assertEquals(beforeTap, state)
    }

    @Test
    fun `game is complete once every item is matched`() {
        var state = initialState()
        state = MatchingGame.onItemTapped(state, "lion_a")
        state = MatchingGame.onItemTapped(state, "lion_b")
        assertFalse(state.isComplete)

        state = MatchingGame.onItemTapped(state, "bird_a")
        state = MatchingGame.onItemTapped(state, "bird_b")
        assertTrue(state.isComplete)
    }
}
