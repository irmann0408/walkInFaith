package com.bibleadventures.game.stories

import androidx.compose.ui.geometry.Offset
import com.bibleadventures.R

/**
 * Static content for Esther: The New Queen — the first of 5 short Esther
 * chapters. Kept separate from the game engine packages under `game/puzzles`
 * so those stay reusable by future chapters.
 */
object EstherNewQueenContent {

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

    val choiceOptions: List<ChoiceOptionDef> = listOf(
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
}
