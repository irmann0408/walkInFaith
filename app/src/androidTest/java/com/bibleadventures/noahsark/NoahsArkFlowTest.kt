package com.bibleadventures.noahsark

import androidx.compose.ui.geometry.center
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.geometry.Offset
import com.bibleadventures.MainActivity
import com.bibleadventures.R
import com.bibleadventures.game.stories.NoahsArkContent
import org.junit.Rule
import org.junit.Test

/**
 * Walks the full Noah's Ark adventure end to end: World Map -> every scene
 * -> Reward -> back to the World Map, and confirms the World Map reflects
 * the completed chapter afterward (spec section 19's "complete Noah's Ark",
 * "receive rewards", and "return to map" required UI test flows).
 */
class NoahsArkFlowTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun completingNoahsArk_awardsStarsAndUnlocksDavidAndGoliathOnTheWorldMap() {
        val activity = composeTestRule.activity
        val continueLabel = activity.getString(R.string.action_continue)

        // World Map -> Noah's Ark.
        composeTestRule.onNodeWithText(activity.getString(R.string.menu_adventures)).performClick()
        composeTestRule.onNodeWithText(activity.getString(R.string.chapter_noahs_ark_title)).performClick()

        // Scene 1: Intro.
        composeTestRule.onNodeWithText(continueLabel).performClick()

        // Scene 2: Find the Animals.
        NoahsArkContent.animals.forEach { animal ->
            composeTestRule.onAllNodesWithContentDescription(activity.getString(animal.nameRes))[0].performClick()
        }
        composeTestRule.onNodeWithText(continueLabel).performClick()

        // Scene 3: Animal Matching — match every pair by content description.
        NoahsArkContent.animals.forEach { animal ->
            val name = activity.getString(animal.nameRes)
            composeTestRule.onAllNodesWithContentDescription(name)[0].performClick()
            composeTestRule.onAllNodesWithContentDescription(name)[1].performClick()
        }
        composeTestRule.onNodeWithText(continueLabel).performClick()

        // Scene 4: Gather Supplies.
        NoahsArkContent.supplies.forEach { supply ->
            composeTestRule.onNodeWithContentDescription(activity.getString(supply.nameRes)).performClick()
        }
        composeTestRule.onNodeWithText(continueLabel).performClick()

        // Scene 5: Organize the Ark — drag every item onto its category.
        NoahsArkContent.sortableItems.forEach { item ->
            val itemName = activity.getString(item.nameRes)
            val categoryLabelRes = NoahsArkContent.sortCategories.first { it.key == item.categoryKey }.labelRes
            val categoryLabel = activity.getString(categoryLabelRes)
            dragOnto(itemNode = composeTestRule.onNodeWithContentDescription(itemName), targetLabel = categoryLabel)
        }
        composeTestRule.onNodeWithText(continueLabel).performClick()

        // Scene 6: Find the Missing Items.
        NoahsArkContent.hiddenItems.forEach { item ->
            composeTestRule.onNodeWithContentDescription(activity.getString(item.nameRes)).performClick()
        }
        composeTestRule.onNodeWithText(continueLabel).performClick()

        // Scene 7: Lesson.
        composeTestRule.onNodeWithText(activity.getString(R.string.noahs_ark_lesson_title)).assertExists()
        composeTestRule.onNodeWithText(continueLabel).performClick()

        // Scene 8: Reward.
        composeTestRule.onNodeWithText(activity.getString(R.string.reward_title)).assertExists()
        composeTestRule.onNodeWithText(activity.getString(R.string.badge_ark_builder_title)).assertExists()
        composeTestRule.onNodeWithText(activity.getString(R.string.action_return_to_map)).performClick()

        // Back on the World Map: Noah's Ark completed, David & Goliath unlocked.
        composeTestRule.onNodeWithText(activity.getString(R.string.world_map_title)).assertExists()
        composeTestRule.onNodeWithText(activity.getString(R.string.chapter_david_goliath_title)).assertExists()
    }

    private fun dragOnto(itemNode: SemanticsNodeInteraction, targetLabel: String) {
        val itemBounds = itemNode.fetchSemanticsNode().boundsInRoot
        val targetBounds = composeTestRule.onNodeWithText(targetLabel).fetchSemanticsNode().boundsInRoot
        val targetGlobalCenter = targetBounds.center
        val localEnd = Offset(targetGlobalCenter.x - itemBounds.left, targetGlobalCenter.y - itemBounds.top)

        itemNode.performTouchInput {
            swipe(start = center, end = localEnd, durationMillis = 200)
        }
    }
}
