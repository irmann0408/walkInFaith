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
        listOf(GridTileType.PATH, GridTileType.COLLECTIBLE, GridTileType.PATH),
        listOf(GridTileType.WALL, GridTileType.CHECKPOINT, GridTileType.WALL),
        listOf(GridTileType.PATH, GridTileType.PATH, GridTileType.GOAL),
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
    fun `stepping onto a collectible collects it and is idempotent on revisit`() {
        var state = GridMazeGame.onDirectionPressed(initialState(), Direction.RIGHT)

        assertEquals(GridMazeOutcome.COLLECTED, state.lastOutcome)
        assertTrue(state.hasCollectible)
        assertEquals(setOf(GridPosition(0, 1)), state.collectedPositions)

        state = GridMazeGame.onDirectionPressed(state, Direction.LEFT)
        state = GridMazeGame.onDirectionPressed(state, Direction.RIGHT)

        assertEquals(GridMazeOutcome.MOVED, state.lastOutcome)
        assertEquals(setOf(GridPosition(0, 1)), state.collectedPositions)
    }

    @Test
    fun `reaching the checkpoint without a collectible leaves it unactivated`() {
        val stateNextToCheckpoint = GridMazeState(grid = testGrid, playerPosition = GridPosition(0, 1))

        val result = GridMazeGame.onDirectionPressed(stateNextToCheckpoint, Direction.DOWN)

        assertEquals(GridMazeOutcome.CHECKPOINT_NEEDS_COLLECTIBLE, result.lastOutcome)
        assertFalse(result.checkpointActivated)
    }

    @Test
    fun `reaching the checkpoint with a collectible activates it`() {
        var state = initialState()
        state = GridMazeGame.onDirectionPressed(state, Direction.RIGHT) // collect
        state = GridMazeGame.onDirectionPressed(state, Direction.DOWN) // reach checkpoint

        assertEquals(GridMazeOutcome.CHECKPOINT_ACTIVATED, state.lastOutcome)
        assertTrue(state.checkpointActivated)
    }

    @Test
    fun `the goal only completes the chapter once the checkpoint is activated`() {
        val unactivatedAtGoal = GridMazeState(grid = testGrid, playerPosition = GridPosition(2, 2))
        assertFalse(unactivatedAtGoal.isComplete)

        val activatedAtGoal = unactivatedAtGoal.copy(checkpointActivated = true)
        assertTrue(activatedAtGoal.isComplete)
    }

    @Test
    fun `a map with no checkpoint tile completes on reaching the goal alone`() {
        // . . .
        // . # .
        // . . I
        val checkpointFreeGrid = listOf(
            listOf(GridTileType.PATH, GridTileType.PATH, GridTileType.PATH),
            listOf(GridTileType.PATH, GridTileType.WALL, GridTileType.PATH),
            listOf(GridTileType.PATH, GridTileType.PATH, GridTileType.GOAL),
        )

        val atGoal = GridMazeState(grid = checkpointFreeGrid, playerPosition = GridPosition(2, 2))

        assertTrue(atGoal.isComplete)
    }

    @Test
    fun `once complete, further presses are a no-op`() {
        val completeState = GridMazeState(
            grid = testGrid,
            playerPosition = GridPosition(2, 2),
            checkpointActivated = true,
        )

        val result = GridMazeGame.onDirectionPressed(completeState, Direction.UP)

        assertEquals(completeState, result)
    }
}
