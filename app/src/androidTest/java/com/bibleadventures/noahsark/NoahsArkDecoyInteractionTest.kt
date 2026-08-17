package com.bibleadventures.noahsark

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.bibleadventures.MainActivity
import com.bibleadventures.R
import com.bibleadventures.data.local.playerProfileDataStore
import com.bibleadventures.game.stories.NoahsArkContent
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Covers the decoy items added to Find the Animals and Organize the Ark:
 * tapping/dropping one shows scene-specific feedback, never enables
 * Continue prematurely, and stays interactive after repeated attempts. Kept separate
 * from [NoahsArkFlowTest] so that "happy path" walk doesn't have to carry
 * decoy-specific assertions.
 */
class NoahsArkDecoyInteractionTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    // A prior test run (or this suite's own other tests) may have already
    // solved find_animals/organize_ark for real, which would otherwise make
    // Continue appear early via the "skip an already-completed puzzle"
    // shortcut — clearing the profile first keeps this test deterministic
    // regardless of what ran before it, same idiom as
    // PlayerProfileLocalDataSourceInstrumentedTest.
    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        runBlocking { context.playerProfileDataStore.edit { it.clear() } }
    }

    @Test
    fun decoysShowFeedback_neverCompleteTheScene_andStayInteractiveAfterRepeatedTaps() {
        val activity = composeTestRule.activity
        val nextPageLabel = activity.getString(R.string.action_next_page)

        composeTestRule.onNodeWithText(activity.getString(R.string.menu_adventures)).performClick()
        composeTestRule.onNodeWithText(activity.getString(R.string.chapter_noahs_ark_title)).performClick()
        composeTestRule.onNodeWithText(nextPageLabel).performClick() // Intro -> Find Animals context
        composeTestRule.onNodeWithText(nextPageLabel).performClick() // context -> Find Animals

        // --- Find the Animals: tapping the rock never counts, never completes the scene.
        val rockName = activity.getString(R.string.decoy_rock)
        val notAnAnimal = activity.getString(R.string.feedback_not_an_animal)
        repeat(2) {
            composeTestRule.onNodeWithContentDescription(rockName).performClick()
            composeTestRule.onNodeWithText(notAnAnimal).assertExists()
        }
        composeTestRule.onNodeWithText(nextPageLabel).assertDoesNotExist()

        NoahsArkContent.animals.forEach { animal ->
            composeTestRule.onNodeWithContentDescription(activity.getString(animal.nameRes)).performClick()
        }
        composeTestRule.onNodeWithText(nextPageLabel).performClick() // Find Animals -> Matching

        // --- Animal Matching is out of scope for decoys; match every pair to move on.
        NoahsArkContent.animals.forEach { animal ->
            val name = activity.getString(animal.nameRes)
            composeTestRule.onAllNodesWithContentDescription(name)[0].performClick()
            composeTestRule.onAllNodesWithContentDescription(name)[1].performClick()
        }
        composeTestRule.onNodeWithText(nextPageLabel).performClick() // Matching -> Organize Ark context
        composeTestRule.onNodeWithText(nextPageLabel).performClick() // context -> Organize the Ark

        // --- Organize the Ark: dropping the hammer on any category never places it or
        // completes the scene.
        val hammerName = activity.getString(R.string.decoy_hammer)
        val doesntBelong = activity.getString(R.string.feedback_doesnt_belong)
        val firstCategoryLabel = activity.getString(NoahsArkContent.sortCategories.first().labelRes)
        repeat(2) {
            dragOnto(composeTestRule.onNodeWithContentDescription(hammerName), firstCategoryLabel)
            composeTestRule.onNodeWithText(doesntBelong).assertExists()
        }
        composeTestRule.onNodeWithText(nextPageLabel).assertDoesNotExist()

        NoahsArkContent.sortableItems.filter { it.categoryKey != null }.forEach { item ->
            val itemName = activity.getString(item.nameRes)
            val categoryLabelRes = NoahsArkContent.sortCategories.first { it.key == item.categoryKey }.labelRes
            val categoryLabel = activity.getString(categoryLabelRes)
            dragOnto(composeTestRule.onNodeWithContentDescription(itemName), categoryLabel)
        }
        composeTestRule.onNodeWithText(nextPageLabel).assertExists()
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
