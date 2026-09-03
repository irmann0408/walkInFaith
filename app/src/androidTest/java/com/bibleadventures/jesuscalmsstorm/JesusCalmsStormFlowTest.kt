package com.bibleadventures.jesuscalmsstorm

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
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
import com.bibleadventures.completeGoodSamaritan
import com.bibleadventures.completeNoahsArk
import com.bibleadventures.game.puzzles.gridmaze.Direction
import com.bibleadventures.game.puzzles.rhythmlane.RhythmLaneChart
import com.bibleadventures.game.puzzles.slidingpuzzle.SlidingPuzzleGame
import com.bibleadventures.game.puzzles.slidingpuzzle.SlidingPuzzleGameState
import com.bibleadventures.game.stories.DanielContent
import com.bibleadventures.game.stories.EstherContent
import com.bibleadventures.game.stories.Feeding5000Content
import com.bibleadventures.game.stories.JerichoContent
import com.bibleadventures.game.stories.JesusCalmsStormContent
import org.junit.Rule
import org.junit.Test

/**
 * Walks the full Jesus Calms the Storm adventure end to end — the last
 * chapter in the chain, built from 4 real mini-puzzles (loading the boat
 * heaviest-first, bailing against the densest `rhythmlane` chart in the
 * app, reaching Jesus through a genuine dead-end-laden maze, and speaking
 * "Peace, be still" at exactly the right moment), all reused from this
 * app's existing engines — no new engine. It's locked until all 7 prior
 * chapters are completed — and this device's save data persists real state
 * across test runs — so this test completes every prerequisite itself
 * rather than assuming it's already done, to stay deterministic regardless
 * of what ran before it (same pattern as every other late-chapter flow
 * test in this app). Since this is the *last* chapter in the chain, no
 * other flow test needs to complete it as a prerequisite — this is the
 * only file that walks it.
 */
