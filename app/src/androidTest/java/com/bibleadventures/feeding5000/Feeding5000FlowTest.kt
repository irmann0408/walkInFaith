package com.bibleadventures.feeding5000

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
import com.bibleadventures.game.puzzles.gridmaze.Direction
import com.bibleadventures.game.puzzles.rhythmlane.RhythmLaneChart
import com.bibleadventures.game.puzzles.slidingpuzzle.SlidingPuzzleGame
import com.bibleadventures.game.puzzles.slidingpuzzle.SlidingPuzzleGameState
import com.bibleadventures.game.puzzles.slingshot.SlingshotGameState
import com.bibleadventures.game.stories.DanielContent
import com.bibleadventures.game.stories.DavidGoliathContent
import com.bibleadventures.game.stories.EstherContent
import com.bibleadventures.game.stories.Feeding5000Content
import com.bibleadventures.game.stories.GoodSamaritanContent
import com.bibleadventures.game.stories.JerichoContent
import com.bibleadventures.game.stories.NoahsArkContent
import org.junit.Rule
import org.junit.Test

// ic_goliath_shield.xml's silhouette is narrower than its own bounding box —
// its visible top edge (where the mark's line sits, and what the hit-test
// actually checks) spans x=12..52 of a 64-wide viewport, mirroring
// DavidGoliathSlingPracticeScreen.kt's own SHIELD_TOP_EDGE_*_RATIO constants.
private const val SLING_SHIELD_TOP_EDGE_LEFT_RATIO = 12f / 64f
private const val SLING_SHIELD_TOP_EDGE_RIGHT_RATIO = 52f / 64f

/**
 * Walks the full Feeding the 5,000 adventure end to end — 6 real
 * mini-puzzles (gathering the crowd into exact-sum seating circles,
 * searching for the boy, finding his loaves and fish among decoys, the
 * miracle multiplication word problems, and a two-phase serve/catch
 * finale), replacing an external blueprint's weaker "tap to multiply" and
 * "patience timer" mechanics. It's locked until Noah's Ark, David and
 * Goliath, Good Samaritan, Daniel, Esther, and Jericho are completed — and
 * this device's save data persists real state across test runs — so this
 * test completes all six prerequisites itself rather than assuming they're
 * already done, to stay deterministic regardless of what ran before it
 * (same pattern as JerichoFlowTest/EstherFlowTest).
 *
 * Jericho is one chapter (4 mini-puzzles) — see JerichoFlowTest for the
 * thorough walkthrough asserting its own reward details; this test only
 * needs to clear it as a prerequisite.
 */
