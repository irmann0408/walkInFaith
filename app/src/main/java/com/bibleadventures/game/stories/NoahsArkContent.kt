package com.bibleadventures.game.stories

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.geometry.Offset
import com.bibleadventures.R

enum class NoahsArkScene {
    INTRO,
    FIND_ANIMALS,
    ANIMAL_MATCHING,
    GATHER_SUPPLIES,
    ORGANIZE_ARK,
    FIND_MISSING_ITEMS,
    LESSON,
    REWARD,
}

data class AnimalDef(val id: String, @DrawableRes val iconRes: Int, @StringRes val nameRes: Int)
data class SupplyDef(val id: String, @DrawableRes val iconRes: Int, @StringRes val nameRes: Int)
data class SortCategoryDef(val key: String, @StringRes val labelRes: Int)
data class SortableItemDef(
    val id: String,
    @DrawableRes val iconRes: Int,
    @StringRes val nameRes: Int,
    val categoryKey: String,
)
data class HiddenItemDef(
    val id: String,
    @DrawableRes val iconRes: Int,
    @StringRes val nameRes: Int,
    /** Fractional (0..1) position within the scene area. */
    val position: Offset,
)

/**
 * Static content for the Noah's Ark chapter. Kept separate from the game
 * engine packages under `game/puzzles` so those stay reusable by future
 * chapters — this file is the only thing that's Noah's-Ark-specific.
 */
object NoahsArkContent {

    val introDialogueLines: List<Int> = listOf(
        R.string.noahs_ark_intro_line_1,
        R.string.noahs_ark_intro_line_2,
    )

    val animals: List<AnimalDef> = listOf(
        AnimalDef("lion", R.drawable.ic_animal_lion, R.string.animal_lion),
        AnimalDef("elephant", R.drawable.ic_animal_elephant, R.string.animal_elephant),
        AnimalDef("giraffe", R.drawable.ic_animal_giraffe, R.string.animal_giraffe),
        AnimalDef("sheep", R.drawable.ic_animal_sheep, R.string.animal_sheep),
        AnimalDef("rabbit", R.drawable.ic_animal_rabbit, R.string.animal_rabbit),
        AnimalDef("bird", R.drawable.ic_animal_bird, R.string.animal_bird),
    )

    val supplies: List<SupplyDef> = listOf(
        SupplyDef("bread", R.drawable.ic_supply_bread, R.string.supply_bread),
        SupplyDef("fruit", R.drawable.ic_supply_fruit, R.string.supply_fruit),
        SupplyDef("water", R.drawable.ic_supply_water, R.string.supply_water),
        SupplyDef("grain", R.drawable.ic_supply_grain, R.string.supply_grain),
    )

    val sortCategories: List<SortCategoryDef> = listOf(
        SortCategoryDef("animals", R.string.sort_category_animals),
        SortCategoryDef("food", R.string.sort_category_food),
        SortCategoryDef("supplies", R.string.sort_category_supplies),
    )

    val sortableItems: List<SortableItemDef> = listOf(
        SortableItemDef("sort_lion", R.drawable.ic_animal_lion, R.string.animal_lion, categoryKey = "animals"),
        SortableItemDef("sort_sheep", R.drawable.ic_animal_sheep, R.string.animal_sheep, categoryKey = "animals"),
        SortableItemDef("sort_bread", R.drawable.ic_supply_bread, R.string.supply_bread, categoryKey = "food"),
        SortableItemDef("sort_fruit", R.drawable.ic_supply_fruit, R.string.supply_fruit, categoryKey = "food"),
        SortableItemDef("sort_water", R.drawable.ic_supply_water, R.string.supply_water, categoryKey = "supplies"),
        SortableItemDef("sort_grain", R.drawable.ic_supply_grain, R.string.supply_grain, categoryKey = "supplies"),
    )

    // Large tap targets are applied at render time regardless of icon size,
    // to avoid pixel-hunting (spec section 9).
    val hiddenItems: List<HiddenItemDef> = listOf(
        HiddenItemDef("hidden_bread", R.drawable.ic_supply_bread, R.string.supply_bread, Offset(0.2f, 0.3f)),
        HiddenItemDef("hidden_water", R.drawable.ic_supply_water, R.string.supply_water, Offset(0.75f, 0.25f)),
        HiddenItemDef("hidden_grain", R.drawable.ic_supply_grain, R.string.supply_grain, Offset(0.5f, 0.65f)),
        HiddenItemDef("hidden_fruit", R.drawable.ic_supply_fruit, R.string.supply_fruit, Offset(0.82f, 0.78f)),
    )
}
