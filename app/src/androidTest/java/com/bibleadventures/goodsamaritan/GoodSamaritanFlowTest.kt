package com.bibleadventures.goodsamaritan

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import com.bibleadventures.MainActivity
import com.bibleadventures.R
import com.bibleadventures.completeDavidGoliath
import com.bibleadventures.completeNoahsArk
import com.bibleadventures.game.puzzles.gridmaze.Direction
import com.bibleadventures.game.puzzles.roadblock.Direction as RoadblockDirection
import com.bibleadventures.game.stories.GoodSamaritanContent
import org.junit.Rule
import org.junit.Test

/**
 * Walks the full Good Samaritan adventure end to end. It's locked until
 * David and Goliath is completed, which is itself locked until Noah's Ark
 * is completed — and this device's save data persists real state across
 * test runs — so this test completes both prerequisite chapters itself
 * rather than assuming they're already done, to stay deterministic
 * regardless of what ran before it (same pattern as DavidGoliathFlowTest).
 */
class GoodSamaritanFlowTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun completingGoodSamaritan_awardsStarsAndUnlocksDanielOnTheWorldMap() {
        val activity = composeTestRule.activity
        val continueLabel = activity.getString(R.string.action_continue)
        val nextPageLabel = activity.getString(R.string.action_next_page)

        composeTestRule.onNodeWithText(activity.getString(R.string.menu_adventures)).performClick()
        composeTestRule.completeNoahsArk()
        composeTestRule.completeDavidGoliath()

        // World Map -> Good Samaritan (now unlocked).
        composeTestRule.onNodeWithText(activity.getString(R.string.chapter_good_samaritan_title)).performClick()

        // Scene 1: Dangerous Road video.
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // Scene 2: Passing By — a sliding-block puzzle, solved with a hand-verified move sequence.
        completePassingBy()
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // Scene 3: The Priest video.
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // Scene 4: The Levite video.
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // Scene 5: Explore — solve the maze with a hand-verified move sequence. The
        // helping-beat overlay appears automatically the instant the traveler is
        // treated (blocking further D-pad input underneath), so it's dismissed
        // inline the moment it's detected rather than at one hardcoded step index.
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

        composeTestRule.onNodeWithText(nextPageLabel).performClick() // Explore -> Samaritan Arrives video

        // Scene 6: Samaritan Arrives video.
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // Scene 7: Reward.
        composeTestRule.onNodeWithText(activity.getString(R.string.reward_title)).assertExists()
        composeTestRule.onNodeWithText(activity.getString(R.string.badge_good_neighbor_title)).assertExists()
        composeTestRule.onNodeWithText(activity.getString(R.string.action_return_to_map)).performClick()

        // Back on the World Map: Good Samaritan completed, Daniel and the Lions unlocked.
        composeTestRule.onNodeWithText(activity.getString(R.string.world_map_title)).assertExists()
        composeTestRule.onNodeWithText(activity.getString(R.string.chapter_daniel_title)).assertExists()
    }

    /**
     * Replays GoodSamaritanContent.passingBySolution as real drag gestures.
     * Each block's own content description (the protagonist's, or an excuse
     * block's own label) is unique on screen, and the exit gate's rendered
     * width — a single grid cell — gives the exact pixels-per-cell needed
     * to turn a hand-verified (direction, distance) move into a real swipe.
     */
    private fun completePassingBy() {
        val activity = composeTestRule.activity
        val gateBounds = composeTestRule
            .onNodeWithContentDescription(activity.getString(R.string.good_samaritan_passing_by_exit_gate_content_description))
            .fetchSemanticsNode()
            .boundsInRoot
        val cellSizePx = gateBounds.width

        val blockContentDescriptions = mapOf(
            "religious_leader" to activity.getString(R.string.good_samaritan_passing_by_protagonist_content_description),
            "ritual_purity" to activity.getString(R.string.good_samaritan_passing_by_excuse_ritual_purity),
            "fear_of_ambush" to activity.getString(R.string.good_samaritan_passing_by_excuse_fear_of_ambush),
            "strict_schedule" to activity.getString(R.string.good_samaritan_passing_by_excuse_strict_schedule),
            "not_my_problem" to activity.getString(R.string.good_samaritan_passing_by_excuse_not_my_problem),
        )

        GoodSamaritanContent.passingBySolution.forEach { move ->
            val description = blockContentDescriptions.getValue(move.blockId)
            val magnitude = cellSizePx * move.distance
            val delta = when (move.direction) {
                RoadblockDirection.UP -> Offset(0f, -magnitude)
                RoadblockDirection.DOWN -> Offset(0f, magnitude)
                RoadblockDirection.LEFT -> Offset(-magnitude, 0f)
                RoadblockDirection.RIGHT -> Offset(magnitude, 0f)
            }
            composeTestRule.onNodeWithContentDescription(description).performTouchInput {
                swipe(start = center, end = center + delta, durationMillis = 200)
            }
        }
    }
}