class Feeding5000FlowTest {

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
    fun completingFeeding5000_awardsStarsAndShowsGenerousHeartBadge() {
        val activity = composeTestRule.activity
        val continueLabel = activity.getString(R.string.action_continue)

        composeTestRule.onNodeWithText(activity.getString(R.string.menu_adventures)).performClick()
        completeNoahsArk(continueLabel)
        completeDavidGoliath(continueLabel)
        completeGoodSamaritan(continueLabel)
        completeDaniel(continueLabel)
        completeEsther(continueLabel)
        completeJericho(continueLabel)

        // World Map -> Feeding the 5,000 (now unlocked).
        scrollToChapterOnWorldMap(activity.getString(R.string.chapter_feeding_5000_title))
        composeTestRule.onNodeWithText(activity.getString(R.string.chapter_feeding_5000_title)).performClick()

        // Scene 1: Intro.
        composeTestRule.onNodeWithText(continueLabel).performClick()

        // Scene 1b: So Many People context.
        composeTestRule.onNodeWithText(continueLabel).performClick()

        // Scene 2: Gathering the Crowd — a groupfill drag puzzle, randomly
        // generated each run (families pooled and shuffled from a random
        // partition of each circle's target), solved live by reading the
        // headcounts and running sums currently on screen and running a real
        // exact-bin-fill backtracking search over them (same "read live
        // state, run a real solver" discipline as solveSpiesEscapePuzzle's
        // BFS).
        completeGatheringCrowd()
        composeTestRule.onNodeWithText(continueLabel).performClick()

        // Scene 2b: Not Enough context.
        composeTestRule.onNodeWithText(continueLabel).performClick()

        // Scene 3: Searching for Food — a single hidden-object target.
        composeTestRule.onNodeWithContentDescription(activity.getString(R.string.feeding_5000_searching_for_food_boy_content_description)).performClick()
        composeTestRule.onNodeWithText(continueLabel).performClick()

        // Scene 3b: A Boy's Lunch context.
        composeTestRule.onNodeWithText(continueLabel).performClick()

        // Scene 4: The Boy's Gift — find exactly 5 loaves and 2 fish among
        // decoys (decoys have no content description and are never wired to
        // a click handler, so they can never be tapped by this test).
        completeBoysGift()
        composeTestRule.onNodeWithText(continueLabel).performClick()

        // Scene 5: Choice — flavor-only, matching every other chapter's
        // Choice convention.
        composeTestRule.onNodeWithText(activity.getString(R.string.feeding_5000_choice_option_1)).performClick()
        composeTestRule.onNodeWithText(continueLabel).performClick()

        // Scene 5b: Jesus Gives Thanks context.
        composeTestRule.onNodeWithText(continueLabel).performClick()

        // Scene 6: The Miracle Multiplication — 5 random multiplication
        // problems; a wrong guess is free (no failure state), so just try
        // each of the 3 positionally-tagged choices until the note count
        // advances, same discipline as completeBlowShofar.
        completeMiracleMultiplication()
        composeTestRule.onNodeWithText(continueLabel).performClick()

        // Scene 6b: Enough For Everyone context.
        composeTestRule.onNodeWithText(continueLabel).performClick()

        // Scene 7: Serving the Crowd — gridmaze, finale phase A. Walk out to
        // all 7 groups (any order), replacing the old catch-the-falling-
        // bread version which read as receiving food, not giving it away.
        completeServing()

        // Scene 8: Gathering the Leftovers — rhythmlane, finale phase B,
        // chained straight in with no narrative break. A single basket now
        // slides between the 3 lanes via left/right buttons rather than
        // 3 independently-tappable lanes — an ordinary, un-frozen Continue
        // tap navigates there (freezing the clock around a *navigating*
        // click turned out unreliable — see completeCatching's KDoc for why
        // it's unnecessary anyway).
        composeTestRule.onNodeWithText(continueLabel).performClick()
        composeTestRule.onNodeWithText(activity.getString(R.string.feeding_5000_catching_title)).assertExists()
        completeCatching()
        composeTestRule.onNodeWithText(continueLabel).performClick()

        // Scene 9: Lesson.
        composeTestRule.onNodeWithText(activity.getString(R.string.feeding_5000_lesson_title)).assertExists()
        composeTestRule.onNodeWithText(continueLabel).performClick()

        // Scene 10: Reward.
        composeTestRule.onNodeWithText(activity.getString(R.string.reward_title)).assertExists()
        composeTestRule.onNodeWithText(activity.getString(R.string.badge_generous_heart_title)).assertExists()
        composeTestRule.onNodeWithText(activity.getString(R.string.action_return_to_map)).performClick()

        // Back on the World Map: Feeding the 5,000 completed.
        composeTestRule.onNodeWithText(activity.getString(R.string.world_map_title)).assertExists()
    }

