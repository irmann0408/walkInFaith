package com.bibleadventures.game.puzzles.racemaze

import kotlin.math.hypot
import kotlin.math.min

/**
 * Pure real-time movement/collision logic for Daniel's "Race to the Den"
 * maze — no Compose/Android dependency, no notion of wall-clock time (the
 * screen owns the frame clock and passes an explicit tick-to-tick
 * [deltaSeconds]), same contract as
 * [com.bibleadventures.game.puzzles.dungeon.DungeonGame.tick].
 */
object RaceMazeGame {

    /** Fraction of one cell the player occupies, for wall collision — mirrors [com.bibleadventures.game.puzzles.dungeon.DungeonGame.PLAYER_RADIUS]; tune on-device against this maze art's real corridor width. */
    const val PLAYER_RADIUS = 0.3f

    const val TRIGGER_RADIUS = 0.4f

    /** Below this, a joystick reading is dead-zone noise, not intent — mirrors [com.bibleadventures.game.puzzles.dungeon.DungeonGame.MIN_JOYSTICK_MAGNITUDE]. */
    const val MIN_JOYSTICK_MAGNITUDE = 0.15f

    const val PLAYER_SPEED_CELLS_PER_SECOND = 2.0f

    /**
     * The largest [deltaSeconds] a single collision-resolution step is ever
     * given, regardless of how large the caller's own [deltaSeconds] is —
     * [tick] splits a bigger one into several steps this size or smaller.
     * This engine's walls are thin lines (a band only [PLAYER_RADIUS] * 2
     * wide) rather than whole blocked cells, so — unlike
     * [com.bibleadventures.game.puzzles.dungeon.DungeonGame], whose
     * opaque, cell-sized walls give a full frame's worth of margin — a
     * single oversized step (e.g. a one-time jank right as this screen
     * first composes and decodes its background image, which is exactly
     * when this was first caught on-device) could move the player clean
     * across that thin band in one leap, since collision here is only ever
     * checked at each step's *destination* point, never swept along the
     * path. At [PLAYER_SPEED_CELLS_PER_SECOND]'s max speed this bounds a
     * single step's travel to `2.0 * (1/30) ≈ 0.067` cells — well under the
     * `0.6`-cell band — while adding zero overhead on an ordinary ~60fps
     * frame, where the real delta is already below this and the loop in
     * [tick] runs exactly once.
     */
    private const val MAX_STEP_SECONDS = 1f / 30f

    /**
     * A hard ceiling on the *total* [deltaSeconds] one [tick] call will
     * ever process, so a truly extreme gap (the app resuming from being
     * backgrounded for a long time, say) can't blow up into an enormous
     * number of [MAX_STEP_SECONDS] sub-steps in a single call — a full
     * second of travel is already far more than this maze needs to cross
     * in one tick.
     */
    private const val MAX_TOTAL_DELTA_SECONDS = 1f

    /**
     * `verticalWalls[row]`'s char at index `col` = wall between `(row, col)`
     * and `(row, col + 1)`; `horizontalWalls[row]`'s char at index `col` =
     * wall between `(row, col)` and `(row + 1, col)`. `1` = wall, anything
     * else = open. [start]/[goal] are already cell-unit positions (not
     * row/col pairs) — a caller wanting an ordinary cell center uses
     * `Vector2(col + 0.5f, row + 0.5f)`, matching
     * [com.bibleadventures.game.puzzles.dungeon.DungeonGame.fromLayout]'s
     * own convention, but a border doorway drawn off-grid in the art (as
     * Daniel's own maze content does for its start/goal) can pass a
     * different position instead.
     */
    fun fromWalls(
        verticalWalls: List<String>,
        horizontalWalls: List<String>,
        start: Vector2,
        goal: Vector2,
    ): RaceMazeGameState = RaceMazeGameState(
        verticalWalls = verticalWalls.map { row -> row.map { it == '1' } },
        horizontalWalls = horizontalWalls.map { row -> row.map { it == '1' } },
        playerPosition = start,
        goalPosition = goal,
    )

