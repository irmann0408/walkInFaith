package com.bibleadventures.game.stories

import com.bibleadventures.R
import com.bibleadventures.game.puzzles.gridmaze.Direction
import com.bibleadventures.game.puzzles.rhythmlane.RhythmLaneChart
import com.bibleadventures.game.puzzles.rhythmlane.RhythmNote

/**
 * Static content for the Jesus Calms the Storm chapter (Mark 4:35-41), the
 * last chapter in the game's chain. Built entirely from existing
 * `game/puzzles` engines at their moderate-to-hardest tier
 * (`stackbuild`, `rhythmlane` x2, `gridmaze`) — no new puzzle engine, per
 * this app's own "check every existing engine before building a new one"
 * rule — with content deliberately tuned to be the hardest example of each
 * engine's own use in the app so far, per an explicit "no easy puzzle"
 * request.
 */
object JesusCalmsStormContent {

    val introDialogueLines: List<Int> = listOf(
        R.string.jesus_calms_storm_intro_line_1,
        R.string.jesus_calms_storm_intro_line_2,
    )

    val loadingContextLines: List<Int> = listOf(
        R.string.jesus_calms_storm_loading_context_line_1,
        R.string.jesus_calms_storm_loading_context_line_2,
    )

    /**
     * Six items loaded aboard before departure (Mark 4:36), each assigned a
     * random distinct "weight" 1-99 fresh every playthrough — same
     * technique as Jericho's twelve memorial stones (`campStoneIds`), just
     * reskinned and sorted **descending** (heaviest first) instead of
     * ascending, since heavy cargo loads first for ballast. The cushion is
     * a deliberate callback: it's what Jesus sleeps on two scenes later.
     */
    val boatItemIds: List<String> = listOf("anchor", "water_jars", "fishing_nets", "food_basket", "oars", "cushion")

    val stormContextLines: List<Int> = listOf(
        R.string.jesus_calms_storm_storm_context_line_1,
        R.string.jesus_calms_storm_storm_context_line_2,
    )

    /**
     * The densest `rhythmlane` chart in the app — 5 notes per loop (every
     * prior 3-lane chart has 3), spaced exactly 600ms apart (comfortably
     * clear of the shared `HIT_WINDOW_MS = 300L` on both sides, so no
     * note's window overlaps its neighbor's) but tighter than any existing
     * chart's average spacing, for a genuinely relentless "furious squall"
     * pace (Mark 4:37). `requiredHits = 15` (three full clean loops) is
     * higher than Feeding the 5,000's Catching (12) — the hardest
     * sustained rhythm-lane challenge in the app so far, by design.
     */
    val bailingChart = RhythmLaneChart(
        notes = listOf(
            RhythmNote("wave_1", lane = 0, hitTimeMs = 500),
            RhythmNote("wave_2", lane = 2, hitTimeMs = 1100),
            RhythmNote("wave_3", lane = 1, hitTimeMs = 1700),
            RhythmNote("wave_4", lane = 0, hitTimeMs = 2300),
            RhythmNote("wave_5", lane = 2, hitTimeMs = 2900),
        ),
        loopDurationMs = 3400,
    )
    const val BAILING_REQUIRED_HITS = 15

    val choiceOptions: List<ChoiceOptionDef> = listOf(
        ChoiceOptionDef("oars", R.string.jesus_calms_storm_choice_option_1, R.string.jesus_calms_storm_choice_reaction_1),
        ChoiceOptionDef("mast", R.string.jesus_calms_storm_choice_option_2, R.string.jesus_calms_storm_choice_reaction_2),
        ChoiceOptionDef("cry_out", R.string.jesus_calms_storm_choice_option_3, R.string.jesus_calms_storm_choice_reaction_3),
    )

    val findJesusContextLines: List<Int> = listOf(
        R.string.jesus_calms_storm_findjesus_context_line_1,
        R.string.jesus_calms_storm_findjesus_context_line_2,
    )

    // . = path, # = wall, S = start, D = goal (Jesus, asleep in the stern).
    // 9 rows x 7 cols — a genuine perfect maze (exactly one route between
    // any two cells, generated via randomized spanning-tree backtracking
    // and verified by BFS), not a hand-guessed layout: real dead-end
    // branches force real backtracking, and the only path from S to D is
    // 30 moves — longer than Daniel's Darius maze (28), since this sits
    // later in the game and should read as a step up, not a repeat.
    val reachingJesusMapLayout: List<String> = listOf(
        "S....#.",
        "####.#.",
        ".....#.",
        ".#####.",
        ".#.....",
        ".###.#.",
        "...#.#.",
        "##.#.#.",
        ".....#D",
    )

    // Hand-verified (BFS) 30-move solution from (0,0) to Jesus at (8,6).
    // Used by the instrumented flow test to replay a known-solvable path
    // deterministically, since the map itself is a fixed perfect maze, not
    // shuffled per playthrough (same reasoning as every other hand-authored
    // gridmaze layout in this app, e.g. GoodSamaritanContent.mapLayout).
    val reachingJesusSolutionPath: List<Direction> = listOf(
        Direction.RIGHT, Direction.RIGHT, Direction.RIGHT, Direction.RIGHT,
        Direction.DOWN, Direction.DOWN,
        Direction.LEFT, Direction.LEFT, Direction.LEFT, Direction.LEFT,
        Direction.DOWN, Direction.DOWN, Direction.DOWN, Direction.DOWN,
        Direction.RIGHT, Direction.RIGHT,
        Direction.DOWN, Direction.DOWN,
        Direction.RIGHT, Direction.RIGHT,
        Direction.UP, Direction.UP, Direction.UP, Direction.UP,
        Direction.RIGHT, Direction.RIGHT,
        Direction.DOWN, Direction.DOWN, Direction.DOWN, Direction.DOWN,
    )

    val calmContextLines: List<Int> = listOf(
        R.string.jesus_calms_storm_calm_context_line_1,
        R.string.jesus_calms_storm_calm_context_line_2,
    )

    /**
     * The climax (Mark 4:39) — 3 notes in strict narrative order, one per
     * word: PEACE (lane 0), BE (lane 1), STILL (lane 2). Unlike every other
     * `rhythmlane` chart in this app, the 3 lanes here are static,
     * always-visible word buttons (same tap-when-lit shape as Esther's
     * Corridor/Jericho's marches), not a steered object — the player must
     * recognize *which word* is currently live, not just track a spatial
     * position, which is what makes this genuinely hard despite only
     * needing 3 hits total (`requiredHits = 3`, said once, matching the
     * instant, one-time nature of the miracle — repeating it would
     * undercut the narrative beat).
     */
    val peaceBeStillChart = RhythmLaneChart(
        notes = listOf(
            RhythmNote("peace", lane = 0, hitTimeMs = 800),
            RhythmNote("be", lane = 1, hitTimeMs = 1800),
            RhythmNote("still", lane = 2, hitTimeMs = 2800),
        ),
        loopDurationMs = 3600,
    )
    const val PEACE_BE_STILL_REQUIRED_HITS = 3
}
