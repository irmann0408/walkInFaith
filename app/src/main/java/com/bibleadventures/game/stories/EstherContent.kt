package com.bibleadventures.game.stories

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.geometry.Offset
import com.bibleadventures.R
import com.bibleadventures.game.puzzles.decisionpath.DecisionStep
import com.bibleadventures.game.puzzles.gridmaze.Direction
import com.bibleadventures.game.puzzles.gridmaze.GridPosition
import com.bibleadventures.game.puzzles.stealth.GuardDef
import com.bibleadventures.game.puzzles.stealth.GuardPatrolStep

data class SudokuIconDef(val key: String, @DrawableRes val iconRes: Int, @StringRes val nameRes: Int)

/**
 * Static content for Esther's Rescue of Her People — one chapter built from
 * 5 sequential mini-puzzles (Royal Attire, Courtyard Stealth, Messenger
 * Sudoku, Corridor Courage Meter, Reveal Haman's Plot). Kept separate from
 * the game engine packages under `game/puzzles` so those stay reusable by
 * future chapters.
 *
 * This merges what were briefly 5 separate chapters (New Queen, Secret
 * Plot, Threat, Brave Approach, Banquets & Rescue) back into one, per
 * playtesting feedback that splitting them felt disjointed. The banquet
 * jigsaw mini-game (`dragsort`, the same engine Organize the Ark already
 * uses) is dropped entirely — a playtester found it a repeat and too easy
 * — so the banquet-preparation beats below are narrative-only context, not
 * a puzzle.
 */
object EstherContent {

    val introDialogueLines: List<Int> = listOf(
        R.string.esther_new_queen_intro_line_1,
        R.string.esther_new_queen_intro_line_2,
    )

    val searchContextLines: List<Int> = listOf(
        R.string.esther_new_queen_search_context_line_1,
        R.string.esther_new_queen_search_context_line_2,
    )

    val crownedContextLines: List<Int> = listOf(
        R.string.esther_new_queen_crowned_context_line_1,
        R.string.esther_new_queen_crowned_context_line_2,
    )

    val greetingChoiceOptions: List<ChoiceOptionDef> = listOf(
        ChoiceOptionDef("kindly", R.string.esther_new_queen_choice_option_1, R.string.esther_new_queen_choice_reaction_1),
        ChoiceOptionDef("listen", R.string.esther_new_queen_choice_option_2, R.string.esther_new_queen_choice_reaction_2),
        ChoiceOptionDef("patient", R.string.esther_new_queen_choice_option_3, R.string.esther_new_queen_choice_reaction_3),
    )

    // Large tap targets are applied at render time regardless of icon size,
    // to avoid pixel-hunting (spec section 9).
    val royalAttireItems: List<HiddenItemDef> = listOf(
        HiddenItemDef("item_crown", R.drawable.ic_item_crown, R.string.esther_new_queen_item_crown, Offset(0.5f, 0.2f)),
        HiddenItemDef("item_robe", R.drawable.ic_item_robe, R.string.esther_new_queen_item_robe, Offset(0.2f, 0.55f)),
        HiddenItemDef("item_sash", R.drawable.ic_item_sash, R.string.esther_new_queen_item_sash, Offset(0.78f, 0.45f)),
        HiddenItemDef("item_perfume", R.drawable.ic_item_perfume, R.string.esther_new_queen_item_perfume, Offset(0.35f, 0.8f)),
        HiddenItemDef("item_sandals", R.drawable.ic_item_sandals, R.string.esther_new_queen_item_sandals, Offset(0.65f, 0.75f)),
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

    val decisionChoiceOptions: List<ChoiceOptionDef> = listOf(
        ChoiceOptionDef("go_to_king", R.string.esther_brave_approach_choice_option_1, R.string.esther_brave_approach_choice_reaction_1),
        ChoiceOptionDef("trust_god", R.string.esther_brave_approach_choice_option_2, R.string.esther_brave_approach_choice_reaction_2),
        ChoiceOptionDef("if_i_perish", R.string.esther_brave_approach_choice_option_3, R.string.esther_brave_approach_choice_reaction_3),
    )

    val fastingContextLines: List<Int> = listOf(
        R.string.esther_brave_approach_fasting_context_line_1,
        R.string.esther_brave_approach_fasting_context_line_2,
    )

    val scepterContextLines: List<Int> = listOf(
        R.string.esther_brave_approach_scepter_context_line_1,
        R.string.esther_brave_approach_scepter_context_line_2,
    )

    /** How many total tap-progress "ticks" fill the courage meter (see MeterGameState). */
    const val CORRIDOR_REQUIRED_PROGRESS = 10

    /** Narrative-only now that the banquet jigsaw is dropped — no puzzle follows. */
    val planningContextLines: List<Int> = listOf(
        R.string.esther_banquets_rescue_planning_context_line_1,
        R.string.esther_banquets_rescue_planning_context_line_2,
    )

    val secondBanquetContextLines: List<Int> = listOf(
        R.string.esther_banquets_rescue_second_banquet_context_line_1,
        R.string.esther_banquets_rescue_second_banquet_context_line_2,
    )

    val savedContextLines: List<Int> = listOf(
        R.string.esther_banquets_rescue_saved_context_line_1,
        R.string.esther_banquets_rescue_saved_context_line_2,
        R.string.esther_banquets_rescue_saved_context_line_3,
    )

    /** A short, 3-step guided sequence reusing `decisionpath` — same engine as Jericho's march. */
    val revealSteps: List<DecisionStep> = listOf(
        DecisionStep("begin", "speak_calmly", listOf("speak_calmly", "shout_angrily")),
        DecisionStep("reveal", "tell_truth", listOf("tell_truth", "stay_silent")),
        DecisionStep("name", "name_haman", listOf("name_haman", "blame_another")),
    )

    val revealStepPromptLabels: Map<String, Int> = mapOf(
        "begin" to R.string.esther_banquets_rescue_reveal_step1_prompt,
        "reveal" to R.string.esther_banquets_rescue_reveal_step2_prompt,
        "name" to R.string.esther_banquets_rescue_reveal_step3_prompt,
    )

    data class RevealOptionDef(val id: String, val labelRes: Int)

    val revealOptions: List<RevealOptionDef> = listOf(
        RevealOptionDef("speak_calmly", R.string.esther_banquets_rescue_reveal_option_speak_calmly),
        RevealOptionDef("shout_angrily", R.string.esther_banquets_rescue_reveal_option_shout_angrily),
        RevealOptionDef("tell_truth", R.string.esther_banquets_rescue_reveal_option_tell_truth),
        RevealOptionDef("stay_silent", R.string.esther_banquets_rescue_reveal_option_stay_silent),
        RevealOptionDef("name_haman", R.string.esther_banquets_rescue_reveal_option_name_haman),
        RevealOptionDef("blame_another", R.string.esther_banquets_rescue_reveal_option_blame_another),
    )
}
