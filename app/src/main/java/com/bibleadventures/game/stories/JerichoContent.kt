package com.bibleadventures.game.stories

import androidx.compose.ui.geometry.Offset
import com.bibleadventures.R
import com.bibleadventures.game.puzzles.rhythmlane.RhythmLaneChart
import com.bibleadventures.game.puzzles.rhythmlane.RhythmNote

/** One of the 12 memorial stones — no position needed, "Setting Up Camp" renders a plain tray, not a hidden-object search. */
data class CampStoneDef(val id: String, val nameRes: Int)

/**
 * One colored note in the Shofar sequence. Unlike [DanielContent.lionsDenPoints]'s
 * fixed list-order-is-tap-order shape, both the required tap order and each note's
 * on-screen position are shuffled fresh per playthrough (see
 * [com.bibleadventures.ui.screens.jericho.JerichoViewModel]'s `newShofarPlacements`) —
 * this def only carries the color's identity, not its placement.
 */
data class ShofarNoteDef(val id: String, val nameRes: Int)

/** One shuffled-per-playthrough placement of a [ShofarNoteDef]: which color, at which screen position. [position] is fractional (0..1) within the scene area. */
data class ShofarNotePlacement(val id: String, val nameRes: Int, val position: Offset)

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
     * Reuses `rhythmlane` exactly as built for Esther's corridor — now at
     * the *same* 3-lane parameterization as "The Long Corridor" itself, just
     * paced slower. Each successful hit is one full, unhurried lap around
     * the wall — six hits, six days (Joshua 6:3) — so "Day X of 6" is read
     * directly off [com.bibleadventures.game.puzzles.rhythmlane.RhythmLaneGameState.hits],
     * no separate day-counting state needed. A missed beat is never a
     * setback (no "guards' awareness" danger meter, unlike the original
     * blueprint this was adapted from) — it simply doesn't advance the day
     * count yet, matching this app's non-negotiable no-failure-state rule.
     */
    val sixDayMarchChart = RhythmLaneChart(
        notes = listOf(
            RhythmNote("step_1", lane = 0, hitTimeMs = 800),
            RhythmNote("step_2", lane = 1, hitTimeMs = 2000),
            RhythmNote("step_3", lane = 2, hitTimeMs = 3200),
            RhythmNote("step_4", lane = 0, hitTimeMs = 4400),
            RhythmNote("step_5", lane = 1, hitTimeMs = 5600),
            RhythmNote("step_6", lane = 2, hitTimeMs = 6800),
        ),
        loopDurationMs = 7600,
    )
    const val SIX_DAY_MARCH_REQUIRED_HITS = 6

    // The seventh day's instructions: march seven times, then the trumpets and the
    // shout (Joshua 6:4, 6:15-16).
    val seventhDayContextLines: List<Int> = listOf(
        R.string.jericho_seventh_day_context_line_1,
        R.string.jericho_seventh_day_context_line_2,
    )

    /** Same 3-lane shape as [sixDayMarchChart], deliberately reused again — the text
     *  itself says "do the march again, seven times, faster" (Joshua 6:15), so
     *  repeating the mechanic here serves the narrative rather than reading as an
     *  accidental duplicate. Notes fall roughly twice as fast, and 7 hits instead of
     *  6 — one per lap. */
    val fastMarchChart = RhythmLaneChart(
        notes = listOf(
            RhythmNote("lap_1", lane = 0, hitTimeMs = 300),
            RhythmNote("lap_2", lane = 1, hitTimeMs = 800),
            RhythmNote("lap_3", lane = 2, hitTimeMs = 1300),
            RhythmNote("lap_4", lane = 0, hitTimeMs = 1800),
            RhythmNote("lap_5", lane = 1, hitTimeMs = 2300),
            RhythmNote("lap_6", lane = 2, hitTimeMs = 2800),
            RhythmNote("lap_7", lane = 0, hitTimeMs = 3300),
        ),
        loopDurationMs = 3800,
    )
    const val FAST_MARCH_REQUIRED_HITS = 7

    /**
     * Reuses `game/puzzles/sequence` exactly as-is (already Daniel's Lions'
     * Den mechanic) — but unlike Lions' Den's fixed discovery-based order,
     * Blow the Shofar tells the player which color to tap next via an
     * on-screen message, and shuffles both the required tap order and each
     * note's screen position fresh every playthrough (see
     * [com.bibleadventures.ui.screens.jericho.JerichoViewModel]) so the
     * puzzle stays a real puzzle despite the guidance (Joshua 6:4's seven
     * trumpets, reframed as five taps for a shorter, kid-paced puzzle).
     */
    val shofarNoteColors: List<ShofarNoteDef> = listOf(
        ShofarNoteDef("red", R.string.jericho_shofar_note_red),
        ShofarNoteDef("orange", R.string.jericho_shofar_note_orange),
        ShofarNoteDef("yellow", R.string.jericho_shofar_note_yellow),
        ShofarNoteDef("green", R.string.jericho_shofar_note_green),
        ShofarNoteDef("blue", R.string.jericho_shofar_note_blue),
    )

    /** Fixed pool of 5 screen positions, arranged in a curved horn-like arc — which color lands in which slot is shuffled per playthrough. */
    val shofarNotePositionSlots: List<Offset> = listOf(
        Offset(0.15f, 0.55f),
        Offset(0.3f, 0.3f),
        Offset(0.5f, 0.18f),
        Offset(0.7f, 0.3f),
        Offset(0.85f, 0.55f),
    )

    /** How many taps fill the Shout meter and bring the wall down — a plain counter, not a new engine (see JerichoUiState). */
    const val SHOUT_REQUIRED_TAPS = 15

    val rahabSavedContextLines: List<Int> = listOf(
        R.string.jericho_rahab_saved_context_line_1,
        R.string.jericho_rahab_saved_context_line_2,
    )
}
