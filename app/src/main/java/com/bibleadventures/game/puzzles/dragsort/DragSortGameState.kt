package com.bibleadventures.game.puzzles.dragsort

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

data class SortableItem(
    val id: String,
    @DrawableRes val iconRes: Int,
    @StringRes val contentDescriptionRes: Int,
    val categoryKey: String,
)

data class SortCategory(val key: String, @StringRes val labelRes: Int)

/** Never FAILED — an incorrect drop just returns the item to try again (spec section 9). */
enum class SortOutcome { NONE, CORRECT, TRY_AGAIN }

data class DragSortGameState(
    val items: List<SortableItem>,
    val categories: List<SortCategory>,
    /** itemId -> the category it was correctly placed into. */
    val placedItems: Map<String, String> = emptyMap(),
    val lastOutcome: SortOutcome = SortOutcome.NONE,
) {
    val isComplete: Boolean get() = placedItems.size == items.size
}
