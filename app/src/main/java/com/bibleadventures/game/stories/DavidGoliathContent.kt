package com.bibleadventures.game.stories

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.geometry.Offset
import com.bibleadventures.R
import com.bibleadventures.game.puzzles.rhythmlane.RhythmLaneChart
import com.bibleadventures.game.puzzles.rhythmlane.RhythmNote

data class ChoiceOptionDef(val id: String, @StringRes val textRes: Int, @StringRes val reactionTextRes: Int)

/** One numeral count in the Sheep Counting matching game — [numeralIconRes] and [sheepGroupIconRes] share a pairKey. */
data class SheepCountDef(val count: Int, @DrawableRes val numeralIconRes: Int, @DrawableRes val sheepGroupIconRes: Int, @StringRes val nameRes: Int)

/**
 * Static content for the David and Goliath chapter. Kept separate from the
 * game engine packages under `game/puzzles` so those stay reusable by future
 * chapters — this file is the only thing that's David-and-Goliath-specific.
 */
object DavidGoliathContent {

    val introDialogueLines: List<Int> = listOf(
        R.string.david_goliath_intro_line_1,
        R.string.david_goliath_intro_line_2,
    )

    val sheepCountingContextLines: List<Int> = listOf(
        R.string.david_goliath_sheep_counting_context_line_1,
        R.string.david_goliath_sheep_counting_context_line_2,
    )

    val dodgeContextLines: List<Int> = listOf(
        R.string.david_goliath_dodge_context_line_1,
        R.string.david_goliath_dodge_context_line_2,
    )

    val chooseStonesContextLines: List<Int> = listOf(
        R.string.david_goliath_choose_stones_context_line_1,
        R.string.david_goliath_choose_stones_context_line_2,
    )

    val slingPracticeContextLines: List<Int> = listOf(
        R.string.david_goliath_sling_practice_context_line_1,
        R.string.david_goliath_sling_practice_context_line_2,
    )

    // All five stones share one icon — 1 Samuel 17:40 just says "five smooth
    // stones," with no textual basis for five distinct rock types; only the
    // name/position varies. Large tap targets are applied at render time
    // regardless of icon size, to avoid pixel-hunting (spec section 9).
    val stones: List<HiddenItemDef> = listOf(
        HiddenItemDef("stone_1", R.drawable.ic_stone_smooth, R.string.stone_1, Offset(0.15f, 0.25f)),
        HiddenItemDef("stone_2", R.drawable.ic_stone_smooth, R.string.stone_2, Offset(0.7f, 0.2f)),
        HiddenItemDef("stone_3", R.drawable.ic_stone_smooth, R.string.stone_3, Offset(0.4f, 0.45f)),
        HiddenItemDef("stone_4", R.drawable.ic_stone_smooth, R.string.stone_4, Offset(0.85f, 0.55f)),
        HiddenItemDef("stone_5", R.drawable.ic_stone_smooth, R.string.stone_5, Offset(0.25f, 0.75f)),
    )

    // Positioned like a stone (own fixed spot, shuffled in with the real stones)
    // rather than extending the tray-based DecoyItemDef with an unused position.
    val riverbedDecoy: HiddenItemDef = HiddenItemDef("decoy_boot", R.drawable.ic_decoy_boot, R.string.decoy_boot, Offset(0.6f, 0.85f))

    val choiceOptions: List<ChoiceOptionDef> = listOf(
        ChoiceOptionDef("trust", R.string.david_goliath_choice_option_1, R.string.david_goliath_choice_reaction_1),
        ChoiceOptionDef("unafraid", R.string.david_goliath_choice_option_2, R.string.david_goliath_choice_reaction_2),
        ChoiceOptionDef("declaration", R.string.david_goliath_choice_option_3, R.string.david_goliath_choice_reaction_3),
    )

    // Numerals 1-5 matched to a same-count sheep icon — unlike the stones,
    // the count itself is the point, so each count needs its own distinct art.
    val sheepCounts: List<SheepCountDef> = listOf(
        SheepCountDef(1, R.drawable.ic_numeral_1, R.drawable.ic_sheep_group_1, R.string.david_goliath_sheep_count_1),
        SheepCountDef(2, R.drawable.ic_numeral_2, R.drawable.ic_sheep_group_2, R.string.david_goliath_sheep_count_2),
        SheepCountDef(3, R.drawable.ic_numeral_3, R.drawable.ic_sheep_group_3, R.string.david_goliath_sheep_count_3),
        SheepCountDef(4, R.drawable.ic_numeral_4, R.drawable.ic_sheep_group_4, R.string.david_goliath_sheep_count_4),
        SheepCountDef(5, R.drawable.ic_numeral_5, R.drawable.ic_sheep_group_5, R.string.david_goliath_sheep_count_5),
    )

    /**
     * Reuses `rhythmlane`'s lane-steering shape (established by Feeding the
     * 5,000's Gathering the Leftovers) via the inverse `RhythmLaneGame.onLaneAvoided`
     * — David steers himself OUT of a rolling rock's lane instead of into a
     * falling item's lane. 3 required avoids, one rock per lane (lanes
     * 1,0,2 in that order, so the very first rock lands in the character's
     * default starting lane, forcing a real first move rather than an
     * automatic free avoid); even 1000ms gaps give plenty of reaction time.
     */
    val crossingValleyChart = RhythmLaneChart(
        notes = listOf(
            RhythmNote("rock_1", lane = 1, hitTimeMs = 800),
            RhythmNote("rock_2", lane = 0, hitTimeMs = 1800),
            RhythmNote("rock_3", lane = 2, hitTimeMs = 2800),
        ),
        loopDurationMs = 3600,
    )
    const val CROSSING_VALLEY_REQUIRED_AVOIDS = 3
}
