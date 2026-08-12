package com.bibleadventures.game.stories

import androidx.compose.ui.geometry.Offset
import com.bibleadventures.R
import com.bibleadventures.game.puzzles.gridmaze.Direction
import com.bibleadventures.game.puzzles.rhythmlane.RhythmLaneChart
import com.bibleadventures.game.puzzles.rhythmlane.RhythmNote

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

    val stealthContextLines: List<Int> = listOf(
        R.string.daniel_stealth_context_line_1,
        R.string.daniel_stealth_context_line_2,
    )

    /**
     * A literal reskin of [DavidGoliathContent.crossingValleyChart] — same
     * `rhythmlane`-via-`onLaneAvoided` shape, same 3 lanes/3 required
     * avoids, only the hazard (an official blocking the hallway instead of
     * a rolling rock) and background differ. Framed as getting past, not
     * hiding — Daniel 6:10 has him praying openly.
     */
    val hurryToPrayChart = RhythmLaneChart(
        notes = listOf(
            RhythmNote("official_1", lane = 1, hitTimeMs = 800),
            RhythmNote("official_2", lane = 0, hitTimeMs = 1800),
            RhythmNote("official_3", lane = 2, hitTimeMs = 2800),
        ),
        loopDurationMs = 3600,
    )
    const val HURRY_TO_PRAY_REQUIRED_AVOIDS = 3

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
