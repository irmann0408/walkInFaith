package com.bibleadventures.game.stories

import androidx.compose.ui.geometry.Offset
import com.bibleadventures.R
import com.bibleadventures.game.puzzles.gridmaze.Direction
import com.bibleadventures.game.puzzles.rhythmlane.RhythmLaneChart
import com.bibleadventures.game.puzzles.rhythmlane.RhythmNote

/**
 * A purely visual distractor in The Boy's Gift basket search — never
 * registered as a [com.bibleadventures.game.puzzles.hiddenobject.HiddenItem],
 * so tapping one is a screen-level no-op by construction, not an engine
 * concern (same zero-risk "decoy" pattern used elsewhere in this app).
 */
data class DecoyItem(val id: String, val position: Offset, val iconRes: Int)

/**
 * Static content for the Feeding the 5,000 chapter. Kept separate from the
 * game engine packages under `game/puzzles` so those stay reusable by
 * future chapters — same convention as every other chapter's content file.
 *
 * Five real mini-puzzles mapped onto John 6:1-14 and Mark 6:35-44 (details
 * cross-checked against the actual WEB text, not assumed from memory —
 * John uniquely has the Philip/Andrew/boy detail, Mark uniquely has the
 * "ranks of hundreds and fifties" detail, so both accounts are combined):
 * gathering the crowd into seating groups, searching the crowd for the boy
 * with the loaves and fish, finding exactly what's in his basket, the
 * miracle of multiplication as real arithmetic (not a tap-to-watch-numbers-
 * grow gimmick), and a two-phase serve-then-gather finale. Random
 * generation for the crowd-grouping and multiplication puzzles lives in
 * `Feeding5000ViewModel`, not here — this file stays static content only.
 */
object Feeding5000Content {

    val introDialogueLines: List<Int> = listOf(
        R.string.feeding_5000_intro_line_1,
        R.string.feeding_5000_intro_line_2,
    )

    val crowdContextLines: List<Int> = listOf(
        R.string.feeding_5000_crowd_context_line_1,
        R.string.feeding_5000_crowd_context_line_2,
    )

    /** Mark 6:39-40 — "sit down in ranks, by hundreds and by fifties." Family headcounts partitioning these are generated fresh every playthrough. */
    val groupFillCircleTargets: List<Int> = listOf(50, 50, 100)

    val searchingContextLines: List<Int> = listOf(
        R.string.feeding_5000_searching_context_line_1,
        R.string.feeding_5000_searching_context_line_2,
    )

    /**
     * Purely visual crowd fill for Searching for Food — without these the
     * boy was the only figure on screen at all, making him trivially
     * spottable rather than something to search for. Same screen-level-only
     * decoy pattern as [boysGiftDecoys]: never registered as a
     * [com.bibleadventures.game.puzzles.hiddenobject.HiddenItem], so tapping
     * one is a no-op by construction. The boy is distinguished from every
     * one of these only by carrying a basket ([R.drawable.ic_boy_with_basket]
     * vs. the crowd's plain `ic_crowd_person_1`..`_5`) — that's the actual
     * search target, not his position or robe color.
     *
     * Five robe-color variants (`ic_crowd_person_1`..`_5`, cycled evenly, 4
     * of each) so the crowd doesn't read as 20 identical clones. All 20
     * positions sit within [bg_feeding_hillside.xml]'s grass band (y >=
     * ~0.58 of the square viewport, comfortably below its sky/hill horizon
     * around y=0.4-0.6) — an earlier pass scattered several up into the sky
     * fraction, which read as people floating rather than standing on the
     * ground.
     */
    val searchingForFoodDecoys: List<DecoyItem> = listOf(
        DecoyItem("decoy_person_1", Offset(0.04f, 0.58f), R.drawable.ic_crowd_person_2),
        DecoyItem("decoy_person_2", Offset(0.16f, 0.60f), R.drawable.ic_crowd_person_4),
        DecoyItem("decoy_person_3", Offset(0.30f, 0.58f), R.drawable.ic_crowd_person_1),
        DecoyItem("decoy_person_4", Offset(0.42f, 0.60f), R.drawable.ic_crowd_person_5),
        DecoyItem("decoy_person_5", Offset(0.68f, 0.58f), R.drawable.ic_crowd_person_3),
        DecoyItem("decoy_person_6", Offset(0.80f, 0.60f), R.drawable.ic_crowd_person_2),
        DecoyItem("decoy_person_7", Offset(0.92f, 0.58f), R.drawable.ic_crowd_person_4),
        DecoyItem("decoy_person_8", Offset(0.06f, 0.70f), R.drawable.ic_crowd_person_1),
        DecoyItem("decoy_person_9", Offset(0.20f, 0.68f), R.drawable.ic_crowd_person_5),
        DecoyItem("decoy_person_10", Offset(0.34f, 0.70f), R.drawable.ic_crowd_person_3),
        DecoyItem("decoy_person_11", Offset(0.46f, 0.68f), R.drawable.ic_crowd_person_2),
        DecoyItem("decoy_person_12", Offset(0.62f, 0.72f), R.drawable.ic_crowd_person_4),
        DecoyItem("decoy_person_13", Offset(0.75f, 0.70f), R.drawable.ic_crowd_person_1),
        DecoyItem("decoy_person_14", Offset(0.88f, 0.68f), R.drawable.ic_crowd_person_5),
        DecoyItem("decoy_person_15", Offset(0.10f, 0.82f), R.drawable.ic_crowd_person_3),
        DecoyItem("decoy_person_16", Offset(0.24f, 0.84f), R.drawable.ic_crowd_person_2),
        DecoyItem("decoy_person_17", Offset(0.38f, 0.80f), R.drawable.ic_crowd_person_4),
        DecoyItem("decoy_person_18", Offset(0.50f, 0.84f), R.drawable.ic_crowd_person_1),
        DecoyItem("decoy_person_19", Offset(0.66f, 0.82f), R.drawable.ic_crowd_person_5),
        DecoyItem("decoy_person_20", Offset(0.85f, 0.84f), R.drawable.ic_crowd_person_3),
    )

