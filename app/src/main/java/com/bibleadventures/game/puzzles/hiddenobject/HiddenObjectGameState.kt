package com.bibleadventures.game.puzzles.hiddenobject

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.geometry.Offset

data class HiddenItem(
    val id: String,
    /** Fractional (0..1) position within the scene area. */
    val position: Offset,
    @DrawableRes val iconRes: Int,
    @StringRes val contentDescriptionRes: Int,
)

data class HiddenObjectGameState(
    val items: List<HiddenItem>,
    val foundIds: Set<String> = emptySet(),
) {
    val isComplete: Boolean get() = foundIds.size == items.size
}
