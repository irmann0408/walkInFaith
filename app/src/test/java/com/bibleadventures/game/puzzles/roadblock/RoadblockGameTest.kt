package com.bibleadventures.game.puzzles.roadblock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoadblockGameTest {

    private fun stateOf(vararg blocks: Block, protagonistId: String = "p", exitColumns: Set<Int> = setOf(2), rows: Int = 6, cols: Int = 5): RoadblockGameState =
        RoadblockGameState(rows = rows, cols = cols, blocks = blocks.toList(), protagonistId = protagonistId, exitColumns = exitColumns)

    // VERTICAL: the protagonist is confined to its own column, exactly like every
    // other block is confined to its own axis — never a free 4-directional mover.
    private val protagonist = Block(id = "p", origin = CellPosition(0, 2), length = 1, orientation = Orientation.VERTICAL)
    private val horizontalBlock = Block(id = "h", origin = CellPosition(2, 1), length = 2, orientation = Orientation.HORIZONTAL)
    private val verticalBlock = Block(id = "v", origin = CellPosition(2, 2), length = 2, orientation = Orientation.VERTICAL)
    private val fixedBlock = Block(id = "f", origin = CellPosition(3, 2), length = 1, orientation = Orientation.HORIZONTAL, isFixed = true)

    @Test
    fun `fromLayout infers a horizontal block, a vertical block, a fixed block, and the protagonist's single cell`() {
        val layout = listOf(
            "..P..",
            ".....",
            ".HH..",
            "..F..",
            "..V..",
            "..V..",
        )
        val specs = listOf(
            RoadblockBlockSpec("p", 'P', forcedOrientation = Orientation.VERTICAL),
            RoadblockBlockSpec("h", 'H'),
            RoadblockBlockSpec("f", 'F', isFixed = true),
            RoadblockBlockSpec("v", 'V'),
        )

        val state = RoadblockGame.fromLayout(layout, specs, protagonistId = "p", exitColumns = setOf(2))

        assertEquals(6, state.rows)
        assertEquals(5, state.cols)
        val p = state.blocks.first { it.id == "p" }
        assertEquals(CellPosition(0, 2), p.origin)
        assertEquals(1, p.length)
        assertEquals(Orientation.VERTICAL, p.orientation)
        val h = state.blocks.first { it.id == "h" }
        assertEquals(Orientation.HORIZONTAL, h.orientation)
        assertEquals(CellPosition(2, 1), h.origin)
        assertEquals(2, h.length)
        val f = state.blocks.first { it.id == "f" }
        assertTrue(f.isFixed)
        val v = state.blocks.first { it.id == "v" }
        assertEquals(Orientation.VERTICAL, v.orientation)
        assertEquals(CellPosition(4, 2), v.origin)
        assertEquals(2, v.length)
    }

    @Test(expected = IllegalStateException::class)
    fun `fromLayout rejects a single-cell block with no forcedOrientation`() {
        RoadblockGame.fromLayout(
            layout = listOf("P"),
            blockSpecs = listOf(RoadblockBlockSpec("p", 'P')),
            protagonistId = "p",
            exitColumns = setOf(0),
        )
    }

    @Test
    fun `isComplete is false until the protagonist exits`() {
        assertFalse(stateOf(protagonist).isComplete)
    }

    @Test
    fun `a horizontal block cannot slide UP or DOWN even with open cells`() {
        val state = stateOf(protagonist, horizontalBlock)
        assertEquals(0, RoadblockGame.maxSlideDistance(state, "h", Direction.UP))
        assertEquals(0, RoadblockGame.maxSlideDistance(state, "h", Direction.DOWN))
    }

    @Test
    fun `a vertical block cannot slide LEFT or RIGHT even with open cells`() {
        val state = stateOf(protagonist, verticalBlock)
        assertEquals(0, RoadblockGame.maxSlideDistance(state, "v", Direction.LEFT))
        assertEquals(0, RoadblockGame.maxSlideDistance(state, "v", Direction.RIGHT))
    }

    @Test
    fun `a block's slide along its own axis stops exactly at the grid edge`() {
        val state = stateOf(protagonist, horizontalBlock) // cols 1-2 in a 5-wide (0..4) board
        assertEquals(1, RoadblockGame.maxSlideDistance(state, "h", Direction.LEFT)) // stops at cols 0-1
        assertEquals(2, RoadblockGame.maxSlideDistance(state, "h", Direction.RIGHT)) // stops at cols 3-4
    }

    @Test
    fun `a block's slide stops exactly one cell short of another block, never overlapping`() {
        val blocker = Block(id = "blocker", origin = CellPosition(2, 4), length = 1, orientation = Orientation.HORIZONTAL)
        val state = stateOf(protagonist, horizontalBlock, blocker)
        // Unobstructed this would reach cols 3-4 (distance 2) — the blocker at col 4 caps it one cell short.
        assertEquals(1, RoadblockGame.maxSlideDistance(state, "h", Direction.RIGHT))
    }

    @Test
    fun `the fixed block never slides in any direction`() {
        val state = stateOf(protagonist, fixedBlock)
        Direction.entries.forEach { direction ->
            assertEquals(0, RoadblockGame.maxSlideDistance(state, "f", direction))
        }
    }

    @Test
    fun `onSlideAttempted on the fixed block is a full no-op besides BLOCKED`() {
        val state = stateOf(protagonist, fixedBlock)
        val next = RoadblockGame.onSlideAttempted(state, "f", Direction.DOWN, 3)
        assertEquals(state.blocks, next.blocks)
        assertEquals(RoadblockOutcome.BLOCKED, next.lastOutcome)
    }

    @Test
    fun `the protagonist is axis-locked exactly like every other block — never a free 4-directional mover`() {
        // This is the whole point of the puzzle: if the protagonist could
        // dodge sideways, the player could route around every obstacle
        // without ever needing to clear one.
        val state = stateOf(protagonist.copy(origin = CellPosition(3, 2)))
        assertEquals(0, RoadblockGame.maxSlideDistance(state, "p", Direction.LEFT))
        assertEquals(0, RoadblockGame.maxSlideDistance(state, "p", Direction.RIGHT))
        assertTrue(RoadblockGame.maxSlideDistance(state, "p", Direction.UP) > 0)
        assertTrue(RoadblockGame.maxSlideDistance(state, "p", Direction.DOWN) > 0)
    }

    @Test
    fun `the protagonist stops one cell short of a blocker along its own axis`() {
        val centered = protagonist.copy(origin = CellPosition(3, 2))
        val up = Block("up", CellPosition(1, 2), 1, Orientation.HORIZONTAL)
        val down = Block("down", CellPosition(5, 2), 1, Orientation.HORIZONTAL)
        val state = stateOf(centered, up, down)

        assertEquals(1, RoadblockGame.maxSlideDistance(state, "p", Direction.UP))
        assertEquals(1, RoadblockGame.maxSlideDistance(state, "p", Direction.DOWN))
    }

    @Test
    fun `the protagonist is blocked by the bottom wall at a non-gate column`() {
        val state = stateOf(protagonist.copy(origin = CellPosition(4, 0))) // col 0 is not in exitColumns={2}
        assertEquals(1, RoadblockGame.maxSlideDistance(state, "p", Direction.DOWN)) // reaches row 5, no further
    }

    @Test
    fun `the protagonist exits through a gate column`() {
        val state = stateOf(protagonist.copy(origin = CellPosition(4, 2))) // col 2 is in exitColumns={2}
        assertEquals(2, RoadblockGame.maxSlideDistance(state, "p", Direction.DOWN)) // row 5, then off-grid

        val next = RoadblockGame.onSlideAttempted(state, "p", Direction.DOWN, 2)
        assertTrue(next.exitedProtagonist)
        assertEquals(RoadblockOutcome.EXITED, next.lastOutcome)
        assertTrue(next.isComplete)
        // Regression: an EXITED commit must still record the block's final
        // position — otherwise a screen resetting its own drag offset to zero
        // afterward has nothing to hold the visual in place, and the tile
        // visibly snaps straight back to where the drag started.
        assertEquals(CellPosition(6, 2), next.blocks.first { it.id == "p" }.origin)
    }

    @Test
    fun `a non-protagonist block can never pass the bottom edge, even aligned with a gate column`() {
        // Must be VERTICAL to be allowed to move DOWN at all (otherwise the axis lock,
        // not the bottom wall, is what's actually being tested).
        val atGateColumn = Block(id = "v2", origin = CellPosition(4, 2), length = 1, orientation = Orientation.VERTICAL)
        val state = stateOf(protagonist, atGateColumn)
        assertEquals(1, RoadblockGame.maxSlideDistance(state, "v2", Direction.DOWN)) // the wall stops it at row 5, same as any other column
    }

    @Test
    fun `onSlideAttempted clamps an over-large distance down to the legal maximum`() {
        val state = stateOf(protagonist, horizontalBlock)
        val next = RoadblockGame.onSlideAttempted(state, "h", Direction.LEFT, 99)
        assertEquals(CellPosition(2, 0), next.blocks.first { it.id == "h" }.origin)
        assertEquals(RoadblockOutcome.MOVED, next.lastOutcome)
    }

    @Test
    fun `onSlideAttempted with 0 legal cells only changes lastOutcome to BLOCKED`() {
        val state = stateOf(protagonist, horizontalBlock)
        val next = RoadblockGame.onSlideAttempted(state, "h", Direction.UP, 1) // axis-locked
        assertEquals(state.blocks, next.blocks)
        assertEquals(RoadblockOutcome.BLOCKED, next.lastOutcome)
    }

    @Test
    fun `once complete, every maxSlideDistance is 0 and onSlideAttempted is a full no-op`() {
        val state = stateOf(protagonist.copy(origin = CellPosition(4, 2)), horizontalBlock)
        val complete = RoadblockGame.onSlideAttempted(state, "p", Direction.DOWN, 2)
        assertTrue(complete.isComplete)

        assertEquals(0, RoadblockGame.maxSlideDistance(complete, "p", Direction.UP))
        assertEquals(0, RoadblockGame.maxSlideDistance(complete, "h", Direction.LEFT))
        assertEquals(complete, RoadblockGame.onSlideAttempted(complete, "h", Direction.LEFT, 1))
    }

    @Test
    fun `two blocks with non-overlapping motion ranges can each reach their own true max`() {
        val far = Block(id = "far", origin = CellPosition(0, 0), length = 1, orientation = Orientation.HORIZONTAL)
        val state = stateOf(protagonist, horizontalBlock, far)
        assertEquals(1, RoadblockGame.maxSlideDistance(state, "h", Direction.LEFT))
        assertEquals(2, RoadblockGame.maxSlideDistance(state, "h", Direction.RIGHT))
    }

    // A rectangular (multi-cell) protagonist, matching the actual game content —
    // "let's make the priest/levite tile a rectangle just like the rest of the
    // tiles" — must be able to hang half off the board as a real intermediate
    // state, only fully exiting once every one of its cells has cleared.

    @Test
    fun `a length-2 protagonist can hang partially off the board as a legal intermediate move`() {
        // origin row 3-4 in a 6-row (0..5) board: the bottom cell reaches row 5
        // (the last valid row) after 1 step, row 6 (off-grid) after 2.
        val twoCell = Block(id = "p", origin = CellPosition(3, 2), length = 2, orientation = Orientation.VERTICAL)
        val state = stateOf(twoCell)

        val next = RoadblockGame.onSlideAttempted(state, "p", Direction.DOWN, 2)
        assertFalse(next.exitedProtagonist)
        assertEquals(RoadblockOutcome.MOVED, next.lastOutcome)
        assertEquals(CellPosition(5, 2), next.blocks.first { it.id == "p" }.origin) // bottom cell now at row 6, off-grid; top cell still at row 5
    }

    @Test
    fun `a length-2 protagonist only fully exits once every cell has cleared the bottom edge`() {
        val twoCell = Block(id = "p", origin = CellPosition(3, 2), length = 2, orientation = Orientation.VERTICAL)
        val state = stateOf(twoCell)

        // Top cell starts row 3, bottom row 4 — needs the TOP cell (the trailing
        // one) to reach row 6 for a full exit, i.e. distance 3, not 2.
        assertEquals(3, RoadblockGame.maxSlideDistance(state, "p", Direction.DOWN))

        val partial = RoadblockGame.onSlideAttempted(state, "p", Direction.DOWN, 2)
        assertFalse(partial.isComplete)

        val full = RoadblockGame.onSlideAttempted(partial, "p", Direction.DOWN, 1)
        assertTrue(full.exitedProtagonist)
        assertEquals(RoadblockOutcome.EXITED, full.lastOutcome)
    }

    @Test
    fun `a length-2 protagonist cannot exit through a non-gate column even partially`() {
        val twoCell = Block(id = "p", origin = CellPosition(3, 0), length = 2, orientation = Orientation.VERTICAL) // col 0, not in exitColumns={2}
        val state = stateOf(twoCell)
        assertEquals(1, RoadblockGame.maxSlideDistance(state, "p", Direction.DOWN)) // stops at the wall, row 5 — no partial hang-off allowed off-gate
    }

    // Generic engine proof of the requested "slide a vertical tile to enable
    // sliding a horizontal tile" dependency — the real chained puzzle content
    // lives in GoodSamaritanContent/GoodSamaritanViewModelTest, but this
    // confirms the underlying rule with a minimal, self-contained fixture.
    @Test
    fun `a horizontal block's escape only opens up after a vertical blocker sharing its destination cell moves away`() {
        val mover = Block(id = "h", origin = CellPosition(1, 1), length = 2, orientation = Orientation.HORIZONTAL) // cells (1,1),(1,2)
        // Row 1 is this block's own *leading* row for a downward slide, so a single
        // step down is enough to vacate it — sliding a 2-cell block by less than its
        // own length never clears a row at its *trailing* edge, only its leading one.
        val blocker = Block(id = "v", origin = CellPosition(1, 3), length = 2, orientation = Orientation.VERTICAL) // cells (1,3),(2,3) — sits exactly where "h" needs to slide into
        var state = stateOf(protagonist, mover, blocker)

        // "h" wants to clear column 2 by sliding right onto (1,2),(1,3) — blocked, (1,3) is occupied.
        assertEquals(0, RoadblockGame.maxSlideDistance(state, "h", Direction.RIGHT))

        // Slide the vertical blocker down and out of row 1 entirely.
        state = RoadblockGame.onSlideAttempted(state, "v", Direction.DOWN, 1)
        assertEquals(CellPosition(2, 3), state.blocks.first { it.id == "v" }.origin)

        // Now "h" can make the exact same move that was illegal a moment ago
        // (it can actually go further now, but 1 cell is all it needs to clear).
        assertTrue(RoadblockGame.maxSlideDistance(state, "h", Direction.RIGHT) >= 1)
        val next = RoadblockGame.onSlideAttempted(state, "h", Direction.RIGHT, 1)
        assertEquals(CellPosition(1, 2), next.blocks.first { it.id == "h" }.origin)
    }
}
