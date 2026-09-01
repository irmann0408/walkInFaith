package com.bibleadventures

import android.content.Context
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.bibleadventures.data.local.playerProfileDataStore
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * This test asserts on the Hairstyle picker — reset the save file before/
 * after since this app's single save file persists real state across test
 * runs, same pattern as `WorldMapNavigationTest`'s reset for the same
 * shared-save-file reason.
 */
class CharacterNavigationTest {

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
    fun selectingAHairstyle_persistsAcrossLeavingAndReturningToTheScreen() {
        val characterLabel = composeTestRule.activity.getString(R.string.menu_character)
        val screenTitle = composeTestRule.activity.getString(R.string.character_screen_title)
        val braidedLabel = composeTestRule.activity.getString(R.string.character_hairstyle_braided)
        val braidedSelectedDescription = composeTestRule.activity.getString(
            R.string.character_option_selected_content_description,
            braidedLabel,
        )
        val backDescription = composeTestRule.activity.getString(R.string.action_back)
        val appName = composeTestRule.activity.getString(R.string.app_name)

        composeTestRule.onNodeWithText(characterLabel).performClick()
        composeTestRule.onNodeWithText(screenTitle).assertExists()

        // onHairstyleSelected doesn't update local state optimistically — uiState only
        // reflects the new selection once the write round-trips back through the
        // repository's Flow, which Compose's idle-wait doesn't cover (it only waits on
        // pending recomposition, not arbitrary async I/O), so wait for it explicitly
        // rather than asserting immediately after the click.
        composeTestRule.onNodeWithContentDescription(braidedLabel).performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithContentDescription(braidedSelectedDescription).fetchSemanticsNodes().isNotEmpty()
        }

        // The selection is persisted asynchronously through DataStore, and leaving this
        // screen destroys this CharacterViewModel — returning creates a fresh one that
        // reads the repository from scratch. Compose's own idle-wait only covers pending
        // recomposition, not that underlying disk write, so wait for it to actually land
        // before asserting a freshly-created screen reflects it, rather than assuming a
        // single immediate check catches up in time.
        composeTestRule.onNodeWithContentDescription(backDescription).performClick()
        composeTestRule.onNodeWithText(appName).assertExists()

        composeTestRule.onNodeWithText(characterLabel).performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithContentDescription(braidedSelectedDescription).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
