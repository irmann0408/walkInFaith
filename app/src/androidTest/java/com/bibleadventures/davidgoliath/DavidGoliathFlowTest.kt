package com.bibleadventures.davidgoliath

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
import com.bibleadventures.MainActivity
import com.bibleadventures.R
import com.bibleadventures.game.puzzles.dodge.DodgeLane
import com.bibleadventures.game.stories.DavidGoliathContent
import com.bibleadventures.game.stories.NoahsArkContent
import org.junit.Rule
import org.junit.Test

/**
 * Walks the full David and Goliath adventure end to end. David & Goliath is
 * locked until Noah's Ark is completed, and this device's save data can carry
 * real state over between test runs (see docs/PROJECT_STATUS.md's Milestone 5
 * "Continue Adventure" note) — so this test completes Noah's Ark itself first
 * rather than assuming it's already done, to stay deterministic regardless of
 * what ran before it.
 */
class DavidGoliathFlowTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun completingDavidAndGoliath_awardsStarsAndUnlocksGoodSamaritanOnTheWorldMap() {
        val activity = composeTestRule.activity
        val continueLabel = activity.getString(R.string.action_continue)

        composeTestRule.onNodeWithText(activity.getString(R.string.menu_adventures)).performClick()
        completeNoahsArk(continueLabel)

        // World Map -> David & Goliath (now unlocked).
        composeTestRule.onNodeWithText(activity.getString(R.string.chapter_david_goliath_title)).performClick()

        // Scene 1: Intro.
        composeTestRule.onNodeWithText(continueLabel).performClick()

        // Scene 1b: Counting the Flock context card.
        composeTestRule.onNodeWithText(continueLabel).performClick()

        // Scene 1c: Count the Sheep — flip every numeral/sheep-group pair.
        DavidGoliathContent.sheepCounts.forEach { count ->
            val name = activity.getString(count.nameRes)
            composeTestRule.onAllNodesWithContentDescription(name)[0].performClick()
            composeTestRule.onAllNodesWithContentDescription(name)[1].performClick()
        }
        composeTestRule.onNodeWithText(continueLabel).performClick()

        // Scene 1d: Choose the Stones context card.
        composeTestRule.onNodeWithText(continueLabel).performClick()

        // Scene 2: Choose the Stones. The decoy (an old boot) is deliberately left untapped.
        DavidGoliathContent.stones.forEach { stone ->
            composeTestRule.onNodeWithContentDescription(activity.getString(stone.nameRes)).performClick()
        }
        composeTestRule.onNodeWithText(continueLabel).performClick()

        // Scene 2b: Sling Practice context card.
        composeTestRule.onNodeWithText(continueLabel).performClick()

        // Scene 3: Choice — any option is valid.
        composeTestRule.onNodeWithText(activity.getString(R.string.david_goliath_choice_option_1)).performClick()
        composeTestRule.onNodeWithText(continueLabel).performClick()

        // Scene 3b: Crossing the Valley context card.
        composeTestRule.onNodeWithText(continueLabel).performClick()

        // Scene 3c: Cross the Valley — step to the safe lane for every beat. Fully
        // static/self-paced (no clock to freeze), so this is a plain tap sequence.
        val leftLabel = activity.getString(R.string.david_goliath_dodge_lane_left)
        val rightLabel = activity.getString(R.string.david_goliath_dodge_lane_right)
        DavidGoliathContent.dodgeBeats.forEach { beat ->
            val safeLabel = if (beat.hazardLane == DodgeLane.LEFT) rightLabel else leftLabel
            composeTestRule.onNodeWithText(safeLabel).performClick()
        }
        composeTestRule.onNodeWithText(continueLabel).performClick()

        // Scene 4: Sling Practice. Let the screen fully compose (with the clock
        // still auto-advancing) before freezing it — freezing immediately after
        // navigating can catch the new screen before its first frame lands, so
        // even static elements like the stone aren't in the semantics tree yet.
        composeTestRule.onNodeWithText(activity.getString(R.string.david_goliath_sling_practice_title)).assertExists()

        // The mark moves on a real-time animation, so the clock is frozen and the
        // drag aims exactly at wherever it's currently frozen — guarantees a
        // deterministic hit regardless of animation phase or CI timing, with no
        // production-code changes needed to support it. Confirmed on-device:
        // rememberInfiniteTransition-based animations don't progress once the
        // test clock is frozen (a known Compose testing limitation, not
        // something this codebase can work around from the test side) — they
        // reliably stay at their initialValue (MARK_MIN_FRACTION) the whole
        // time. The shield's fractional bounds in
        // DavidGoliathSlingPracticeScreen.kt were positioned to genuinely
        // include that value for exactly this reason, so this simple
        // freeze-and-drag technique still produces a real hit.
        composeTestRule.mainClock.autoAdvance = false
        val markDescription = activity.getString(R.string.david_goliath_sling_target_mark_content_description)
        val stoneDescription = activity.getString(R.string.david_goliath_sling_stone_content_description)
        val stoneNode = composeTestRule.onNodeWithContentDescription(stoneDescription)
        dragOntoContentDescription(itemNode = stoneNode, targetContentDescription = markDescription)
        composeTestRule.mainClock.autoAdvance = true

