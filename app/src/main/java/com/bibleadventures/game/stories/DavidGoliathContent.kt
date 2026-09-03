package com.bibleadventures.game.stories

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.bibleadventures.R

data class ChoiceOptionDef(val id: String, @StringRes val textRes: Int, @StringRes val reactionTextRes: Int)

/** One numeral count in the Sheep Counting matching game — [numeralIconRes] and [sheepGroupIconRes] share a pairKey. */
data class SheepCountDef(val count: Int, @DrawableRes val numeralIconRes: Int, @DrawableRes val sheepGroupIconRes: Int, @StringRes val nameRes: Int)

/**
 * Static content for the David and Goliath chapter. Kept separate from the
 * game engine packages under `game/puzzles` so those stay reusable by future
 * chapters — this file is the only thing that's David-and-Goliath-specific.
 */
object DavidGoliathContent {

    val choiceOptions: List<ChoiceOptionDef> = listOf(
        ChoiceOptionDef("trust", R.string.david_goliath_choice_option_1, R.string.david_goliath_choice_reaction_1),
        ChoiceOptionDef("unafraid", R.string.david_goliath_choice_option_2, R.string.david_goliath_choice_reaction_2),
        ChoiceOptionDef("declaration", R.string.david_goliath_choice_option_3, R.string.david_goliath_choice_reaction_3),
    )

    // Numerals 1-5 matched to a same-count sheep icon — unlike the stones,
    // the count itself is the point, so each count needs its own distinct art.
    val sheepCounts: List<SheepCountDef> = listOf(
        SheepCountDef(1, R.drawable.ic_numeral_1, R.drawable.ic_sheep_group_1, R.string.david_goliath_sheep_count_1),
        SheepCountDef(2, R.drawable.ic_numeral_2, R.drawable.ic_sheep_group_2, R.string.david_goliath_sheep_count_2),
        SheepCountDef(3, R.drawable.ic_numeral_3, R.drawable.ic_sheep_group_3, R.string.david_goliath_sheep_count_3),
        SheepCountDef(4, R.drawable.ic_numeral_4, R.drawable.ic_sheep_group_4, R.string.david_goliath_sheep_count_4),
        SheepCountDef(5, R.drawable.ic_numeral_5, R.drawable.ic_sheep_group_5, R.string.david_goliath_sheep_count_5),
    )

}
