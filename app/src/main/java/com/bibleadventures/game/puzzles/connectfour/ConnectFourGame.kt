package com.bibleadventures.game.puzzles.connectfour

import kotlin.math.abs
import kotlin.random.Random

/**
 * Pure transition logic — no Compose/Android dependency. The UI owns all
 * timing (e.g. a short pause before animating the opponent's drop, or
 * before resetting the board after a loss); this only ever resolves one
 * drop at a time via [onPlayerColumnTapped]/[onOpponentMove].
 */
object ConnectFourGame {

    fun newGame(columns: Int = 7, rows: Int = 6): ConnectFourGameState =
        ConnectFourGameState(columns = columns, rows = rows, grid = List(rows) { List(columns) { Slot.EMPTY } })

    fun onPlayerColumnTapped(state: ConnectFourGameState, column: Int): ConnectFourGameState {
        if (state.outcome != ConnectFourOutcome.NONE || !state.isPlayerTurn) return state
        val next = dropStone(state, column, Slot.PLAYER) ?: return state
        return if (next.outcome == ConnectFourOutcome.NONE) next.copy(isPlayerTurn = false) else next
    }

    /** Called by the UI once it's ready to reveal the opponent's move (e.g. after a short "thinking" delay). */
    fun onOpponentMove(state: ConnectFourGameState, random: Random = Random.Default): ConnectFourGameState {
        if (state.outcome != ConnectFourOutcome.NONE || state.isPlayerTurn) return state
        val column = chooseOpponentColumn(state, random)
        val next = dropStone(state, column, Slot.OPPONENT) ?: return state
        return if (next.outcome == ConnectFourOutcome.NONE) next.copy(isPlayerTurn = true) else next
    }

    /**
     * Deliberately simple, beatable heuristic — not a minimax-perfect
     * solver: (1) take an immediate win if one exists, (2) otherwise block
     * the player's immediate win, (3) otherwise prefer the column closest
     * to center (a mild, standard Connect Four opening heuristic).
     */
    fun chooseOpponentColumn(state: ConnectFourGameState, random: Random = Random.Default): Int {
        val validColumns = (0 until state.columns).filter { !state.isColumnFull(it) }
        if (validColumns.isEmpty()) return 0

        validColumns.firstOrNull { wouldWin(state, it, Slot.OPPONENT) }?.let { return it }
        validColumns.firstOrNull { wouldWin(state, it, Slot.PLAYER) }?.let { return it }

        val center = (state.columns - 1) / 2
        val closestDistance = validColumns.minOf { abs(it - center) }
        return validColumns.filter { abs(it - center) == closestDistance }.random(random)
    }

    private fun wouldWin(state: ConnectFourGameState, column: Int, slot: Slot): Boolean {
        val row = state.lowestEmptyRow(column) ?: return false
        val grid = placeStone(state.grid, row, column, slot)
        return hasConnectFour(grid, row, column, slot)
    }

    private fun dropStone(state: ConnectFourGameState, column: Int, slot: Slot): ConnectFourGameState? {
        val row = state.lowestEmptyRow(column) ?: return null
        val grid = placeStone(state.grid, row, column, slot)
        val outcome = when {
            hasConnectFour(grid, row, column, slot) -> {
                if (slot == Slot.PLAYER) ConnectFourOutcome.PLAYER_WON else ConnectFourOutcome.OPPONENT_WON
            }
            grid.all { r -> r.none { it == Slot.EMPTY } } -> ConnectFourOutcome.DRAW
            else -> ConnectFourOutcome.NONE
        }
        return state.copy(grid = grid, outcome = outcome)
    }

    private fun placeStone(grid: List<List<Slot>>, row: Int, column: Int, slot: Slot): List<List<Slot>> =
        grid.mapIndexed { r, rowCells ->
            if (r == row) rowCells.mapIndexed { c, cell -> if (c == column) slot else cell } else rowCells
        }

    private val DIRECTIONS = listOf(0 to 1, 1 to 0, 1 to 1, 1 to -1)

    private fun hasConnectFour(grid: List<List<Slot>>, row: Int, column: Int, slot: Slot): Boolean {
        val rows = grid.size
        val cols = grid[0].size
        return DIRECTIONS.any { (dr, dc) ->
            var count = 1
            var r = row + dr
            var c = column + dc
            while (r in 0 until rows && c in 0 until cols && grid[r][c] == slot) {
                count++
                r += dr
                c += dc
            }
            r = row - dr
            c = column - dc
            while (r in 0 until rows && c in 0 until cols && grid[r][c] == slot) {
                count++
                r -= dr
                c -= dc
            }
            count >= 4
        }
    }
}
