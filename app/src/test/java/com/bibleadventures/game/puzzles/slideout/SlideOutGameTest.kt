package com.bibleadventures.game.puzzles.slideout

import com.bibleadventures.game.stories.DanielContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SlideOutGameTest {

    // A tiny fixture with one clean outer latch and one latch it blocks:
    //   . A .
    //   . B .
    // A (row 0, col 1) exits UP immediately. B (row 1, col 1) also exits
    // UP, but its path passes straight through A's cell, so B stays
    // BLOCKED until A is released.
    private fun initialState() = SlideOutGame.fromGrid(rows = 2, cols = 3) { row, col ->
        when (row to col) {
            0 to 1 -> SlideDirection.UP
            1 to 1 -> SlideDirection.UP
            else -> null
        }
    }

    @Test
    fun `fromGrid creates a latch only where direction returns non-null, with deterministic ids`() {
        val state = initialState()

        assertEquals(2, state.rows)
        assertEquals(3, state.cols)
        assertEquals(
            listOf(
                LatchBlock(id = "latch_0_1", position = CellPosition(0, 1), direction = SlideDirection.UP),
                LatchBlock(id = "latch_1_1", position = CellPosition(1, 1), direction = SlideDirection.UP),
            ),
            state.blocks,
        )
    }

    @Test
    fun `fromGrid can fill every cell of the grid, for a dense board`() {
        val state = SlideOutGame.fromGrid(rows = 3, cols = 3) { _, _ -> SlideDirection.UP }

        assertEquals(9, state.blocks.size)
    }

    @Test
    fun `canRelease is true for a latch with a clear path to the board edge`() {
        val state = initialState()

        assertTrue(SlideOutGame.canRelease(state, "latch_0_1"))
    }

    @Test
    fun `canRelease is false when another latch sits in the path`() {
        val state = initialState()

        assertFalse(SlideOutGame.canRelease(state, "latch_1_1"))
    }

    @Test
    fun `canRelease is false for an id that is not a currently remaining latch`() {
        val state = initialState()

        assertFalse(SlideOutGame.canRelease(state, "nonexistent"))
    }

    @Test
    fun `tapping a latch with a clear path releases it and removes it from the board`() {
        val state = initialState()

        val result = SlideOutGame.onBlockTapped(state, "latch_0_1")

        assertEquals(SlideOutOutcome.RELEASED, result.lastOutcome)
        assertNull(result.lastBlockedId)
        assertTrue(result.blocks.none { it.id == "latch_0_1" })
        assertEquals(1, result.blocks.size)
    }

    @Test
    fun `tapping a blocked latch changes nothing but the outcome and lastBlockedId`() {
        val state = initialState()

        val result = SlideOutGame.onBlockTapped(state, "latch_1_1")

        assertEquals(SlideOutOutcome.BLOCKED, result.lastOutcome)
        assertEquals("latch_1_1", result.lastBlockedId)
        assertEquals(state.blocks, result.blocks)
    }

    @Test
    fun `an inner latch stays blocked until its outer latch is released, then succeeds`() {
        var state = initialState()

        state = SlideOutGame.onBlockTapped(state, "latch_1_1")
        assertEquals(SlideOutOutcome.BLOCKED, state.lastOutcome)

        state = SlideOutGame.onBlockTapped(state, "latch_0_1")
        assertEquals(SlideOutOutcome.RELEASED, state.lastOutcome)

        state = SlideOutGame.onBlockTapped(state, "latch_1_1")
        assertEquals(SlideOutOutcome.COMPLETE, state.lastOutcome)
        assertTrue(state.isComplete)
    }

    @Test
    fun `releasing the last remaining latch reports COMPLETE instead of RELEASED`() {
        var state = initialState()
        state = SlideOutGame.onBlockTapped(state, "latch_0_1")

        state = SlideOutGame.onBlockTapped(state, "latch_1_1")

        assertEquals(SlideOutOutcome.COMPLETE, state.lastOutcome)
        assertTrue(state.isComplete)
    }

    @Test
    fun `tapping an id that is not a remaining latch is a full no-op`() {
        val state = initialState()

        val result = SlideOutGame.onBlockTapped(state, "nonexistent")

        assertEquals(state, result)
    }

    @Test
    fun `once complete, onBlockTapped is always a full no-op`() {
        var state = initialState()
        state = SlideOutGame.onBlockTapped(state, "latch_0_1")
        state = SlideOutGame.onBlockTapped(state, "latch_1_1")
        val completeState = state

        assertEquals(completeState, SlideOutGame.onBlockTapped(completeState, "latch_0_1"))
    }

    @Test
    fun `replaying the production board's hand-verified solution order reaches isComplete`() {
        var state = SlideOutGame.fromGrid(
            DanielContent.WINDOW_LATCH_ROWS,
            DanielContent.WINDOW_LATCH_COLS,
            DanielContent::windowLatchDirection,
        )
        assertEquals(DanielContent.WINDOW_LATCH_ROWS * DanielContent.WINDOW_LATCH_COLS, state.blocks.size)

        DanielContent.windowLatchSolutionOrder.forEach { latch ->
            state = SlideOutGame.onBlockTapped(state, latch.id)
            check(state.lastOutcome != SlideOutOutcome.BLOCKED) { "Latch '${latch.id}' was unexpectedly blocked — windowLatchSolutionOrder is not actually a valid release order" }
        }

        assertTrue(state.isComplete)
    }

    @Test
    fun `the production board is solvable by greedily releasing any currently-releasable latch, regardless of order`() {
        var state = SlideOutGame.fromGrid(
            DanielContent.WINDOW_LATCH_ROWS,
            DanielContent.WINDOW_LATCH_COLS,
            DanielContent::windowLatchDirection,
        )

        while (!state.isComplete) {
            val releasable = state.blocks.firstOrNull { SlideOutGame.canRelease(state, it.id) }
                ?: error("Deadlocked with ${state.blocks.size} latches remaining and none releasable: ${state.blocks}")
            state = SlideOutGame.onBlockTapped(state, releasable.id)
        }

        assertTrue(state.isComplete)
    }

    @Test
    fun `the production board has genuine directional variety, not every edge row-column pointing straight out`() {
        val state = SlideOutGame.fromGrid(
            DanielContent.WINDOW_LATCH_ROWS,
            DanielContent.WINDOW_LATCH_COLS,
            DanielContent::windowLatchDirection,
        )
        fun directionOf(row: Int, col: Int) = state.blocks.first { it.position == CellPosition(row, col) }.direction

        val topRow = (0 until DanielContent.WINDOW_LATCH_COLS).map { col -> directionOf(0, col) }
        val bottomRow = (0 until DanielContent.WINDOW_LATCH_COLS).map { col -> directionOf(DanielContent.WINDOW_LATCH_ROWS - 1, col) }
        val leftCol = (0 until DanielContent.WINDOW_LATCH_ROWS).map { row -> directionOf(row, 0) }
        val rightCol = (0 until DanielContent.WINDOW_LATCH_ROWS).map { row -> directionOf(row, DanielContent.WINDOW_LATCH_COLS - 1) }

        assertTrue("top row was entirely UP: $topRow", topRow.any { it != SlideDirection.UP })
        assertTrue("bottom row was entirely DOWN: $bottomRow", bottomRow.any { it != SlideDirection.DOWN })
        assertTrue("left column was entirely LEFT: $leftCol", leftCol.any { it != SlideDirection.LEFT })
        assertTrue("right column was entirely RIGHT: $rightCol", rightCol.any { it != SlideDirection.RIGHT })
    }

    @Test
    fun `an arbitrary rectangular fully tiled board using the nearest-edge rule is always solvable`() {
        // Not just the production 6x6 board — confirms the "sort by distance
        // to the chosen edge, ascending" property proven in
        // DanielContent.windowLatchDirection's doc comment generalizes to
        // any grid shape, including a non-square one.
        val rows = 5
        val cols = 8
        fun distanceToEdge(row: Int, col: Int): Int =
            listOf(row, rows - 1 - row, col, cols - 1 - col).min()
        fun direction(row: Int, col: Int): SlideDirection {
            val candidates = listOf(
                SlideDirection.UP to row,
                SlideDirection.DOWN to (rows - 1 - row),
                SlideDirection.LEFT to col,
                SlideDirection.RIGHT to (cols - 1 - col),
            )
            return candidates.minBy { it.second }.first
        }

        var state = SlideOutGame.fromGrid(rows, cols) { row, col -> direction(row, col) }
        val releaseOrder = (0 until rows).flatMap { row -> (0 until cols).map { col -> row to col } }
            .sortedBy { (row, col) -> distanceToEdge(row, col) }

        releaseOrder.forEach { (row, col) ->
            state = SlideOutGame.onBlockTapped(state, "latch_${row}_$col")
            check(state.lastOutcome != SlideOutOutcome.BLOCKED) { "latch_${row}_$col was unexpectedly blocked" }
        }

        assertTrue(state.isComplete)
    }
}
