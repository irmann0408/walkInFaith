package com.bibleadventures.goodsamaritan

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import com.bibleadventures.MainActivity
import com.bibleadventures.R
import com.bibleadventures.completeDavidGoliath
import com.bibleadventures.completeExploreDungeon
import com.bibleadventures.completeNoahsArk
import com.bibleadventures.game.puzzles.roadblock.Direction as RoadblockDirection
import com.bibleadventures.game.stories.GoodSamaritanContent
import org.junit.Rule
import org.junit.Test

/**
 * Walks the full Good Samaritan adventure end to end. It's locked until
 * David and Goliath is completed, which is itself locked until Noah's Ark
 * is completed — and this device's save data persists real state across
 * test runs — so this test completes both prerequisite chapters itself
 * rather than assuming they're already done, to stay deterministic
 * regardless of what ran before it (same pattern as DavidGoliathFlowTest).
 */
class GoodSamaritanFlowTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun completingGoodSamaritan_awardsStarsAndUnlocksDanielOnTheWorldMap() {
        val activity = composeTestRule.activity
        val continueLabel = activity.getString(R.string.action_continue)
        val nextPageLabel = activity.getString(R.string.action_next_page)

        composeTestRule.onNodeWithText(activity.getString(R.string.menu_adventures)).performClick()
        composeTestRule.completeNoahsArk()
        composeTestRule.completeDavidGoliath()

        // World Map -> Good Samaritan (now unlocked).
        composeTestRule.onNodeWithText(activity.getString(R.string.chapter_good_samaritan_title)).performClick()

        // Scene 1: Dangerous Road video.
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // Scene 2: Passing By — a sliding-block puzzle, solved with a hand-verified move sequence.
        completePassingBy()
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // Scene 3: The Priest video.
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // Scene 4: The Levite video.
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // Scene 5: Explore — a real-time "mini dungeon" now (analog joystick,
        // bandit encounters, medical-supply resource economy), steered via
        // ChapterFlowHelpers.completeExploreDungeon() rather than a private
        // copy here — see that function's own doc comment for why this one
        // mechanic is a deliberate exception to this file's usual
        // "thorough test keeps its own copy" convention.
        composeTestRule.completeExploreDungeon()
        val helpingBeatTitle = activity.getString(R.string.good_samaritan_helping_beat_title)
        if (composeTestRule.onAllNodesWithText(helpingBeatTitle).fetchSemanticsNodes().isNotEmpty()) {
            // This "Continue" belongs to HelpingBeatOverlay, a full-screen dialog.
            composeTestRule.onNodeWithText(continueLabel).performClick()
        }

        composeTestRule.onNodeWithText(nextPageLabel).performClick() // Explore -> Samaritan Arrives video

        // Scene 6: Samaritan Arrives video.
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // Scene 7: Reward.
        composeTestRule.onNodeWithText(activity.getString(R.string.reward_title)).assertExists()
        composeTestRule.onNodeWithText(activity.getString(R.string.badge_good_neighbor_title)).assertExists()
        composeTestRule.onNodeWithText(activity.getString(R.string.action_return_to_map)).performClick()

        // Back on the World Map: Good Samaritan completed, Daniel and the Lions unlocked.
        composeTestRule.onNodeWithText(activity.getString(R.string.world_map_title)).assertExists()
        composeTestRule.onNodeWithText(activity.getString(R.string.chapter_daniel_title)).assertExists()
    }

    /**
     * Replays every one of GoodSamaritanContent.passingByLevels' own hand-
     * verified `solution`s as real drag gestures, in order, tapping the
     * character itself between levels to advance in place (see
     * GoodSamaritanViewModel.onPassingByNextLevel) — "Next Page" is
     * reserved for actually leaving this scene once the last level is
     * solved (the caller's own job, same as every other transition in this
     * flow), per on-device feedback that reusing it for "advance to the
     * next of the 4 puzzles" too read as leaving the scene early every time
     * a level finished. Every non-target tile in a level now shares one
     * visible label (that level's own spotlighted excuse), so a specific
     * tile is found by its `Modifier.testTag(block.id)` instead of by
     * content description, which is no longer unique enough to
     * disambiguate. The exit gate's rendered width — a single grid cell —
     * gives the exact pixels-per-cell needed to turn a hand-verified
     * (direction, distance) move into a real swipe; re-queried fresh per
     * level since each level's board has different dimensions.
     */
    private fun completePassingBy() {
        val activity = composeTestRule.activity
        val nextLevelDescription = activity.getString(R.string.good_samaritan_passing_by_next_level_content_description)

        GoodSamaritanContent.passingByLevels.forEachIndexed { index, level ->
            val gateBounds = composeTestRule
                .onNodeWithContentDescription(activity.getString(R.string.good_samaritan_passing_by_exit_gate_content_description))
                .fetchSemanticsNode()
                .boundsInRoot
            val cellSizePx = gateBounds.width

            level.solution.forEach { move ->
                val magnitude = cellSizePx * move.distance
                val delta = when (move.direction) {
                    RoadblockDirection.UP -> Offset(0f, -magnitude)
                    RoadblockDirection.DOWN -> Offset(0f, magnitude)
                    RoadblockDirection.LEFT -> Offset(-magnitude, 0f)
                    RoadblockDirection.RIGHT -> Offset(magnitude, 0f)
                }
                composeTestRule.onNodeWithTag(move.blockId).performTouchInput {
                    swipe(start = center, end = center + delta, durationMillis = 200)
                }
            }

            if (index != GoodSamaritanContent.passingByLevels.lastIndex) {
                composeTestRule.onNodeWithContentDescription(nextLevelDescription).performClick()
            }
        }
    }
}
