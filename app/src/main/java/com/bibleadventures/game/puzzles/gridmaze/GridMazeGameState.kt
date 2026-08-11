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
 * "collect an item, then reach the checkpoint, then the goal").
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

    val isComplete: Boolean
        get() = (checkpointActivated || !hasCheckpointTile) &&
            grid[playerPosition.row][playerPosition.col] == GridTileType.GOAL
}
