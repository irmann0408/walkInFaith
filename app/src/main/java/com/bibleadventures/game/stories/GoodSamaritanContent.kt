package com.bibleadventures.game.stories

import com.bibleadventures.R
import com.bibleadventures.game.puzzles.gridmaze.Direction

/**
 * Static content for the Good Samaritan chapter. Kept separate from
 * `game/puzzles/gridmaze` so that pure engine stays reusable by any future
 * maze-shaped chapter — this file is the only thing that's Good-Samaritan-
 * specific.
 */
object GoodSamaritanContent {

    val introDialogueLines: List<Int> = listOf(
        R.string.good_samaritan_intro_line_1,
        R.string.good_samaritan_intro_line_2,
    )

    val exploreContextLines: List<Int> = listOf(
        R.string.good_samaritan_explore_context_line_1,
        R.string.good_samaritan_explore_context_line_2,
    )

    // Shown once the traveler is treated, paralleling Luke 10:34's specific,
    // non-branching sequence of care — nothing here is a player choice.
    val helpingBeatLines: List<Int> = listOf(
        R.string.good_samaritan_helping_beat_line_1,
        R.string.good_samaritan_helping_beat_line_2,
        R.string.good_samaritan_helping_beat_line_3,
    )

    // 10x10 map, row-major. '.' path, '#' rocky-terrain wall, 'X' bandit wall
    // (mechanically identical to '#' — see GridMazeGame; the two only differ
    // in which icon the screen draws for that cell), 'M' medicine, 'T' the
    // injured traveler, 'I' the Inn, 'S' the start (a walkable path tile).
    // Verified solvable by hand (BFS from start): start -> the medicine at
    // (0,2) -> a connected route to the traveler at (2,9) -> a separate
    // connected route to the Inn at (9,9), all in one connected component.
    // Not shuffled per playthrough (unlike other chapters' item order) —
    // randomizing tile layout risks an unsolvable maze with no in-app
    // solver/validator in scope for this pass.
    val mapLayout: List<String> = listOf(
        "S.M.......",
        ".##.###X..",
        ".......#.T",
        "##.###.#.#",
        "...#M.....",
        ".#.######.",
        ".#......#.",
        "X#####.##.",
        ".....#....",
        ".......##I",
    )

    // A hand-verified 20-move solution (9 right, 2 down, 1 left, 2 down,
    // 1 right, 5 down) — collects the medicine at (0,2) while crossing row
    // 0, treats the traveler at (2,9), then reaches the Inn at (9,9).
    // Used by the instrumented flow test to replay a known-solvable path
    // deterministically, since the map itself is intentionally not shuffled.
    val solutionPath: List<Direction> = listOf(
        Direction.RIGHT, Direction.RIGHT, Direction.RIGHT, Direction.RIGHT, Direction.RIGHT,
        Direction.RIGHT, Direction.RIGHT, Direction.RIGHT, Direction.RIGHT,
        Direction.DOWN, Direction.DOWN,
        Direction.LEFT,
        Direction.DOWN, Direction.DOWN,
        Direction.RIGHT,
        Direction.DOWN, Direction.DOWN, Direction.DOWN, Direction.DOWN, Direction.DOWN,
    )
}
