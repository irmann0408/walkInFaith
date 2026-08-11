package com.bibleadventures.game.stories

import com.bibleadventures.R

/**
 * Static content for Esther: The Brave Approach — the fourth of 5 short
 * Esther chapters. Kept separate from the game engine packages under
 * `game/puzzles` so those stay reusable by future chapters.
 */
object EstherBraveApproachContent {

    val introDialogueLines: List<Int> = listOf(
        R.string.esther_brave_approach_intro_line_1,
        R.string.esther_brave_approach_intro_line_2,
    )

    val fastingContextLines: List<Int> = listOf(
        R.string.esther_brave_approach_fasting_context_line_1,
        R.string.esther_brave_approach_fasting_context_line_2,
    )

    val scepterContextLines: List<Int> = listOf(
        R.string.esther_brave_approach_scepter_context_line_1,
        R.string.esther_brave_approach_scepter_context_line_2,
    )

    val choiceOptions: List<ChoiceOptionDef> = listOf(
        ChoiceOptionDef("go_to_king", R.string.esther_brave_approach_choice_option_1, R.string.esther_brave_approach_choice_reaction_1),
        ChoiceOptionDef("trust_god", R.string.esther_brave_approach_choice_option_2, R.string.esther_brave_approach_choice_reaction_2),
        ChoiceOptionDef("if_i_perish", R.string.esther_brave_approach_choice_option_3, R.string.esther_brave_approach_choice_reaction_3),
    )

    /** How many total tap-progress "ticks" fill the courage meter (see MeterGameState). */
    const val CORRIDOR_REQUIRED_PROGRESS = 10
}
