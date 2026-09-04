package com.bibleadventures.game.puzzles.slideout

/**
 * Pure transition logic for the "Arrow Block: Slide Out" puzzle — no
 * Compose/Android dependency. The screen owns all fly-off/shake animation
 * timing and only commits a tap's result immediately, then plays the
 * matching cosmetic animation afterward (same "engine judges instantly,
 * screen animates on top" split as every other tap-driven engine in this
 * app, e.g. `RoadblockGame.onSlideAttempted`).
 */
object SlideOutGame {

    /**
     * Content-authoring helper for a dense, fully (or mostly) tiled board:
     * calls [direction] once per cell of a [rows] x [cols] grid, creating a
     * latch there unless it returns null (an empty background cell). Ids
     * are deterministic from position (`"latch_<row>_<col>"`), for stable
     * identity across recompositions and in tests — same convention as
     * `DungeonGame.fromLayout`'s `"trap_<row>_<col>"` ids.
     */
    fun fromGrid(rows: Int, cols: Int, direction: (row: Int, col: Int) -> SlideDirection?): SlideOutGameState {
        val blocks = mutableListOf<LatchBlock>()
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val cellDirection = direction(row, col) ?: continue
                blocks += LatchBlock(id = "latch_${row}_$col", position = CellPosition(row, col), direction = cellDirection)
            }
        }
        return SlideOutGameState(rows = rows, cols = cols, blocks = blocks)
    }

    /** True if [blockId]'s straight-line path from its own cell to the board edge, along its own direction, is clear of every other still-present latch. False if the id isn't a currently-remaining latch. */
    fun canRelease(state: SlideOutGameState, blockId: String): Boolean {
        val block = state.blocks.firstOrNull { it.id == blockId } ?: return false
        val occupied = state.blocks.filter { it.id != blockId }.map { it.position }.toSet()
        var cell = block.position
        while (true) {
            cell = cell.shiftedBy(block.direction)
            if (cell.row !in 0 until state.rows || cell.col !in 0 until state.cols) return true
            if (cell in occupied) return false
        }
    }

    /**
     * Tapping a latch: a clear path removes it (reporting [SlideOutOutcome.COMPLETE]
     * if it was the last one remaining, else [SlideOutOutcome.RELEASED]) and
     * clears [SlideOutGameState.lastBlockedId]. A blocked path leaves
     * [SlideOutGameState.blocks] untouched, reporting
     * [SlideOutOutcome.BLOCKED] with [blockId] recorded as the stuck latch.
     * A no-op (state returned as-is, [SlideOutGameState.lastOutcome]
     * unchanged) if [blockId] isn't currently a remaining latch, or the
     * puzzle is already complete.
     */
    fun onBlockTapped(state: SlideOutGameState, blockId: String): SlideOutGameState {
        if (state.isComplete) return state
        if (state.blocks.none { it.id == blockId }) return state

        return if (canRelease(state, blockId)) {
            val remainingBlocks = state.blocks.filter { it.id != blockId }
            state.copy(
                blocks = remainingBlocks,
                lastOutcome = if (remainingBlocks.isEmpty()) SlideOutOutcome.COMPLETE else SlideOutOutcome.RELEASED,
                lastBlockedId = null,
            )
        } else {
            state.copy(lastOutcome = SlideOutOutcome.BLOCKED, lastBlockedId = blockId)
        }
    }

    private fun CellPosition.shiftedBy(direction: SlideDirection): CellPosition = when (direction) {
        SlideDirection.UP -> copy(row = row - 1)
        SlideDirection.DOWN -> copy(row = row + 1)
        SlideDirection.LEFT -> copy(col = col - 1)
        SlideDirection.RIGHT -> copy(col = col + 1)
    }
}
