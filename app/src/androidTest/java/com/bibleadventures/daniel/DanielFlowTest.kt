package com.bibleadventures.daniel

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import com.bibleadventures.MainActivity
import com.bibleadventures.R
import com.bibleadventures.completeDavidGoliath
import com.bibleadventures.completeGoodSamaritan
import com.bibleadventures.completeNoahsArk
import com.bibleadventures.game.puzzles.gridmaze.Direction
import com.bibleadventures.game.puzzles.slideout.SlideDirection
import com.bibleadventures.game.stories.DanielContent
import org.junit.Rule
import org.junit.Test

/**
 * Walks the full Daniel and the Lions adventure end to end. It's locked
 * until Noah's Ark, David and Goliath, and Good Samaritan are completed —
 * and this device's save data persists real state across test runs — so
 * this test completes all three prerequisites itself rather than assuming
 * they're already done, to stay deterministic regardless of what ran before
 * it (same pattern as GoodSamaritanFlowTest).
 */
class DanielFlowTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun completingDaniel_awardsStarsAndUnlocksEstherOnTheWorldMap() {
        val activity = composeTestRule.activity
        val nextPageLabel = activity.getString(R.string.action_next_page)

        composeTestRule.onNodeWithText(activity.getString(R.string.menu_adventures)).performClick()
        composeTestRule.completeNoahsArk()
        composeTestRule.completeDavidGoliath()
        composeTestRule.completeGoodSamaritan()

        // World Map -> Daniel and the Lions (now unlocked).
        composeTestRule.onNodeWithText(activity.getString(R.string.chapter_daniel_title)).performClick()

        // Scene 1: Intro.
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // Scene 1b: A Shuttered Window context card.
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // Scene 2: Open the Window — a fully tiled 6x6 board where every
        // latch points at its own nearest edge; tap every latch in the
        // hand-verified outside-in release order (see
        // DanielContent.windowLatchSolutionOrder), each one identified by
        // its own row/column/direction content description.
        composeTestRule.onNodeWithText(activity.getString(R.string.daniel_window_title)).assertExists()
        DanielContent.windowLatchSolutionOrder.forEach { latch ->
            val stringRes = when (latch.direction) {
                SlideDirection.UP -> R.string.daniel_window_latch_up_content_description
                SlideDirection.DOWN -> R.string.daniel_window_latch_down_content_description
                SlideDirection.LEFT -> R.string.daniel_window_latch_left_content_description
                SlideDirection.RIGHT -> R.string.daniel_window_latch_right_content_description
            }
            val description = activity.getString(stringRes, latch.position.row + 1, latch.position.col + 1)
            composeTestRule.onNodeWithContentDescription(description).performClick()
        }
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // Scene 3: Choice — Daniel's prayer, flavor-only.
        composeTestRule.onNodeWithText(activity.getString(R.string.daniel_choice_option_1)).performClick()
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // Scene 3b: Into the Lions' Den context card.
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // Scene 4: The Angel's Shield — 5 random math problems, one per
        // light. Two wrong answers in a row now replace the problem instead
        // of leaving the last choice a guaranteed-correct guess (see
        // DecisionPathGame.WRONG_ATTEMPTS_BEFORE_NEW_STEP), so the old
        // "just try each of the 3 choices" trick no longer reliably solves
        // it — compute the real answer from the displayed problem instead.
        repeat(DanielContent.LIONS_DEN_PROBLEM_COUNT) {
            solveLionsDenProblem()
        }
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // Scene 4b: Darius's Long Night context card.
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // Scene 5: Darius's Maze — solve with a hand-verified move sequence.
        val upLabel = activity.getString(R.string.daniel_darius_direction_up)
        val downLabel = activity.getString(R.string.daniel_darius_direction_down)
        val mazeLeftLabel = activity.getString(R.string.daniel_darius_direction_left)
        val mazeRightLabel = activity.getString(R.string.daniel_darius_direction_right)

        // The start tile (0,0) is the map's top-left corner, so pressing Up
        // walks off the edge — a deliberate, harmless BLOCKED move (doesn't
        // change position) confirming the maze's new accessibility feedback
        // text actually renders, not just that the engine reports it.
        composeTestRule.onNodeWithContentDescription(upLabel).performClick()
        composeTestRule.onNodeWithText(activity.getString(R.string.grid_maze_feedback_blocked)).assertExists()

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

        // Scene 6: Lesson.
        composeTestRule.onNodeWithText(activity.getString(R.string.daniel_lesson_title)).assertExists()
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // Scene 7: Reward.
        composeTestRule.onNodeWithText(activity.getString(R.string.reward_title)).assertExists()
        composeTestRule.onNodeWithText(activity.getString(R.string.badge_faithful_heart_title)).assertExists()
        composeTestRule.onNodeWithText(activity.getString(R.string.action_return_to_map)).performClick()

        // Back on the World Map: Daniel completed, Esther unlocked.
        composeTestRule.onNodeWithText(activity.getString(R.string.world_map_title)).assertExists()
        composeTestRule.onNodeWithText(activity.getString(R.string.chapter_esther_title)).assertExists()
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

    /**
     * Reads the displayed "%d + %d = ?" / "%d − %d = ?" problem, computes
     * the real answer, and taps the matching choice by its content
     * description (each `AnswerChoice` exposes its own value as its content
     * description). Deterministic by construction, so it also stays correct
     * once wrong answers can replace the problem mid-attempt — unlike a
     * blind "try all 3 choices" trial, this never taps a wrong answer at all.
     */
    private fun solveLionsDenProblem() {
        val problemText = composeTestRule.onNodeWithTag("lions_den_problem").fetchSemanticsNode()
            .config[SemanticsProperties.Text].joinToString(separator = "") { it.text }
        val operands = Regex("\\d+").findAll(problemText).map { it.value.toInt() }.toList()
        val correctValue = if ("−" in problemText) operands[0] - operands[1] else operands[0] + operands[1]
        composeTestRule.onNodeWithContentDescription(correctValue.toString()).performClick()
    }
}
