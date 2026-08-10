package com.bibleadventures

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

/**
 * Confirms "My Badges"/"Scripture Cards" route to their real gallery screens
 * instead of ComingSoonScreen, and that back navigation returns to the menu.
 * Doesn't assert earned-vs-locked state for a specific badge/card, since that
 * depends on save data this test doesn't control (e.g. whether Noah's Ark was
 * already completed by another test run on this device) — earned/locked
 * rendering itself is covered by BadgesViewModelTest/ScriptureCardsViewModelTest.
 */
class BadgesAndScriptureCardsNavigationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun tappingMyBadges_showsTheBadgesGalleryAndBackReturnsToTheMenu() {
        val badgesLabel = composeTestRule.activity.getString(R.string.menu_badges)
        val badgesTitle = composeTestRule.activity.getString(R.string.badges_title)
        val appName = composeTestRule.activity.getString(R.string.app_name)
        val badgeTitle = composeTestRule.activity.getString(R.string.badge_ark_builder_title)

        composeTestRule.onNodeWithText(badgesLabel).performClick()
        composeTestRule.onNodeWithText(badgesTitle).assertExists()
        composeTestRule.onNodeWithText(badgeTitle).assertExists()

        composeTestRule.onNodeWithContentDescription(
            composeTestRule.activity.getString(R.string.action_back),
        ).performClick()
        composeTestRule.onNodeWithText(appName).assertExists()
    }

    @Test
    fun tappingScriptureCards_showsTheScriptureCardsGalleryAndBackReturnsToTheMenu() {
        val scriptureCardsLabel = composeTestRule.activity.getString(R.string.menu_scripture_cards)
        val scriptureCardsTitle = composeTestRule.activity.getString(R.string.scripture_cards_title)
        val appName = composeTestRule.activity.getString(R.string.app_name)

        composeTestRule.onNodeWithText(scriptureCardsLabel).performClick()
        composeTestRule.onNodeWithText(scriptureCardsTitle).assertExists()

        composeTestRule.onNodeWithContentDescription(
            composeTestRule.activity.getString(R.string.action_back),
        ).performClick()
        composeTestRule.onNodeWithText(appName).assertExists()
    }
}
