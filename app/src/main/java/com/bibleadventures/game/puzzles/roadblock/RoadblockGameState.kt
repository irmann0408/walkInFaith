package com.bibleadventures.game.puzzles.roadblock

enum class Direction { UP, DOWN, LEFT, RIGHT }

enum class Orientation { HORIZONTAL, VERTICAL }

/** Never FAILED — a slide that can't go anywhere is BLOCKED, not lost progress; every move stays fully reversible (spec section 9). */
enum class RoadblockOutcome { NONE, MOVED, BLOCKED, EXITED }

data class CellPosition(val row: Int, val col: Int)

/**
 * A rectangular piece occupying [length] consecutive cells starting at
 * [origin], running along [orientation]. [isFixed] blocks (the injured
 * man) are never movable regardless of anything else. Genuine Rush-Hour/
 * Unblock-Me rule: **every** block, including the one whose id equals
 * [RoadblockGameState.protagonistId], is locked to sliding along its own
 * [orientation]'s axis by [RoadblockGame] — the protagonist is never a
 * free 4-directional mover (an earlier version of this engine let it move
 * any direction, which let the player route around every obstacle without
 * ever needing to clear one, defeating the whole puzzle). The protagonist
 * gets exactly one privilege instead: it's the only block ever allowed to
 * leave the grid, through [RoadblockGameState.exitColumns] — and it can be
 * any [length], not just one cell: a multi-cell protagonist slides off the
 * bottom progressively (one cell can be past the edge while the rest of
 * it is still legally on the board), fully exiting only once every one of
 * its cells has cleared — see [RoadblockGame.maxSlideDistance]. Since a
 * single-cell block's [orientation] can't be inferred from its own shape,
 * content authors it explicitly via [RoadblockBlockSpec.forcedOrientation]
 * — pick whichever axis leads to the exit (VERTICAL for a bottom gate).
 */
data class Block(
    val id: String,
    val origin: CellPosition,
    val length: Int,
    val orientation: Orientation,
    val isFixed: Boolean = false,
) {
    val cells: List<CellPosition>
        get() = (0 until length).map { offset ->
            if (orientation == Orientation.HORIZONTAL) origin.copy(col = origin.col + offset) else origin.copy(row = origin.row + offset)
        }
}

/**
 * A Rush-Hour/Unblock-Me-style sliding block board. [exitColumns] are the
 * only columns where the bottom edge (row >= [rows]) isn't a wall — and
 * even there, only [protagonistId]'s block may ever pass through it; for
 * every other block the bottom edge is an unconditional wall regardless
 * of column. Deliberately kept generic (no chapter-specific concepts) so
 * any future rush-hour-shaped scene can reuse it — see
 * `GoodSamaritanContent` for this app's first, chapter-specific board.
 */
data class RoadblockGameState(
    val rows: Int,
    val cols: Int,
    val blocks: List<Block>,
    val protagonistId: String,
    val exitColumns: Set<Int>,
    val exitedProtagonist: Boolean = false,
    val lastOutcome: RoadblockOutcome = RoadblockOutcome.NONE,
) {
    val isComplete: Boolean get() = exitedProtagonist
}

/**
 * Maps one ASCII layout letter to the block id/fixedness a content file
 * wants for it — see [RoadblockGame.fromLayout]. [forcedOrientation]
 * overrides shape-based inference, needed for any single-cell block (most
 * notably the protagonist) since one cell has no shape to infer an axis
 * from.
 */
data class RoadblockBlockSpec(val id: String, val letter: Char, val isFixed: Boolean = false, val forcedOrientation: Orientation? = null)

/** One step of a hand-verified solution — mirrors `GoodSamaritanContent.solutionPath`'s role for the grid maze. */
data class RoadblockMove(val blockId: String, val direction: Direction, val distance: Int)