    val boysGiftContextLines: List<Int> = listOf(
        R.string.feeding_5000_boys_gift_context_line_1,
        R.string.feeding_5000_boys_gift_context_line_2,
    )

    /**
     * Purely visual distractors for The Boy's Gift — see [DecoyItem]'s doc
     * comment. 25 total, across 4 shapes (`ic_stone_smooth`, `ic_decoy_rock`
     * — a distinct angular shape, not just a recolor — `ic_decoy_frog`,
     * `ic_decoy_leaf`) so the basket reads as genuinely cluttered rather
     * than a handful of items on empty cloth; hand-placed to fill the whole
     * frame while keeping clear of [Feeding5000ViewModel.boysGiftRealItems]'s
     * 7 positions.
     */
    val boysGiftDecoys: List<DecoyItem> = listOf(
        DecoyItem("decoy_stone_1", Offset(0.42f, 0.45f), R.drawable.ic_stone_smooth),
        DecoyItem("decoy_stone_2", Offset(0.68f, 0.22f), R.drawable.ic_stone_smooth),
        DecoyItem("decoy_stone_3", Offset(0.22f, 0.15f), R.drawable.ic_stone_smooth),
        DecoyItem("decoy_frog_1", Offset(0.48f, 0.72f), R.drawable.ic_decoy_frog),
        DecoyItem("decoy_frog_2", Offset(0.72f, 0.15f), R.drawable.ic_decoy_frog),
        DecoyItem("decoy_stone_4", Offset(0.08f, 0.08f), R.drawable.ic_stone_smooth),
        DecoyItem("decoy_rock_1", Offset(0.40f, 0.08f), R.drawable.ic_decoy_rock),
        DecoyItem("decoy_frog_3", Offset(0.56f, 0.08f), R.drawable.ic_decoy_frog),
        DecoyItem("decoy_leaf_1", Offset(0.88f, 0.08f), R.drawable.ic_decoy_leaf),
        DecoyItem("decoy_stone_5", Offset(0.08f, 0.26f), R.drawable.ic_stone_smooth),
        DecoyItem("decoy_rock_2", Offset(0.40f, 0.26f), R.drawable.ic_decoy_rock),
        DecoyItem("decoy_frog_4", Offset(0.56f, 0.26f), R.drawable.ic_decoy_frog),
        DecoyItem("decoy_leaf_2", Offset(0.88f, 0.26f), R.drawable.ic_decoy_leaf),
        DecoyItem("decoy_stone_6", Offset(0.08f, 0.44f), R.drawable.ic_stone_smooth),
        DecoyItem("decoy_rock_3", Offset(0.24f, 0.44f), R.drawable.ic_decoy_rock),
        DecoyItem("decoy_frog_5", Offset(0.56f, 0.44f), R.drawable.ic_decoy_frog),
        DecoyItem("decoy_leaf_3", Offset(0.72f, 0.44f), R.drawable.ic_decoy_leaf),
        DecoyItem("decoy_stone_7", Offset(0.88f, 0.44f), R.drawable.ic_stone_smooth),
        DecoyItem("decoy_rock_4", Offset(0.72f, 0.62f), R.drawable.ic_decoy_rock),
        DecoyItem("decoy_frog_6", Offset(0.08f, 0.80f), R.drawable.ic_decoy_frog),
        DecoyItem("decoy_leaf_4", Offset(0.24f, 0.80f), R.drawable.ic_decoy_leaf),
        DecoyItem("decoy_stone_8", Offset(0.40f, 0.80f), R.drawable.ic_stone_smooth),
        DecoyItem("decoy_rock_5", Offset(0.56f, 0.80f), R.drawable.ic_decoy_rock),
        DecoyItem("decoy_frog_7", Offset(0.72f, 0.80f), R.drawable.ic_decoy_frog),
        DecoyItem("decoy_leaf_5", Offset(0.88f, 0.80f), R.drawable.ic_decoy_leaf),
    )

