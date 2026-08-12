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
        // Every MenuItemId now routes to a real screen (Parent Area was the last
        // holdout — see ParentAreaFlowTest for its gated flow). This just confirms
        // forward navigation and back both work; the screen's title shows
        // regardless of the parental gate's lock state, so no need to solve it here.
        val parentAreaLabel = composeTestRule.activity.getString(R.string.menu_parent_area)
        val parentAreaTitle = composeTestRule.activity.getString(R.string.parent_area_screen_title)
        val appName = composeTestRule.activity.getString(R.string.app_name)

        composeTestRule.onNodeWithText(parentAreaLabel).performClick()
        composeTestRule.onNodeWithText(parentAreaTitle).assertExists()

        composeTestRule.onNodeWithContentDescription(
            composeTestRule.activity.getString(R.string.action_back),
        ).performClick()
        composeTestRule.onNodeWithText(appName).assertExists()
    }
}
