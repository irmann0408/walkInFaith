package com.bibleadventures

import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class SettingsNavigationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun togglingMusic_persistsAcrossLeavingAndReturningToTheScreen() {
        val settingsLabel = composeTestRule.activity.getString(R.string.menu_settings)
        val screenTitle = composeTestRule.activity.getString(R.string.settings_screen_title)
        val musicLabel = composeTestRule.activity.getString(R.string.settings_music_label)
        val backDescription = composeTestRule.activity.getString(R.string.action_back)
        val appName = composeTestRule.activity.getString(R.string.app_name)

        composeTestRule.onNodeWithText(settingsLabel).performClick()
        composeTestRule.onNodeWithText(screenTitle).assertExists()

        // Music defaults to on — toggling it off, then confirming the switch stays
        // off after leaving and returning, is the real regression to guard against.
        composeTestRule.onNodeWithContentDescription(musicLabel).performClick()

        // The toggle is persisted asynchronously through DataStore, and leaving this
        // screen destroys this SettingsViewModel — returning creates a fresh one that
        // reads the repository from scratch. Compose's own idle-wait only covers pending
        // recomposition, not that underlying disk write, so wait for it to actually land
        // before asserting a freshly-created screen reflects it.
        composeTestRule.onNodeWithContentDescription(backDescription).performClick()
        composeTestRule.onNodeWithText(appName).assertExists()

        composeTestRule.onNodeWithText(settingsLabel).performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            runCatching { composeTestRule.onNodeWithContentDescription(musicLabel).assertIsOff() }.isSuccess
        }
    }
}
