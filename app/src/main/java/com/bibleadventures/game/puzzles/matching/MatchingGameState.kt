package com.bibleadventures.game.puzzles.matching

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

data class MatchItem(
    val id: String,
    @DrawableRes val iconRes: Int,
    @StringRes val contentDescriptionRes: Int,
    val pairKey: String,
)

/** Never FAILED — mistakes just prompt another try (spec section 9). */
enum class MatchOutcome { NONE, CORRECT, TRY_AGAIN }

data class MatchingGameState(
    val items: List<MatchItem>,
    val selectedId: String? = null,
    val matchedIds: Set<String> = emptySet(),
    val lastOutcome: MatchOutcome = MatchOutcome.NONE,
) {
    val isComplete: Boolean get() = matchedIds.size == items.size
}
