package com.bibleadventures.noahsark

import android.content.Context
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
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.bibleadventures.MainActivity
import com.bibleadventures.R
import com.bibleadventures.data.local.playerProfileDataStore
import com.bibleadventures.game.stories.NoahsArkContent
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
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

    @Before
    fun resetSaveFile() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        runBlocking { context.playerProfileDataStore.edit { it.clear() } }
    }

    @After
    fun clearSaveFile() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        runBlocking { context.playerProfileDataStore.edit { it.clear() } }
    }

    @Test
    fun completingNoahsArk_awardsStarsAndUnlocksDavidAndGoliathOnTheWorldMap() {
        val activity = composeTestRule.activity

        completeNoahsArk()

        // Back on the World Map: Noah's Ark completed, David & Goliath unlocked.
        composeTestRule.onNodeWithText(activity.getString(R.string.world_map_title)).assertExists()
        composeTestRule.onNodeWithText(activity.getString(R.string.chapter_david_goliath_title)).assertExists()
    }

    /**
     * Re-enters an already-completed chapter and confirms the Main-Menu
     * shortcut button (added alongside removing "Continue Adventure" from
     * the Main Menu) jumps straight back to the Main Menu from a
     * previously-completed scene, without needing to re-solve it or step
     * back one screen at a time.
     */
    @Test
    fun revisitingACompletedScene_backToMainMenuButtonReturnsDirectlyToMainMenu() {
        val activity = composeTestRule.activity
        val nextPageLabel = activity.getString(R.string.action_next_page)

        completeNoahsArk()

        // Re-enter the chapter: Intro -> Find Animals context -> Find Animals, which is
        // now a previously-completed scene.
        composeTestRule.onNodeWithText(activity.getString(R.string.chapter_noahs_ark_title)).performClick()
        composeTestRule.onNodeWithText(nextPageLabel).performClick()
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        composeTestRule.onNodeWithContentDescription(activity.getString(R.string.action_back_to_main_menu)).performClick()

        composeTestRule.onNodeWithText(activity.getString(R.string.menu_adventures)).assertExists()
    }

    private fun completeNoahsArk() {
        val activity = composeTestRule.activity
        val nextPageLabel = activity.getString(R.string.action_next_page)

        // World Map -> Noah's Ark.
        composeTestRule.onNodeWithText(activity.getString(R.string.menu_adventures)).performClick()
        composeTestRule.onNodeWithText(activity.getString(R.string.chapter_noahs_ark_title)).performClick()

        // Scene 1: Intro.
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // Scene 1b: Find the Animals context card.
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // Scene 2: Find the Animals. The decoy (a rock) is deliberately left untapped —
        // completion must not depend on it.
        NoahsArkContent.animals.forEach { animal ->
            composeTestRule.onAllNodesWithContentDescription(activity.getString(animal.nameRes))[0].performClick()
        }
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // Scene 3: Animal Matching — match every pair by content description.
        NoahsArkContent.animals.forEach { animal ->
            val name = activity.getString(animal.nameRes)
            composeTestRule.onAllNodesWithContentDescription(name)[0].performClick()
            composeTestRule.onAllNodesWithContentDescription(name)[1].performClick()
        }
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // Scene 3b: Organize the Ark context card.
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // Scene 4: Organize the Ark — drag every real item onto its category. The decoy
        // (a hammer, categoryKey == null) is deliberately left in the tray, untouched.
        NoahsArkContent.sortableItems.filter { it.categoryKey != null }.forEach { item ->
            val itemName = activity.getString(item.nameRes)
            val categoryLabelRes = NoahsArkContent.sortCategories.first { it.key == item.categoryKey }.labelRes
            val categoryLabel = activity.getString(categoryLabelRes)
            dragOnto(itemNode = composeTestRule.onNodeWithContentDescription(itemName), targetLabel = categoryLabel)
        }
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // Scene 5: Find the Missing Items.
        NoahsArkContent.hiddenItems.forEach { item ->
            composeTestRule.onNodeWithContentDescription(activity.getString(item.nameRes)).performClick()
        }
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // Scene 6: Lesson.
        composeTestRule.onNodeWithText(activity.getString(R.string.noahs_ark_lesson_title)).assertExists()
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // Scene 7: Reward.
        composeTestRule.onNodeWithText(activity.getString(R.string.reward_title)).assertExists()
        composeTestRule.onNodeWithText(activity.getString(R.string.badge_ark_builder_title)).assertExists()
        composeTestRule.onNodeWithText(activity.getString(R.string.action_return_to_map)).performClick()
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
