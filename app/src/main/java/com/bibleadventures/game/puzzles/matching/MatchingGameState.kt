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

/**
 * Memory/concentration state: cards start face down. [selectedIds] holds the
 * 0-2 cards currently face up and not yet resolved as a match; [matchedIds]
 * holds cards that are face up permanently.
 */
data class MatchingGameState(
    val items: List<MatchItem>,
    val selectedIds: List<String> = emptyList(),
    val matchedIds: Set<String> = emptySet(),
    val lastOutcome: MatchOutcome = MatchOutcome.NONE,
) {
    val isComplete: Boolean get() = matchedIds.size == items.size
    fun isFaceUp(id: String): Boolean = id in matchedIds || id in selectedIds
}
