package com.bibleadventures.game.puzzles.dragsort

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

data class SortableItem(
    val id: String,
    @DrawableRes val iconRes: Int,
    @StringRes val contentDescriptionRes: Int,
    /** Null means this item belongs in no category — a decoy, never a real placement. */
    val categoryKey: String?,
)

data class SortCategory(val key: String, @StringRes val labelRes: Int)

/**
 * Never FAILED — an incorrect drop just returns the item to try again (spec
 * section 9). [NOT_SORTABLE] is a distinct, equally non-punishing outcome for a
 * decoy item that belongs in no category at all, rather than just the wrong one.
 */
enum class SortOutcome { NONE, CORRECT, TRY_AGAIN, NOT_SORTABLE }

data class DragSortGameState(
    val items: List<SortableItem>,
    val categories: List<SortCategory>,
    /** itemId -> the category it was correctly placed into. */
    val placedItems: Map<String, String> = emptyMap(),
    val lastOutcome: SortOutcome = SortOutcome.NONE,
) {
    /** Decoys (null categoryKey) are never required to complete the scene. */
    val isComplete: Boolean get() = placedItems.size == items.count { it.categoryKey != null }
}
