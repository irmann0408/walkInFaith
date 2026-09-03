package com.bibleadventures.jericho

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import com.bibleadventures.MainActivity
import com.bibleadventures.R
import com.bibleadventures.completeDavidGoliath
import com.bibleadventures.completeNoahsArk
import com.bibleadventures.game.puzzles.gridmaze.Direction
import com.bibleadventures.game.puzzles.rhythmlane.RhythmLaneChart
import com.bibleadventures.game.puzzles.slidingpuzzle.SlidingPuzzleGame
import com.bibleadventures.game.puzzles.slidingpuzzle.SlidingPuzzleGameState
import com.bibleadventures.game.stories.DanielContent
import com.bibleadventures.game.stories.EstherContent
import com.bibleadventures.game.stories.GoodSamaritanContent
import com.bibleadventures.game.stories.JerichoContent
import org.junit.Rule
import org.junit.Test

/**
 * Walks the full Battle of Jericho adventure end to end — rebuilt with 4
 * real mini-puzzles (the spies' rope escape, setting up camp, the six-day
 * silent march, and the seventh-day fast march/shofar/shout finale),
 * replacing the old 4-flashcard "March and the Shout" that had no real
 * challenge. It's locked until Noah's Ark, David and Goliath, Good
 * Samaritan, Daniel, and Esther are completed — and this device's save
 * data persists real state across test runs — so this test completes all
 * five prerequisites itself rather than assuming they're already done, to
 * stay deterministic regardless of what ran before it (same pattern as
 * EstherFlowTest). This is also the test that finally re-confirms the
 * original chain's tail: completing Jericho unlocks Feeding the 5,000.
 *
 * Esther is one chapter (4 sequential mini-puzzles) — see EstherFlowTest
 * for the thorough walkthrough asserting its own reward details; this test
 * only needs to clear it as a prerequisite.
 */
class JerichoFlowTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    /**
     * The World Map is a LazyColumn — items far outside the composition
     * window don't exist in the semantics tree until scrolled into view, so
     * plain performScrollTo() (which requires the node to already exist)
     * isn't enough. Scrolling the tagged list itself via performScrollToNode
     * incrementally scrolls until a matching item is composed.
     */
    private fun scrollToChapterOnWorldMap(title: String) {
        composeTestRule.onNodeWithTag("world_map_chapter_list").performScrollToNode(hasText(title))
    }

    @Test
    fun completingJericho_awardsStarsAndUnlocksFeeding5000OnTheWorldMap() {
        val activity = composeTestRule.activity
        val continueLabel = activity.getString(R.string.action_continue)
        val nextPageLabel = activity.getString(R.string.action_next_page)

        composeTestRule.onNodeWithText(activity.getString(R.string.menu_adventures)).performClick()
        composeTestRule.completeNoahsArk()
        composeTestRule.completeDavidGoliath()
        completeGoodSamaritan(continueLabel)
        completeDaniel()
        completeEsther()

        // World Map -> The Battle of Jericho (now unlocked).
        scrollToChapterOnWorldMap(activity.getString(R.string.chapter_jericho_title))
        composeTestRule.onNodeWithText(activity.getString(R.string.chapter_jericho_title)).performClick()

        // Scene 1: Intro.
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // Scene 1b: Rahab's House context.
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // Scene 1c: Rahab Helps the Spies (narrative-only).
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // Scene 2: Spies Escape — a 3x3 sliding puzzle, genuinely randomly shuffled
        // each run (SlidingPuzzleGame.newShuffled uses Random.Default), so it's
        // solved live by reading the board and running a real BFS, not a hardcoded
        // tap sequence.
        solveSpiesEscapePuzzle()
        composeTestRule.onNodeWithText(nextPageLabel).performClick() // leaves the puzzle screen itself

        // Scene 2b: Over the Wall context (rope, scarlet cord, 3 days).
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // Scene 3: Choice — trusting an unusual plan, flavor-only.
        composeTestRule.onNodeWithText(activity.getString(R.string.jericho_choice_option_1)).performClick()
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // Scene 3b: Crossing the Jordan context.
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // Scene 4: Setting Up Camp — 12 memorial stones, each randomly
        // valued 1-99, dragged onto the monument in ascending order.
        completeSettingUpCamp()
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // Scene 4b: Camp by the River context -> The Walls of Jericho context.
        composeTestRule.onNodeWithText(nextPageLabel).performClick()
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // Scene 5: The Silent March — six taps across three lanes, one per day, on the beat.
        completeMarch(JerichoContent.sixDayMarchChart, JerichoContent.SIX_DAY_MARCH_REQUIRED_HITS, R.string.jericho_six_day_march_lane_content_description)
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // Scene 5b: The Seventh Day context.
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // Scene 6: Seven Times Around — the same march mechanic again, faster.
        completeMarch(JerichoContent.fastMarchChart, JerichoContent.FAST_MARCH_REQUIRED_HITS, R.string.jericho_fast_march_lane_content_description)
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // Scene 7: Blow the Shofar — 5 random multiplication/division
        // problems; a wrong guess is free (no failure state), so just try
        // each of the 3 positionally-tagged choices until the note count
        // advances.
        completeBlowShofar()
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // Scene 8: Shout! — a plain tap counter, every tap makes progress.
        val shoutDescription = activity.getString(R.string.jericho_shout_button_content_description)
        repeat(JerichoContent.SHOUT_REQUIRED_TAPS) {
            composeTestRule.onNodeWithContentDescription(shoutDescription).performClick()
        }
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // Scene 8b: Rahab is Saved context.
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // Scene 9: Lesson.
        composeTestRule.onNodeWithText(activity.getString(R.string.jericho_lesson_title)).assertExists()
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // Scene 5: Reward.
        composeTestRule.onNodeWithText(activity.getString(R.string.reward_title)).assertExists()
        composeTestRule.onNodeWithText(activity.getString(R.string.badge_faithful_steps_title)).assertExists()
        composeTestRule.onNodeWithText(activity.getString(R.string.action_return_to_map)).performClick()

        // Back on the World Map: Jericho completed, Feeding the 5,000 unlocked — closing
        // the loop back to the original chain's tail.
        composeTestRule.onNodeWithText(activity.getString(R.string.world_map_title)).assertExists()
        scrollToChapterOnWorldMap(activity.getString(R.string.chapter_feeding_5000_title))
        composeTestRule.onNodeWithText(activity.getString(R.string.chapter_feeding_5000_title)).assertExists()
    }


    /** Walks Good Samaritan end to end (mirrors GoodSamaritanFlowTest) so Daniel unlocks. */
    private fun completeGoodSamaritan(continueLabel: String) {
        val activity = composeTestRule.activity
        val nextPageLabel = activity.getString(R.string.action_next_page)

        scrollToChapterOnWorldMap(activity.getString(R.string.chapter_good_samaritan_title))
        composeTestRule.onNodeWithText(activity.getString(R.string.chapter_good_samaritan_title)).performClick()

        composeTestRule.onNodeWithText(nextPageLabel).performClick() // Intro
        composeTestRule.onNodeWithText(nextPageLabel).performClick() // The Road to Jericho context

        val upLabel = activity.getString(R.string.good_samaritan_direction_up)
        val downLabel = activity.getString(R.string.good_samaritan_direction_down)
        val leftLabel = activity.getString(R.string.good_samaritan_direction_left)
        val rightLabel = activity.getString(R.string.good_samaritan_direction_right)
        val helpingBeatTitle = activity.getString(R.string.good_samaritan_helping_beat_title)

        GoodSamaritanContent.solutionPath.forEach { direction ->
            val label = when (direction) {
                Direction.UP -> upLabel
                Direction.DOWN -> downLabel
                Direction.LEFT -> leftLabel
                Direction.RIGHT -> rightLabel
            }
            composeTestRule.onNodeWithContentDescription(label).performClick()

            val helpingBeatShown = composeTestRule.onAllNodesWithText(helpingBeatTitle).fetchSemanticsNodes().isNotEmpty()
            if (helpingBeatShown) {
                // This "Continue" belongs to HelpingBeatOverlay, a full-screen dialog.
                composeTestRule.onNodeWithText(continueLabel).performClick()
            }
        }

        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        composeTestRule.onNodeWithText(activity.getString(R.string.good_samaritan_lesson_title)).assertExists()
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        composeTestRule.onNodeWithText(activity.getString(R.string.reward_title)).assertExists()
        composeTestRule.onNodeWithText(activity.getString(R.string.action_return_to_map)).performClick()
    }

    /** Walks Daniel and the Lions end to end (mirrors DanielFlowTest) so Esther unlocks. */
    private fun completeDaniel() {
        val activity = composeTestRule.activity
        val nextPageLabel = activity.getString(R.string.action_next_page)

        scrollToChapterOnWorldMap(activity.getString(R.string.chapter_daniel_title))
        composeTestRule.onNodeWithText(activity.getString(R.string.chapter_daniel_title)).performClick()

        composeTestRule.onNodeWithText(nextPageLabel).performClick() // Intro
        composeTestRule.onNodeWithText(nextPageLabel).performClick() // Hurrying to Pray context

        completeLaneAvoid(
            chart = DanielContent.hurryToPrayChart,
            requiredAvoids = DanielContent.HURRY_TO_PRAY_REQUIRED_AVOIDS,
            titleRes = R.string.daniel_stealth_title,
            progressLabelRes = R.string.daniel_stealth_progress_label,
            characterContentDescriptionRes = R.string.daniel_stealth_character_content_description,
            moveLeftLabelRes = R.string.daniel_stealth_move_left_content_description,
            moveRightLabelRes = R.string.daniel_stealth_move_right_content_description,
        )
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        composeTestRule.onNodeWithText(activity.getString(R.string.daniel_choice_option_1)).performClick()
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        composeTestRule.onNodeWithText(nextPageLabel).performClick() // Into the Lions' Den context

        // The Angel's Shield — 5 random math problems. Two wrong answers in a
        // row now replace the problem instead of leaving the last choice a
        // guaranteed-correct guess, so compute the real answer instead of
        // trying all 3 choices blind.
        repeat(DanielContent.LIONS_DEN_PROBLEM_COUNT) {
            solveLionsDenProblem()
        }
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        composeTestRule.onNodeWithText(nextPageLabel).performClick() // Darius's Long Night context

        val upLabel = activity.getString(R.string.daniel_darius_direction_up)
        val downLabel = activity.getString(R.string.daniel_darius_direction_down)
        val mazeLeftLabel = activity.getString(R.string.daniel_darius_direction_left)
        val mazeRightLabel = activity.getString(R.string.daniel_darius_direction_right)
        DanielContent.dariusSolutionPath.forEach { direction ->
            val label = when (direction) {
                Direction.UP -> upLabel
                Direction.DOWN -> downLabel
                Direction.LEFT -> mazeLeftLabel
                Direction.RIGHT -> mazeRightLabel
            }
            composeTestRule.onNodeWithContentDescription(label).performClick()
        }
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        composeTestRule.onNodeWithText(activity.getString(R.string.daniel_lesson_title)).assertExists()
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        composeTestRule.onNodeWithText(activity.getString(R.string.reward_title)).assertExists()
        composeTestRule.onNodeWithText(activity.getString(R.string.action_return_to_map)).performClick()
    }

    /**
     * Walks the merged Esther's Rescue of Her People chapter end to end so
     * Jericho unlocks. See EstherFlowTest for the thorough walkthrough that
     * also asserts the chapter's own reward details (one badge, all 5
     * scripture cards); this only needs to clear it as a prerequisite.
     */
    private fun completeEsther() {
        val activity = composeTestRule.activity
        val nextPageLabel = activity.getString(R.string.action_next_page)

        scrollToChapterOnWorldMap(activity.getString(R.string.chapter_esther_title))
        composeTestRule.onNodeWithText(activity.getString(R.string.chapter_esther_title)).performClick()

        composeTestRule.onNodeWithText(nextPageLabel).performClick() // Intro
        composeTestRule.onNodeWithText(nextPageLabel).performClick() // Chosen for the Palace context

        EstherContent.royalAttireItems.forEach { item ->
            composeTestRule.onNodeWithContentDescription(activity.getString(item.nameRes)).performClick()
        }
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        composeTestRule.onNodeWithText(nextPageLabel).performClick() // Esther Becomes Queen context

        composeTestRule.onNodeWithText(activity.getString(R.string.esther_new_queen_choice_option_1)).performClick()
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        composeTestRule.onNodeWithText(nextPageLabel).performClick() // A Dangerous Secret context

        val upLabel = activity.getString(R.string.esther_secret_plot_direction_up)
        val downLabel = activity.getString(R.string.esther_secret_plot_direction_down)
        val stealthLeftLabel = activity.getString(R.string.esther_secret_plot_direction_left)
        val stealthRightLabel = activity.getString(R.string.esther_secret_plot_direction_right)
        EstherContent.courtyardSolutionPath.forEach { direction ->
            val label = when (direction) {
                Direction.UP -> upLabel
                Direction.DOWN -> downLabel
                Direction.LEFT -> stealthLeftLabel
                Direction.RIGHT -> stealthRightLabel
            }
            composeTestRule.onNodeWithContentDescription(label).performClick()
        }
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        composeTestRule.onNodeWithText(nextPageLabel).performClick() // The King is Warned context
        composeTestRule.onNodeWithText(nextPageLabel).performClick() // A Wicked Law context

        // Hand-solved 5x5 Latin square (cell = (row + col) mod 5): fill every
        // empty cell left by EstherContent.sudokuGivens, row by row.
        val iconKeyToLabel = mapOf(
            "star" to R.string.esther_threat_icon_star,
            "moon" to R.string.esther_threat_icon_moon,
            "sun" to R.string.esther_threat_icon_sun,
            "drop" to R.string.esther_threat_icon_drop,
            "leaf" to R.string.esther_threat_icon_leaf,
        )
        val sudokuSolution = listOf(
            Triple(0, 2, "sun"), Triple(0, 3, "drop"),
            Triple(1, 1, "sun"), Triple(1, 3, "leaf"),
            Triple(2, 2, "leaf"), Triple(2, 4, "moon"),
            Triple(3, 0, "drop"), Triple(3, 3, "moon"),
            Triple(4, 1, "star"), Triple(4, 4, "drop"),
        )
        sudokuSolution.forEach { (row, col, iconKey) ->
            val cellLabel = activity.getString(R.string.esther_threat_sudoku_cell_content_description, row + 1, col + 1)
            composeTestRule.onNodeWithContentDescription(cellLabel, substring = true).performClick()
            composeTestRule.onNodeWithContentDescription(activity.getString(iconKeyToLabel.getValue(iconKey))).performClick()
        }
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        composeTestRule.onNodeWithText(nextPageLabel).performClick() // The City Mourns and Fasts context

        composeTestRule.onNodeWithText(activity.getString(R.string.esther_brave_approach_choice_option_1)).performClick()
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        composeTestRule.onNodeWithText(nextPageLabel).performClick() // Three Days of Fasting context

        // Corridor Courage Meter — a 3-lane rhythm mini-game. Freeze the clock and
        // advance to each authored note's exact time so every tap lands; a
        // mistimed tap would just be a no-op (see RhythmLaneGame's no-failure design).
        // Leads straight into the Lesson now — Reveal Haman's Plot and its
        // surrounding context cards were dropped to tighten the chapter's tail end.
        completeCorridorRhythmLane()
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        composeTestRule.onNodeWithText(activity.getString(R.string.esther_brave_approach_lesson_title)).assertExists()
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        composeTestRule.onNodeWithText(activity.getString(R.string.reward_title)).assertExists()
        // The Reward screen scrolls (5 scripture cards + badge won't fit one
        // screen), so Return to Map sits below the fold — scroll to it first.
        val returnToMapNode = composeTestRule.onNodeWithText(activity.getString(R.string.action_return_to_map))
        returnToMapNode.performScrollTo()
        returnToMapNode.performClick()
    }

    /**
     * Freezes the Compose test clock and advances it to each of the
     * corridor's authored notes' exact `hitTimeMs` in turn, tapping that
     * note's lane — fully deterministic, no timing luck. Loops through the
     * chart as many times as needed to reach `CORRIDOR_REQUIRED_HITS`. The
     * screen drives its scroll clock with a manual `withFrameNanos`
     * accumulator specifically so `mainClock.advanceTimeBy(...)` can
     * control it this way (unlike `rememberInfiniteTransition`, which
     * Sling Practice's tests found doesn't progress under a frozen clock).
     */
    private fun completeCorridorRhythmLane() {
        val activity = composeTestRule.activity
        val laneDescriptions = (1..3).map {
            activity.getString(R.string.esther_brave_approach_corridor_lane_content_description, it)
        }
        val chart = EstherContent.corridorChart

        composeTestRule.mainClock.autoAdvance = false
        var currentMs = 0L
        var hits = 0
        var loopIndex = 0L
        while (hits < EstherContent.CORRIDOR_REQUIRED_HITS) {
            chart.notes.forEach { note ->
                if (hits < EstherContent.CORRIDOR_REQUIRED_HITS) {
                    val targetMs = loopIndex * chart.loopDurationMs + note.hitTimeMs
                    composeTestRule.mainClock.advanceTimeBy(targetMs - currentMs)
                    currentMs = targetMs
                    composeTestRule.onNodeWithContentDescription(laneDescriptions[note.lane]).performClick()
                    hits++
                }
            }
            loopIndex++
        }
        composeTestRule.mainClock.autoAdvance = true
    }

    /**
     * The Spies Escape sliding puzzle is genuinely randomly shuffled each
     * run ([SlidingPuzzleGame.newShuffled] uses `Random.Default`, unlike
     * every hand-verified deterministic map/chart elsewhere in this app),
     * so there's no fixed tap sequence to hardcode. Instead: read the live
     * board off its tiles' screen positions, solve it with a real
     * breadth-first search over [SlidingPuzzleGame]'s own transition
     * function (a 3x3 board's state space is small — this finishes in well
     * under a second), then tap each moved tile's number in order.
     */
    private fun solveSpiesEscapePuzzle() {
        val activity = composeTestRule.activity
        val size = JerichoContent.SPIES_ESCAPE_GRID_SIZE
        val board = readSlidingPuzzleBoard(size)
        val solutionTileValues = solveSlidingPuzzle(SlidingPuzzleGameState(tiles = board, size = size))

        solutionTileValues.forEach { value ->
            val description = activity.getString(R.string.jericho_spies_escape_tile_content_description, value)
            composeTestRule.onNodeWithContentDescription(description).performClick()
        }
    }

    /** Reconstructs the board (row-major, empty slot as 0) from each tile's on-screen position. */
    private fun readSlidingPuzzleBoard(size: Int): List<Int> {
        val activity = composeTestRule.activity
        val emptyDescription = activity.getString(R.string.jericho_spies_escape_empty_content_description)

        val positioned = mutableListOf<Pair<Offset, Int>>()
        val emptyBounds = composeTestRule.onNodeWithContentDescription(emptyDescription).fetchSemanticsNode().boundsInRoot
        positioned += Offset(emptyBounds.left, emptyBounds.top) to 0
        for (value in 1 until size * size) {
            val description = activity.getString(R.string.jericho_spies_escape_tile_content_description, value)
            val bounds = composeTestRule.onNodeWithContentDescription(description).fetchSemanticsNode().boundsInRoot
            positioned += Offset(bounds.left, bounds.top) to value
        }

        return positioned.sortedWith(compareBy({ it.first.y }, { it.first.x })).map { it.second }
    }

    /** Plain BFS over [SlidingPuzzleGame]'s real transition function — returns the tile *value* tapped at each step. */
    private fun solveSlidingPuzzle(start: SlidingPuzzleGameState): List<Int> {
        if (start.isComplete) return emptyList()

        val visited = mutableSetOf(start.tiles)
        val queue = ArrayDeque<SlidingPuzzleGameState>()
        val cameFrom = mutableMapOf<List<Int>, Pair<List<Int>, Int>>() // state -> (previous state, tapped value)
        queue.add(start)

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (current.isComplete) {
                val path = mutableListOf<Int>()
                var stateKey = current.tiles
                while (stateKey in cameFrom) {
                    val (previous, value) = cameFrom.getValue(stateKey)
                    path.add(0, value)
                    stateKey = previous
                }
                return path
            }

            val emptyIndex = current.emptyIndex
            val row = emptyIndex / current.size
            val col = emptyIndex % current.size
            val neighborIndices = buildList {
                if (row > 0) add(emptyIndex - current.size)
                if (row < current.size - 1) add(emptyIndex + current.size)
                if (col > 0) add(emptyIndex - 1)
                if (col < current.size - 1) add(emptyIndex + 1)
            }

            neighborIndices.forEach { index ->
                val next = SlidingPuzzleGame.onTileTapped(current, index)
                if (next.tiles !in visited) {
                    visited += next.tiles
                    cameFrom[next.tiles] = current.tiles to current.tiles[index]
                    queue.add(next)
                }
            }
        }
        error("No solution found — should never happen, SlidingPuzzleGame.newShuffled is always solvable")
    }

    /**
     * Stone values are random every playthrough (1-99, no duplicates), so
     * this can't know in advance which stone is smallest. Checking whether
     * a given value's content description currently exists is a cheap
     * semantics-tree query, not a real gesture, so scanning 1..99 to find
     * the smallest value still present among the remaining tray stones is
     * fast — and that smallest value is always the correct next stone,
     * since the required order is ascending.
     */
    private fun completeSettingUpCamp() {
        val activity = composeTestRule.activity
        val dropZoneDescription = activity.getString(R.string.jericho_camp_dropzone_content_description)

        repeat(JerichoContent.campStoneIds.size) {
            val smallestRemainingValue = (1..99).first { value ->
                val label = activity.getString(R.string.jericho_camp_stone_content_description, value)
                composeTestRule.onAllNodesWithContentDescription(label).fetchSemanticsNodes().isNotEmpty()
            }
            val label = activity.getString(R.string.jericho_camp_stone_content_description, smallestRemainingValue)
            dragOntoContentDescription(
                itemNode = composeTestRule.onNodeWithContentDescription(label),
                targetContentDescription = dropZoneDescription,
            )
            composeTestRule.waitForIdle()
        }
    }

    /**
     * Freezes the Compose test clock and advances it to each of [chart]'s
     * authored notes' exact `hitTimeMs` in turn, tapping that note's lane —
     * same deterministic frozen-clock technique as Esther's corridor
     * (`completeCorridorRhythmLane`), reused here now that both `SixDayMarch`
     * and `FastMarch` share Corridor's 3-lane layout. Loops through the
     * chart as many times as needed to reach [requiredHits].
     */
    private fun completeMarch(chart: RhythmLaneChart, requiredHits: Int, laneContentDescriptionRes: Int) {
        val activity = composeTestRule.activity
        val laneDescriptions = (1..3).map { activity.getString(laneContentDescriptionRes, it) }

        composeTestRule.mainClock.autoAdvance = false
        var currentMs = 0L
        var hits = 0
        var loopIndex = 0L
        while (hits < requiredHits) {
            chart.notes.forEach { note ->
                if (hits < requiredHits) {
                    val targetMs = loopIndex * chart.loopDurationMs + note.hitTimeMs
                    composeTestRule.mainClock.advanceTimeBy(targetMs - currentMs)
                    currentMs = targetMs
                    composeTestRule.onNodeWithContentDescription(laneDescriptions[note.lane]).performClick()
                    hits++
                }
            }
            loopIndex++
        }
        composeTestRule.mainClock.autoAdvance = true
    }

    /**
     * Reads the displayed "%d × %d = ?" / "%d ÷ %d = ?" problem, computes
     * the real answer, and taps the matching choice by its content
     * description. Two wrong answers in a row now replace the problem
     * instead of leaving the last choice a guaranteed-correct guess (see
     * `DecisionPathGame.WRONG_ATTEMPTS_BEFORE_NEW_STEP`), so the old "try
     * each of the 3 choices" trick no longer reliably solves it.
     */
    private fun completeBlowShofar() {
        repeat(JerichoContent.shofarNoteIds.size) {
            solveShofarProblem()
        }
    }

    private fun solveShofarProblem() {
        val problemText = composeTestRule.onNodeWithTag("shofar_problem").fetchSemanticsNode()
            .config[SemanticsProperties.Text].joinToString(separator = "") { it.text }
        val operands = Regex("\\d+").findAll(problemText).map { it.value.toInt() }.toList()
        val correctValue = if ("÷" in problemText) operands[0] / operands[1] else operands[0] * operands[1]
        composeTestRule.onNodeWithContentDescription(correctValue.toString()).performClick()
    }

    /** Same technique as [solveShofarProblem], for Daniel's Angel's Shield "%d + %d = ?" / "%d − %d = ?" problems. */
    private fun solveLionsDenProblem() {
        val problemText = composeTestRule.onNodeWithTag("lions_den_problem").fetchSemanticsNode()
            .config[SemanticsProperties.Text].joinToString(separator = "") { it.text }
        val operands = Regex("\\d+").findAll(problemText).map { it.value.toInt() }.toList()
        val correctValue = if ("−" in problemText) operands[0] - operands[1] else operands[0] + operands[1]
        composeTestRule.onNodeWithContentDescription(correctValue.toString()).performClick()
    }

    /**
     * Crossing the Valley / Hurrying to Pray's rhythmlane "avoid" mechanic
     * auto-judges every frame purely from the character's current lane (see
     * `RhythmLaneGame.onLaneAvoided`) — same shape as Feeding the 5,000's
     * Gathering the Leftovers catch mechanic, so it inherits the same
     * implicit-idle-sync unpredictability: Compose's idle-sync (which runs
     * as an ordinary part of `performClick()`, even under
     * `mainClock.autoAdvance = false`) pumps this screen's infinite
     * `withFrameNanos` loop forward by an unpredictable amount before test
     * code regains control, so a schedule computed from an assumed start of
     * 0 would land on the wrong moments.
     *
     * Sidesteps that entirely: freeze the clock, then for each of the 3
     * lanes, park the character there and advance the clock by one full
     * `chart.loopDurationMs` — since every note recurs exactly once per
     * loop, a full-loop dwell in a lane is guaranteed to pass through (and
     * avoid) every note assigned to that lane exactly once, regardless of
     * where in the loop the clock actually started. Progress is read live
     * off the progress-label text after every sweep, so it's also robust to
     * however many "free" avoids already happened before this function got
     * control.
     */
    private fun completeLaneAvoid(
        chart: RhythmLaneChart,
        requiredAvoids: Int,
        titleRes: Int,
        progressLabelRes: Int,
        characterContentDescriptionRes: Int,
        moveLeftLabelRes: Int,
        moveRightLabelRes: Int,
    ) {
        val activity = composeTestRule.activity
        val lanes = chart.notes.map { it.lane }.distinct().sorted()

        // Let the screen fully compose (with the clock still auto-advancing)
        // before freezing it — freezing immediately after navigating can
        // catch the new screen before its first frame lands, so even static
        // elements like the progress label aren't in the semantics tree yet.
        composeTestRule.onNodeWithText(activity.getString(titleRes)).assertExists()

        composeTestRule.mainClock.autoAdvance = false
        var safetyRounds = 0
        while (currentLaneAvoidHits(progressLabelRes, requiredAvoids) < requiredAvoids) {
            check(safetyRounds++ < 20) { "Lane-avoid puzzle didn't reach $requiredAvoids avoids after 20 full sweep rounds — stuck at ${currentLaneAvoidHits(progressLabelRes, requiredAvoids)}" }
            lanes.forEach { lane ->
                if (currentLaneAvoidHits(progressLabelRes, requiredAvoids) < requiredAvoids) {
                    moveCharacterToLane(lane, characterContentDescriptionRes, moveLeftLabelRes, moveRightLabelRes)
                    composeTestRule.mainClock.advanceTimeBy(chart.loopDurationMs)
                }
            }
        }
        composeTestRule.mainClock.autoAdvance = true
    }

    private fun currentLaneAvoidHits(progressLabelRes: Int, requiredAvoids: Int): Int {
        val activity = composeTestRule.activity
        return (0..requiredAvoids).first { candidateHits ->
            val label = activity.getString(progressLabelRes, candidateHits, requiredAvoids)
            composeTestRule.onAllNodesWithText(label).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun currentCharacterLane(characterContentDescriptionRes: Int): Int {
        val activity = composeTestRule.activity
        return (1..3).first { candidateLane ->
            val label = activity.getString(characterContentDescriptionRes, candidateLane)
            composeTestRule.onAllNodesWithContentDescription(label).fetchSemanticsNodes().isNotEmpty()
        } - 1
    }

    private fun moveCharacterToLane(targetLane: Int, characterContentDescriptionRes: Int, moveLeftLabelRes: Int, moveRightLabelRes: Int) {
        val activity = composeTestRule.activity
        val moveLeftLabel = activity.getString(moveLeftLabelRes)
        val moveRightLabel = activity.getString(moveRightLabelRes)

        while (currentCharacterLane(characterContentDescriptionRes) != targetLane) {
            val label = if (currentCharacterLane(characterContentDescriptionRes) < targetLane) moveRightLabel else moveLeftLabel
            composeTestRule.onNodeWithContentDescription(label).performClick()
        }
    }

    private fun dragOntoContentDescription(itemNode: SemanticsNodeInteraction, targetContentDescription: String) {
        val itemBounds = itemNode.fetchSemanticsNode().boundsInRoot
        val targetBounds = composeTestRule.onNodeWithContentDescription(targetContentDescription).fetchSemanticsNode().boundsInRoot
        val targetGlobalCenter = targetBounds.center
        val localEnd = Offset(targetGlobalCenter.x - itemBounds.left, targetGlobalCenter.y - itemBounds.top)

        itemNode.performTouchInput {
            swipe(start = center, end = localEnd, durationMillis = 200)
        }
    }
}
