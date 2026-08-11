package com.bibleadventures.game.stories

import com.bibleadventures.R
import com.bibleadventures.game.puzzles.gridmaze.Direction
import com.bibleadventures.game.puzzles.gridmaze.GridPosition
import com.bibleadventures.game.puzzles.stealth.GuardDef
import com.bibleadventures.game.puzzles.stealth.GuardPatrolStep

/**
 * Static content for Esther: The Secret Plot — the second of 5 short Esther
 * chapters. Kept separate from the game engine packages under `game/puzzles`
 * so those stay reusable by future chapters.
 */
object EstherSecretPlotContent {

    val introDialogueLines: List<Int> = listOf(
        R.string.esther_secret_plot_intro_line_1,
        R.string.esther_secret_plot_intro_line_2,
    )

    val dangerContextLines: List<Int> = listOf(
        R.string.esther_secret_plot_danger_context_line_1,
        R.string.esther_secret_plot_danger_context_line_2,
    )

    val warnedContextLines: List<Int> = listOf(
        R.string.esther_secret_plot_warned_context_line_1,
        R.string.esther_secret_plot_warned_context_line_2,
    )

    // . = path, # = wall, S = start, G = goal. 5 rows x 3 cols.
    val courtyardMapLayout: List<String> = listOf(
        "..G",
        ".#.",
        "...",
        ".#.",
        ".S.",
    )

    /**
     * One guard alternates standing watch at the two side cells of the
     * courtyard's open middle row — hand-authored, deterministic, and
     * verified solvable by tracing every move by hand (same discipline as
     * every other map in this app). The guard's watched cell is exactly
     * where it's standing, not a projected cone, so the pattern is easy for
     * a child to observe and predict.
     */
    val courtyardGuards: List<GuardDef> = listOf(
        GuardDef(
            patrol = listOf(
                GuardPatrolStep(position = GridPosition(2, 0), watchedCells = setOf(GridPosition(2, 0))),
                GuardPatrolStep(position = GridPosition(2, 2), watchedCells = setOf(GridPosition(2, 2))),
            ),
        ),
    )

    /**
     * Hand-verified: LEFT up the left column never lands on a watched cell
     * at the moment of arrival, then crosses to the goal along the top row.
     */
    val courtyardSolutionPath: List<Direction> = listOf(
        Direction.LEFT,
        Direction.UP,
        Direction.UP,
        Direction.UP,
        Direction.UP,
        Direction.RIGHT,
        Direction.RIGHT,
    )
}
