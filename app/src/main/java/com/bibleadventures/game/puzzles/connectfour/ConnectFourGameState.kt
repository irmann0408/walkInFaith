package com.bibleadventures.game.puzzles.connectfour

enum class Slot { EMPTY, PLAYER, OPPONENT }

/**
 * The only mini-game in this app with a real loss condition — a deliberate,
 * confirmed exception to the project's "no failure states" rule (see the
 * architectural decisions log). [OPPONENT_WON] and [DRAW] are never a dead
 * end though: the UI resets the whole board for another round rather than
 * showing any kind of game-over screen, so losing costs nothing but a
 * moment before trying again.
 */
enum class ConnectFourOutcome { NONE, PLAYER_WON, OPPONENT_WON, DRAW }

/**
 * Standard Connect Four rules: drop into a column, gravity settles the
 * piece into the lowest empty row, 4 in a row (any direction) wins.
 * [grid]`[row][col]`, row 0 is the bottom row.
 */
data class ConnectFourGameState(
    val columns: Int = 7,
    val rows: Int = 6,
    val grid: List<List<Slot>> = List(rows) { List(columns) { Slot.EMPTY } },
    val isPlayerTurn: Boolean = true,
    val outcome: ConnectFourOutcome = ConnectFourOutcome.NONE,
) {
    fun lowestEmptyRow(column: Int): Int? = (0 until rows).firstOrNull { grid[it][column] == Slot.EMPTY }

    fun isColumnFull(column: Int): Boolean = lowestEmptyRow(column) == null

    val isBoardFull: Boolean get() = (0 until columns).all { isColumnFull(it) }
}
