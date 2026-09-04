package com.bibleadventures.game.stories

import com.bibleadventures.R
import com.bibleadventures.game.puzzles.racemaze.RaceMazeGame
import com.bibleadventures.game.puzzles.racemaze.Vector2
import com.bibleadventures.game.puzzles.slideout.CellPosition
import com.bibleadventures.game.puzzles.slideout.LatchBlock
import com.bibleadventures.game.puzzles.slideout.SlideDirection
import kotlin.random.Random

/** Shared across chapters (also used by Jericho's Blow the Shofar) — same shape everywhere, same reason [MathProblem] is. */
enum class MathOperator { ADD, SUBTRACT, MULTIPLY, DIVIDE }

/**
 * One math problem in a "solve it, pick from 3 choices" puzzle — [choiceValues]
 * holds all 3 answer choices (including [correctValue]), already shuffled.
 * Shared shape, not Daniel-specific: Jericho's Blow the Shofar reuses this
 * exact data class for its own multiplication/division problems, same
 * "reuse a content shape once a second chapter needs it" precedent as
 * [ChoiceOptionDef].
 */
data class MathProblem(val id: String, val operandA: Int, val operandB: Int, val operator: MathOperator, val choiceValues: List<Int>) {
    val correctValue: Int
        get() = when (operator) {
            MathOperator.ADD -> operandA + operandB
            MathOperator.SUBTRACT -> operandA - operandB
            MathOperator.MULTIPLY -> operandA * operandB
            MathOperator.DIVIDE -> operandA / operandB
        }
}

/**
 * Static content for the Daniel and the Lions chapter. Kept separate from
 * `game/puzzles/dodge`, `game/puzzles/sequence`, and `game/puzzles/gridmaze`
 * so those pure engines stay reusable — this file is the only thing that's
 * Daniel-specific.
 */
object DanielContent {

    /**
     * The window is a fully packed [WINDOW_LATCH_ROWS] x [WINDOW_LATCH_COLS]
     * grid of latches — no empty background cells — up from the original
     * 8-latch plus-shaped board, which on-device testing found trivially
     * easy for the intended 7+ audience. Every single cell holds a latch.
     */
    const val WINDOW_LATCH_ROWS = 6
    const val WINDOW_LATCH_COLS = 6

    private const val WINDOW_LATCH_SEED = 20260904L

    /**
     * Generated once with a fixed seed (never reshuffled per playthrough,
     * same "static, hand-verified" spirit as every other hand-authored
     * layout in this file) via a "decide the release order first" build:
     * starting from the full board, repeatedly collect every (cell,
     * direction) pair whose path to the board edge is currently clear of
     * every other *not-yet-assigned* cell, pick one at random, assign that
     * cell that direction, and remove it from the pool — then repeat until
     * every cell has a direction. Because a cell is only ever assigned a
     * direction whose path is already clear of everything still left in the
     * pool, and the pool only ever shrinks, the resulting order is
     * *guaranteed* to be a valid release sequence: a cell's direction can
     * depend only on cells that get assigned (and thus released) strictly
     * before it. This is what gives real variety — a top-row cell isn't
     * limited to pointing up; once enough of the board around it has
     * already been claimed by earlier picks, pointing down, left, or right
     * can just as easily be the one that happens to be clear — while still
     * ruling out hand-authored deadlocks by construction, not by luck.
     * `SlideOutGameTest` both replays [windowLatchSolutionOrder] and
     * independently re-verifies the resulting board with the engine's own
     * greedy solver as a second check.
     */
    private val windowLatchGeneration: List<LatchBlock> = run {
        val random = Random(WINDOW_LATCH_SEED)
        val remaining = (0 until WINDOW_LATCH_ROWS).flatMap { row ->
            (0 until WINDOW_LATCH_COLS).map { col -> CellPosition(row, col) }
        }.toMutableSet()

        fun isClear(cell: CellPosition, direction: SlideDirection): Boolean {
            var current = cell
            while (true) {
                current = when (direction) {
                    SlideDirection.UP -> current.copy(row = current.row - 1)
                    SlideDirection.DOWN -> current.copy(row = current.row + 1)
                    SlideDirection.LEFT -> current.copy(col = current.col - 1)
                    SlideDirection.RIGHT -> current.copy(col = current.col + 1)
                }
                if (current.row !in 0 until WINDOW_LATCH_ROWS || current.col !in 0 until WINDOW_LATCH_COLS) return true
                if (current in remaining) return false
            }
        }

        val order = mutableListOf<LatchBlock>()
        while (remaining.isNotEmpty()) {
            val candidates = remaining.flatMap { cell -> SlideDirection.values().filter { isClear(cell, it) }.map { cell to it } }
            check(candidates.isNotEmpty()) { "windowLatchGeneration deadlocked with ${remaining.size} cells left: $remaining" }
            val (cell, direction) = candidates[random.nextInt(candidates.size)]
            order += LatchBlock(id = "latch_${cell.row}_${cell.col}", position = cell, direction = direction)
            remaining -= cell
        }
        order
    }

