package com.bibleadventures.game.stories

import com.bibleadventures.R
import com.bibleadventures.game.puzzles.decisionpath.DecisionStep

/**
 * Static content for the Esther's Rescue of Her People chapter. Kept
 * separate from `game/puzzles/decisionpath` so that pure engine stays
 * reusable — this file is the only thing that's Esther-specific.
 */
object EstherContent {

    val introDialogueLines: List<Int> = listOf(
        R.string.esther_intro_line_1,
        R.string.esther_intro_line_2,
    )

    val kingsTroubleContextLines: List<Int> = listOf(
        R.string.esther_kings_trouble_context_line_1,
        R.string.esther_kings_trouble_context_line_2,
    )

    val hamansAngerContextLines: List<Int> = listOf(
        R.string.esther_hamans_anger_context_line_1,
        R.string.esther_hamans_anger_context_line_2,
    )

    // Flavor-only responses at Esther 4:14/4:16's real decision point — no branching,
    // matching DanielContent.choiceOptions and DavidGoliathContent.choiceOptions.
    val choiceOptions: List<ChoiceOptionDef> = listOf(
        ChoiceOptionDef("go_to_king", R.string.esther_choice_option_1, R.string.esther_choice_reaction_1),
        ChoiceOptionDef("trust_god", R.string.esther_choice_option_2, R.string.esther_choice_reaction_2),
        ChoiceOptionDef("if_i_perish", R.string.esther_choice_option_3, R.string.esther_choice_reaction_3),
    )

    val scepterContextLines: List<Int> = listOf(
        R.string.esther_scepter_context_line_1,
        R.string.esther_scepter_context_line_2,
    )

    // The Two Banquets (Esther 5:3-8, 7:1-6): she deliberately waits through the first
    // banquet and the second invitation before finally speaking at the second banquet —
    // teaches discernment/timing, distinct in feel from Jericho's obedience-vs-force
    // mechanic even though both reuse the same DecisionPathGame engine underneath.
    val banquetSteps: List<DecisionStep> = listOf(
        DecisionStep("first_banquet", correctOptionId = "wait", optionIds = listOf("wait", "speak_now")),
        DecisionStep("second_invite", correctOptionId = "wait", optionIds = listOf("wait", "speak_now")),
        DecisionStep("second_banquet", correctOptionId = "speak_now", optionIds = listOf("speak_now", "wait")),
    )

    val truthRevealedContextLines: List<Int> = listOf(
        R.string.esther_truth_revealed_context_line_1,
        R.string.esther_truth_revealed_context_line_2,
        R.string.esther_truth_revealed_context_line_3,
    )
}
