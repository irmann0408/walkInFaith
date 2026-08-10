package com.bibleadventures.game.stories

import androidx.annotation.StringRes
import androidx.compose.ui.geometry.Offset
import com.bibleadventures.R

data class ChoiceOptionDef(val id: String, @StringRes val textRes: Int, @StringRes val reactionTextRes: Int)

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
}