    /**
     * One frame of movement + collision. [joystickInput] is the raw (not
     * pre-normalized) knob vector — its magnitude (0..1) scales speed for
     * real analog feel, its direction sets heading. [deltaSeconds] is real
     * elapsed wall-time since the *previous* [tick] call, internally split
     * into [MAX_STEP_SECONDS]-sized (or smaller) sub-steps — see that
     * constant's own doc comment for why a single oversized step is unsafe
     * here specifically. A full no-op once [RaceMazeGameState.isComplete].
     */
    fun tick(state: RaceMazeGameState, joystickInput: Vector2, deltaSeconds: Float): RaceMazeGameState {
        if (state.isComplete) return state

        val magnitude = hypot(joystickInput.x, joystickInput.y)
        if (magnitude < MIN_JOYSTICK_MAGNITUDE) return state

        val dirX = joystickInput.x / magnitude
        val dirY = joystickInput.y / magnitude
        val speed = PLAYER_SPEED_CELLS_PER_SECOND * min(magnitude, 1f)

        var current = state
        var remaining = deltaSeconds.coerceAtMost(MAX_TOTAL_DELTA_SECONDS)
        while (remaining > 0f && !current.isComplete) {
            val step = min(remaining, MAX_STEP_SECONDS)
            current = advance(current, dirX, dirY, speed, step)
            remaining -= step
        }
        return current
    }

    /**
     * A single collision-resolved movement step — the actual per-axis
     * sweep, same reasoning as `DungeonGame.tick`: resolve X first (Y held
     * at its old value), then Y using the already-resolved X, so a
     * diagonal push into a wall slides along it instead of stopping dead.
     * Safe to call repeatedly with small [deltaSeconds] values (see
     * [MAX_STEP_SECONDS]); unsafe with an arbitrarily large one.
     */
    private fun advance(state: RaceMazeGameState, dirX: Float, dirY: Float, speed: Float, deltaSeconds: Float): RaceMazeGameState {
        val oldPosition = state.playerPosition
        val candidateX = oldPosition.x + dirX * speed * deltaSeconds
        val candidateY = oldPosition.y + dirY * speed * deltaSeconds

        val resolvedX = if (collidesWithWall(state, candidateX, oldPosition.y)) oldPosition.x else candidateX
        val resolvedY = if (collidesWithWall(state, resolvedX, candidateY)) oldPosition.y else candidateY

        return state.copy(playerPosition = Vector2(resolvedX, resolvedY))
    }

    /**
     * Point-vs-every-wall-*segment* overlap: each wall is a thin line on a
     * cell boundary (not a whole blocked cell, unlike
     * [com.bibleadventures.game.puzzles.dungeon.DungeonGame.collidesWithWall]),
     * expanded by [PLAYER_RADIUS] along the axis it separates cells on — the
     * same point-vs-expanded-rect (Minkowski-sum) trick, just against a
     * 1-cell-long sliver instead of a full cell. Map edges count as walls
     * too. Brute-force scan over every segment (~350 for this maze's 14x14
     * grid) — fine at this scale, even at 60fps, same as the dungeon
     * engine's own per-cell scan.
     */
    private fun collidesWithWall(state: RaceMazeGameState, x: Float, y: Float): Boolean {
        val rows = state.rows
        val cols = state.cols
        if (x - PLAYER_RADIUS < 0f || x + PLAYER_RADIUS > cols || y - PLAYER_RADIUS < 0f || y + PLAYER_RADIUS > rows) return true

        for (row in 0 until rows) {
            for (col in 0 until cols - 1) {
                if (!state.verticalWalls[row][col]) continue
                val lineX = (col + 1).toFloat()
                if (x >= lineX - PLAYER_RADIUS && x <= lineX + PLAYER_RADIUS && y >= row && y <= row + 1) return true
            }
        }
        for (row in 0 until rows - 1) {
            for (col in 0 until cols) {
                if (!state.horizontalWalls[row][col]) continue
                val lineY = (row + 1).toFloat()
                if (y >= lineY - PLAYER_RADIUS && y <= lineY + PLAYER_RADIUS && x >= col && x <= col + 1) return true
            }
        }
        return false
    }
}