    /** Walks Noah's Ark end to end (mirrors NoahsArkFlowTest) so David & Goliath unlocks. */
    private fun completeNoahsArk(continueLabel: String) {
        val activity = composeTestRule.activity

        scrollToChapterOnWorldMap(activity.getString(R.string.chapter_noahs_ark_title))
        composeTestRule.onNodeWithText(activity.getString(R.string.chapter_noahs_ark_title)).performClick()

        composeTestRule.onNodeWithText(continueLabel).performClick() // Intro
        composeTestRule.onNodeWithText(continueLabel).performClick() // Find Animals context

        NoahsArkContent.animals.forEach { animal ->
            composeTestRule.onAllNodesWithContentDescription(activity.getString(animal.nameRes))[0].performClick()
        }
        composeTestRule.onNodeWithText(continueLabel).performClick()

        NoahsArkContent.animals.forEach { animal ->
            val name = activity.getString(animal.nameRes)
            composeTestRule.onAllNodesWithContentDescription(name)[0].performClick()
            composeTestRule.onAllNodesWithContentDescription(name)[1].performClick()
        }
        composeTestRule.onNodeWithText(continueLabel).performClick()

        composeTestRule.onNodeWithText(continueLabel).performClick() // Organize the Ark context

        NoahsArkContent.sortableItems.filter { it.categoryKey != null }.forEach { item ->
            val itemName = activity.getString(item.nameRes)
            val categoryLabelRes = NoahsArkContent.sortCategories.first { it.key == item.categoryKey }.labelRes
            val categoryLabel = activity.getString(categoryLabelRes)
            dragOntoText(itemNode = composeTestRule.onNodeWithContentDescription(itemName), targetText = categoryLabel)
        }
        composeTestRule.onNodeWithText(continueLabel).performClick()

        NoahsArkContent.hiddenItems.forEach { item ->
            composeTestRule.onNodeWithContentDescription(activity.getString(item.nameRes)).performClick()
        }
        composeTestRule.onNodeWithText(continueLabel).performClick()

        composeTestRule.onNodeWithText(continueLabel).performClick() // Lesson

        composeTestRule.onNodeWithText(activity.getString(R.string.action_return_to_map)).performClick()
    }

    /** Walks David and Goliath end to end (mirrors DavidGoliathFlowTest) so Good Samaritan unlocks. */
    private fun completeDavidGoliath(continueLabel: String) {
        val activity = composeTestRule.activity

        scrollToChapterOnWorldMap(activity.getString(R.string.chapter_david_goliath_title))
        composeTestRule.onNodeWithText(activity.getString(R.string.chapter_david_goliath_title)).performClick()

        composeTestRule.onNodeWithText(continueLabel).performClick() // Intro
        composeTestRule.onNodeWithText(continueLabel).performClick() // Counting the Flock context

        DavidGoliathContent.sheepCounts.forEach { count ->
            val name = activity.getString(count.nameRes)
            composeTestRule.onAllNodesWithContentDescription(name)[0].performClick()
            composeTestRule.onAllNodesWithContentDescription(name)[1].performClick()
        }
        composeTestRule.onNodeWithText(continueLabel).performClick()

        composeTestRule.onNodeWithText(continueLabel).performClick() // Choose the Stones context

        DavidGoliathContent.stones.forEach { stone ->
            composeTestRule.onNodeWithContentDescription(activity.getString(stone.nameRes)).performClick()
        }
        composeTestRule.onNodeWithText(continueLabel).performClick()

        composeTestRule.onNodeWithText(continueLabel).performClick() // Sling Practice context

        composeTestRule.onNodeWithText(activity.getString(R.string.david_goliath_choice_option_1)).performClick()
        composeTestRule.onNodeWithText(continueLabel).performClick()

        composeTestRule.onNodeWithText(continueLabel).performClick() // Crossing the Valley context

        completeLaneAvoid(
            chart = DavidGoliathContent.crossingValleyChart,
            requiredAvoids = DavidGoliathContent.CROSSING_VALLEY_REQUIRED_AVOIDS,
            titleRes = R.string.david_goliath_dodge_title,
            progressLabelRes = R.string.david_goliath_dodge_progress_label,
            characterContentDescriptionRes = R.string.david_goliath_dodge_character_content_description,
            moveLeftLabelRes = R.string.david_goliath_dodge_move_left_content_description,
            moveRightLabelRes = R.string.david_goliath_dodge_move_right_content_description,
        )
        composeTestRule.onNodeWithText(continueLabel).performClick()

        completeSlingPractice()

        composeTestRule.onNodeWithText(activity.getString(R.string.feedback_great_job)).assertExists()
        composeTestRule.onNodeWithText(continueLabel).performClick()

        composeTestRule.onNodeWithText(activity.getString(R.string.david_goliath_lesson_title)).assertExists()
        composeTestRule.onNodeWithText(continueLabel).performClick()

        composeTestRule.onNodeWithText(activity.getString(R.string.reward_title)).assertExists()
        composeTestRule.onNodeWithText(activity.getString(R.string.action_return_to_map)).performClick()
    }

