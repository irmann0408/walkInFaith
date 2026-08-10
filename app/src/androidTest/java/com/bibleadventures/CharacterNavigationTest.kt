package com.bibleadventures

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class CharacterNavigationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

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

        composeTestRule.onNodeWithContentDescription(braidedLabel).performClick()
        composeTestRule.onNodeWithContentDescription(braidedSelectedDescription).assertExists()

        composeTestRule.onNodeWithContentDescription(backDescription).performClick()
        composeTestRule.onNodeWithText(appName).assertExists()

        composeTestRule.onNodeWithText(characterLabel).performClick()
        composeTestRule.onNodeWithContentDescription(braidedSelectedDescription).assertExists()
    }
}
