package com.bibleadventures.noahsark

import android.content.Context
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.bibleadventures.MainActivity
import com.bibleadventures.R
import com.bibleadventures.completeNoahsArk
import com.bibleadventures.data.local.playerProfileDataStore
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

        composeTestRule.onNodeWithText(activity.getString(R.string.menu_adventures)).performClick()
        composeTestRule.completeNoahsArk()

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

        composeTestRule.onNodeWithText(activity.getString(R.string.menu_adventures)).performClick()
        composeTestRule.completeNoahsArk()

        // Re-enter the chapter: the graph always starts at the Intro video,
        // regardless of prior completion; tapping past it lands on Find the
        // Tools, which is now a previously-completed scene.
        composeTestRule.onNodeWithText(activity.getString(R.string.chapter_noahs_ark_title)).performClick()
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        composeTestRule.onNodeWithContentDescription(activity.getString(R.string.action_back_to_main_menu)).performClick()

        composeTestRule.onNodeWithText(activity.getString(R.string.menu_adventures)).assertExists()
    }
}
