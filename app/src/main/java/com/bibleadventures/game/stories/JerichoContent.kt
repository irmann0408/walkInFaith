package com.bibleadventures.game.stories

import androidx.compose.ui.geometry.Offset
import com.bibleadventures.R
import com.bibleadventures.game.puzzles.rhythmlane.RhythmLaneChart
import com.bibleadventures.game.puzzles.rhythmlane.RhythmNote

/** One of the 12 memorial stones — no position needed, "Setting Up Camp" renders a plain tray, not a hidden-object search. */
data class CampStoneDef(val id: String, val nameRes: Int)

/**
 * One colored note in the Shofar sequence — list order in
 * [JerichoContent.shofarNotes] *is* the required tap order, same shape as
 * [DanielContent.lionsDenPoints]. [position] is fractional (0..1) within
 * the scene area, arranged in a curved, horn-like arc.
 */
data class ShofarNoteDef(val id: String, val nameRes: Int, val position: Offset)

/**
 * Static content for the Battle of Jericho chapter. Kept separate from
 * the game engine packages under `game/puzzles` so those stay reusable by
 * future chapters.
 *
 * Rebuilt with 4 real mini-puzzles mapped onto Joshua 2, 4, and 6 (details
 * cross-checked against the actual WEB text, not assumed from memory):
 * Rahab's spies escaping down the wall by rope (a sliding-tile puzzle),
 * the twelve memorial stones at Gilgal (a simple collect-and-place
 * puzzle), the six days of silent marching, and the seventh day's faster
 * march + shofar + shout finale. Replaces the old 4-flashcard "March and
 * the Shout" — this chapter was flagged as too easy with nothing but
 * pick-the-obvious-option choices.
 */
object JerichoContent {

    val introDialogueLines: List<Int> = listOf(
        R.string.jericho_intro_line_1,
        R.string.jericho_intro_line_2,
    )

    val rahabHouseContextLines: List<Int> = listOf(
        R.string.jericho_rahab_house_context_line_1,
        R.string.jericho_rahab_house_context_line_2,
    )

    // Rahab hides the spies (Joshua 2:2-7) — deliberately stops short of the escape
    // itself now, which the new Spies Escape puzzle + its payoff context resolve.
    val rahabHelpingLines: List<Int> = listOf(
        R.string.jericho_rahab_helping_line_1,
        R.string.jericho_rahab_helping_line_2,
    )

    /** A 3x3 grid (8 tiles) — the classic 15-puzzle's smaller, kid-tractable cousin. */
    const val SPIES_ESCAPE_GRID_SIZE = 3

    // Rahab lowers the spies by a rope through her window (Joshua 2:15), the 3-day
    // hiding instruction, and the scarlet-cord promise (Joshua 2:16-21) — the payoff
    // to solving the Spies Escape puzzle.
    val spiesEscapedContextLines: List<Int> = listOf(
        R.string.jericho_spies_escaped_context_line_1,
        R.string.jericho_spies_escaped_context_line_2,
        R.string.jericho_spies_escaped_context_line_3,
    )

    // Flavor-only responses at Israel's real decision point — trusting God's unusual
    // plan rather than a normal battle plan — no branching, matching every other
    // chapter's Choice scene.
    val choiceOptions: List<ChoiceOptionDef> = listOf(
        ChoiceOptionDef("follow_plan", R.string.jericho_choice_option_1, R.string.jericho_choice_reaction_1),
        ChoiceOptionDef("trust_god", R.string.jericho_choice_option_2, R.string.jericho_choice_reaction_2),
        ChoiceOptionDef("obey_step_by_step", R.string.jericho_choice_option_3, R.string.jericho_choice_reaction_3),
    )

    // Crossing the Jordan on dry ground and choosing the twelve men (Joshua 3-4:1-5).
    val campContextLines: List<Int> = listOf(
        R.string.jericho_camp_context_line_1,
        R.string.jericho_camp_context_line_2,
    )

