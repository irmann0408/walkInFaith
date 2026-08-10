package com.bibleadventures.noahsark

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.bibleadventures.MainActivity
import com.bibleadventures.R
import com.bibleadventures.game.stories.NoahsArkContent
import org.junit.Rule
import org.junit.Test

/**
 * Drives the app all the way to the Animal Matching scene, then verifies the
 * spec section 9 requirement directly: a correct pair says "Great job!", an
 * incorrect pair says "Try another one!" — never anything that reads as
 * failure — and a mismatch never blocks making a correct match afterward.
 */
class AnimalMatchingGameTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun matchingCorrectPair_showsGreatJob_andIncorrectPair_showsTryAnotherOne() {
        val activity = composeTestRule.activity
        val adventuresLabel = activity.getString(R.string.menu_adventures)
        val noahsArkTitle = activity.getString(R.string.chapter_noahs_ark_title)
        val continueLabel = activity.getString(R.string.action_continue)
        val greatJob = activity.getString(R.string.feedback_great_job)
        val tryAnotherOne = activity.getString(R.string.feedback_try_another_one)

        composeTestRule.onNodeWithText(adventuresLabel).performClick()
        composeTestRule.onNodeWithText(noahsArkTitle).performClick()
        composeTestRule.onNodeWithText(continueLabel).performClick() // Intro -> Find Animals context
        composeTestRule.onNodeWithText(continueLabel).performClick() // context -> Find Animals

        NoahsArkContent.animals.forEach { animal ->
            val name = activity.getString(animal.nameRes)
            composeTestRule.onAllNodesWithContentDescription(name)[0].performClick()
        }
        composeTestRule.onNodeWithText(continueLabel).performClick() // Find Animals -> Matching

        val lionName = activity.getString(NoahsArkContent.animals.first { it.id == "lion" }.nameRes)
        val elephantName = activity.getString(NoahsArkContent.animals.first { it.id == "elephant" }.nameRes)

        // Mismatch first: never punished, and doesn't block trying again.
        composeTestRule.onAllNodesWithContentDescription(lionName)[0].performClick()
        composeTestRule.onAllNodesWithContentDescription(elephantName)[0].performClick()
        composeTestRule.onNodeWithText(tryAnotherOne).assertExists()

        // Now match the pair correctly (the two distinct lion tiles).
        composeTestRule.onAllNodesWithContentDescription(lionName)[0].performClick()
        composeTestRule.onAllNodesWithContentDescription(lionName)[1].performClick()
        composeTestRule.onNodeWithText(greatJob).assertExists()
    }
}
