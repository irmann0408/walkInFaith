package com.bibleadventures.game.puzzles.sudoku

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SudokuGameTest {

    private fun freshState(): SudokuGameState = SudokuGameState(
        size = 3,
        givens = mapOf((0 to 0) to "A"),
    )

    @Test
    fun `a given cell cannot be overwritten`() {
        val next = SudokuGame.onCellFilled(freshState(), 0, 0, "B")

        assertEquals(freshState(), next)
    }

    @Test
    fun `placing a duplicate icon in the same row is rejected without committing`() {
        val next = SudokuGame.onCellFilled(freshState(), 0, 1, "A")

        assertEquals(SudokuOutcome.CONFLICT, next.lastOutcome)
        assertTrue(next.filled.isEmpty())
    }

    @Test
    fun `placing a duplicate icon in the same column is rejected without committing`() {
        val next = SudokuGame.onCellFilled(freshState(), 1, 0, "A")

        assertEquals(SudokuOutcome.CONFLICT, next.lastOutcome)
        assertTrue(next.filled.isEmpty())
    }

    @Test
    fun `a valid placement commits and is PLACED when its row is not yet complete`() {
        val next = SudokuGame.onCellFilled(freshState(), 0, 1, "B")

        assertEquals(SudokuOutcome.PLACED, next.lastOutcome)
        assertEquals("B", next.filled[0 to 1])
    }

    @Test
    fun `completing a row returns ROW_COMPLETE and records it`() {
        var state = freshState()
        state = SudokuGame.onCellFilled(state, 0, 1, "B")
        state = SudokuGame.onCellFilled(state, 0, 2, "C")

        assertEquals(SudokuOutcome.ROW_COMPLETE, state.lastOutcome)
        assertEquals(setOf(0), state.completedRows)
    }

    @Test
    fun `filling every cell in the grid returns COMPLETE`() {
        var state = freshState()
        state = SudokuGame.onCellFilled(state, 0, 1, "B")
        state = SudokuGame.onCellFilled(state, 0, 2, "C")
        state = SudokuGame.onCellFilled(state, 1, 0, "B")
        state = SudokuGame.onCellFilled(state, 1, 1, "C")
        state = SudokuGame.onCellFilled(state, 1, 2, "A")
        state = SudokuGame.onCellFilled(state, 2, 0, "C")
        state = SudokuGame.onCellFilled(state, 2, 1, "A")
        state = SudokuGame.onCellFilled(state, 2, 2, "B")

        assertEquals(SudokuOutcome.COMPLETE, state.lastOutcome)
        assertTrue(state.isComplete)
        assertEquals(setOf(0, 1, 2), state.completedRows)
    }

    @Test
    fun `clearing a placed cell un-marks a completed row`() {
        var state = freshState()
        state = SudokuGame.onCellFilled(state, 0, 1, "B")
        state = SudokuGame.onCellFilled(state, 0, 2, "C")
        assertEquals(setOf(0), state.completedRows)

        state = SudokuGame.onCellCleared(state, 0, 2)

        assertFalse(state.filled.containsKey(0 to 2))
        assertTrue(state.completedRows.isEmpty())
        assertFalse(state.isComplete)
    }

    @Test
    fun `clearing a given cell is a no-op`() {
        val state = freshState()

        val next = SudokuGame.onCellCleared(state, 0, 0)

        assertEquals(state, next)
    }
}
