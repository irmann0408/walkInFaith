package com.bibleadventures.game.stories

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.geometry.Offset
import com.bibleadventures.R
import com.bibleadventures.game.puzzles.gridmaze.Direction
import com.bibleadventures.game.puzzles.gridmaze.GridPosition
import com.bibleadventures.game.puzzles.rhythmlane.RhythmLaneChart
import com.bibleadventures.game.puzzles.rhythmlane.RhythmNote
import com.bibleadventures.game.puzzles.stealth.GuardDef
import com.bibleadventures.game.puzzles.stealth.GuardPatrolStep

data class SudokuIconDef(val key: String, @DrawableRes val iconRes: Int, @StringRes val nameRes: Int)

/**
 * Static content for Esther's Rescue of Her People — one chapter built from
 * 4 sequential mini-puzzles (Royal Attire, Courtyard Stealth, Messenger
 * Sudoku, Corridor Courage Meter). Kept separate from the game engine
 * packages under `game/puzzles` so those stay reusable by future chapters.
 *
 * This merges what were briefly 5 separate chapters (New Queen, Secret
 * Plot, Threat, Brave Approach, Banquets & Rescue) back into one, per
 * playtesting feedback that splitting them felt disjointed. The banquet
 * jigsaw mini-game (`dragsort`, the same engine Organize the Ark already
 * uses) is dropped entirely — a playtester found it a repeat and too easy.
 * Reveal Haman's Plot and its surrounding context cards (banquet prep, the
 * second banquet, Haman's downfall) were dropped too, per the user's
 * explicit request to tighten the chapter's tail end — Corridor now leads
 * straight into the Lesson. The Esther 7:3 scripture card tied to that
 * story beat is still awarded on the Reward screen even though the beat
 * itself is no longer played through.
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

    /**
     * Purely visual distractors for Royal Attire — see [DecoyItem]'s doc
     * comment. 20 entries, 20 *distinct* new icons (not the same shape
     * repeated) so the chamber reads as genuinely cluttered with palace
     * belongings, not just 5 real items floating on an empty background.
     * Hand-placed across the whole frame (`bg_esther_new_queen_chamber.xml`
     * is a uniform camouflage-blob backdrop with no sky/ground split to
     * avoid), staying clear of the 5 real items' own positions above.
     */
    val royalAttireDecoys: List<DecoyItem> = listOf(
        DecoyItem("decoy_mirror", Offset(0.08f, 0.12f), R.drawable.ic_decoy_mirror),
        DecoyItem("decoy_vase", Offset(0.28f, 0.12f), R.drawable.ic_decoy_vase),
        DecoyItem("decoy_candle", Offset(0.48f, 0.10f), R.drawable.ic_decoy_candle),
        DecoyItem("decoy_book", Offset(0.68f, 0.12f), R.drawable.ic_decoy_book),
        DecoyItem("decoy_goblet", Offset(0.88f, 0.12f), R.drawable.ic_decoy_goblet),
        DecoyItem("decoy_fan", Offset(0.08f, 0.35f), R.drawable.ic_decoy_fan),
        DecoyItem("decoy_jewelry_box", Offset(0.28f, 0.35f), R.drawable.ic_decoy_jewelry_box),
        DecoyItem("decoy_comb", Offset(0.48f, 0.35f), R.drawable.ic_decoy_comb),
        DecoyItem("decoy_pillow", Offset(0.68f, 0.35f), R.drawable.ic_decoy_pillow),
        DecoyItem("decoy_tassel", Offset(0.88f, 0.35f), R.drawable.ic_decoy_tassel),
        DecoyItem("decoy_chair", Offset(0.08f, 0.58f), R.drawable.ic_decoy_chair),
        DecoyItem("decoy_plant", Offset(0.28f, 0.60f), R.drawable.ic_decoy_plant),
        DecoyItem("decoy_bowl", Offset(0.48f, 0.58f), R.drawable.ic_decoy_bowl),
        DecoyItem("decoy_ring", Offset(0.68f, 0.58f), R.drawable.ic_decoy_ring),
        DecoyItem("decoy_necklace", Offset(0.88f, 0.58f), R.drawable.ic_decoy_necklace),
        DecoyItem("decoy_scroll", Offset(0.08f, 0.85f), R.drawable.ic_decoy_scroll),
        DecoyItem("decoy_oil_lamp", Offset(0.28f, 0.85f), R.drawable.ic_decoy_oil_lamp),
        DecoyItem("decoy_rug", Offset(0.48f, 0.88f), R.drawable.ic_decoy_rug),
        DecoyItem("decoy_hairpin", Offset(0.68f, 0.85f), R.drawable.ic_decoy_hairpin),
        DecoyItem("decoy_hairbrush", Offset(0.88f, 0.85f), R.drawable.ic_decoy_hairbrush),
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
     * One guard walks back and forth across the courtyard's open middle
     * row — left, middle, right, middle, left, ... — hand-authored,
     * deterministic, and verified solvable by tracing every move by hand
     * (same discipline as every other map in this app). Includes the
     * middle cell on *both* legs of the patrol (not just left->right) so
     * the cycle is a genuine back-and-forth walk rather than a left/right
     * teleport that skips over the middle — [StealthGame.onDirectionPressed]
     * advances one patrol step per player move via `patrol[turnIndex %
     * patrol.size]`, so a 2-step list would otherwise jump straight from
     * the right cell back to the left one with nothing in between. The
     * guard's watched cell is exactly where it's standing, not a projected
     * cone, so the pattern is easy for a child to observe and predict.
     */
    val courtyardGuards: List<GuardDef> = listOf(
        GuardDef(
            patrol = listOf(
                GuardPatrolStep(position = GridPosition(2, 0), watchedCells = setOf(GridPosition(2, 0))),
                GuardPatrolStep(position = GridPosition(2, 1), watchedCells = setOf(GridPosition(2, 1))),
                GuardPatrolStep(position = GridPosition(2, 2), watchedCells = setOf(GridPosition(2, 2))),
                GuardPatrolStep(position = GridPosition(2, 1), watchedCells = setOf(GridPosition(2, 1))),
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

    /**
     * A short, evenly-paced, hand-authored 3-lane pattern (800ms between
     * notes — moderate tempo, appropriate for a 7+ audience) that loops
     * forever. "Down and back" across the lanes (left, center, right,
     * right, center, left) so the pattern is easy to learn and predict.
     */
    val corridorChart = RhythmLaneChart(
        notes = listOf(
            RhythmNote("beat_1", lane = 0, hitTimeMs = 400),
            RhythmNote("beat_2", lane = 1, hitTimeMs = 1200),
            RhythmNote("beat_3", lane = 2, hitTimeMs = 2000),
            RhythmNote("beat_4", lane = 2, hitTimeMs = 2800),
            RhythmNote("beat_5", lane = 1, hitTimeMs = 3600),
            RhythmNote("beat_6", lane = 0, hitTimeMs = 4400),
        ),
        loopDurationMs = 4800,
    )

    /** How many successful lane hits complete the corridor (see RhythmLaneGameState) — spans more than one chart loop. */
    const val CORRIDOR_REQUIRED_HITS = 10
}
