package com.bibleadventures.game.puzzles.roadblock

/**
 * Pure transition logic — no Compose/Android dependency. The UI owns all
 * drag-gesture handling and clamps a live drag against [maxSlideDistance]
 * before ever calling [onSlideAttempted] (same "screen decides where to
 * visually clamp/snap, engine only judges" split as every other drag-based
 * engine in this app, e.g. `GroupFillGame.canAccept`).
 */
object RoadblockGame {

    /**
     * Content-authoring helper: infers each block's origin/length/
     * orientation from where its letter's cells appear in [layout] — a
     * letter repeated across one row is HORIZONTAL, down one column is
     * VERTICAL. A single occurrence (typically the protagonist) has no
     * shape to infer an axis from, so [RoadblockBlockSpec.forcedOrientation]
     * is required for it.
     */
    fun fromLayout(
        layout: List<String>,
        blockSpecs: List<RoadblockBlockSpec>,
        protagonistId: String,
        exitColumns: Set<Int>,
    ): RoadblockGameState {
        val blocks = blockSpecs.map { spec ->
            val cells = mutableListOf<CellPosition>()
            layout.forEachIndexed { row, line ->
                line.forEachIndexed { col, char -> if (char == spec.letter) cells += CellPosition(row, col) }
            }
            check(cells.isNotEmpty()) { "No cells found for block '${spec.id}' (letter '${spec.letter}')" }
            val origin = cells.minWith(compareBy({ it.row }, { it.col }))
            // A fixed single cell (e.g. the injured man authored as one cell rather than
            // two) never actually slides, so its axis is moot — only a *movable*
            // single-cell block (the protagonist) must say which axis it's locked to.
            check(cells.size > 1 || spec.isFixed || spec.forcedOrientation != null) {
                "Single-cell movable block '${spec.id}' needs an explicit forcedOrientation — its shape alone can't imply an axis"
            }
            val orientation = spec.forcedOrientation
                ?: if (cells.size > 1 && cells.all { it.row == origin.row }) Orientation.HORIZONTAL else Orientation.VERTICAL
            Block(id = spec.id, origin = origin, length = cells.size, orientation = orientation, isFixed = spec.isFixed)
        }
        return RoadblockGameState(
            rows = layout.size,
            cols = layout[0].length,
            blocks = blocks,
            protagonistId = protagonistId,
            exitColumns = exitColumns,
        )
    }

    /**
     * How many cells [blockId] could legally slide in [direction] right
     * now, stopping at the first wall or occupied cell. Returns 0 for: a
     * fixed block in any direction; **any** block (protagonist included)
     * moving off its own orientation's axis — the protagonist is never a
     * free 4-directional mover, or the player could just route around
     * every obstacle without ever needing to clear one; a completed
     * puzzle. The protagonist is the only block ever allowed past the
     * bottom wall, and only through a column in
     * [RoadblockGameState.exitColumns] — for a multi-cell protagonist this
     * is progressive: a cell that's already past the last row counts as
     * reachable the moment the *rest* of the block is still legally
     * placed, so a 2-cell protagonist can hang halfway off the board as an
     * intermediate step. The loop stops the instant every one of the
     * block's cells has cleared the bottom edge — that's the true maximum,
     * continuing further would just count the same off-grid position
     * again and again (see [onSlideAttempted] for what happens then).
     */
    fun maxSlideDistance(state: RoadblockGameState, blockId: String, direction: Direction): Int {
        if (state.isComplete) return 0
        val block = state.blocks.firstOrNull { it.id == blockId } ?: return 0
        if (block.isFixed) return 0
        if (direction.axis() != block.orientation) return 0
        val canExitThisWay = blockId == state.protagonistId && direction == Direction.DOWN

        val occupied = state.blocks.filter { it.id != blockId }.flatMap { it.cells }.toSet()
        var distance = 0
        while (true) {
            val candidate = block.cells.map { it.shiftedBy(direction, distance + 1) }
            if (canExitThisWay && candidate.all { it.isExited(state) }) {
                distance += 1
                break
            }
            val allLegal = candidate.all { cell ->
                (canExitThisWay && cell.isExited(state)) ||
                    (cell.row in 0 until state.rows && cell.col in 0 until state.cols && cell !in occupied)
            }
            if (!allLegal) break
            distance += 1
        }
        return distance
    }

    /**
     * Commits a slide. [cellsAttempted] is clamped to [maxSlideDistance] —
     * never rejected outright, matching "no failure state": an
     * over-ambitious drag just lands at the legal maximum. If every one of
     * the protagonist's cells has cleared the bottom edge at the committed
     * distance, sets [RoadblockGameState.exitedProtagonist] and
     * [RoadblockOutcome.EXITED] instead of moving it to a half-off-grid
     * position — a *partial* exit (one cell still on the board) is a
     * perfectly normal [RoadblockOutcome.MOVED], left hanging off the edge
     * until the next slide finishes the job. 0 legal cells is a full no-op
     * besides [RoadblockOutcome.BLOCKED]. Once complete, always a full
     * no-op.
     */
    fun onSlideAttempted(state: RoadblockGameState, blockId: String, direction: Direction, cellsAttempted: Int): RoadblockGameState {
        if (state.isComplete) return state
        val legalDistance = maxSlideDistance(state, blockId, direction)
        val distance = cellsAttempted.coerceIn(0, legalDistance)
        if (distance == 0) return state.copy(lastOutcome = RoadblockOutcome.BLOCKED)

        val block = state.blocks.first { it.id == blockId }
        val canExitThisWay = blockId == state.protagonistId && direction == Direction.DOWN
        val exits = canExitThisWay && block.cells.map { it.shiftedBy(direction, distance) }.all { it.isExited(state) }
        // Move the block's origin regardless of which branch this is — an EXITED
        // commit still needs its final (now fully off-grid) position recorded, or
        // the screen's drag offset resets to zero against an origin that never
        // moved, visually snapping the tile straight back to where it started.
        val movedBlock = block.copy(origin = block.origin.shiftedBy(direction, distance))
        val nextBlocks = state.blocks.map { if (it.id == blockId) movedBlock else it }

        return if (exits) {
            state.copy(blocks = nextBlocks, exitedProtagonist = true, lastOutcome = RoadblockOutcome.EXITED)
        } else {
            state.copy(blocks = nextBlocks, lastOutcome = RoadblockOutcome.MOVED)
        }
    }

    /** Past the bottom edge, in a column the board actually opens up at. */
    private fun CellPosition.isExited(state: RoadblockGameState): Boolean = row >= state.rows && col in state.exitColumns

    private fun Direction.axis(): Orientation = when (this) {
        Direction.UP, Direction.DOWN -> Orientation.VERTICAL
        Direction.LEFT, Direction.RIGHT -> Orientation.HORIZONTAL
    }

    private fun CellPosition.shiftedBy(direction: Direction, distance: Int): CellPosition = when (direction) {
        Direction.UP -> copy(row = row - distance)
        Direction.DOWN -> copy(row = row + distance)
        Direction.LEFT -> copy(col = col - distance)
        Direction.RIGHT -> copy(col = col + distance)
    }
}