    private val windowLatchDirectionByPosition: Map<CellPosition, SlideDirection> =
        windowLatchGeneration.associate { it.position to it.direction }

    fun windowLatchDirection(row: Int, col: Int): SlideDirection = windowLatchDirectionByPosition.getValue(CellPosition(row, col))

    /**
     * The exact order [windowLatchGeneration] assigned latches in — already
     * a valid release order by construction (see that property's doc
     * comment). Used by both `SlideOutGameTest`'s end-to-end replay and the
     * instrumented flow test's tap sequence, the same role
     * `dariusSolutionPath`/`passingBySolution` play for their own puzzles.
     */
    val windowLatchSolutionOrder: List<LatchBlock> = windowLatchGeneration

    // Flavor-only responses at Daniel 6:10's real decision point — he "went
    // to his house... and prayed, and gave thanks before his God, as he did
    // before." No branching, matching DavidGoliathContent.choiceOptions.
    val choiceOptions: List<ChoiceOptionDef> = listOf(
        ChoiceOptionDef("thankful", R.string.daniel_choice_option_1, R.string.daniel_choice_reaction_1),
        ChoiceOptionDef("trusting", R.string.daniel_choice_option_2, R.string.daniel_choice_reaction_2),
        ChoiceOptionDef("unafraid", R.string.daniel_choice_option_3, R.string.daniel_choice_reaction_3),
    )

    /** How many math problems (one shield ring each) form the Angel's Shield puzzle. */
    const val LIONS_DEN_PROBLEM_COUNT = 5

    // Daniel's dawn hurry through the palace to the lions' den (6:19),
    // reframed with the player's own joystick-driven "Race to the Den" maze
    // — replaces the older blocky D-pad maze (was "Darius's Maze") with a
    // real hand-drawn corridor maze (game art/Race to the Den Maze.png),
    // navigated by the same continuous-movement joystick engine as Good
    // Samaritan's "mini dungeon" (game/puzzles/racemaze, a thin-wall
    // adaptation of game/puzzles/dungeon's collision model — walls here are
    // drawn ON cell boundaries, not whole blocked cells).
    //
    // Traced from "Race to the Den Maze outline 2.png" (a thick red-ink
    // tracing laid directly over the real background art, at the same size
    // and crop — supersedes an earlier, separately-exported outline image
    // that traced the same walls but wasn't guaranteed pixel-aligned to
    // `bg_daniel_race_to_the_den_maze.png`, the actual shipped background;
    // re-tracing from this exact overlay confirmed the wall data itself was
    // already correct — only one far corner cell differed — but is what let
    // the doorway/border measurements below be trusted precisely) and
    // independently cross-checked against "Race to the Den Maze successful
    // path.png" (a blue-ink solution overlay of one valid route,
    // entering/exiting at the same row 7 this trace resolved to). Verified
    // solvable by BFS/flood-fill: 192 of the 196 cells form one connected
    // body; the excluded 4 cells (5,10)/(5,11)/(6,11)/(7,11) are a
    // legitimate dead-end pocket, not on any required path.
    const val RACE_MAZE_SIZE = 14

