package com.bibleadventures.game.puzzles.stealth

import com.bibleadventures.game.puzzles.gridmaze.Direction
import com.bibleadventures.game.puzzles.gridmaze.GridPosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StealthGameTest {

    // 3x3 courtyard:
    //   PATH PATH PATH
    //   PATH WALL PATH
    //   PATH PATH GOAL
    // One guard alternates watching (2,1) on even turns and (0,1) on odd turns.
    private fun freshState(): StealthGameState {
        val grid = listOf(
            listOf(StealthTileType.PATH, StealthTileType.PATH, StealthTileType.PATH),
            listOf(StealthTileType.PATH, StealthTileType.WALL, StealthTileType.PATH),
            listOf(StealthTileType.PATH, StealthTileType.PATH, StealthTileType.GOAL),
        )
        val guard = GuardDef(
            patrol = listOf(
                GuardPatrolStep(GridPosition(2, 0), watchedCells = setOf(GridPosition(2, 1))),
                GuardPatrolStep(GridPosition(0, 0), watchedCells = setOf(GridPosition(0, 1))),
            ),
        )
        return StealthGameState(
            grid = grid,
            startPosition = GridPosition(0, 0),
            playerPosition = GridPosition(0, 0),
            guards = listOf(guard),
        )
    }

    @Test
    fun `moving out of bounds is blocked and does not advance the guard patrol`() {
        val next = StealthGame.onDirectionPressed(freshState(), Direction.UP)

        assertEquals(StealthOutcome.BLOCKED, next.lastOutcome)
        assertEquals(GridPosition(0, 0), next.playerPosition)
        assertEquals(0, next.turnIndex)
    }

    @Test
    fun `moving into a wall is blocked and does not advance the guard patrol`() {
        val afterDown = StealthGame.onDirectionPressed(freshState(), Direction.DOWN)
        val afterRight = StealthGame.onDirectionPressed(afterDown, Direction.RIGHT)

        assertEquals(StealthOutcome.BLOCKED, afterRight.lastOutcome)
        assertEquals(GridPosition(1, 0), afterRight.playerPosition)
        assertEquals(1, afterRight.turnIndex)
    }

    @Test
    fun `moving into a freshly watched cell is spotted and resets to start, guard cycle still advances`() {
        // Turn 0 -> 1: guard patrol step 1 watches (0,1). Moving RIGHT from (0,0) lands there.
        val spotted = StealthGame.onDirectionPressed(freshState(), Direction.RIGHT)

        assertEquals(StealthOutcome.SPOTTED, spotted.lastOutcome)
        assertEquals(GridPosition(0, 0), spotted.playerPosition)
        assertEquals(1, spotted.turnIndex)
    }

    @Test
    fun `moving to an unwatched cell is a normal move`() {
        val moved = StealthGame.onDirectionPressed(freshState(), Direction.DOWN)

        assertEquals(StealthOutcome.MOVED, moved.lastOutcome)
        assertEquals(GridPosition(1, 0), moved.playerPosition)
        assertEquals(1, moved.turnIndex)
    }

    @Test
    fun `reaching the goal while unwatched completes the puzzle`() {
        var state = freshState()
        state = StealthGame.onDirectionPressed(state, Direction.DOWN) // (1,0), turn 1
        state = StealthGame.onDirectionPressed(state, Direction.DOWN) // (2,0), turn 2
        state = StealthGame.onDirectionPressed(state, Direction.RIGHT) // (2,1), turn 3
        state = StealthGame.onDirectionPressed(state, Direction.RIGHT) // (2,2) GOAL, turn 4

        assertEquals(StealthOutcome.COMPLETE, state.lastOutcome)
        assertEquals(GridPosition(2, 2), state.playerPosition)
        assertTrue(state.isComplete)
    }

    @Test
    fun `once complete, further presses are a no-op`() {
        var state = freshState()
        state = StealthGame.onDirectionPressed(state, Direction.DOWN)
        state = StealthGame.onDirectionPressed(state, Direction.DOWN)
        state = StealthGame.onDirectionPressed(state, Direction.RIGHT)
        state = StealthGame.onDirectionPressed(state, Direction.RIGHT)
        assertTrue(state.isComplete)

        val unchanged = StealthGame.onDirectionPressed(state, Direction.LEFT)

        assertEquals(state, unchanged)
    }
}
