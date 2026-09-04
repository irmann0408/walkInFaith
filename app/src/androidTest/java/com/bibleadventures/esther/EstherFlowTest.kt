package com.bibleadventures.esther

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
import com.bibleadventures.completeDaniel
import com.bibleadventures.completeDavidGoliath
import com.bibleadventures.completeGoodSamaritan
import com.bibleadventures.completeNoahsArk
import com.bibleadventures.game.puzzles.gridmaze.Direction
import com.bibleadventures.game.stories.EstherContent
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
 * used at the time — was dropped for being both redundant and too easy).
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
        val nextPageLabel = activity.getString(R.string.action_next_page)

        composeTestRule.onNodeWithText(activity.getString(R.string.menu_adventures)).performClick()
        composeTestRule.completeNoahsArk()
        composeTestRule.completeDavidGoliath()
        composeTestRule.completeGoodSamaritan()
        composeTestRule.completeDaniel()

        scrollToChapterOnWorldMap(activity.getString(R.string.chapter_esther_title))
        composeTestRule.onNodeWithText(activity.getString(R.string.chapter_esther_title)).performClick()

        // Intro -> Chosen for the Palace context.
        composeTestRule.onNodeWithText(nextPageLabel).performClick()
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // Royal Attire (hidden object).
        EstherContent.royalAttireItems.forEach { item ->
            composeTestRule.onNodeWithContentDescription(activity.getString(item.nameRes)).performClick()
        }
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // Esther Becomes Queen context -> Greeting choice.
        composeTestRule.onNodeWithText(nextPageLabel).performClick()
        composeTestRule.onNodeWithText(activity.getString(R.string.esther_new_queen_choice_option_1)).performClick()
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // A Dangerous Secret context.
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

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
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // The King is Warned context -> A Wicked Law context.
        composeTestRule.onNodeWithText(nextPageLabel).performClick()
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

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
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // The City Mourns and Fasts context -> decision choice.
        composeTestRule.onNodeWithText(nextPageLabel).performClick()
        composeTestRule.onNodeWithText(activity.getString(R.string.esther_brave_approach_choice_option_1)).performClick()
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // Three Days of Fasting context.
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // Corridor Courage Meter — a 3-lane rhythm mini-game. Freeze the clock and
        // advance to each authored note's exact time so every tap lands; a
        // mistimed tap would just be a no-op (see RhythmLaneGame's no-failure design).
        // Leads straight into the Lesson now — Reveal Haman's Plot and its
        // surrounding context cards were dropped to tighten the chapter's tail end.
        completeCorridorRhythmLane()
        composeTestRule.onNodeWithText(nextPageLabel).performClick()
        composeTestRule.onNodeWithText(activity.getString(R.string.esther_brave_approach_lesson_title)).assertExists()
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

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
