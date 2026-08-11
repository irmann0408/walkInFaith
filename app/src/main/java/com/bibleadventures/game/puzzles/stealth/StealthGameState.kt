package com.bibleadventures.game.puzzles.stealth

import com.bibleadventures.game.puzzles.gridmaze.GridPosition

enum class StealthTileType { PATH, WALL, GOAL }

// Never FAILED — being spotted just walks the player back to startPosition.
enum class StealthOutcome { NONE, MOVED, BLOCKED, SPOTTED, COMPLETE }

/**
 * One step in a guard's deterministic patrol cycle: where the guard stands
 * and which cells it currently watches. Watched cells are hand-authored per
 * step, not computed from a facing direction/angle — keeps the pattern
 * simple, testable, and a fair, learnable rhythm for a young player, same
 * spirit as this app's other hand-verified maps.
 */
data class GuardPatrolStep(val position: GridPosition, val watchedCells: Set<GridPosition>)

data class GuardDef(val patrol: List<GuardPatrolStep>)

data class StealthGameState(
    val grid: List<List<StealthTileType>>,
    val startPosition: GridPosition,
    val playerPosition: GridPosition,
    val guards: List<GuardDef>,
    val turnIndex: Int = 0,
    val lastOutcome: StealthOutcome = StealthOutcome.NONE,
) {
    val isComplete: Boolean
        get() = grid[playerPosition.row][playerPosition.col] == StealthTileType.GOAL

    val watchedCells: Set<GridPosition>
        get() = guards.flatMap { guard -> guard.patrol[turnIndex % guard.patrol.size].watchedCells }.toSet()
}
