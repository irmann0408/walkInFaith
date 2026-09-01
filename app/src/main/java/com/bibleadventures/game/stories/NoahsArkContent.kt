package com.bibleadventures.game.stories

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.geometry.Offset
import com.bibleadventures.R

data class AnimalDef(val id: String, @DrawableRes val iconRes: Int, @StringRes val nameRes: Int)

/** One of the 4 supply icons a "Load the Ark" basket can be skinned with — purely cosmetic, unrelated to the arithmetic. */
data class LoadArkSupplyKindDef(val id: String, @DrawableRes val iconRes: Int, @StringRes val nameRes: Int)

/**
 * A tap target for "Find the Tools for Building the Ark" — unlike the generic
 * [HiddenItemDef]/`HiddenItem` shape, the tool isn't rendered as its own visible
 * floating icon; it's already baked into the single background scene image
 * (`ic_noahs_ark_find_tools_scene`), so this needs its own hit-box *size*, not
 * just a center position. [position]/[size] are both fractional (0..1) of the
 * scene area — [position] is the tap target's *center*, matching how the
 * positions were measured (pixel-matched against the source art, not hand-
 * guessed) rather than a top-left corner.
 */
data class FindToolsHotspotDef(
    val id: String,
    @DrawableRes val iconRes: Int,
    @StringRes val nameRes: Int,
    val position: Offset,
    val size: Offset,
)
// HiddenItemDef and DecoyItemDef live in ContentDefs.kt (same package) — shared
// with DavidGoliathContent.kt, not Noah's-Ark-specific despite living here originally.

/**
 * Static content for the Noah's Ark chapter. Kept separate from the game
 * engine packages under `game/puzzles` so those stay reusable by future
 * chapters — this file is the only thing that's Noah's-Ark-specific.
 */
object NoahsArkContent {

    val animals: List<AnimalDef> = listOf(
        AnimalDef("lion", R.drawable.ic_animal_lion, R.string.animal_lion),
        AnimalDef("elephant", R.drawable.ic_animal_elephant, R.string.animal_elephant),
        AnimalDef("giraffe", R.drawable.ic_animal_giraffe, R.string.animal_giraffe),
        AnimalDef("sheep", R.drawable.ic_animal_sheep, R.string.animal_sheep),
        AnimalDef("rabbit", R.drawable.ic_animal_rabbit, R.string.animal_rabbit),
        AnimalDef("bird", R.drawable.ic_animal_bird, R.string.animal_bird),
        AnimalDef("camel", R.drawable.ic_animal_camel, R.string.animal_camel),
        AnimalDef("monkey", R.drawable.ic_animal_monkey, R.string.animal_monkey),
    )

    /**
     * "Load the Ark" — 3 deck capacities to fill exactly, each split into a
     * handful of numbered supply baskets at runtime (see
     * [com.bibleadventures.ui.screens.noahsark.NoahsArkViewModel]'s use of
     * [com.bibleadventures.game.puzzles.groupfill.GroupFillGame.randomSolvablePartition]).
     * Small, addable numbers appropriate for the app's 7+ target age —
     * real running-sum arithmetic, not binary classification.
     */
    val loadArkDeckTargets: List<Int> = listOf(10, 12, 14)

    val loadArkSupplyKinds: List<LoadArkSupplyKindDef> = listOf(
        LoadArkSupplyKindDef("bread", R.drawable.ic_supply_bread, R.string.supply_bread),
        LoadArkSupplyKindDef("fruit", R.drawable.ic_supply_fruit, R.string.supply_fruit),
        LoadArkSupplyKindDef("grain", R.drawable.ic_supply_grain, R.string.supply_grain),
        LoadArkSupplyKindDef("water", R.drawable.ic_supply_water, R.string.supply_water),
    )

    /**
     * 10 tap targets, 2 instances each of 5 tools, pixel-matched against the
     * source art (`game art/ark find the tools.png` + its grid-outline
     * reference) rather than estimated — every tool appears twice in the
     * scene, so ids are suffixed `_1`/`_2` per instance for independent
     * found-tracking while sharing one content-description per tool type.
     */
    val findToolsHotspots: List<FindToolsHotspotDef> = listOf(
        FindToolsHotspotDef("axe_1", R.drawable.ic_tool_axe, R.string.tool_axe, Offset(0.307f, 0.290f), Offset(0.055f, 0.098f)),
        FindToolsHotspotDef("axe_2", R.drawable.ic_tool_axe, R.string.tool_axe, Offset(0.195f, 0.601f), Offset(0.056f, 0.093f)),
        FindToolsHotspotDef("hammer_1", R.drawable.ic_tool_hammer, R.string.tool_hammer, Offset(0.688f, 0.292f), Offset(0.053f, 0.107f)),
        FindToolsHotspotDef("hammer_2", R.drawable.ic_tool_hammer, R.string.tool_hammer, Offset(0.338f, 0.707f), Offset(0.063f, 0.089f)),
        FindToolsHotspotDef("chisel_1", R.drawable.ic_tool_chisel, R.string.tool_chisel, Offset(0.557f, 0.290f), Offset(0.049f, 0.106f)),
        FindToolsHotspotDef("chisel_2", R.drawable.ic_tool_chisel, R.string.tool_chisel, Offset(0.588f, 0.809f), Offset(0.055f, 0.102f)),
        FindToolsHotspotDef("nail_1", R.drawable.ic_tool_nail, R.string.tool_nail, Offset(0.656f, 0.401f), Offset(0.048f, 0.095f)),
        FindToolsHotspotDef("nail_2", R.drawable.ic_tool_nail, R.string.tool_nail, Offset(0.487f, 0.705f), Offset(0.051f, 0.102f)),
        FindToolsHotspotDef("wood_saw_1", R.drawable.ic_tool_wood_saw, R.string.tool_wood_saw, Offset(0.425f, 0.497f), Offset(0.092f, 0.091f)),
        FindToolsHotspotDef("wood_saw_2", R.drawable.ic_tool_wood_saw, R.string.tool_wood_saw, Offset(0.644f, 0.598f), Offset(0.082f, 0.075f)),
    )

    /** Aspect ratio (width / height) of `ic_noahs_ark_find_tools_scene` — the background is a fixed 1024x559 image, never cropped. */
    const val FIND_TOOLS_SCENE_ASPECT_RATIO = 1024f / 559f
}
