package com.bibleadventures.game.puzzles.sequence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SequenceGameTest {

    private val pointIds = listOf("point_1", "point_2", "point_3", "point_4", "point_5")

    @Test
    fun `tapping the next expected point connects it and reports POINT_CONNECTED`() {
        val state = SequenceGame.onPointTapped(SequenceGameState(pointIds), tappedId = "point_1")

        assertEquals(SequenceOutcome.POINT_CONNECTED, state.lastOutcome)
        assertEquals(listOf("point_1"), state.connectedIds)
        assertFalse(state.isComplete)
    }

    @Test
    fun `tapping out of order reports OUT_OF_ORDER and never undoes prior progress`() {
        var state = SequenceGame.onPointTapped(SequenceGameState(pointIds), tappedId = "point_1")
        state = SequenceGame.onPointTapped(state, tappedId = "point_3") // out of order, expected point_2

        assertEquals(SequenceOutcome.OUT_OF_ORDER, state.lastOutcome)
        assertEquals(listOf("point_1"), state.connectedIds)
        assertEquals("point_2", state.nextExpectedId)
    }

    @Test
    fun `re-tapping an already-connected point is a no-op`() {
        var state = SequenceGame.onPointTapped(SequenceGameState(pointIds), tappedId = "point_1")
        val afterFirstTap = state

        state = SequenceGame.onPointTapped(state, tappedId = "point_1")

        assertEquals(afterFirstTap, state)
    }

    @Test
    fun `tapping every point in order completes the sequence`() {
        var state = SequenceGameState(pointIds)
        pointIds.forEach { id -> state = SequenceGame.onPointTapped(state, tappedId = id) }

        assertEquals(SequenceOutcome.COMPLETE, state.lastOutcome)
        assertTrue(state.isComplete)
        assertEquals(null, state.nextExpectedId)
    }

    @Test
    fun `once complete, further taps are a no-op`() {
        var state = SequenceGameState(listOf("point_1"))
        state = SequenceGame.onPointTapped(state, tappedId = "point_1")
        val afterComplete = state

        state = SequenceGame.onPointTapped(state, tappedId = "point_1")

        assertEquals(afterComplete, state)
    }
}
