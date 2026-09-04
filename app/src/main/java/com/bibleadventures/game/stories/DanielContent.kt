package com.bibleadventures.game.stories

import androidx.compose.ui.geometry.Offset
import com.bibleadventures.R
import com.bibleadventures.game.puzzles.gridmaze.Direction
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

    val introDialogueLines: List<Int> = listOf(
        R.string.daniel_intro_line_1,
        R.string.daniel_intro_line_2,
    )

    val windowContextLines: List<Int> = listOf(
        R.string.daniel_window_context_line_1,
        R.string.daniel_window_context_line_2,
    )

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

    val lionsDenContextLines: List<Int> = listOf(
        R.string.daniel_lions_den_context_line_1,
        R.string.daniel_lions_den_context_line_2,
    )

    /** How many math problems (one per light) form the Angel's Shield puzzle. */
    const val LIONS_DEN_PROBLEM_COUNT = 5

    // Five positions arranged in an arc/dome over Daniel, so lighting them in
    // order reads as a shield of light forming overhead once complete. Purely
    // visual/progress now — the lights themselves aren't tappable; solving
    // [com.bibleadventures.ui.screens.daniel.DanielViewModel]'s randomly
    // generated math problems is what advances them.
    val lionsDenLightPositions: List<Offset> = listOf(
        Offset(0.15f, 0.55f),
        Offset(0.3f, 0.3f),
        Offset(0.5f, 0.18f),
        Offset(0.7f, 0.3f),
        Offset(0.85f, 0.55f),
    )

    val dariusContextLines: List<Int> = listOf(
        R.string.daniel_darius_context_line_1,
        R.string.daniel_darius_context_line_2,
    )

    // 7x7 map, row-major. '.' path, '#' wall, 'S' the start (a walkable path
    // tile), 'D' the lions' den (goal). No collectible/checkpoint tile —
    // this reframes the blueprint's "decree maze to find a stamp" (Darius
    // could not revoke his own sealed law, Daniel 6:8/6:15) as his dawn
    // hurry through the palace to the den (6:19): just reach the goal.
    // Verified solvable by hand (BFS from start): a single connected
    // component reaches (6,6) from (0,0). Not shuffled per playthrough,
    // same reasoning as GoodSamaritanContent.mapLayout.
    val dariusMapLayout: List<String> = listOf(
        "S..#...",
        "##.#.#.",
        "...#.#.",
        ".###.#.",
        ".#...#.",
        ".#.###.",
        "......D",
    )

    // A hand-verified 28-move BFS solution from (0,0) to the den at (6,6).
    // Used by the instrumented flow test to replay a known-solvable path
    // deterministically, since the map itself is intentionally not shuffled.
    val dariusSolutionPath: List<Direction> = listOf(
        Direction.RIGHT, Direction.RIGHT,
        Direction.DOWN, Direction.DOWN,
        Direction.LEFT, Direction.LEFT,
        Direction.DOWN, Direction.DOWN, Direction.DOWN, Direction.DOWN,
        Direction.RIGHT, Direction.RIGHT,
        Direction.UP, Direction.UP,
        Direction.RIGHT, Direction.RIGHT,
        Direction.UP, Direction.UP, Direction.UP, Direction.UP,
        Direction.RIGHT, Direction.RIGHT,
        Direction.DOWN, Direction.DOWN, Direction.DOWN, Direction.DOWN, Direction.DOWN,
        Direction.DOWN,
    )
}
