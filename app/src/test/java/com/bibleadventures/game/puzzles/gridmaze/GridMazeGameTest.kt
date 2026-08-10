package com.bibleadventures.game.puzzles.gridmaze

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GridMazeGameTest {

    // A tiny 3x3 test map:
    // . M .
    // # T #
    // . . I
    private val testGrid = listOf(
        listOf(GridTileType.PATH, GridTileType.MEDICINE, GridTileType.PATH),
        listOf(GridTileType.WALL, GridTileType.TRAVELER, GridTileType.WALL),
        listOf(GridTileType.PATH, GridTileType.PATH, GridTileType.INN),
    )

    private fun initialState() = GridMazeState(grid = testGrid, playerPosition = GridPosition(0, 0))

    @Test
    fun `walking into a wall is a same-position no-op, never a failure`() {
        val state = GridMazeGame.onDirectionPressed(initialState(), Direction.DOWN)

        assertEquals(GridMazeOutcome.BLOCKED, state.lastOutcome)
        assertEquals(GridPosition(0, 0), state.playerPosition)
    }

    @Test
    fun `walking out of bounds is a same-position no-op`() {
        val state = GridMazeGame.onDirectionPressed(initialState(), Direction.UP)

        assertEquals(GridMazeOutcome.BLOCKED, state.lastOutcome)
        assertEquals(GridPosition(0, 0), state.playerPosition)
    }

    @Test
    fun `stepping onto medicine collects it and is idempotent on revisit`() {
        var state = GridMazeGame.onDirectionPressed(initialState(), Direction.RIGHT)

        assertEquals(GridMazeOutcome.MEDICINE_COLLECTED, state.lastOutcome)
        assertTrue(state.hasMedicine)
        assertEquals(setOf(GridPosition(0, 1)), state.medicineCollected)

        state = GridMazeGame.onDirectionPressed(state, Direction.LEFT)
        state = GridMazeGame.onDirectionPressed(state, Direction.RIGHT)

        assertEquals(GridMazeOutcome.MOVED, state.lastOutcome)
        assertEquals(setOf(GridPosition(0, 1)), state.medicineCollected)
    }

    @Test
    fun `reaching the traveler without medicine leaves them untreated`() {
        val stateNextToTraveler = GridMazeState(grid = testGrid, playerPosition = GridPosition(0, 1))

        val result = GridMazeGame.onDirectionPressed(stateNextToTraveler, Direction.DOWN)

        assertEquals(GridMazeOutcome.TRAVELER_NEEDS_MEDICINE, result.lastOutcome)
        assertFalse(result.travelerTreated)
    }

    @Test
    fun `reaching the traveler with medicine treats them`() {
        var state = initialState()
        state = GridMazeGame.onDirectionPressed(state, Direction.RIGHT) // collect medicine
        state = GridMazeGame.onDirectionPressed(state, Direction.DOWN) // reach traveler

        assertEquals(GridMazeOutcome.TRAVELER_TREATED, state.lastOutcome)
        assertTrue(state.travelerTreated)
    }

    @Test
    fun `the inn only completes the chapter once the traveler is treated`() {
        val untreatedAtInn = GridMazeState(grid = testGrid, playerPosition = GridPosition(2, 2))
        assertFalse(untreatedAtInn.isComplete)

        val treatedAtInn = untreatedAtInn.copy(travelerTreated = true)
        assertTrue(treatedAtInn.isComplete)
    }

    @Test
    fun `once complete, further presses are a no-op`() {
        val completeState = GridMazeState(
            grid = testGrid,
            playerPosition = GridPosition(2, 2),
            travelerTreated = true,
        )

        val result = GridMazeGame.onDirectionPressed(completeState, Direction.UP)

        assertEquals(completeState, result)
    }
}
