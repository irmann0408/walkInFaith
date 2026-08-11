package com.bibleadventures.game.stories

import com.bibleadventures.R
import com.bibleadventures.game.puzzles.decisionpath.DecisionStep

/**
 * Static content for Esther: The Banquets & Rescue — the fifth and final of
 * 5 short Esther chapters. Kept separate from the game engine packages
 * under `game/puzzles` so those stay reusable by future chapters.
 */
object EstherBanquetsRescueContent {

    val introDialogueLines: List<Int> = listOf(
        R.string.esther_banquets_rescue_intro_line_1,
        R.string.esther_banquets_rescue_intro_line_2,
    )

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

    /**
     * Each food item has exactly one unique table zone via `categoryKey` —
     * a degenerate 1-item-per-1-category case of `dragsort`, same engine
     * as Organize the Ark, just re-themed as a banquet-table jigsaw.
     */
    val zoneCategories: List<SortCategoryDef> = listOf(
        SortCategoryDef("zone_bread", R.string.esther_banquets_rescue_zone_bread),
        SortCategoryDef("zone_fruit", R.string.esther_banquets_rescue_zone_fruit),
        SortCategoryDef("zone_honey", R.string.esther_banquets_rescue_zone_honey),
        SortCategoryDef("zone_wine", R.string.esther_banquets_rescue_zone_wine),
        SortCategoryDef("zone_meat", R.string.esther_banquets_rescue_zone_meat),
    )

    val foodItems: List<SortableItemDef> = listOf(
        SortableItemDef("food_bread", R.drawable.ic_supply_bread, R.string.supply_bread, categoryKey = "zone_bread"),
        SortableItemDef("food_fruit", R.drawable.ic_supply_fruit, R.string.supply_fruit, categoryKey = "zone_fruit"),
        SortableItemDef("food_honey", R.drawable.ic_supply_honey, R.string.supply_honey, categoryKey = "zone_honey"),
        SortableItemDef("food_wine", R.drawable.ic_supply_wine, R.string.supply_wine, categoryKey = "zone_wine"),
        SortableItemDef("food_meat", R.drawable.ic_supply_meat, R.string.supply_meat, categoryKey = "zone_meat"),
    )

    /** A short, 3-step guided sequence reusing `decisionpath` — same engine as Jericho's march and the old banquet-timing mechanic. */
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