    /** Walks Good Samaritan end to end (mirrors GoodSamaritanFlowTest) so Daniel unlocks. */
    private fun completeGoodSamaritan(continueLabel: String) {
        val activity = composeTestRule.activity

        scrollToChapterOnWorldMap(activity.getString(R.string.chapter_good_samaritan_title))
        composeTestRule.onNodeWithText(activity.getString(R.string.chapter_good_samaritan_title)).performClick()

        composeTestRule.onNodeWithText(continueLabel).performClick() // Intro
        composeTestRule.onNodeWithText(continueLabel).performClick() // The Road to Jericho context

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
                composeTestRule.onNodeWithText(continueLabel).performClick()
            }
        }

        composeTestRule.onNodeWithText(continueLabel).performClick()

        composeTestRule.onNodeWithText(activity.getString(R.string.good_samaritan_lesson_title)).assertExists()
        composeTestRule.onNodeWithText(continueLabel).performClick()

        composeTestRule.onNodeWithText(activity.getString(R.string.reward_title)).assertExists()
        composeTestRule.onNodeWithText(activity.getString(R.string.action_return_to_map)).performClick()
    }

    /** Walks Daniel and the Lions end to end (mirrors DanielFlowTest) so Esther unlocks. */
    private fun completeDaniel(continueLabel: String) {
        val activity = composeTestRule.activity

        scrollToChapterOnWorldMap(activity.getString(R.string.chapter_daniel_title))
        composeTestRule.onNodeWithText(activity.getString(R.string.chapter_daniel_title)).performClick()

        composeTestRule.onNodeWithText(continueLabel).performClick() // Intro
        composeTestRule.onNodeWithText(continueLabel).performClick() // Hurrying to Pray context

        completeLaneAvoid(
            chart = DanielContent.hurryToPrayChart,
            requiredAvoids = DanielContent.HURRY_TO_PRAY_REQUIRED_AVOIDS,
            titleRes = R.string.daniel_stealth_title,
            progressLabelRes = R.string.daniel_stealth_progress_label,
            characterContentDescriptionRes = R.string.daniel_stealth_character_content_description,
            moveLeftLabelRes = R.string.daniel_stealth_move_left_content_description,
            moveRightLabelRes = R.string.daniel_stealth_move_right_content_description,
        )
        composeTestRule.onNodeWithText(continueLabel).performClick()

        composeTestRule.onNodeWithText(activity.getString(R.string.daniel_choice_option_1)).performClick()
        composeTestRule.onNodeWithText(continueLabel).performClick()

        composeTestRule.onNodeWithText(continueLabel).performClick() // Into the Lions' Den context

        // The Angel's Shield — 5 random math problems. Two wrong answers in a
        // row now replace the problem instead of leaving the last choice a
        // guaranteed-correct guess, so compute the real answer instead of
        // trying all 3 choices blind.
        repeat(DanielContent.LIONS_DEN_PROBLEM_COUNT) {
            solveLionsDenProblem()
        }
        composeTestRule.onNodeWithText(continueLabel).performClick()

        composeTestRule.onNodeWithText(continueLabel).performClick() // Darius's Long Night context

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
        composeTestRule.onNodeWithText(continueLabel).performClick()

        composeTestRule.onNodeWithText(activity.getString(R.string.daniel_lesson_title)).assertExists()
        composeTestRule.onNodeWithText(continueLabel).performClick()

        composeTestRule.onNodeWithText(activity.getString(R.string.reward_title)).assertExists()
        composeTestRule.onNodeWithText(activity.getString(R.string.action_return_to_map)).performClick()
    }

    /**
     * Walks the merged Esther's Rescue of Her People chapter end to end so
     * Jericho unlocks. See EstherFlowTest for the thorough walkthrough that
     * also asserts the chapter's own reward details (one badge, all 5
     * scripture cards); this only needs to clear it as a prerequisite.
     */
    private fun completeEsther(continueLabel: String) {
        val activity = composeTestRule.activity

        scrollToChapterOnWorldMap(activity.getString(R.string.chapter_esther_title))
        composeTestRule.onNodeWithText(activity.getString(R.string.chapter_esther_title)).performClick()

        composeTestRule.onNodeWithText(continueLabel).performClick() // Intro
        composeTestRule.onNodeWithText(continueLabel).performClick() // Chosen for the Palace context

        EstherContent.royalAttireItems.forEach { item ->
            composeTestRule.onNodeWithContentDescription(activity.getString(item.nameRes)).performClick()
        }
        composeTestRule.onNodeWithText(continueLabel).performClick()

        composeTestRule.onNodeWithText(continueLabel).performClick() // Esther Becomes Queen context

        composeTestRule.onNodeWithText(activity.getString(R.string.esther_new_queen_choice_option_1)).performClick()
        composeTestRule.onNodeWithText(continueLabel).performClick()

        composeTestRule.onNodeWithText(continueLabel).performClick() // A Dangerous Secret context

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
        composeTestRule.onNodeWithText(continueLabel).performClick()

        composeTestRule.onNodeWithText(continueLabel).performClick() // The King is Warned context
        composeTestRule.onNodeWithText(continueLabel).performClick() // A Wicked Law context

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
        composeTestRule.onNodeWithText(continueLabel).performClick()

        composeTestRule.onNodeWithText(continueLabel).performClick() // The City Mourns and Fasts context

        composeTestRule.onNodeWithText(activity.getString(R.string.esther_brave_approach_choice_option_1)).performClick()
        composeTestRule.onNodeWithText(continueLabel).performClick()

        composeTestRule.onNodeWithText(continueLabel).performClick() // Three Days of Fasting context

        completeCorridorRhythmLane()
        composeTestRule.onNodeWithText(continueLabel).performClick()

        composeTestRule.onNodeWithText(activity.getString(R.string.esther_brave_approach_lesson_title)).assertExists()
        composeTestRule.onNodeWithText(continueLabel).performClick()

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
    private fun completeJericho(continueLabel: String) {
        val activity = composeTestRule.activity

        scrollToChapterOnWorldMap(activity.getString(R.string.chapter_jericho_title))
        composeTestRule.onNodeWithText(activity.getString(R.string.chapter_jericho_title)).performClick()

        composeTestRule.onNodeWithText(continueLabel).performClick() // Intro
        composeTestRule.onNodeWithText(continueLabel).performClick() // Rahab's House context
        composeTestRule.onNodeWithText(continueLabel).performClick() // Rahab Helps the Spies (narrative-only)

        solveSpiesEscapePuzzle()
        composeTestRule.onNodeWithText(continueLabel).performClick() // leaves the puzzle screen itself

        composeTestRule.onNodeWithText(continueLabel).performClick() // Over the Wall context

        composeTestRule.onNodeWithText(activity.getString(R.string.jericho_choice_option_1)).performClick()
        composeTestRule.onNodeWithText(continueLabel).performClick()

        composeTestRule.onNodeWithText(continueLabel).performClick() // Crossing the Jordan context

        completeSettingUpCamp()
        composeTestRule.onNodeWithText(continueLabel).performClick()

        composeTestRule.onNodeWithText(continueLabel).performClick() // Camp by the River context
        composeTestRule.onNodeWithText(continueLabel).performClick() // The Walls of Jericho context

        completeMarch(JerichoContent.sixDayMarchChart, JerichoContent.SIX_DAY_MARCH_REQUIRED_HITS, R.string.jericho_six_day_march_lane_content_description)
        composeTestRule.onNodeWithText(continueLabel).performClick()

        composeTestRule.onNodeWithText(continueLabel).performClick() // The Seventh Day context

        completeMarch(JerichoContent.fastMarchChart, JerichoContent.FAST_MARCH_REQUIRED_HITS, R.string.jericho_fast_march_lane_content_description)
        composeTestRule.onNodeWithText(continueLabel).performClick()

        completeBlowShofar()
        composeTestRule.onNodeWithText(continueLabel).performClick()

        val shoutDescription = activity.getString(R.string.jericho_shout_button_content_description)
        repeat(JerichoContent.SHOUT_REQUIRED_TAPS) {
            composeTestRule.onNodeWithContentDescription(shoutDescription).performClick()
        }
        composeTestRule.onNodeWithText(continueLabel).performClick()

        composeTestRule.onNodeWithText(continueLabel).performClick() // Rahab is Saved context

        composeTestRule.onNodeWithText(activity.getString(R.string.jericho_lesson_title)).assertExists()
        composeTestRule.onNodeWithText(continueLabel).performClick()

        composeTestRule.onNodeWithText(activity.getString(R.string.reward_title)).assertExists()
        composeTestRule.onNodeWithText(activity.getString(R.string.action_return_to_map)).performClick()
    }

    /**
     * Family headcounts and each circle's exact target sum are randomly
     * generated every run (Feeding5000ViewModel.newGroupFillFamilies pools
     * and shuffles a random partition of each circle's target), so there's
     * no fixed drag sequence to hardcode. Instead: read the full remaining
     * headcount multiset and each circle's remaining capacity straight off
     * the screen (cheap semantics-tree queries, not gestures — same
     * scanning idiom as completeSettingUpCamp), solve a real exact-bin-fill
     * assignment with backtracking, then execute it by dragging one tile of
     * each assigned headcount into its assigned circle. Since a family tile
     * stays present in the tray only until it's actually placed, re-querying
     * "the first remaining tile with this headcount" after each drag is
     * always safe — same "may be duplicate description" indexing precedent
     * as completeNoahsArk's repeated-animal-name handling.
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

    /**
     * Exact bin-fill backtracking: assigns each of [headcounts] to a circle
     * index (into [targets]) so every circle's assigned values sum exactly
     * to its target. The puzzle is solvable by construction — each circle's
     * target was itself partitioned into the pooled family headcounts in
     * the first place — so a valid assignment always exists. Sorting
     * [headcounts] descending before calling this (larger items placed
     * first) keeps the search fast; with at most ~15 items this finishes
     * instantly regardless.
     */
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

    /**
     * The Boy's Gift items keep their content-description node in the tree
     * after being found (just disabled, same as every other hiddenobject
     * screen), so — like completeNoahsArk's repeated-animal-name items —
     * each of the 5 loaves and 2 fish must be addressed by a fixed index
     * over all current matches rather than always re-querying index 0.
     */
    private fun completeBoysGift() {
        val activity = composeTestRule.activity
        val loafLabel = activity.getString(R.string.feeding_5000_boys_gift_loaf_content_description)
        val fishLabel = activity.getString(R.string.feeding_5000_boys_gift_fish_content_description)

        repeat(5) { index -> composeTestRule.onAllNodesWithContentDescription(loafLabel)[index].performClick() }
        repeat(2) { index -> composeTestRule.onAllNodesWithContentDescription(fishLabel)[index].performClick() }
    }

    /**
     * Reads the displayed "%d × %d = ?" problem, computes the real answer,
     * and taps the matching choice by its content description. Two wrong
     * answers in a row now replace the problem instead of leaving the last
     * choice a guaranteed-correct guess (see
     * `DecisionPathGame.WRONG_ATTEMPTS_BEFORE_NEW_STEP`), so the old "try
     * each of the 3 choices" trick no longer reliably solves it.
     */
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
     * Walks `Feeding5000Content.servingSolutionPath` (a hand-verified BFS
     * route visiting all 7 groups, any order works since completion only
     * needs every collectible gathered) — same D-pad-solution-path-replay
     * technique as `GoodSamaritanFlowTest`'s Explore scene and
     * `DanielFlowTest`'s Darius maze, simpler here since there's no
     * checkpoint overlay to dismiss mid-walk.
     */
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

    /**
     * Freezes the Compose test clock and advances it to each of [chart]'s
     * authored notes' exact `hitTimeMs` in turn, tapping that note's lane —
     * same deterministic frozen-clock technique as Esther's corridor
     * (`completeCorridorRhythmLane`), reused here for Jericho's two march
     * phases, all of which share the same 3-independently-tappable-lanes
     * layout. Feeding the 5,000's Serving the Crowd moved off this shape
     * entirely (now a `gridmaze` walk, see [completeServing]); Gathering
     * the Leftovers is a different shape too — one basket, moved into
     * position rather than tapped — so it gets its own helper,
     * [completeCatching]. Loops through the chart as many times as needed
     * to reach [requiredHits].
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
     * Gathering the Leftovers has a single basket that must be steered into
     * a note's lane to catch it — the catch is judged automatically every
     * frame purely from the basket's current position (see
     * `Feeding5000ViewModel.onCatchingTimeAdvanced`), there's no "tap this
     * lane" action left to perform, unlike every other `rhythmlane` scene in
     * this app. That auto-judge design turns out to make exact-timestamp
     * scheduling (the technique every other frozen-clock helper in this
     * file uses) unreliable here: Compose's implicit idle-sync — which runs
     * as an ordinary part of `performClick()`, even under
     * `mainClock.autoAdvance = false` — pumps this screen's infinite
     * `withFrameNanos` loop forward by an unpredictable amount before test
     * code regains control, so the *actual* starting elapsedMs is never
     * knowable, and a schedule computed from an assumed 0 lands on the
     * wrong moments.
     *
     * Sidesteps that entirely: freeze the clock as the very first thing this
     * function does (once already safely on this screen via an ordinary,
     * un-frozen navigating click — freezing *around* that click instead
     * turned out unreliable, sometimes leaving the click unable to find its
     * target at all), then for each of the 3 lanes, park the basket there
     * and advance the clock by one full `chart.loopDurationMs` — since every
     * note recurs exactly once per loop, a full-loop dwell in a lane is
     * guaranteed to pass through (and catch) every note assigned to that
     * lane exactly once, regardless of *where* in the loop the clock
     * actually started. Progress is read live off the progress-label text
     * after every sweep (not assumed), so it's also robust to however many
     * "free" catches already happened before this function got control.
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
     * `RhythmLaneGame.onLaneAvoided`) — same shape as Gathering the
     * Leftovers' catch mechanic, so it inherits the same implicit-idle-sync
     * unpredictability [completeCatching] documents. Sidesteps it the same
     * way: freeze the clock, then for each of the 3 lanes, park the
     * character there and advance the clock by one full
     * `chart.loopDurationMs` — since every note recurs exactly once per
     * loop, a full-loop dwell in a lane is guaranteed to pass through (and
     * avoid) every note assigned to that lane exactly once, regardless of
     * where in the loop the clock actually started. Progress is read live
     * off the progress-label text after every sweep.
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
     * Sling Practice's target mark animates continuously with no
     * time-based stopping condition reachable on its own (unlike Crossing
     * the Valley/Hurrying to Pray, which self-complete given enough elapsed
     * time even with zero player input) — so, unlike [completeLaneAvoid] or
     * [completeCatching], querying semantics while the clock auto-advances
     * can never reach idle here; the mark's `LaunchedEffect` has nothing
     * that would ever let it stop on its own. Freezes the clock as the
     * very first thing this function does (once already safely on this
     * screen via an ordinary, un-frozen navigating click), then drives the
     * mark forward in small deterministic steps via `advanceTimeBy` —
     * reading the mark's *actual* rendered position after each step
     * (derived from the shield image's own rendered bounds via the same
     * top-edge ratios `DavidGoliathSlingPracticeScreen.kt` uses to pick its
     * hit test's true perimeter, not the image's wider, partly transparent
     * bounding box) and dragging the stone onto it the moment it's within
     * the shield's true span. Repeats until
     * [SlingshotGameState.requiredHits] real hits land (a miss never loses
     * progress, per SlingshotGame's own design, and the shield relocates
     * after every hit, so re-reading it live on each step is required, not
     * just once).
     */
    private fun completeSlingPractice() {
        val activity = composeTestRule.activity
        val markDescription = activity.getString(R.string.david_goliath_sling_target_mark_content_description)
        val stoneDescription = activity.getString(R.string.david_goliath_sling_stone_content_description)
        val shieldDescriptionPrefix = activity.getString(R.string.david_goliath_sling_shield_content_description, "")
        val requiredHits = SlingshotGameState().requiredHits

        composeTestRule.mainClock.autoAdvance = false
        // One explicit frame to let this screen's first composition (and
        // its progress label) land before any query — freezing the clock
        // doesn't itself wait for anything to compose.
        composeTestRule.mainClock.advanceTimeByFrame()

        var safetySteps = 0
        while (currentSlingHits(requiredHits) < requiredHits) {
            check(safetySteps++ < 1500) { "Sling Practice didn't reach $requiredHits hits after 1500 clock steps — stuck at ${currentSlingHits(requiredHits)}" }

            val markBounds = composeTestRule.onNodeWithContentDescription(markDescription).fetchSemanticsNode().boundsInRoot
            val shieldImageBounds = composeTestRule.onNodeWithContentDescription(shieldDescriptionPrefix, substring = true).fetchSemanticsNode().boundsInRoot
            val shieldTrueLeft = shieldImageBounds.left + SLING_SHIELD_TOP_EDGE_LEFT_RATIO * shieldImageBounds.width
            val shieldTrueRight = shieldImageBounds.left + SLING_SHIELD_TOP_EDGE_RIGHT_RATIO * shieldImageBounds.width

            if (markBounds.center.x in shieldTrueLeft..shieldTrueRight) {
                val stoneNode = composeTestRule.onNodeWithContentDescription(stoneDescription)
                dragOntoContentDescription(itemNode = stoneNode, targetContentDescription = markDescription)
            } else {
                composeTestRule.mainClock.advanceTimeBy(50L)
            }
        }

        composeTestRule.mainClock.autoAdvance = true
    }

    private fun currentSlingHits(requiredHits: Int): Int {
        val activity = composeTestRule.activity
        return (0..requiredHits).first { candidateHits ->
            val label = activity.getString(R.string.david_goliath_sling_practice_progress_label, candidateHits, requiredHits)
            composeTestRule.onAllNodesWithText(label).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun dragOntoText(itemNode: SemanticsNodeInteraction, targetText: String) {
        val itemBounds = itemNode.fetchSemanticsNode().boundsInRoot
        val targetBounds = composeTestRule.onNodeWithText(targetText).fetchSemanticsNode().boundsInRoot
        val targetGlobalCenter = targetBounds.center
        val localEnd = Offset(targetGlobalCenter.x - itemBounds.left, targetGlobalCenter.y - itemBounds.top)

        itemNode.performTouchInput {
            swipe(start = center, end = localEnd, durationMillis = 200)
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
