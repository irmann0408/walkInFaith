package com.bibleadventures.game.puzzles.rhythmlane

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RhythmLaneGameTest {

    private val chart = RhythmLaneChart(
        notes = listOf(
            RhythmNote("n0", lane = 0, hitTimeMs = 400),
            RhythmNote("n1", lane = 1, hitTimeMs = 1200),
            RhythmNote("n2", lane = 2, hitTimeMs = 2000),
        ),
        loopDurationMs = 2400,
    )

    private fun freshState(requiredHits: Int = 10): RhythmLaneGameState =
        RhythmLaneGameState(chart = chart, requiredHits = requiredHits)

    @Test
    fun `tapping the right lane exactly on time is a PERFECT hit`() {
        val next = RhythmLaneGame.onLaneTapped(freshState(), lane = 0, nowMs = 400)

        assertEquals(1, next.hits)
        assertEquals(NoteJudgment.PERFECT, next.lastJudgment)
    }

    @Test
    fun `tapping the right lane a little early or late within the window is a GREAT hit`() {
        val next = RhythmLaneGame.onLaneTapped(freshState(), lane = 0, nowMs = 550)

        assertEquals(1, next.hits)
        assertEquals(NoteJudgment.GREAT, next.lastJudgment)
    }

    @Test
    fun `tapping a lane with no nearby note is a pure no-op`() {
        val state = freshState()
        val next = RhythmLaneGame.onLaneTapped(state, lane = 0, nowMs = 2000)

        assertEquals(state, next)
    }

    @Test
    fun `tapping the wrong lane never matches a note scheduled for another lane`() {
        val state = freshState()
        val next = RhythmLaneGame.onLaneTapped(state, lane = 1, nowMs = 400)

        assertEquals(state, next)
    }

    @Test
    fun `onTimeAdvanced marks a passed note MISSED without reducing hits`() {
        var state = freshState()
        state = RhythmLaneGame.onLaneTapped(state, lane = 1, nowMs = 1200) // one real hit, so hits > 0
        val hitsBefore = state.hits

        state = RhythmLaneGame.onTimeAdvanced(state, nowMs = 2400 + 2000) // well past note n2's window

        assertEquals(hitsBefore, state.hits)
        assertEquals(NoteJudgment.MISSED, state.lastJudgment)
    }

    @Test
    fun `a missed note can still be hit again on the next loop iteration`() {
        var state = freshState()
        state = RhythmLaneGame.onTimeAdvanced(state, nowMs = 1000) // n0 (lane 0) passes unhit in loop 0
        assertEquals(0, state.hits)

        // Same note id, same lane, one loop later (2400ms + 400ms).
        state = RhythmLaneGame.onLaneTapped(state, lane = 0, nowMs = 2400 + 400)

        assertEquals(1, state.hits)
    }

    @Test
    fun `hits never exceeds requiredHits`() {
        var state = freshState(requiredHits = 1)
        state = RhythmLaneGame.onLaneTapped(state, lane = 0, nowMs = 400)
        assertTrue(state.isComplete)

        // Next loop's n0 would normally be a fresh hit, but the game is already complete.
        val unchanged = RhythmLaneGame.onLaneTapped(state, lane = 0, nowMs = 2400 + 400)

        assertEquals(state, unchanged)
    }

    @Test
    fun `isComplete becomes true once requiredHits is reached`() {
        var state = freshState(requiredHits = 2)
        state = RhythmLaneGame.onLaneTapped(state, lane = 0, nowMs = 400)
        assertTrue(!state.isComplete)

        state = RhythmLaneGame.onLaneTapped(state, lane = 1, nowMs = 1200)

        assertTrue(state.isComplete)
    }

    @Test
    fun `once complete, onTimeAdvanced is also a no-op`() {
        var state = freshState(requiredHits = 1)
        state = RhythmLaneGame.onLaneTapped(state, lane = 0, nowMs = 400)
        assertTrue(state.isComplete)

        val unchanged = RhythmLaneGame.onTimeAdvanced(state, nowMs = 10_000)

        assertEquals(state, unchanged)
    }

    @Test
    fun `progressFraction reflects hits relative to requiredHits`() {
        var state = freshState(requiredHits = 4)
        state = RhythmLaneGame.onLaneTapped(state, lane = 0, nowMs = 400)

        assertEquals(0.25f, state.progressFraction, 0.001f)
    }

    @Test
    fun `a fresh state has no last judgment`() {
        assertNull(freshState().lastJudgment)
    }
}
