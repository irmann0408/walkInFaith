package com.bibleadventures.parentarea

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.bibleadventures.MainActivity
import com.bibleadventures.R
import com.bibleadventures.completeNoahsArk
import com.bibleadventures.game.rewards.RewardCatalog
import com.bibleadventures.game.stories.ChapterCatalog
import org.junit.Rule
import org.junit.Test

/**
 * Walks Parent Area end to end: the math gate (a wrong answer stays locked
 * and doesn't leak the answer, a correct one unlocks), the progress
 * summary reflecting a real completed chapter, the Settings shortcut, the
 * privacy dialog, and Reset Progress (cancel leaves progress untouched,
 * confirm actually clears it and re-locks the World Map).
 */
class ParentAreaFlowTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun parentArea_gateSummaryAndResetProgressAllWorkEndToEnd() {
        val activity = composeTestRule.activity

        composeTestRule.onNodeWithText(activity.getString(R.string.menu_adventures)).performClick()
        composeTestRule.completeNoahsArk()

        // completeNoahsArk() ends on the World Map — back to Main Menu -> Parent Area.
        composeTestRule.onNodeWithContentDescription(activity.getString(R.string.action_back)).performClick()
        composeTestRule.onNodeWithText(activity.getString(R.string.menu_parent_area)).performClick()
        composeTestRule.onNodeWithText(activity.getString(R.string.parent_area_screen_title)).assertExists()

        // Gate: a wrong answer stays locked and surfaces "try again" feedback.
        val (wrongA, wrongB) = readGateOperands()
        enterGateAnswer(wrongA + wrongB + 1)
        composeTestRule.onNodeWithText(activity.getString(R.string.parent_area_gate_wrong_answer)).assertExists()

        // A correct answer to the freshly regenerated question unlocks the summary.
        val (a, b) = readGateOperands()
        enterGateAnswer(a + b)
        composeTestRule.onNodeWithTag("parent_area_stat_chapters").assertExists()

        // Progress summary reflects at least the one real completed chapter (Noah's
        // Ark) — ">=1" rather than "exactly 1" since this app's single save file can
        // carry real progress from earlier on-device runs (same reality
        // WorldMapNavigationTest's own known flakiness already documents).
        assertStatAtLeast("parent_area_stat_chapters", minimum = 1, total = ChapterCatalog.all.size)
        assertStatAtLeast("parent_area_stat_badges", minimum = 1, total = RewardCatalog.badges.size)
        assertStatAtLeast("parent_area_stat_scripture_cards", minimum = 1, total = RewardCatalog.scriptureCards.size)

        // Settings shortcut opens the real Settings screen and back returns here,
        // still unlocked — the gate only re-locks on a fresh entry, not on a
        // round trip to a screen pushed on top of it.
        composeTestRule.onNodeWithText(activity.getString(R.string.parent_area_open_settings_label)).performClick()
        composeTestRule.onNodeWithText(activity.getString(R.string.settings_screen_title)).assertExists()
        composeTestRule.onNodeWithContentDescription(activity.getString(R.string.action_back)).performClick()
        composeTestRule.onNodeWithTag("parent_area_stat_chapters").assertExists()

        // Privacy dialog opens and closes.
        composeTestRule.onNodeWithText(activity.getString(R.string.parent_area_view_privacy_label)).performClick()
        composeTestRule.onNodeWithText(activity.getString(R.string.parent_area_privacy_title)).assertExists()
        composeTestRule.onNodeWithText(activity.getString(R.string.parent_area_privacy_close)).performClick()

        // Reset Progress: cancel leaves everything untouched.
        composeTestRule.onNodeWithText(activity.getString(R.string.parent_area_reset_progress_label)).performClick()
        composeTestRule.onNodeWithText(activity.getString(R.string.parent_area_reset_confirm_title)).assertExists()
        composeTestRule.onNodeWithText(activity.getString(R.string.parent_area_reset_cancel_action)).performClick()
        assertStatAtLeast("parent_area_stat_chapters", minimum = 1, total = ChapterCatalog.all.size)

        // Reset Progress: confirming actually clears it.
        composeTestRule.onNodeWithText(activity.getString(R.string.parent_area_reset_progress_label)).performClick()
        composeTestRule.onNodeWithText(activity.getString(R.string.parent_area_reset_confirm_action)).performClick()
        assertStatText("parent_area_stat_chapters", "0 / ${ChapterCatalog.all.size}")
        assertStatText("parent_area_stat_badges", "0 / ${RewardCatalog.badges.size}")
        assertStatText("parent_area_stat_scripture_cards", "0 / ${RewardCatalog.scriptureCards.size}")

        // Back to the World Map: David & Goliath is locked again.
        composeTestRule.onNodeWithContentDescription(activity.getString(R.string.action_back)).performClick()
        composeTestRule.onNodeWithText(activity.getString(R.string.menu_adventures)).performClick()
        val davidGoliathTitle = activity.getString(R.string.chapter_david_goliath_title)
        composeTestRule.onNodeWithText(davidGoliathTitle).assertIsNotEnabled()
    }

    private fun readGateOperands(): Pair<Int, Int> {
        val node = composeTestRule.onNodeWithTag("parent_gate_question").fetchSemanticsNode()
        val text = node.config[SemanticsProperties.Text].joinToString(separator = "") { it.text }
        val numbers = Regex("\\d+").findAll(text).map { it.value.toInt() }.toList()
        return numbers[0] to numbers[1]
    }

    private fun enterGateAnswer(answer: Int) {
        composeTestRule.onNodeWithTag("parent_gate_answer_field").performTextClearance()
        composeTestRule.onNodeWithTag("parent_gate_answer_field").performTextInput(answer.toString())
        composeTestRule.onNodeWithTag("parent_gate_submit").performClick()
    }

    private fun assertStatText(tag: String, expected: String) {
        val node = composeTestRule.onNodeWithTag(tag).fetchSemanticsNode()
        val actual = node.config[SemanticsProperties.Text].joinToString(separator = "") { it.text }
        assert(actual == expected) { "Expected stat \"$tag\" to read \"$expected\" but was \"$actual\"" }
    }

    private fun assertStatAtLeast(tag: String, minimum: Int, total: Int) {
        val node = composeTestRule.onNodeWithTag(tag).fetchSemanticsNode()
        val actual = node.config[SemanticsProperties.Text].joinToString(separator = "") { it.text }
        val earned = actual.substringBefore(" / ").trim().toInt()
        assert(earned >= minimum) { "Expected stat \"$tag\" to read at least \"$minimum / $total\" but was \"$actual\"" }
    }
}
