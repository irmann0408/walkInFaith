package com.bibleadventures.game.stories

import com.bibleadventures.R
import com.bibleadventures.game.puzzles.decisionpath.DecisionStep

/** One tappable action in the March and the Shout mechanic. */
data class MarchOptionDef(val id: String, val labelRes: Int, val iconRes: Int)

/**
 * Static content for the Battle of Jericho chapter. Kept separate from
 * `game/puzzles/decisionpath` so that pure engine stays reusable — this
 * file is the only thing that's Jericho-specific.
 */
object JerichoContent {

    val introDialogueLines: List<Int> = listOf(
        R.string.jericho_intro_line_1,
        R.string.jericho_intro_line_2,
    )

    val rahabHouseContextLines: List<Int> = listOf(
        R.string.jericho_rahab_house_context_line_1,
        R.string.jericho_rahab_house_context_line_2,
    )

    // Rahab's own act of helping the spies (Joshua 2) — narrative-only, matching Good
    // Samaritan's helping-beat precedent, per the user's steer that the march itself
    // (not a second puzzle here) is this chapter's real point.
    val rahabHelpingLines: List<Int> = listOf(
        R.string.jericho_rahab_helping_line_1,
        R.string.jericho_rahab_helping_line_2,
        R.string.jericho_rahab_helping_line_3,
    )

    // Flavor-only responses at Israel's real decision point — trusting God's unusual
    // plan rather than a normal battle plan — no branching, matching every other
    // chapter's Choice scene.
    val choiceOptions: List<ChoiceOptionDef> = listOf(
        ChoiceOptionDef("follow_plan", R.string.jericho_choice_option_1, R.string.jericho_choice_reaction_1),
        ChoiceOptionDef("trust_god", R.string.jericho_choice_option_2, R.string.jericho_choice_reaction_2),
        ChoiceOptionDef("obey_step_by_step", R.string.jericho_choice_option_3, R.string.jericho_choice_reaction_3),
    )

    // The March and the Shout (Joshua 6): 4 steps. Every "wrong" option across all 4
    // steps is a brute-force/premature alternative — the mechanic itself teaches
    // "obey the plan, don't force it," not just the surrounding narration. Step 4's
    // wrong option ("stay_silent") was the *correct* answer at step 2, reinforcing
    // that obedience means following the current instruction, not a fixed rule.
    val marchSteps: List<DecisionStep> = listOf(
        DecisionStep("day1", correctOptionId = "march_quietly", optionIds = listOf("march_quietly", "attack_gate")),
        DecisionStep("more_days", correctOptionId = "stay_silent", optionIds = listOf("stay_silent", "shout_now")),
        DecisionStep("day7", correctOptionId = "march_seven_times", optionIds = listOf("march_seven_times", "break_wall_by_force")),
        DecisionStep("shout", correctOptionId = "blow_horns_and_shout", optionIds = listOf("blow_horns_and_shout", "stay_silent")),
    )

    val marchStepDayLabels: Map<String, Int> = mapOf(
        "day1" to R.string.jericho_step_day1,
        "more_days" to R.string.jericho_step_more_days,
        "day7" to R.string.jericho_step_day7,
        "shout" to R.string.jericho_step_shout,
    )

    // The "force" option icons stay abstract (never a wielded weapon or depicted
    // impact) — matches the bandit-wall and Goliath-shield precedents of never
    // depicting violence even where the text implies it.
    val marchOptions: List<MarchOptionDef> = listOf(
        MarchOptionDef("march_quietly", R.string.jericho_option_march_quietly, R.drawable.ic_march_footprint),
        MarchOptionDef("attack_gate", R.string.jericho_option_attack_gate, R.drawable.ic_force_crossed),
        MarchOptionDef("stay_silent", R.string.jericho_option_stay_silent, R.drawable.ic_stay_silent),
        MarchOptionDef("shout_now", R.string.jericho_option_shout_now, R.drawable.ic_trumpet_shout),
        MarchOptionDef("march_seven_times", R.string.jericho_option_march_seven_times, R.drawable.ic_march_footprint),
        MarchOptionDef("break_wall_by_force", R.string.jericho_option_break_wall_by_force, R.drawable.ic_force_crossed),
        MarchOptionDef("blow_horns_and_shout", R.string.jericho_option_blow_horns_and_shout, R.drawable.ic_trumpet_shout),
    )

    val rahabSavedContextLines: List<Int> = listOf(
        R.string.jericho_rahab_saved_context_line_1,
        R.string.jericho_rahab_saved_context_line_2,
    )
}