    /** Twelve stones "from the middle of the Jordan," one per tribe (Joshua 4:1-9) — order-independent, tap each to place it. */
    val campStones: List<CampStoneDef> = listOf(
        CampStoneDef("stone_1", R.string.jericho_camp_stone_1),
        CampStoneDef("stone_2", R.string.jericho_camp_stone_2),
        CampStoneDef("stone_3", R.string.jericho_camp_stone_3),
        CampStoneDef("stone_4", R.string.jericho_camp_stone_4),
        CampStoneDef("stone_5", R.string.jericho_camp_stone_5),
        CampStoneDef("stone_6", R.string.jericho_camp_stone_6),
        CampStoneDef("stone_7", R.string.jericho_camp_stone_7),
        CampStoneDef("stone_8", R.string.jericho_camp_stone_8),
        CampStoneDef("stone_9", R.string.jericho_camp_stone_9),
        CampStoneDef("stone_10", R.string.jericho_camp_stone_10),
        CampStoneDef("stone_11", R.string.jericho_camp_stone_11),
        CampStoneDef("stone_12", R.string.jericho_camp_stone_12),
    )

    // "Arrange tents + trust Joshua's leadership" folded into narrative context here,
    // rather than a second required interaction alongside the stone-placing puzzle.
    val tentsContextLines: List<Int> = listOf(
        R.string.jericho_tents_context_line_1,
        R.string.jericho_tents_context_line_2,
    )

    val wallsContextLines: List<Int> = listOf(
        R.string.jericho_walls_context_line_1,
        R.string.jericho_walls_context_line_2,
    )

    /**
     * Reuses `rhythmlane` exactly as built for Esther's corridor, at its
     * simplest possible parameterization: a single lane (always `0`), one
     * note per loop. Each successful hit is one full, unhurried lap around
     * the wall — six hits, six days (Joshua 6:3) — so "Day X of 6" is read
     * directly off [com.bibleadventures.game.puzzles.rhythmlane.RhythmLaneGameState.hits],
     * no separate day-counting state needed. A missed beat is never a
     * setback (no "guards' awareness" danger meter, unlike the original
     * blueprint this was adapted from) — it simply doesn't advance the day
     * count yet, matching this app's non-negotiable no-failure-state rule.
     */
    val sixDayMarchChart = RhythmLaneChart(
        notes = listOf(RhythmNote("step", lane = 0, hitTimeMs = 700)),
        loopDurationMs = 1400,
    )
    const val SIX_DAY_MARCH_REQUIRED_HITS = 6

    // The seventh day's instructions: march seven times, then the trumpets and the
    // shout (Joshua 6:4, 6:15-16).
    val seventhDayContextLines: List<Int> = listOf(
        R.string.jericho_seventh_day_context_line_1,
        R.string.jericho_seventh_day_context_line_2,
    )

    /** Same shape as [sixDayMarchChart], deliberately reused again — the text itself
     *  says "do the march again, seven times, faster" (Joshua 6:15), so repeating
     *  the mechanic here serves the narrative rather than reading as an accidental
     *  duplicate. Half the loop duration, and 7 hits instead of 6 — one per lap. */
    val fastMarchChart = RhythmLaneChart(
        notes = listOf(RhythmNote("step", lane = 0, hitTimeMs = 300)),
        loopDurationMs = 600,
    )
    const val FAST_MARCH_REQUIRED_HITS = 7

    /**
     * Reuses `game/puzzles/sequence` exactly as-is (already Daniel's Lions'
     * Den mechanic) — list order here *is* the required tap order. As in
     * Lions' Den, the correct order isn't told upfront; an out-of-order tap
     * safely re-prompts without losing progress, so a child discovers it by
     * trying (Joshua 6:4's seven trumpets, reframed as five taps for a
     * shorter, kid-paced puzzle).
     */
    val shofarNotes: List<ShofarNoteDef> = listOf(
        ShofarNoteDef("red", R.string.jericho_shofar_note_red, Offset(0.15f, 0.55f)),
        ShofarNoteDef("orange", R.string.jericho_shofar_note_orange, Offset(0.3f, 0.3f)),
        ShofarNoteDef("yellow", R.string.jericho_shofar_note_yellow, Offset(0.5f, 0.18f)),
        ShofarNoteDef("green", R.string.jericho_shofar_note_green, Offset(0.7f, 0.3f)),
        ShofarNoteDef("blue", R.string.jericho_shofar_note_blue, Offset(0.85f, 0.55f)),
    )

    /** How many taps fill the Shout meter and bring the wall down — a plain counter, not a new engine (see JerichoUiState). */
    const val SHOUT_REQUIRED_TAPS = 15

    val rahabSavedContextLines: List<Int> = listOf(
        R.string.jericho_rahab_saved_context_line_1,
        R.string.jericho_rahab_saved_context_line_2,
    )
}