    /**
     * The castle/den doorways are drawn straddling the row 6/7 boundary
     * line in the art (not centered inside row 7's own cell) and pushed
     * out horizontally as close to their own border as
     * [RaceMazeGame.PLAYER_RADIUS] safely allows, rather than that cell's
     * usual `col + 0.5` center — both measured directly from the exact
     * overlay: the doorway gap in both borders centers almost exactly on
     * grid line 7 (row 6/7's shared boundary), not row 7's own cell-center
     * line. Using the ordinary `(col + 0.5, row + 0.5)` cell-center
     * convention here visually placed the character about half a cell
     * below the real opening and well short of the border itself.
     */
    val raceMazeStart: Vector2 = Vector2(RaceMazeGame.PLAYER_RADIUS, 7.0f)
    val raceMazeGoal: Vector2 = Vector2(RACE_MAZE_SIZE - RaceMazeGame.PLAYER_RADIUS, 7.0f)

    /** `raceMazeVerticalWalls[row]`'s char at index `col` = wall between `(row,col)` and `(row,col+1)`. `1` = wall, `.` = open. */
    val raceMazeVerticalWalls: List<String> = listOf(
        "...11..1.....",
        "1...11..1.1.1",
        "1.....11..111",
        ".1...111..1.1",
        "1.1...1..1..1",
        "11..111111.11",
        "11..1..11.111",
        "111...11.1111",
        "...11....11.1",
        ".1.111.11.11.",
        "1.11111.11111",
        "11111.11.1.11",
        ".11..1...11..",
        ".....1....1..",
    )

    /** `raceMazeHorizontalWalls[row]`'s char at index `col` = wall between `(row,col)` and `(row+1,col)`. `1` = wall, `.` = open. */
    val raceMazeHorizontalWalls: List<String> = listOf(
        ".11...1.1.111.",
        ".1.11....1....",
        "..1111...1.1..",
        ".1.111..1.11..",
        "..1.1.1...11..",
        "..111..1..1...",
        "....11...1....",
        "..11.111...1..",
        "1..1..11.1..1.",
        ".......11.....",
        ".1...1..1.1...",
        "......1.1..11.",
        "11.111.111..11",
    )

    /**
     * A hand-verified 31-move BFS shortest solution from [raceMazeStart] to
     * [raceMazeGoal], collapsed into straight-line waypoints (cell-center
     * coordinates, matching [GoodSamaritanContent.dungeonRouteWaypoints]'s
     * own `Vector2(col + 0.5f, row + 0.5f)` convention, except the first and
     * last points which reuse [raceMazeStart]/[raceMazeGoal] directly (their
     * own doorway-aligned position, not a plain cell center). Used by the
     * instrumented flow test to steer the real joystick deterministically,
     * and doubles as an automatic re-verification that the hand-traced wall
     * data above is actually solvable.
     */
    val raceMazeSolutionWaypoints: List<Vector2> = listOf(
        raceMazeStart,
        Vector2(0.5f, 3.5f),
        Vector2(1.5f, 3.5f),
        Vector2(1.5f, 2.5f),
        Vector2(6.5f, 2.5f),
        Vector2(6.5f, 1.5f),
        Vector2(8.5f, 1.5f),
        Vector2(8.5f, 2.5f),
        Vector2(10.5f, 2.5f),
        Vector2(10.5f, 1.5f),
        Vector2(9.5f, 1.5f),
        Vector2(9.5f, 0.5f),
        Vector2(13.5f, 0.5f),
        raceMazeGoal,
    )
}
