package com.bibleadventures.game.puzzles.racemaze

import kotlin.math.sqrt

/**
 * A plain 2D point in cell-unit coordinates (1.0 = one map cell) — mirrors
 * [com.bibleadventures.game.puzzles.dungeon.Vector2] exactly, duplicated
 * rather than shared across packages so this stays a small, independent,
 * pure-Kotlin engine (same "own position type per engine" precedent as
 * [com.bibleadventures.game.puzzles.slideout.CellPosition] vs
 * [com.bibleadventures.game.puzzles.gridmaze.GridPosition]). Also used,
 * unnormalized, as a raw joystick reading passed into [RaceMazeGame.tick].
 */
data class Vector2(val x: Float, val y: Float) {
    fun distanceTo(other: Vector2): Float {
        val dx = x - other.x
        val dy = y - other.y
        return sqrt(dx * dx + dy * dy)
    }
}

/**
 * A hand-drawn corridor maze: every cell is open floor, and walls are drawn
 * on the *boundaries* between cells — the opposite representation from
 * [com.bibleadventures.game.puzzles.dungeon.DungeonGameState]'s "whole cell
 * is either entirely wall or entirely path" model, needed here because
 * Daniel's "Race to the Den" maze art is a classic pen-and-paper-style maze,
 * not a blocky dungeon layout. [verticalWalls]\[row]\[col] is a wall between
 * `(row, col)` and `(row, col + 1)`; [horizontalWalls]\[row]\[col] is a wall
 * between `(row, col)` and `(row + 1, col)`.
 *
 * No traps/supplies/combat/checkpoint — this engine is Race-to-the-Den-only
 * and hardcodes exactly one completion shape: reach the goal.
 */
data class RaceMazeGameState(
    val verticalWalls: List<List<Boolean>>,
    val horizontalWalls: List<List<Boolean>>,
    val playerPosition: Vector2,
    val goalPosition: Vector2,
) {
    val rows: Int get() = horizontalWalls.size + 1
    val cols: Int get() = verticalWalls[0].size + 1

    /** Live-checked from the current position, not a sticky flag — mirrors [com.bibleadventures.game.puzzles.dungeon.DungeonGameState.isComplete]. */
    val isComplete: Boolean
        get() = playerPosition.distanceTo(goalPosition) <= RaceMazeGame.TRIGGER_RADIUS
}
