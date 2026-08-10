package com.bibleadventures

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class MainMenuNavigationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun launchingTheApp_showsTheMainMenu() {
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.app_name))
            .assertExists()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.menu_adventures))
            .assertExists()
    }

    @Test
    fun tappingAMenuItem_navigatesForwardAndBackNavigationReturnsToTheMenu() {
        // "My Badges" has no real screen yet (unlike "Adventures", which now routes
        // to the World Map — see WorldMapNavigationTest), so it still exercises the
        // generic placeholder flow.
        val badgesLabel = composeTestRule.activity.getString(R.string.menu_badges)
        val comingSoonTitle = composeTestRule.activity.getString(R.string.coming_soon_title)
        val appName = composeTestRule.activity.getString(R.string.app_name)

        composeTestRule.onNodeWithText(badgesLabel).performClick()
        composeTestRule.onNodeWithText(comingSoonTitle).assertExists()

        composeTestRule.onNodeWithContentDescription(
            composeTestRule.activity.getString(R.string.action_back),
        ).performClick()
        composeTestRule.onNodeWithText(appName).assertExists()
    }
}