class JesusCalmsStormFlowTest {

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
    fun completingJesusCalmsStorm_awardsStarsAndShowsUnshakenFaithBadge() {
        val activity = composeTestRule.activity
        val nextPageLabel = activity.getString(R.string.action_next_page)

        composeTestRule.onNodeWithText(activity.getString(R.string.menu_adventures)).performClick()
        composeTestRule.completeNoahsArk()
        composeTestRule.completeDavidGoliath()
        composeTestRule.completeGoodSamaritan()
        completeDaniel()
        completeEsther()
        completeJericho()
        completeFeeding5000()

        // World Map -> Jesus Calms the Storm (now unlocked).
        scrollToChapterOnWorldMap(activity.getString(R.string.chapter_jesus_calms_storm_title))
        composeTestRule.onNodeWithText(activity.getString(R.string.chapter_jesus_calms_storm_title)).performClick()

        // Scene 1: Intro.
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // Scene 1b: Setting Out context.
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // Scene 2: Loading the Boat — a stackbuild puzzle, randomly
        // weighted each run, solved by reading each item's live weight off
        // the tray and placing heaviest first.
        completeLoadingTheBoat()
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // Scene 2b: A Furious Squall context.
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // Scene 3: Bailing the Boat — the densest rhythmlane chart in the
        // app, catch semantics, same sweep-by-full-loop technique as
        // Gathering the Leftovers (completeCatching).
        completeBailingTheBoat()
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // Scene 4: Choice — flavor-only.
        composeTestRule.onNodeWithText(activity.getString(R.string.jesus_calms_storm_choice_option_1)).performClick()
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // Scene 4b: Where Is Jesus? context.
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // Scene 5: Reaching Jesus — a fixed, hand-verified perfect maze (not
        // shuffled per playthrough), replayed via its known solution path,
        // same technique as Daniel's Darius maze.
        JesusCalmsStormContent.reachingJesusSolutionPath.forEach { direction ->
            val label = when (direction) {
                Direction.UP -> activity.getString(R.string.jesus_calms_storm_reaching_jesus_direction_up)
                Direction.DOWN -> activity.getString(R.string.jesus_calms_storm_reaching_jesus_direction_down)
                Direction.LEFT -> activity.getString(R.string.jesus_calms_storm_reaching_jesus_direction_left)
                Direction.RIGHT -> activity.getString(R.string.jesus_calms_storm_reaching_jesus_direction_right)
            }
            composeTestRule.onNodeWithContentDescription(label).performClick()
        }
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // Scene 5b: Quiet! Be Still! context.
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // Scene 6: Peace, Be Still — the climax. 3 static, always-tappable
        // word lanes in strict narrative order, same frozen-clock
        // exact-timestamp technique as Jericho's marches/Esther's corridor
        // (a discrete tap action exists here, unlike Bailing's steered
        // object, so exact scheduling is reliable).
        completePeaceBeStill()
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // Scene 7: Lesson.
        composeTestRule.onNodeWithText(activity.getString(R.string.jesus_calms_storm_lesson_title)).assertExists()
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // Scene 8: Reward.
        composeTestRule.onNodeWithText(activity.getString(R.string.reward_title)).assertExists()
        composeTestRule.onNodeWithText(activity.getString(R.string.badge_unshaken_faith_title)).assertExists()
        composeTestRule.onNodeWithText(activity.getString(R.string.action_return_to_map)).performClick()

        // Back on the World Map: Jesus Calms the Storm completed — the last chapter.
        composeTestRule.onNodeWithText(activity.getString(R.string.world_map_title)).assertExists()
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
     * also asserts the chapter's own reward details; this only needs to
     * clear it as a prerequisite.
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

        val stealthUpLabel = activity.getString(R.string.esther_secret_plot_direction_up)
        val stealthDownLabel = activity.getString(R.string.esther_secret_plot_direction_down)
        val stealthLeftLabel = activity.getString(R.string.esther_secret_plot_direction_left)
        val stealthRightLabel = activity.getString(R.string.esther_secret_plot_direction_right)
        EstherContent.courtyardSolutionPath.forEach { direction ->
            val label = when (direction) {
                Direction.UP -> stealthUpLabel
                Direction.DOWN -> stealthDownLabel
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
     * Walks the Battle of Jericho end to end (mirrors JerichoFlowTest) so
     * Feeding the 5,000 unlocks.
     */
    private fun completeJericho() {
        val activity = composeTestRule.activity
        val nextPageLabel = activity.getString(R.string.action_next_page)

        scrollToChapterOnWorldMap(activity.getString(R.string.chapter_jericho_title))
        composeTestRule.onNodeWithText(activity.getString(R.string.chapter_jericho_title)).performClick()

        composeTestRule.onNodeWithText(nextPageLabel).performClick() // Intro
        composeTestRule.onNodeWithText(nextPageLabel).performClick() // Rahab's House context
        composeTestRule.onNodeWithText(nextPageLabel).performClick() // Rahab Helps the Spies (narrative-only)

        solveSpiesEscapePuzzle()
        composeTestRule.onNodeWithText(nextPageLabel).performClick() // leaves the puzzle screen itself

        composeTestRule.onNodeWithText(nextPageLabel).performClick() // Over the Wall context

        composeTestRule.onNodeWithText(activity.getString(R.string.jericho_choice_option_1)).performClick()
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        composeTestRule.onNodeWithText(nextPageLabel).performClick() // Crossing the Jordan context

        completeSettingUpCamp()
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        composeTestRule.onNodeWithText(nextPageLabel).performClick() // Camp by the River context
        composeTestRule.onNodeWithText(nextPageLabel).performClick() // The Walls of Jericho context

        completeMarch(JerichoContent.sixDayMarchChart, JerichoContent.SIX_DAY_MARCH_REQUIRED_HITS, R.string.jericho_six_day_march_lane_content_description)
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        composeTestRule.onNodeWithText(nextPageLabel).performClick() // The Seventh Day context

        completeMarch(JerichoContent.fastMarchChart, JerichoContent.FAST_MARCH_REQUIRED_HITS, R.string.jericho_fast_march_lane_content_description)
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        completeBlowShofar()
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        val shoutDescription = activity.getString(R.string.jericho_shout_button_content_description)
        repeat(JerichoContent.SHOUT_REQUIRED_TAPS) {
            composeTestRule.onNodeWithContentDescription(shoutDescription).performClick()
        }
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        composeTestRule.onNodeWithText(nextPageLabel).performClick() // Rahab is Saved context

        composeTestRule.onNodeWithText(activity.getString(R.string.jericho_lesson_title)).assertExists()
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        composeTestRule.onNodeWithText(activity.getString(R.string.reward_title)).assertExists()
        composeTestRule.onNodeWithText(activity.getString(R.string.action_return_to_map)).performClick()
    }

    /**
     * Walks Feeding the 5,000 end to end (mirrors Feeding5000FlowTest's own
     * `@Test` body) so Jesus Calms the Storm unlocks. See
     * Feeding5000FlowTest for the thorough walkthrough that also asserts
     * its own reward details; this only needs to clear it as a
     * prerequisite.
     */
    private fun completeFeeding5000() {
        val activity = composeTestRule.activity
        val nextPageLabel = activity.getString(R.string.action_next_page)

        scrollToChapterOnWorldMap(activity.getString(R.string.chapter_feeding_5000_title))
        composeTestRule.onNodeWithText(activity.getString(R.string.chapter_feeding_5000_title)).performClick()

        composeTestRule.onNodeWithText(nextPageLabel).performClick() // Intro
        composeTestRule.onNodeWithText(nextPageLabel).performClick() // So Many People context

        completeGatheringCrowd()
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        composeTestRule.onNodeWithText(nextPageLabel).performClick() // Not Enough context

        composeTestRule.onNodeWithContentDescription(activity.getString(R.string.feeding_5000_searching_for_food_boy_content_description)).performClick()
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        composeTestRule.onNodeWithText(nextPageLabel).performClick() // A Boy's Lunch context

        completeBoysGift()
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        composeTestRule.onNodeWithText(activity.getString(R.string.feeding_5000_choice_option_1)).performClick()
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        composeTestRule.onNodeWithText(nextPageLabel).performClick() // Jesus Gives Thanks context

        completeMiracleMultiplication()
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        composeTestRule.onNodeWithText(nextPageLabel).performClick() // Enough For Everyone context

        completeServing()

        composeTestRule.onNodeWithText(nextPageLabel).performClick()
        composeTestRule.onNodeWithText(activity.getString(R.string.feeding_5000_catching_title)).assertExists()
        completeCatching()
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        composeTestRule.onNodeWithText(activity.getString(R.string.feeding_5000_lesson_title)).assertExists()
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        composeTestRule.onNodeWithText(activity.getString(R.string.reward_title)).assertExists()
        composeTestRule.onNodeWithText(activity.getString(R.string.action_return_to_map)).performClick()
    }

    /**
     * Family headcounts and each circle's exact target sum are randomly
     * generated every run, so there's no fixed drag sequence to hardcode.
     * Instead: read the full remaining headcount multiset and each circle's
     * remaining capacity straight off the screen, solve a real
     * exact-bin-fill assignment with backtracking, then execute it.
     */
    private fun completeGatheringCrowd() {
        val activity = composeTestRule.activity
        val circleTargets = Feeding5000Content.groupFillCircleTargets
        val circleDescriptions = circleTargets.indices.map {
            activity.getString(R.string.feeding_5000_gathering_crowd_circle_content_description, it + 1)
        }

        val remainingHeadcounts = (1..circleTargets.max()).flatMap { headcount ->
            val label = activity.getString(R.string.feeding_5000_gathering_crowd_family_content_description, headcount)
            val count = composeTestRule.onAllNodesWithContentDescription(label).fetchSemanticsNodes().size
            List(count) { headcount }
        }.sortedDescending()

        val assignment = solveGroupFillAssignment(remainingHeadcounts, circleTargets)

        remainingHeadcounts.forEachIndexed { index, headcount ->
            val label = activity.getString(R.string.feeding_5000_gathering_crowd_family_content_description, headcount)
            val itemNode = composeTestRule.onAllNodesWithContentDescription(label)[0]
            dragOntoContentDescription(itemNode = itemNode, targetContentDescription = circleDescriptions[assignment[index]])
            composeTestRule.waitForIdle()
        }
    }

    /** Exact bin-fill backtracking: assigns each of [headcounts] to a circle index (into [targets]) so every circle's assigned values sum exactly to its target. */
    private fun solveGroupFillAssignment(headcounts: List<Int>, targets: List<Int>): List<Int> {
        val assignment = IntArray(headcounts.size) { -1 }
        val remaining = targets.toIntArray()

        fun backtrack(index: Int): Boolean {
            if (index == headcounts.size) return remaining.all { it == 0 }
            val value = headcounts[index]
            for (circle in remaining.indices) {
                if (remaining[circle] >= value) {
                    remaining[circle] -= value
                    assignment[index] = circle
                    if (backtrack(index + 1)) return true
                    remaining[circle] += value
                }
            }
            return false
        }

        check(backtrack(0)) { "No valid group-fill assignment found for $headcounts into $targets" }
        return assignment.toList()
    }

    private fun completeBoysGift() {
        val activity = composeTestRule.activity
        val loafLabel = activity.getString(R.string.feeding_5000_boys_gift_loaf_content_description)
        val fishLabel = activity.getString(R.string.feeding_5000_boys_gift_fish_content_description)

        repeat(5) { index -> composeTestRule.onAllNodesWithContentDescription(loafLabel)[index].performClick() }
        repeat(2) { index -> composeTestRule.onAllNodesWithContentDescription(fishLabel)[index].performClick() }
    }

    private fun solveLionsDenProblem() {
        val problemText = composeTestRule.onNodeWithTag("lions_den_problem").fetchSemanticsNode()
            .config[SemanticsProperties.Text].joinToString(separator = "") { it.text }
        val operands = Regex("\\d+").findAll(problemText).map { it.value.toInt() }.toList()
        val correctValue = if ("−" in problemText) operands[0] - operands[1] else operands[0] + operands[1]
        composeTestRule.onNodeWithContentDescription(correctValue.toString()).performClick()
    }

    private fun completeMiracleMultiplication() {
        repeat(Feeding5000Content.MIRACLE_PROBLEM_COUNT) {
            solveMiracleProblem()
        }
    }

    private fun solveMiracleProblem() {
        val problemText = composeTestRule.onNodeWithTag("miracle_problem").fetchSemanticsNode()
            .config[SemanticsProperties.Text].joinToString(separator = "") { it.text }
        val operands = Regex("\\d+").findAll(problemText).map { it.value.toInt() }.toList()
        composeTestRule.onNodeWithContentDescription((operands[0] * operands[1]).toString()).performClick()
    }

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

    private fun completeServing() {
        val activity = composeTestRule.activity
        val upLabel = activity.getString(R.string.feeding_5000_serving_direction_up)
        val downLabel = activity.getString(R.string.feeding_5000_serving_direction_down)
        val leftLabel = activity.getString(R.string.feeding_5000_serving_direction_left)
        val rightLabel = activity.getString(R.string.feeding_5000_serving_direction_right)

        Feeding5000Content.servingSolutionPath.forEach { direction ->
            val label = when (direction) {
                Direction.UP -> upLabel
                Direction.DOWN -> downLabel
                Direction.LEFT -> leftLabel
                Direction.RIGHT -> rightLabel
            }
            composeTestRule.onNodeWithContentDescription(label).performClick()
        }
    }

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
     * Gathering the Leftovers has a single basket that must be steered into
     * a note's lane to catch it, auto-judged every frame — see
     * Feeding5000FlowTest's own `completeCatching` KDoc for why
     * exact-timestamp scheduling is unreliable here. Sidesteps it by
     * freezing the clock, then for each of the 3 lanes, parking the basket
     * there and advancing a full `chart.loopDurationMs` — every note
     * recurs exactly once per loop, so a full-loop dwell is guaranteed to
     * catch everything assigned to that lane.
     */
    private fun completeCatching() {
        val chart = Feeding5000Content.catchingChart
        val requiredHits = Feeding5000Content.CATCHING_REQUIRED_HITS
        val lanes = chart.notes.map { it.lane }.distinct().sorted()

        composeTestRule.mainClock.autoAdvance = false
        var safetyRounds = 0
        while (currentCatchingHits() < requiredHits) {
            check(safetyRounds++ < 20) { "Gathering the Leftovers didn't reach $requiredHits hits after 20 full sweep rounds — stuck at ${currentCatchingHits()}" }
            lanes.forEach { lane ->
                if (currentCatchingHits() < requiredHits) {
                    moveCatchingBasketTo(lane)
                    composeTestRule.mainClock.advanceTimeBy(chart.loopDurationMs)
                }
            }
        }
        composeTestRule.mainClock.autoAdvance = true
    }

    private fun currentCatchingHits(): Int {
        val activity = composeTestRule.activity
        return (0..Feeding5000Content.CATCHING_REQUIRED_HITS).first { candidateHits ->
            val label = activity.getString(R.string.feeding_5000_catching_progress_label, candidateHits, Feeding5000Content.CATCHING_REQUIRED_HITS)
            composeTestRule.onAllNodesWithText(label).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun currentCatchingBasketLane(): Int {
        val activity = composeTestRule.activity
        return (1..3).first { candidateLane ->
            val label = activity.getString(R.string.feeding_5000_catching_basket_content_description, candidateLane)
            composeTestRule.onAllNodesWithContentDescription(label).fetchSemanticsNodes().isNotEmpty()
        } - 1
    }

    private fun moveCatchingBasketTo(targetLane: Int) {
        val activity = composeTestRule.activity
        val moveLeftLabel = activity.getString(R.string.feeding_5000_catching_move_left_content_description)
        val moveRightLabel = activity.getString(R.string.feeding_5000_catching_move_right_content_description)

        while (currentCatchingBasketLane() != targetLane) {
            val label = if (currentCatchingBasketLane() < targetLane) moveRightLabel else moveLeftLabel
            composeTestRule.onNodeWithContentDescription(label).performClick()
        }
    }

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

    /**
     * Crossing the Valley / Hurrying to Pray's rhythmlane "avoid" mechanic
     * auto-judges every frame purely from the character's current lane —
     * inherits the same implicit-idle-sync unpredictability as Catching.
     * Sidesteps it the same way: freeze the clock, then for each of the 3
     * lanes, park the character there and advance the clock by one full
     * `chart.loopDurationMs`.
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

    /**
     * The 6 boat items' weights are randomly assigned 1-99 fresh every
     * playthrough, so there's no fixed drag sequence to hardcode. Each
     * item's name is fixed and known, though, so its current weight can be
     * read the same way Jericho's camp-stone solver reads stone values:
     * scan 1-99 for the label that currently exists in the tray. Once every
     * item's weight is known, drag them onto the boat heaviest first.
     */
    private fun completeLoadingTheBoat() {
        val activity = composeTestRule.activity
        val dropZoneDescription = activity.getString(R.string.jesus_calms_storm_loading_dropzone_content_description)
        val itemIds = JesusCalmsStormContent.boatItemIds
        val itemNames = itemIds.associateWith { activity.getString(boatItemNameRes(it)) }
        val itemWeights = itemIds.associateWith { itemId ->
            (1..99).first { weight ->
                val label = activity.getString(R.string.jesus_calms_storm_loading_item_content_description, itemNames.getValue(itemId), weight)
                composeTestRule.onAllNodesWithContentDescription(label).fetchSemanticsNodes().isNotEmpty()
            }
        }
        val requiredOrder = itemIds.sortedByDescending { itemWeights.getValue(it) }

        requiredOrder.forEach { itemId ->
            val label = activity.getString(
                R.string.jesus_calms_storm_loading_item_content_description,
                itemNames.getValue(itemId),
                itemWeights.getValue(itemId),
            )
            dragOntoContentDescription(
                itemNode = composeTestRule.onNodeWithContentDescription(label),
                targetContentDescription = dropZoneDescription,
            )
            composeTestRule.waitForIdle()
        }
    }

    private fun boatItemNameRes(itemId: String): Int = when (itemId) {
        "anchor" -> R.string.jesus_calms_storm_loading_item_anchor
        "water_jars" -> R.string.jesus_calms_storm_loading_item_water_jars
        "fishing_nets" -> R.string.jesus_calms_storm_loading_item_fishing_nets
        "food_basket" -> R.string.jesus_calms_storm_loading_item_food_basket
        "oars" -> R.string.jesus_calms_storm_loading_item_oars
        "cushion" -> R.string.jesus_calms_storm_loading_item_cushion
        else -> error("Unknown boat item id: $itemId")
    }

    /**
     * Bailing the Boat has a single disciple that must be steered into a
     * wave's lane to bail it, auto-judged every frame — same shape and same
     * sweep-by-full-loop-duration technique as Gathering the Leftovers
     * (`completeCatching`).
     */
    private fun completeBailingTheBoat() {
        val chart = JesusCalmsStormContent.bailingChart
        val requiredHits = JesusCalmsStormContent.BAILING_REQUIRED_HITS
        val lanes = chart.notes.map { it.lane }.distinct().sorted()

        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.jesus_calms_storm_bailing_title)).assertExists()

        composeTestRule.mainClock.autoAdvance = false
        var safetyRounds = 0
        while (currentBailingHits(requiredHits) < requiredHits) {
            check(safetyRounds++ < 20) { "Bailing the Boat didn't reach $requiredHits hits after 20 full sweep rounds — stuck at ${currentBailingHits(requiredHits)}" }
            lanes.forEach { lane ->
                if (currentBailingHits(requiredHits) < requiredHits) {
                    moveBailingCharacterTo(lane)
                    composeTestRule.mainClock.advanceTimeBy(chart.loopDurationMs)
                }
            }
        }
        composeTestRule.mainClock.autoAdvance = true
    }

    private fun currentBailingHits(requiredHits: Int): Int {
        val activity = composeTestRule.activity
        return (0..requiredHits).first { candidateHits ->
            val label = activity.getString(R.string.jesus_calms_storm_bailing_progress_label, candidateHits, requiredHits)
            composeTestRule.onAllNodesWithText(label).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun currentBailingLane(): Int {
        val activity = composeTestRule.activity
        return (1..3).first { candidateLane ->
            val label = activity.getString(R.string.jesus_calms_storm_bailing_character_content_description, candidateLane)
            composeTestRule.onAllNodesWithContentDescription(label).fetchSemanticsNodes().isNotEmpty()
        } - 1
    }

    private fun moveBailingCharacterTo(targetLane: Int) {
        val activity = composeTestRule.activity
        val moveLeftLabel = activity.getString(R.string.jesus_calms_storm_bailing_move_left_content_description)
        val moveRightLabel = activity.getString(R.string.jesus_calms_storm_bailing_move_right_content_description)

        while (currentBailingLane() != targetLane) {
            val label = if (currentBailingLane() < targetLane) moveRightLabel else moveLeftLabel
            composeTestRule.onNodeWithContentDescription(label).performClick()
        }
    }

    /**
     * Peace, Be Still's 3 word lanes are static, always-tappable buttons
     * (not a steered object), so the exact-timestamp frozen-clock technique
     * (same as `completeMarch`/`completeCorridorRhythmLane`) is reliable
     * here — advancing to each note's precise `hitTimeMs` and tapping its
     * word in turn.
     */
    private fun completePeaceBeStill() {
        val activity = composeTestRule.activity
        val wordLabels = listOf(
            activity.getString(R.string.jesus_calms_storm_peace_be_still_word_peace),
            activity.getString(R.string.jesus_calms_storm_peace_be_still_word_be),
            activity.getString(R.string.jesus_calms_storm_peace_be_still_word_still),
        )
        val chart = JesusCalmsStormContent.peaceBeStillChart
        val requiredHits = JesusCalmsStormContent.PEACE_BE_STILL_REQUIRED_HITS

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
                    composeTestRule.onNodeWithContentDescription(wordLabels[note.lane]).performClick()
                    hits++
                }
            }
            loopIndex++
        }
        composeTestRule.mainClock.autoAdvance = true
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
