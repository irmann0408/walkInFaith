package com.bibleadventures.esther

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
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
import com.bibleadventures.game.puzzles.dodge.DodgeLane
import com.bibleadventures.game.puzzles.gridmaze.Direction
import com.bibleadventures.game.stories.DanielContent
import com.bibleadventures.game.stories.DavidGoliathContent
import com.bibleadventures.game.stories.EstherContent
import com.bibleadventures.game.stories.GoodSamaritanContent
import com.bibleadventures.game.stories.NoahsArkContent
import org.junit.Rule
import org.junit.Test

/**
 * Walks the merged Esther's Rescue of Her People chapter end to end — one
 * chapter, 5 sequential mini-puzzles (Royal Attire, Courtyard Stealth,
 * Messenger Sudoku, Corridor Courage Meter, Reveal Haman's Plot). Replaces
 * the old EstherArcFlowTest, which walked this content as 5 separate
 * chapters before they were consolidated back into one per playtesting
 * feedback (splitting them felt disjointed, and the banquet jigsaw
 * mini-game — a repeat of `dragsort`, the same engine Organize the Ark
 * uses — was dropped for being both redundant and too easy).
 */
class EstherFlowTest {

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
    fun completingEsther_awardsOneBadgeAndAllFiveScriptureCardsAndUnlocksJericho() {
        val activity = composeTestRule.activity
        val continueLabel = activity.getString(R.string.action_continue)

        composeTestRule.onNodeWithText(activity.getString(R.string.menu_adventures)).performClick()
        completeNoahsArk(continueLabel)
        completeDavidGoliath(continueLabel)
        completeGoodSamaritan(continueLabel)
        completeDaniel(continueLabel)

        scrollToChapterOnWorldMap(activity.getString(R.string.chapter_esther_title))
        composeTestRule.onNodeWithText(activity.getString(R.string.chapter_esther_title)).performClick()

        // Intro -> Chosen for the Palace context.
        composeTestRule.onNodeWithText(continueLabel).performClick()
        composeTestRule.onNodeWithText(continueLabel).performClick()

        // Royal Attire (hidden object).
        EstherContent.royalAttireItems.forEach { item ->
            composeTestRule.onNodeWithContentDescription(activity.getString(item.nameRes)).performClick()
        }
        composeTestRule.onNodeWithText(continueLabel).performClick()

        // Esther Becomes Queen context -> Greeting choice.
        composeTestRule.onNodeWithText(continueLabel).performClick()
        composeTestRule.onNodeWithText(activity.getString(R.string.esther_new_queen_choice_option_1)).performClick()
        composeTestRule.onNodeWithText(continueLabel).performClick()

        // A Dangerous Secret context.
        composeTestRule.onNodeWithText(continueLabel).performClick()

        // Courtyard Stealth.
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

        // The King is Warned context -> A Wicked Law context.
        composeTestRule.onNodeWithText(continueLabel).performClick()
        composeTestRule.onNodeWithText(continueLabel).performClick()

        // Messenger Sudoku.
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

        // The City Mourns and Fasts context -> decision choice.
        composeTestRule.onNodeWithText(continueLabel).performClick()
        composeTestRule.onNodeWithText(activity.getString(R.string.esther_brave_approach_choice_option_1)).performClick()
        composeTestRule.onNodeWithText(continueLabel).performClick()

        // Three Days of Fasting context.
        composeTestRule.onNodeWithText(continueLabel).performClick()

        // Corridor Courage Meter — a 3-lane rhythm mini-game. Freeze the clock and
        // advance to each authored note's exact time so every tap lands; a
        // mistimed tap would just be a no-op (see RhythmLaneGame's no-failure design).
        completeCorridorRhythmLane()
        composeTestRule.onNodeWithText(continueLabel).performClick()

        // The Golden Scepter context -> Preparing the Banquet context (narrative
        // only now, no jigsaw puzzle) -> The Second Banquet context.
        composeTestRule.onNodeWithText(continueLabel).performClick()
        composeTestRule.onNodeWithText(continueLabel).performClick()
        composeTestRule.onNodeWithText(continueLabel).performClick()

        // Reveal Haman's Plot.
        composeTestRule.onNodeWithContentDescription(activity.getString(R.string.esther_banquets_rescue_reveal_option_speak_calmly)).performClick()
        composeTestRule.onNodeWithContentDescription(activity.getString(R.string.esther_banquets_rescue_reveal_option_tell_truth)).performClick()
        composeTestRule.onNodeWithContentDescription(activity.getString(R.string.esther_banquets_rescue_reveal_option_name_haman)).performClick()
        composeTestRule.onNodeWithText(continueLabel).performClick()

        // Haman's Plot is Turned Back context -> Lesson.
        composeTestRule.onNodeWithText(continueLabel).performClick()
        composeTestRule.onNodeWithText(activity.getString(R.string.esther_brave_approach_lesson_title)).assertExists()
        composeTestRule.onNodeWithText(continueLabel).performClick()

        // Reward — one badge, all 5 scripture cards.
        composeTestRule.onNodeWithText(activity.getString(R.string.reward_title)).assertExists()
        composeTestRule.onNodeWithText(activity.getString(R.string.badge_courageous_heart_title)).assertExists()
        composeTestRule.onNodeWithText("Esther 2:20").assertExists()
        composeTestRule.onNodeWithText("Esther 2:22").assertExists()
        composeTestRule.onNodeWithText("Esther 4:3").assertExists()
        composeTestRule.onNodeWithText("Esther 4:14").assertExists()
        composeTestRule.onNodeWithText("Esther 7:3").assertExists()
        // The Reward screen scrolls (5 scripture cards + badge won't fit one
        // screen), so Return to Map sits below the fold — scroll to it first.
        val returnToMapNode = composeTestRule.onNodeWithText(activity.getString(R.string.action_return_to_map))
        returnToMapNode.performScrollTo()
        returnToMapNode.performClick()

        // Closing the loop: completing Esther unlocks Jericho.
        composeTestRule.onNodeWithText(activity.getString(R.string.world_map_title)).assertExists()
        scrollToChapterOnWorldMap(activity.getString(R.string.chapter_jericho_title))
        composeTestRule.onNodeWithText(activity.getString(R.string.chapter_jericho_title)).assertExists()
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

        val leftLabel = activity.getString(R.string.david_goliath_dodge_lane_left)
        val rightLabel = activity.getString(R.string.david_goliath_dodge_lane_right)
        DavidGoliathContent.dodgeBeats.forEach { beat ->
            val safeLabel = if (beat.hazardLane == DodgeLane.LEFT) rightLabel else leftLabel
            composeTestRule.onNodeWithText(safeLabel).performClick()
        }
        composeTestRule.onNodeWithText(continueLabel).performClick()

        composeTestRule.onNodeWithText(activity.getString(R.string.david_goliath_sling_practice_title)).assertExists()

        composeTestRule.mainClock.autoAdvance = false
        val markDescription = activity.getString(R.string.david_goliath_sling_target_mark_content_description)
        val stoneDescription = activity.getString(R.string.david_goliath_sling_stone_content_description)
        val stoneNode = composeTestRule.onNodeWithContentDescription(stoneDescription)
        dragOntoContentDescription(itemNode = stoneNode, targetContentDescription = markDescription)
        composeTestRule.mainClock.autoAdvance = true

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

        val leftLabel = activity.getString(R.string.daniel_stealth_lane_left)
        val rightLabel = activity.getString(R.string.daniel_stealth_lane_right)
        DanielContent.stealthBeats.forEach { beat ->
            val safeLabel = if (beat.hazardLane == DodgeLane.LEFT) rightLabel else leftLabel
            composeTestRule.onNodeWithText(safeLabel).performClick()
        }
        composeTestRule.onNodeWithText(continueLabel).performClick()

        composeTestRule.onNodeWithText(activity.getString(R.string.daniel_choice_option_1)).performClick()
        composeTestRule.onNodeWithText(continueLabel).performClick()

        composeTestRule.onNodeWithText(continueLabel).performClick() // Into the Lions' Den context

        DanielContent.lionsDenPoints.forEach { point ->
            composeTestRule.onNodeWithContentDescription(activity.getString(point.nameRes)).performClick()
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
