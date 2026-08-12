package com.bibleadventures.game.puzzles.gridmaze

enum class GridTileType { PATH, WALL, COLLECTIBLE, CHECKPOINT, GOAL }

data class GridPosition(val row: Int, val col: Int)

enum class Direction { UP, DOWN, LEFT, RIGHT }

/** Never FAILED — walking into a wall is a neutral no-op, not a miss (spec section 9). */
enum class GridMazeOutcome { NONE, MOVED, BLOCKED, COLLECTED, CHECKPOINT_NEEDS_COLLECTIBLE, CHECKPOINT_ACTIVATED }

/**
 * Row-major grid; [grid]'s own dimensions define the map size. No Compose/
 * Android dependency — positions are plain ints, not [androidx.compose.ui.geometry.Offset].
 *
 * Generic across chapters: a map with no [GridTileType.CHECKPOINT] tile at
 * all just needs the goal reached (e.g. a simple "hurry there" maze); a map
 * that does have one gates completion on it being activated first (e.g.
 * "collect an item, then reach the checkpoint, then the goal"). A map with
 * no [GridTileType.GOAL] tile at all completes once every
 * [GridTileType.COLLECTIBLE] tile has been visited instead (e.g. "reach
 * every one of these, in any order, no single finish line") — CHECKPOINT is
 * meaningless without a GOAL to gate, so this mode ignores it.
 */
data class GridMazeState(
    val grid: List<List<GridTileType>>,
    val playerPosition: GridPosition,
    val collectedPositions: Set<GridPosition> = emptySet(),
    val checkpointActivated: Boolean = false,
    val lastOutcome: GridMazeOutcome = GridMazeOutcome.NONE,
) {
    val hasCollectible: Boolean get() = collectedPositions.isNotEmpty()

    private val hasCheckpointTile: Boolean get() = grid.any { row -> row.any { it == GridTileType.CHECKPOINT } }
    private val hasGoalTile: Boolean get() = grid.any { row -> row.any { it == GridTileType.GOAL } }
    private val totalCollectibleCount: Int get() = grid.sumOf { row -> row.count { it == GridTileType.COLLECTIBLE } }

    val isComplete: Boolean
        get() = if (hasGoalTile) {
            (checkpointActivated || !hasCheckpointTile) &&
                grid[playerPosition.row][playerPosition.col] == GridTileType.GOAL
        } else {
            totalCollectibleCount > 0 && collectedPositions.size >= totalCollectibleCount
        }
}
