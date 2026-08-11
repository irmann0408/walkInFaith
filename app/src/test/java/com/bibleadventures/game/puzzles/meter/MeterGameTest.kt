package com.bibleadventures.game.puzzles.meter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MeterGameTest {

    private fun freshState(): MeterGameState = MeterGameState(requiredProgress = 10)

    @Test
    fun `a PERFECT tap adds more progress than a GOOD tap`() {
        val perfect = MeterGame.onTapped(freshState(), TapPrecision.PERFECT)
        val good = MeterGame.onTapped(freshState(), TapPrecision.GOOD)

        assertTrue(perfect.progress > good.progress)
    }

    @Test
    fun `an EARLY_OR_LATE tap still adds positive progress`() {
        val next = MeterGame.onTapped(freshState(), TapPrecision.EARLY_OR_LATE)

        assertTrue(next.progress > 0)
        assertEquals(TapPrecision.EARLY_OR_LATE, next.lastPrecision)
    }

    @Test
    fun `progress never exceeds requiredProgress`() {
        var state = MeterGameState(requiredProgress = 3)
        repeat(5) { state = MeterGame.onTapped(state, TapPrecision.PERFECT) }

        assertEquals(3, state.progress)
        assertEquals(1f, state.progressFraction, 0.001f)
    }

    @Test
    fun `isComplete becomes true once enough progress accumulates`() {
        var state = MeterGameState(requiredProgress = 4)
        state = MeterGame.onTapped(state, TapPrecision.GOOD)
        assertTrue(!state.isComplete)

        state = MeterGame.onTapped(state, TapPrecision.PERFECT)
        state = MeterGame.onTapped(state, TapPrecision.GOOD)

        assertTrue(state.isComplete)
    }

    @Test
    fun `once complete, further taps are a no-op`() {
        var state = MeterGameState(requiredProgress = 2)
        state = MeterGame.onTapped(state, TapPrecision.PERFECT)
        assertTrue(state.isComplete)

        val unchanged = MeterGame.onTapped(state, TapPrecision.PERFECT)

        assertEquals(state, unchanged)
    }
}
