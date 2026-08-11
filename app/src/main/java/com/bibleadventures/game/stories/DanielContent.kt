package com.bibleadventures.game.stories

import androidx.annotation.StringRes
import androidx.compose.ui.geometry.Offset
import com.bibleadventures.R
import com.bibleadventures.game.puzzles.dodge.DodgeBeat
import com.bibleadventures.game.puzzles.dodge.DodgeLane
import com.bibleadventures.game.puzzles.gridmaze.Direction

/** One tappable light in the Lions' Den "connect in order" puzzle. Chapter-local — extract only if a second chapter needs this shape. */
data class LightPointDef(val id: String, val position: Offset, @StringRes val nameRes: Int)

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

    // A short, discrete "hurry to the prayer room" sequence, mirroring
    // DavidGoliathContent.dodgeBeats exactly — this is a literal reskin of
    // the same engine, not a new mechanic. An official blocks one side of
    // the hallway; Daniel steps to the clear side to keep moving. Framed as
    // getting past, not hiding — Daniel 6:10 has him praying openly.
    val stealthBeats: List<DodgeBeat> = listOf(
        DodgeBeat("beat_1", DodgeLane.LEFT),
        DodgeBeat("beat_2", DodgeLane.RIGHT),
        DodgeBeat("beat_3", DodgeLane.LEFT),
    )

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

    // Five lights arranged in an arc/dome over Daniel, so connecting them in
    // order reads as a shield of light forming overhead once complete.
    val lionsDenPoints: List<LightPointDef> = listOf(
        LightPointDef("light_1", Offset(0.15f, 0.55f), R.string.daniel_light_1),
        LightPointDef("light_2", Offset(0.3f, 0.3f), R.string.daniel_light_2),
        LightPointDef("light_3", Offset(0.5f, 0.18f), R.string.daniel_light_3),
        LightPointDef("light_4", Offset(0.7f, 0.3f), R.string.daniel_light_4),
        LightPointDef("light_5", Offset(0.85f, 0.55f), R.string.daniel_light_5),
    )

    val lionsDenPointIds: List<String> get() = lionsDenPoints.map { it.id }

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
