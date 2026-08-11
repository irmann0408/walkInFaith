package com.bibleadventures.game.stories

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.bibleadventures.R

data class SudokuIconDef(val key: String, @DrawableRes val iconRes: Int, @StringRes val nameRes: Int)

/**
 * Static content for Esther: The Threat — the third of 5 short Esther
 * chapters. Kept separate from the game engine packages under `game/puzzles`
 * so those stay reusable by future chapters.
 */
object EstherThreatContent {

    val introDialogueLines: List<Int> = listOf(
        R.string.esther_threat_intro_line_1,
        R.string.esther_threat_intro_line_2,
    )

    val decreeContextLines: List<Int> = listOf(
        R.string.esther_threat_decree_context_line_1,
        R.string.esther_threat_decree_context_line_2,
    )

    val mourningContextLines: List<Int> = listOf(
        R.string.esther_threat_mourning_context_line_1,
        R.string.esther_threat_mourning_context_line_2,
    )

    val sudokuIcons: List<SudokuIconDef> = listOf(
        SudokuIconDef("star", R.drawable.ic_sudoku_star, R.string.esther_threat_icon_star),
        SudokuIconDef("moon", R.drawable.ic_sudoku_moon, R.string.esther_threat_icon_moon),
        SudokuIconDef("sun", R.drawable.ic_sudoku_sun, R.string.esther_threat_icon_sun),
        SudokuIconDef("drop", R.drawable.ic_sudoku_drop, R.string.esther_threat_icon_drop),
        SudokuIconDef("leaf", R.drawable.ic_sudoku_leaf, R.string.esther_threat_icon_leaf),
    )

    /**
     * A 5x5 grid where each row/column uses every icon exactly once
     * (derived from the cyclic Latin square cell = (row + col) mod 5).
     * Only these cells are pre-filled as immutable givens — the other 10
     * cells (2 per row) are left for the player to work out by elimination,
     * matching "1 messenger per row completed."
     */
    val sudokuGivens: Map<Pair<Int, Int>, String> = mapOf(
        (0 to 0) to "star", (0 to 1) to "moon", (0 to 4) to "leaf",
        (1 to 0) to "moon", (1 to 2) to "drop", (1 to 4) to "star",
        (2 to 0) to "sun", (2 to 1) to "drop", (2 to 3) to "star",
        (3 to 1) to "leaf", (3 to 2) to "star", (3 to 4) to "sun",
        (4 to 0) to "leaf", (4 to 2) to "moon", (4 to 3) to "sun",
    )
}
