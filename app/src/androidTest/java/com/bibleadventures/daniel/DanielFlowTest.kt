package com.bibleadventures.daniel

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import com.bibleadventures.MainActivity
import com.bibleadventures.R
import com.bibleadventures.game.puzzles.dodge.DodgeLane
import com.bibleadventures.game.puzzles.gridmaze.Direction
import com.bibleadventures.game.stories.DanielContent
import com.bibleadventures.game.stories.DavidGoliathContent
import com.bibleadventures.game.stories.GoodSamaritanContent
import com.bibleadventures.game.stories.NoahsArkContent
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
        val continueLabel = activity.getString(R.string.action_continue)

        composeTestRule.onNodeWithText(activity.getString(R.string.menu_adventures)).performClick()
        completeNoahsArk(continueLabel)
        completeDavidGoliath(continueLabel)
        completeGoodSamaritan(continueLabel)

        // World Map -> Daniel and the Lions (now unlocked).
        composeTestRule.onNodeWithText(activity.getString(R.string.chapter_daniel_title)).performClick()

        // Scene 1: Intro.
        composeTestRule.onNodeWithText(continueLabel).performClick()

        // Scene 1b: Hurrying to Pray context card.
        composeTestRule.onNodeWithText(continueLabel).performClick()

        // Scene 2: Stealth — step to the clear side away from each official.
        val leftLabel = activity.getString(R.string.daniel_stealth_lane_left)
        val rightLabel = activity.getString(R.string.daniel_stealth_lane_right)
        DanielContent.stealthBeats.forEach { beat ->
            val safeLabel = if (beat.hazardLane == DodgeLane.LEFT) rightLabel else leftLabel
            composeTestRule.onNodeWithText(safeLabel).performClick()
        }
        composeTestRule.onNodeWithText(continueLabel).performClick()

        // Scene 3: Choice — Daniel's prayer, flavor-only.
        composeTestRule.onNodeWithText(activity.getString(R.string.daniel_choice_option_1)).performClick()
        composeTestRule.onNodeWithText(continueLabel).performClick()

        // Scene 3b: Into the Lions' Den context card.
        composeTestRule.onNodeWithText(continueLabel).performClick()

        // Scene 4: Lions' Den — connect the lights in order.
        DanielContent.lionsDenPoints.forEach { point ->
            composeTestRule.onNodeWithContentDescription(activity.getString(point.nameRes)).performClick()
        }
        composeTestRule.onNodeWithText(continueLabel).performClick()

        // Scene 4b: Darius's Long Night context card.
        composeTestRule.onNodeWithText(continueLabel).performClick()

        // Scene 5: Darius's Maze — solve with a hand-verified move sequence.
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

        // Scene 6: Lesson.
        composeTestRule.onNodeWithText(activity.getString(R.string.daniel_lesson_title)).assertExists()
        composeTestRule.onNodeWithText(continueLabel).performClick()

        // Scene 7: Reward.
        composeTestRule.onNodeWithText(activity.getString(R.string.reward_title)).assertExists()
        composeTestRule.onNodeWithText(activity.getString(R.string.badge_faithful_heart_title)).assertExists()
        composeTestRule.onNodeWithText(activity.getString(R.string.action_return_to_map)).performClick()

        // Back on the World Map: Daniel completed, Esther: The New Queen unlocked.
        composeTestRule.onNodeWithText(activity.getString(R.string.world_map_title)).assertExists()
        composeTestRule.onNodeWithText(activity.getString(R.string.chapter_esther_new_queen_title)).assertExists()
    }

    /** Walks Noah's Ark end to end (mirrors NoahsArkFlowTest) so David & Goliath unlocks. */
    private fun completeNoahsArk(continueLabel: String) {
        val activity = composeTestRule.activity

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
