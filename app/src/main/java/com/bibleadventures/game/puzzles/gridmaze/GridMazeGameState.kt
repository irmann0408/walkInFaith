package com.bibleadventures.game.puzzles.gridmaze

enum class GridTileType { PATH, WALL, MEDICINE, TRAVELER, INN }

data class GridPosition(val row: Int, val col: Int)

enum class Direction { UP, DOWN, LEFT, RIGHT }

/** Never FAILED — walking into a wall is a neutral no-op, not a miss (spec section 9). */
enum class GridMazeOutcome { NONE, MOVED, BLOCKED, MEDICINE_COLLECTED, TRAVELER_NEEDS_MEDICINE, TRAVELER_TREATED }

/**
 * Row-major grid; [grid]'s own dimensions define the map size. No Compose/
 * Android dependency — positions are plain ints, not [androidx.compose.ui.geometry.Offset].
 */
data class GridMazeState(
    val grid: List<List<GridTileType>>,
    val playerPosition: GridPosition,
    val medicineCollected: Set<GridPosition> = emptySet(),
    val travelerTreated: Boolean = false,
    val lastOutcome: GridMazeOutcome = GridMazeOutcome.NONE,
) {
    val hasMedicine: Boolean get() = medicineCollected.isNotEmpty()

    val isComplete: Boolean
        get() = travelerTreated && grid[playerPosition.row][playerPosition.col] == GridTileType.INN
}