        composeTestRule.onNodeWithText(activity.getString(R.string.feedback_great_job)).assertExists()
        composeTestRule.onNodeWithText(continueLabel).performClick()

        // Scene 5: Lesson.
        composeTestRule.onNodeWithText(activity.getString(R.string.david_goliath_lesson_title)).assertExists()
        composeTestRule.onNodeWithText(continueLabel).performClick()

        // Scene 6: Reward.
        composeTestRule.onNodeWithText(activity.getString(R.string.reward_title)).assertExists()
        composeTestRule.onNodeWithText(activity.getString(R.string.badge_brave_heart_title)).assertExists()
        composeTestRule.onNodeWithText(activity.getString(R.string.action_return_to_map)).performClick()

        // Back on the World Map: David & Goliath completed, Good Samaritan unlocked.
        composeTestRule.onNodeWithText(activity.getString(R.string.world_map_title)).assertExists()
        composeTestRule.onNodeWithText(activity.getString(R.string.chapter_good_samaritan_title)).assertExists()
    }

    /** Walks Noah's Ark end to end (mirrors NoahsArkFlowTest) so David & Goliath unlocks. */
    private fun completeNoahsArk(continueLabel: String) {
        val activity = composeTestRule.activity

        composeTestRule.onNodeWithText(activity.getString(R.string.chapter_noahs_ark_title)).performClick()

        composeTestRule.onNodeWithText(continueLabel).performClick() // Intro
        composeTestRule.onNodeWithText(continueLabel).performClick() // Find Animals context

        NoahsArkContent.animals.forEach { animal ->
            composeTestRule.onAllNodesWithContentDescription(activity.getString(animal.nameRes))[0].performClick()
        }
        composeTestRule.onNodeWithText(continueLabel).performClick()

        NoahsArkContent.animals.forEach { animal ->
            val name = activity.getString(animal.nameRes)
            composeTestRule.onAllNodesWithContentDescription(name)[0].performClick()
            composeTestRule.onAllNodesWithContentDescription(name)[1].performClick()
        }
        composeTestRule.onNodeWithText(continueLabel).performClick()

        composeTestRule.onNodeWithText(continueLabel).performClick() // Organize the Ark context

        NoahsArkContent.sortableItems.filter { it.categoryKey != null }.forEach { item ->
            val itemName = activity.getString(item.nameRes)
            val categoryLabelRes = NoahsArkContent.sortCategories.first { it.key == item.categoryKey }.labelRes
            val categoryLabel = activity.getString(categoryLabelRes)
            dragOntoText(itemNode = composeTestRule.onNodeWithContentDescription(itemName), targetText = categoryLabel)
        }
        composeTestRule.onNodeWithText(continueLabel).performClick()

        NoahsArkContent.hiddenItems.forEach { item ->
            composeTestRule.onNodeWithContentDescription(activity.getString(item.nameRes)).performClick()
        }
        composeTestRule.onNodeWithText(continueLabel).performClick()

        composeTestRule.onNodeWithText(continueLabel).performClick() // Lesson

        composeTestRule.onNodeWithText(activity.getString(R.string.action_return_to_map)).performClick()
    }

    private fun dragOntoText(itemNode: SemanticsNodeInteraction, targetText: String) {
        val itemBounds = itemNode.fetchSemanticsNode().boundsInRoot
        val targetBounds = composeTestRule.onNodeWithText(targetText).fetchSemanticsNode().boundsInRoot
        val targetGlobalCenter = targetBounds.center
        val localEnd = Offset(targetGlobalCenter.x - itemBounds.left, targetGlobalCenter.y - itemBounds.top)

        itemNode.performTouchInput {
            swipe(start = center, end = localEnd, durationMillis = 200)
        }
    }

    private fun dragOntoContentDescription(itemNode: SemanticsNodeInteraction, targetContentDescription: String) {
        val itemBounds = itemNode.fetchSemanticsNode().boundsInRoot
        val targetBounds = composeTestRule.onNodeWithContentDescription(targetContentDescription).fetchSemanticsNode().boundsInRoot
        val targetGlobalCenter = targetBounds.center
        val localEnd = Offset(targetGlobalCenter.x - itemBounds.left, targetGlobalCenter.y - itemBounds.top)

        itemNode.performTouchInput {
            swipe(start = center, end = localEnd, durationMillis = 200)
        }
    }
}
