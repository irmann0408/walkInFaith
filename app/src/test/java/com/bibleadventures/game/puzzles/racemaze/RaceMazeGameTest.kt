package com.bibleadventures.game.puzzles.racemaze

import com.bibleadventures.game.stories.DanielContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RaceMazeGameTest {

    // A tiny 3x3 test maze, all open except a single vertical wall between
    // (0,0) and (0,1) — mirrors DungeonGameTest's own small hand-built
    // fixture rather than the real 14x14 production maze for most tests.
    //   . | . .
    //   . . .
    //   . . .
    private val testVerticalWalls = listOf("1.", "..", "..")
    private val testHorizontalWalls = listOf("...", "...")

    private fun initialState() = RaceMazeGame.fromWalls(
        verticalWalls = testVerticalWalls,
        horizontalWalls = testHorizontalWalls,
        start = Vector2(0.5f, 0.5f),
        goal = Vector2(2.5f, 2.5f),
    )

    @Test
    fun `fromWalls parses wall grids, start, and goal into cell-center positions`() {
        val state = initialState()

        assertEquals(3, state.rows)
        assertEquals(3, state.cols)
        assertEquals(Vector2(0.5f, 0.5f), state.playerPosition)
        assertEquals(Vector2(2.5f, 2.5f), state.goalPosition)
        assertTrue(state.verticalWalls[0][0])
        assertFalse(state.verticalWalls[0][1])
        assertFalse(state.horizontalWalls[0][0])
    }

    @Test
    fun `tick is a no-op below the dead zone`() {
        val state = initialState()

        val result = RaceMazeGame.tick(state, Vector2(0.05f, 0.05f), deltaSeconds = 1f)

        assertEquals(state, result)
    }

    @Test
    fun `tick moves the player toward the joystick direction, scaled by speed and delta time`() {
        val state = initialState().copy(playerPosition = Vector2(1.5f, 1.5f))

        val result = RaceMazeGame.tick(state, Vector2(1f, 0f), deltaSeconds = 0.1f)

        val expectedX = 1.5f + RaceMazeGame.PLAYER_SPEED_CELLS_PER_SECOND * 0.1f
        assertEquals(expectedX, result.playerPosition.x, 0.001f)
        assertEquals(1.5f, result.playerPosition.y, 0.001f)
    }

    @Test
    fun `wall collision blocks the axis that would collide while leaving the other axis free to slide`() {
        // Approaching the vertical wall at x=1 (between col 0 and col 1)
        // diagonally from (0.5, 0.5), one real-frame-sized tick at a time
        // (as real play ticks, rather than one big jump — see the
        // oversized-deltaSeconds test above for why a single large step
        // isn't how this should be exercised): X should stop advancing
        // once it nears the wall's near edge, while Y keeps sliding freely
        // past it (there's no horizontal wall at col 0). Only 15 ticks —
        // this test maze's wall is only one row tall (see testVerticalWalls),
        // so checked much later Y would cross into row 1 and the wall would
        // stop applying to X at all, which is a distinct behavior this test
        // isn't about.
        var state = initialState()
        repeat(15) {
            state = RaceMazeGame.tick(state, Vector2(1f, 1f), deltaSeconds = 1f / 60f)
        }

        assertTrue("X should be blocked before reaching the wall", state.playerPosition.x < 0.7f)
        assertTrue("Y should have advanced well past where X stopped", state.playerPosition.y > state.playerPosition.x)
    }

    @Test
    fun `a candidate position that would cross a map edge is treated as a wall collision`() {
        val state = initialState().copy(playerPosition = Vector2(1.5f, 0.35f))

        val result = RaceMazeGame.tick(state, Vector2(0f, -1f), deltaSeconds = 1f)

        assertEquals(state.playerPosition, result.playerPosition)
    }

    @Test
    fun `an oversized deltaSeconds does not tunnel through a wall`() {
        // Caught on-device: a one-time jank right as this screen first
        // composes and decodes its background image inflated a single
        // tick's deltaSeconds enough that the old, un-split single-step
        // movement leapt clean across a wall's thin (2 * PLAYER_RADIUS)
        // collision band in one jump, since collision was only ever
        // checked at the destination point, never swept along the way.
        // 5 seconds at full speed would try to move the player straight to
        // x=10.5 in one step — clean through the wall at x=1 (and the map's
        // own right edge too) — if tick() didn't internally split this into
        // small, safe sub-steps.
        val state = initialState()

        val result = RaceMazeGame.tick(state, Vector2(1f, 0f), deltaSeconds = 5f)

        assertTrue("should still be blocked at the near side of the wall, not past it", result.playerPosition.x < 1f)
        assertTrue("sub-stepping should still let the player approach the wall, not get stuck at the start", result.playerPosition.x > state.playerPosition.x)
    }

    @Test
    fun `isComplete is true only within TRIGGER_RADIUS of the goal`() {
        val atGoal = initialState().copy(playerPosition = Vector2(2.5f, 2.5f))
        assertTrue(atGoal.isComplete)

        val far = initialState()
        assertFalse(far.isComplete)
    }

    @Test
    fun `once complete, tick is an identity no-op`() {
        val completeState = initialState().copy(playerPosition = Vector2(2.5f, 2.5f))

        assertEquals(completeState, RaceMazeGame.tick(completeState, Vector2(1f, 1f), deltaSeconds = 1f))
    }

    @Test
    fun `replaying the production maze's hand-verified waypoint route reaches isComplete`() {
        var state = RaceMazeGame.fromWalls(
            DanielContent.raceMazeVerticalWalls,
            DanielContent.raceMazeHorizontalWalls,
            DanielContent.raceMazeStart,
            DanielContent.raceMazeGoal,
        )

        DanielContent.raceMazeSolutionWaypoints.forEach { waypoint ->
            state = steerToward(state, waypoint)
        }

        assertTrue(state.isComplete)
    }

    /**
     * Closed-loop steering toward [target], self-correcting every tick from
     * wherever collision actually left the player — mirrors
     * `DungeonGameTest.steerToward` exactly, including normalizing to a
     * fixed magnitude rather than feeding the raw, shrinking
     * distance-to-target vector (which would get stuck once that distance
     * itself drops below [RaceMazeGame.MIN_JOYSTICK_MAGNITUDE]).
     */
    private fun steerToward(state: RaceMazeGameState, target: Vector2, deltaSeconds: Float = 1f / 60f): RaceMazeGameState {
        var current = state
        var safety = 0
        while (!current.isComplete && current.playerPosition.distanceTo(target) > 0.05f) {
            check(safety++ < 5_000) { "Steering toward $target from ${current.playerPosition} did not converge" }
            val dx = target.x - current.playerPosition.x
            val dy = target.y - current.playerPosition.y
            val magnitude = kotlin.math.hypot(dx, dy)
            val direction = Vector2(dx / magnitude, dy / magnitude)
            current = RaceMazeGame.tick(current, direction, deltaSeconds)
        }
        return current
    }
}