    // Flavor-only responses at the boy's real decision point — no branching,
    // matching every other chapter's Choice scene.
    val choiceOptions: List<ChoiceOptionDef> = listOf(
        ChoiceOptionDef("share_fully", R.string.feeding_5000_choice_option_1, R.string.feeding_5000_choice_reaction_1),
        ChoiceOptionDef("keep_some", R.string.feeding_5000_choice_option_2, R.string.feeding_5000_choice_reaction_2),
        ChoiceOptionDef("ask_jesus", R.string.feeding_5000_choice_option_3, R.string.feeding_5000_choice_reaction_3),
    )

    val miracleContextLines: List<Int> = listOf(
        R.string.feeding_5000_miracle_context_line_1,
        R.string.feeding_5000_miracle_context_line_2,
    )

    /** Multiplicands drawn from the miracle's own numbers, scaling up as it grows — multiplier is always single-digit (same tuning as Jericho's Blow the Shofar). */
    val miracleMultiplicandPool: List<Int> = listOf(5, 2, 10, 20, 50)
    const val MIRACLE_PROBLEM_COUNT = 5

    val feastContextLines: List<Int> = listOf(
        R.string.feeding_5000_feast_context_line_1,
        R.string.feeding_5000_feast_context_line_2,
    )

    /**
     * Phase A of the finale — reuses `gridmaze` (Good Samaritan's/Daniel's
     * D-pad grid-walk engine), not `rhythmlane`: an earlier version had the
     * disciple standing still while bread fell into a basket, which read as
     * receiving food rather than distributing it. Walking out to reach each
     * of the 7 groups reads as actually serving them. 8x8 map, row-major.
     * '.' path, '#' a boulder wall, 'B' a bush wall (mechanically identical
     * to '#' — see `GridMazeGame`; the two only differ in which icon the
     * screen draws, same rock/bandit-flavor-only trick Good Samaritan's map
     * already uses), 'C' a group of people (a collectible — reusing
     * `GridMazeState`'s newly-generalized "no goal tile -> complete once
     * every collectible is gathered" mode, since this walk has no single
     * finish line, just 7 stops in any order), 'S' the start. Deliberately
     * sparser than Good Samaritan's 10x10 map (8 walls total, ~12% of
     * cells, vs. Good Samaritan's much denser terrain) — this is a finale
     * beat, not a puzzle about the walls. Verified solvable by hand (BFS
     * from start): all 7 collectibles reachable in one connected component.
     * Not shuffled per playthrough, same reasoning as
     * `GoodSamaritanContent.mapLayout`.
     */
    val servingMapLayout: List<String> = listOf(
        "S..#....",
        ".B..B...",
        "...C...C",
        ".#....B.",
        "C..#.C..",
        ".....B..",
        ".C.....C",
        "...#..C.",
    )
    const val SERVING_GROUP_COUNT = 7

    /**
     * A hand-verified 27-move route visiting all 7 groups (order: (4,0),
     * (6,1), (2,3), (4,5), (2,7), (6,7), (7,6)) — any order works since
     * completion only needs every collectible gathered, not a specific
     * sequence. Used by the instrumented flow test to replay a
     * known-solvable path deterministically, since the map itself is
     * intentionally not shuffled.
     */
    val servingSolutionPath: List<Direction> = listOf(
        Direction.DOWN, Direction.DOWN, Direction.DOWN, Direction.DOWN,
        Direction.DOWN, Direction.DOWN, Direction.RIGHT,
        Direction.UP, Direction.UP, Direction.RIGHT, Direction.UP, Direction.UP, Direction.RIGHT,
        Direction.RIGHT, Direction.RIGHT, Direction.DOWN, Direction.DOWN,
        Direction.RIGHT, Direction.RIGHT, Direction.UP, Direction.UP,
        Direction.DOWN, Direction.DOWN, Direction.DOWN, Direction.DOWN,
        Direction.DOWN, Direction.LEFT,
    )

    /**
     * Phase B of the finale — reuses `rhythmlane` again rather than building
     * new free-form catch physics (confirmed with the user): a single
     * basket must be steered into the right lane before a leftover item
     * "falls" there. Paced slower than the original tap-each-lane version
     * (loopDurationMs 2300 -> 4000, matching Serving's cadence) — that
     * pacing fit a quick reaction tap, but this mechanic asks the player to
     * anticipate and move the basket *before* the drop lands, so even gaps
     * of 1000ms between notes (the same whether the next note is 1 lane
     * away or, worst case, 2 lanes away — lane 2 -> lane 0) give enough time
     * to react. 12 required hits — John 6:13's twelve baskets, exactly.
     */
    val catchingChart = RhythmLaneChart(
        notes = listOf(
            RhythmNote("catch_1", lane = 0, hitTimeMs = 500),
            RhythmNote("catch_2", lane = 1, hitTimeMs = 1500),
            RhythmNote("catch_3", lane = 2, hitTimeMs = 2500),
            RhythmNote("catch_4", lane = 0, hitTimeMs = 3500),
        ),
        loopDurationMs = 4000,
    )
    const val CATCHING_REQUIRED_HITS = 12
}
