package com.bibleadventures.game.puzzles.connectfour

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectFourGameTest {

    private fun emptyGrid(rows: Int, columns: Int): List<List<Slot>> = List(rows) { List(columns) { Slot.EMPTY } }

    /** Builds a grid from a sparse `(row, col) -> Slot` map; every other cell is EMPTY. */
    private fun gridOf(rows: Int, columns: Int, cells: Map<Pair<Int, Int>, Slot>): List<List<Slot>> =
        List(rows) { r -> List(columns) { c -> cells[r to c] ?: Slot.EMPTY } }

    @Test
    fun `a fresh game starts empty with the player to move and no outcome`() {
        val state = ConnectFourGame.newGame(columns = 5, rows = 5)

        assertTrue(state.grid.all { row -> row.all { it == Slot.EMPTY } })
        assertTrue(state.isPlayerTurn)
        assertEquals(ConnectFourOutcome.NONE, state.outcome)
    }

    @Test
    fun `dropping lands in the lowest empty row and stacks on a second drop`() {
        val state = ConnectFourGameState(columns = 5, rows = 5, grid = emptyGrid(5, 5))

        val afterFirst = ConnectFourGame.onPlayerColumnTapped(state, column = 2)
        assertEquals(Slot.PLAYER, afterFirst.grid[0][2])
        assertEquals(false, afterFirst.isPlayerTurn)

        val afterSecond = ConnectFourGame.onPlayerColumnTapped(afterFirst.copy(isPlayerTurn = true), column = 2)
        assertEquals(Slot.PLAYER, afterSecond.grid[1][2])
    }

    @Test
    fun `tapping a full column is a no-op`() {
        val fullColumn = gridOf(5, 5, (0 until 5).associate { r -> (r to 0) to Slot.PLAYER })
        val state = ConnectFourGameState(columns = 5, rows = 5, grid = fullColumn, isPlayerTurn = true)

        assertEquals(state, ConnectFourGame.onPlayerColumnTapped(state, column = 0))
    }

    @Test
    fun `tapping out of turn, or once the game has an outcome, is a no-op`() {
        val notPlayerTurn = ConnectFourGameState(columns = 5, rows = 5, grid = emptyGrid(5, 5), isPlayerTurn = false)
        assertEquals(notPlayerTurn, ConnectFourGame.onPlayerColumnTapped(notPlayerTurn, column = 0))

        val finished = ConnectFourGameState(columns = 5, rows = 5, grid = emptyGrid(5, 5), outcome = ConnectFourOutcome.PLAYER_WON)
        assertEquals(finished, ConnectFourGame.onPlayerColumnTapped(finished, column = 0))

        val finishedOpponentTurn = finished.copy(isPlayerTurn = false)
        assertEquals(finishedOpponentTurn, ConnectFourGame.onOpponentMove(finishedOpponentTurn))
    }

    @Test
    fun `four horizontal in a row wins`() {
        val grid = gridOf(5, 5, mapOf(0 to 0 to Slot.PLAYER, 0 to 1 to Slot.PLAYER, 0 to 2 to Slot.PLAYER))
        val state = ConnectFourGameState(columns = 5, rows = 5, grid = grid, isPlayerTurn = true)

        val result = ConnectFourGame.onPlayerColumnTapped(state, column = 3)

        assertEquals(ConnectFourOutcome.PLAYER_WON, result.outcome)
    }

    @Test
    fun `four vertical in a row wins`() {
        val grid = gridOf(5, 5, mapOf(0 to 0 to Slot.PLAYER, 1 to 0 to Slot.PLAYER, 2 to 0 to Slot.PLAYER))
        val state = ConnectFourGameState(columns = 5, rows = 5, grid = grid, isPlayerTurn = true)

        val result = ConnectFourGame.onPlayerColumnTapped(state, column = 0)

        assertEquals(ConnectFourOutcome.PLAYER_WON, result.outcome)
    }

    @Test
    fun `four on a rising diagonal wins`() {
        // Target line: (0,0),(1,1),(2,2),(3,3) — filler stones sit under the higher cells so gravity lands them correctly.
        val grid = gridOf(
            5, 5,
            mapOf(
                0 to 0 to Slot.PLAYER,
                0 to 1 to Slot.OPPONENT, 1 to 1 to Slot.PLAYER,
                0 to 2 to Slot.OPPONENT, 1 to 2 to Slot.OPPONENT, 2 to 2 to Slot.PLAYER,
                0 to 3 to Slot.OPPONENT, 1 to 3 to Slot.OPPONENT, 2 to 3 to Slot.OPPONENT,
            ),
        )
        val state = ConnectFourGameState(columns = 5, rows = 5, grid = grid, isPlayerTurn = true)

        val result = ConnectFourGame.onPlayerColumnTapped(state, column = 3)

        assertEquals(ConnectFourOutcome.PLAYER_WON, result.outcome)
    }

    @Test
    fun `four on a falling diagonal wins`() {
        // Target line: (3,0),(2,1),(1,2),(0,3) — column 1's next empty row is row 2, completing it.
        val grid = gridOf(
            5, 5,
            mapOf(
                0 to 0 to Slot.OPPONENT, 1 to 0 to Slot.OPPONENT, 2 to 0 to Slot.OPPONENT, 3 to 0 to Slot.PLAYER,
                0 to 1 to Slot.OPPONENT, 1 to 1 to Slot.OPPONENT,
                0 to 2 to Slot.OPPONENT, 1 to 2 to Slot.PLAYER,
                0 to 3 to Slot.PLAYER,
            ),
        )
        val state = ConnectFourGameState(columns = 5, rows = 5, grid = grid, isPlayerTurn = true)

        val result = ConnectFourGame.onPlayerColumnTapped(state, column = 1)

        assertEquals(ConnectFourOutcome.PLAYER_WON, result.outcome)
    }

    @Test
    fun `a full board with no 4-in-a-row anywhere is a draw`() {
        // Hand-verified 4x4 fill with no 4-in-a-row in any row, column, or diagonal.
        // Row0: P P P O | Row1: P O O P | Row2: O O P P | Row3: P P O O — (0,0) left empty for the test's own drop.
        val grid = gridOf(
            4, 4,
            mapOf(
                0 to 1 to Slot.PLAYER, 0 to 2 to Slot.PLAYER, 0 to 3 to Slot.OPPONENT,
                1 to 0 to Slot.PLAYER, 1 to 1 to Slot.OPPONENT, 1 to 2 to Slot.OPPONENT, 1 to 3 to Slot.PLAYER,
                2 to 0 to Slot.OPPONENT, 2 to 1 to Slot.OPPONENT, 2 to 2 to Slot.PLAYER, 2 to 3 to Slot.PLAYER,
                3 to 0 to Slot.PLAYER, 3 to 1 to Slot.PLAYER, 3 to 2 to Slot.OPPONENT, 3 to 3 to Slot.OPPONENT,
            ),
        )
        val state = ConnectFourGameState(columns = 4, rows = 4, grid = grid, isPlayerTurn = true)

        val result = ConnectFourGame.onPlayerColumnTapped(state, column = 0)

        assertEquals(Slot.PLAYER, result.grid[0][0])
        assertTrue(result.isBoardFull)
        assertEquals(ConnectFourOutcome.DRAW, result.outcome)
    }

    @Test
    fun `opponent takes an immediate winning move when available`() {
        val grid = gridOf(5, 5, mapOf(0 to 0 to Slot.OPPONENT, 0 to 1 to Slot.OPPONENT, 0 to 2 to Slot.OPPONENT))
        val state = ConnectFourGameState(columns = 5, rows = 5, grid = grid, isPlayerTurn = false)

        assertEquals(3, ConnectFourGame.chooseOpponentColumn(state))

        val result = ConnectFourGame.onOpponentMove(state)
        assertEquals(ConnectFourOutcome.OPPONENT_WON, result.outcome)
    }

    @Test
    fun `opponent blocks the player's immediate winning move`() {
        val grid = gridOf(5, 5, mapOf(0 to 0 to Slot.PLAYER, 0 to 1 to Slot.PLAYER, 0 to 2 to Slot.PLAYER))
        val state = ConnectFourGameState(columns = 5, rows = 5, grid = grid, isPlayerTurn = false)

        assertEquals(3, ConnectFourGame.chooseOpponentColumn(state))

        val result = ConnectFourGame.onOpponentMove(state)
        assertEquals(Slot.OPPONENT, result.grid[0][3])
        assertEquals(ConnectFourOutcome.NONE, result.outcome)
    }

    @Test
    fun `opponent prefers a center column with no urgent win or block`() {
        val state = ConnectFourGame.newGame(columns = 5, rows = 5).copy(isPlayerTurn = false)

        assertEquals(2, ConnectFourGame.chooseOpponentColumn(state))
    }
}
