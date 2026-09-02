package com.bibleadventures.davidgoliath

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import com.bibleadventures.MainActivity
import com.bibleadventures.R
import com.bibleadventures.completeNoahsArk
import com.bibleadventures.game.puzzles.rhythmlane.RhythmLaneChart
import com.bibleadventures.game.puzzles.slingshot.SlingshotGameState
import com.bibleadventures.game.stories.DavidGoliathContent
import org.junit.Rule
import org.junit.Test

private const val CONNECT_FOUR_COLUMN_COUNT = 7
private const val CONNECT_FOUR_ROW_COUNT = 6

// ic_goliath_shield.xml's silhouette is narrower than its own bounding box —
// its visible top edge (where the mark's line sits, and what the hit-test
// actually checks) spans x=12..52 of a 64-wide viewport, mirroring
// DavidGoliathSlingPracticeScreen.kt's own SHIELD_TOP_EDGE_*_RATIO constants.
private const val SLING_SHIELD_TOP_EDGE_LEFT_RATIO = 12f / 64f
private const val SLING_SHIELD_TOP_EDGE_RIGHT_RATIO = 52f / 64f

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
        val nextPageLabel = activity.getString(R.string.action_next_page)

        composeTestRule.onNodeWithText(activity.getString(R.string.menu_adventures)).performClick()
        composeTestRule.completeNoahsArk()

        // World Map -> David & Goliath (now unlocked).
        composeTestRule.onNodeWithText(activity.getString(R.string.chapter_david_goliath_title)).performClick()

        // Scene 1: Intro video (Faithful Shepherd).
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // Scene 1b: Count the Sheep — flip every numeral/sheep-group pair.
        DavidGoliathContent.sheepCounts.forEach { count ->
            val name = activity.getString(count.nameRes)
            composeTestRule.onAllNodesWithContentDescription(name)[0].performClick()
            composeTestRule.onAllNodesWithContentDescription(name)[1].performClick()
        }
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // Scene 2: Giant's Challenge video.
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // Scene 3: David Arrives video.
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // Scene 4: Choice — any option is valid.
        composeTestRule.onNodeWithText(activity.getString(R.string.david_goliath_choice_option_1)).performClick()
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // Scene 5: Heavy Armor video.
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // Scene 6: Choose the Stones — a Connect Four match against a simple AI opponent.
        completeChooseStones()
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // Scene 7: Five Smooth Stones video.
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // Scene 8: Cross the Valley — steer David out of each rock's lane
        // before it lands (rhythmlane avoid semantics, requires 3 avoids).
        completeLaneAvoid(
            chart = DavidGoliathContent.crossingValleyChart,
            requiredAvoids = DavidGoliathContent.CROSSING_VALLEY_REQUIRED_AVOIDS,
            titleRes = R.string.david_goliath_dodge_title,
            progressLabelRes = R.string.david_goliath_dodge_progress_label,
            characterContentDescriptionRes = R.string.david_goliath_dodge_character_content_description,
            moveLeftLabelRes = R.string.david_goliath_dodge_move_left_content_description,
            moveRightLabelRes = R.string.david_goliath_dodge_move_right_content_description,
        )
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // Scene 9: Sling Practice — 3 real hits required, the shield relocates
        // to a random different zone after each one.
        completeSlingPractice()

        composeTestRule.onNodeWithText(activity.getString(R.string.feedback_great_job)).assertExists()
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // Scene 10: Victory video.
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // Scene 11: Lesson video (Glory to God).
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // Scene 12: Reward.
        composeTestRule.onNodeWithText(activity.getString(R.string.reward_title)).assertExists()
        composeTestRule.onNodeWithText(activity.getString(R.string.badge_brave_heart_title)).assertExists()
        composeTestRule.onNodeWithText(activity.getString(R.string.action_return_to_map)).performClick()

        // Back on the World Map: David & Goliath completed, Good Samaritan unlocked.
        composeTestRule.onNodeWithText(activity.getString(R.string.world_map_title)).assertExists()
        composeTestRule.onNodeWithText(activity.getString(R.string.chapter_good_samaritan_title)).assertExists()
    }

    /**
     * Crossing the Valley's rhythmlane "avoid" mechanic auto-judges every
     * frame purely from the character's current lane (see
     * `RhythmLaneGame.onLaneAvoided`) — same shape as Feeding the 5,000's
     * Gathering the Leftovers catch mechanic, so it inherits the same
     * implicit-idle-sync unpredictability: Compose's idle-sync (which runs
     * as an ordinary part of `performClick()`, even under
     * `mainClock.autoAdvance = false`) pumps this screen's infinite
     * `withFrameNanos` loop forward by an unpredictable amount before test
     * code regains control, so a schedule computed from an assumed start of
     * 0 would land on the wrong moments.
     *
     * Sidesteps that entirely: freeze the clock, then for each of the 3
     * lanes, park the character there and advance the clock by one full
     * `chart.loopDurationMs` — since every note recurs exactly once per
     * loop, a full-loop dwell in a lane is guaranteed to pass through (and
     * avoid) every note assigned to that lane exactly once, regardless of
     * where in the loop the clock actually started. Progress is read live
     * off the progress-label text after every sweep, so it's also robust to
     * however many "free" avoids already happened before this function got
     * control.
     */
    private fun completeLaneAvoid(
        chart: RhythmLaneChart,
        requiredAvoids: Int,
        titleRes: Int,
        progressLabelRes: Int,
        characterContentDescriptionRes: Int,
        moveLeftLabelRes: Int,
        moveRightLabelRes: Int,
    ) {
        val activity = composeTestRule.activity
        val lanes = chart.notes.map { it.lane }.distinct().sorted()

        // Let the screen fully compose (with the clock still auto-advancing)
        // before freezing it — freezing immediately after navigating can
        // catch the new screen before its first frame lands, so even static
        // elements like the progress label aren't in the semantics tree yet.
        composeTestRule.onNodeWithText(activity.getString(titleRes)).assertExists()

        composeTestRule.mainClock.autoAdvance = false
        var safetyRounds = 0
        while (currentLaneAvoidHits(progressLabelRes, requiredAvoids) < requiredAvoids) {
            check(safetyRounds++ < 20) { "Lane-avoid puzzle didn't reach $requiredAvoids avoids after 20 full sweep rounds — stuck at ${currentLaneAvoidHits(progressLabelRes, requiredAvoids)}" }
            lanes.forEach { lane ->
                if (currentLaneAvoidHits(progressLabelRes, requiredAvoids) < requiredAvoids) {
                    moveCharacterToLane(lane, characterContentDescriptionRes, moveLeftLabelRes, moveRightLabelRes)
                    composeTestRule.mainClock.advanceTimeBy(chart.loopDurationMs)
                }
            }
        }
        composeTestRule.mainClock.autoAdvance = true
    }

    private fun currentLaneAvoidHits(progressLabelRes: Int, requiredAvoids: Int): Int {
        val activity = composeTestRule.activity
        return (0..requiredAvoids).first { candidateHits ->
            val label = activity.getString(progressLabelRes, candidateHits, requiredAvoids)
            composeTestRule.onAllNodesWithText(label).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun currentCharacterLane(characterContentDescriptionRes: Int): Int {
        val activity = composeTestRule.activity
        return (1..3).first { candidateLane ->
            val label = activity.getString(characterContentDescriptionRes, candidateLane)
            composeTestRule.onAllNodesWithContentDescription(label).fetchSemanticsNodes().isNotEmpty()
        } - 1
    }

    private fun moveCharacterToLane(targetLane: Int, characterContentDescriptionRes: Int, moveLeftLabelRes: Int, moveRightLabelRes: Int) {
        val activity = composeTestRule.activity
        val moveLeftLabel = activity.getString(moveLeftLabelRes)
        val moveRightLabel = activity.getString(moveRightLabelRes)

        while (currentCharacterLane(characterContentDescriptionRes) != targetLane) {
            val label = if (currentCharacterLane(characterContentDescriptionRes) < targetLane) moveRightLabel else moveLeftLabel
            composeTestRule.onNodeWithContentDescription(label).performClick()
        }
    }

    /**
     * Sling Practice's target mark animates continuously with no
     * time-based stopping condition reachable on its own (unlike Crossing
     * the Valley/Hurrying to Pray, which self-complete given enough elapsed
     * time even with zero player input) — so, unlike [completeLaneAvoid],
     * querying semantics while the clock auto-advances can never reach
     * idle here; the mark's `LaunchedEffect` has nothing that would ever
     * let it stop on its own. Freezes the clock as the very first thing
     * this function does (once already safely on this screen via an
     * ordinary, un-frozen navigating click), then drives the mark forward
     * in small deterministic steps via `advanceTimeBy` — reading the
     * mark's *actual* rendered position after each step (derived from the
     * shield image's own rendered bounds via the same top-edge ratios
     * `DavidGoliathSlingPracticeScreen.kt` uses to pick its hit test's true
     * perimeter, not the image's wider, partly transparent bounding box)
     * and dragging the stone onto it the moment it's within the shield's
     * true span. Repeats until [SlingshotGameState.requiredHits] real hits
     * land (a miss never loses progress, per SlingshotGame's own design,
     * and the shield relocates after every hit, so re-reading it live on
     * each step is required, not just once).
     */
    private fun completeSlingPractice() {
        val activity = composeTestRule.activity
        val markDescription = activity.getString(R.string.david_goliath_sling_target_mark_content_description)
        val stoneDescription = activity.getString(R.string.david_goliath_sling_stone_content_description)
        val shieldDescriptionPrefix = activity.getString(R.string.david_goliath_sling_shield_content_description, "")
        val requiredHits = SlingshotGameState().requiredHits

        composeTestRule.mainClock.autoAdvance = false
        // One explicit frame to let this screen's first composition (and
        // its progress label) land before any query — freezing the clock
        // doesn't itself wait for anything to compose.
        composeTestRule.mainClock.advanceTimeByFrame()

        var safetySteps = 0
        while (currentSlingHits(requiredHits) < requiredHits) {
            check(safetySteps++ < 1500) { "Sling Practice didn't reach $requiredHits hits after 1500 clock steps — stuck at ${currentSlingHits(requiredHits)}" }

            val markBounds = composeTestRule.onNodeWithContentDescription(markDescription).fetchSemanticsNode().boundsInRoot
            val shieldImageBounds = composeTestRule.onNodeWithContentDescription(shieldDescriptionPrefix, substring = true).fetchSemanticsNode().boundsInRoot
            val shieldTrueLeft = shieldImageBounds.left + SLING_SHIELD_TOP_EDGE_LEFT_RATIO * shieldImageBounds.width
            val shieldTrueRight = shieldImageBounds.left + SLING_SHIELD_TOP_EDGE_RIGHT_RATIO * shieldImageBounds.width

            if (markBounds.center.x in shieldTrueLeft..shieldTrueRight) {
                val stoneNode = composeTestRule.onNodeWithContentDescription(stoneDescription)
                dragOntoContentDescription(itemNode = stoneNode, targetContentDescription = markDescription)
            } else {
                composeTestRule.mainClock.advanceTimeBy(50L)
            }
        }

        composeTestRule.mainClock.autoAdvance = true
    }

    private fun currentSlingHits(requiredHits: Int): Int {
        val activity = composeTestRule.activity
        return (0..requiredHits).first { candidateHits ->
            val label = activity.getString(R.string.david_goliath_sling_practice_progress_label, candidateHits, requiredHits)
            composeTestRule.onAllNodesWithText(label).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /**
     * "Choose the Stones" is the one puzzle in the app with a real loss
     * condition (an AI opponent) — a single fixed column-tap script loses to
     * it every time (verified separately: a blind policy's win rate is 0%,
     * since the AI blocks any predictable line before it completes). Reading
     * each cell's own live content description (see `ConnectFourCell` in
     * `DavidGoliathChooseStonesScreen.kt`) lets this mirror the AI's own
     * win-then-block-then-center heuristic from the player's side, which
     * wins roughly 60% of matches in isolation — retried here across up to
     * [maxAttempts] full matches (a loss/draw leaves the board on screen
     * until "Try Again" is tapped, per the user's on-device feedback that an
     * auto-reset felt like it was taking the board away from them) drives
     * the chance of never winning to effectively zero.
     */
    private fun completeChooseStones(maxAttempts: Int = 20, maxTurnsPerAttempt: Int = 30) {
        val activity = composeTestRule.activity
        val playerWonLabel = activity.getString(R.string.david_goliath_choose_stones_player_won)
        val opponentWonLabel = activity.getString(R.string.david_goliath_choose_stones_opponent_won)
        val drawLabel = activity.getString(R.string.david_goliath_choose_stones_draw)
        val yourTurnLabel = activity.getString(R.string.david_goliath_choose_stones_your_turn)
        val tryAgainLabel = activity.getString(R.string.david_goliath_choose_stones_try_again)

        for (attempt in 0 until maxAttempts) {
            turns@ for (turn in 0 until maxTurnsPerAttempt) {
                if (composeTestRule.onAllNodesWithText(playerWonLabel).fetchSemanticsNodes().isNotEmpty()) return
                if (composeTestRule.onAllNodesWithText(opponentWonLabel).fetchSemanticsNodes().isNotEmpty() ||
                    composeTestRule.onAllNodesWithText(drawLabel).fetchSemanticsNodes().isNotEmpty()
                ) {
                    composeTestRule.onNodeWithText(tryAgainLabel).performClick()
                    break@turns
                }
                if (composeTestRule.onAllNodesWithText(yourTurnLabel).fetchSemanticsNodes().isNotEmpty()) {
                    val column = chooseConnectFourColumn()
                    val columnLabel = activity.getString(R.string.david_goliath_choose_stones_column_content_description, column + 1)
                    composeTestRule.onNodeWithContentDescription(columnLabel).performClick()
                }
                composeTestRule.waitForIdle()
            }
        }
        composeTestRule.onNodeWithText(playerWonLabel).assertExists()
    }

    private enum class ConnectFourTestSlot { EMPTY, PLAYER, OPPONENT }

    private fun connectFourCell(column: Int, row: Int): ConnectFourTestSlot {
        val activity = composeTestRule.activity
        val emptyLabel = activity.getString(
            R.string.david_goliath_choose_stones_cell_content_description,
            column + 1, row + 1, activity.getString(R.string.david_goliath_choose_stones_cell_empty),
        )
        if (composeTestRule.onAllNodesWithContentDescription(emptyLabel).fetchSemanticsNodes().isNotEmpty()) return ConnectFourTestSlot.EMPTY
        val playerLabel = activity.getString(
            R.string.david_goliath_choose_stones_cell_content_description,
            column + 1, row + 1, activity.getString(R.string.david_goliath_choose_stones_cell_player),
        )
        if (composeTestRule.onAllNodesWithContentDescription(playerLabel).fetchSemanticsNodes().isNotEmpty()) return ConnectFourTestSlot.PLAYER
        return ConnectFourTestSlot.OPPONENT
    }

    private fun connectFourLowestEmptyRow(column: Int): Int? =
        (0 until CONNECT_FOUR_ROW_COUNT).firstOrNull { row -> connectFourCell(column, row) == ConnectFourTestSlot.EMPTY }

    private fun connectFourWouldWin(column: Int, slot: ConnectFourTestSlot): Boolean {
        val row = connectFourLowestEmptyRow(column) ?: return false
        fun cellAt(c: Int, r: Int): ConnectFourTestSlot = if (c == column && r == row) slot else connectFourCell(c, r)

        return listOf(0 to 1, 1 to 0, 1 to 1, 1 to -1).any { (dr, dc) ->
            var count = 1
            var r = row + dr
            var c = column + dc
            while (r in 0 until CONNECT_FOUR_ROW_COUNT && c in 0 until CONNECT_FOUR_COLUMN_COUNT && cellAt(c, r) == slot) {
                count++; r += dr; c += dc
            }
            r = row - dr
            c = column - dc
            while (r in 0 until CONNECT_FOUR_ROW_COUNT && c in 0 until CONNECT_FOUR_COLUMN_COUNT && cellAt(c, r) == slot) {
                count++; r -= dr; c -= dc
            }
            count >= 4
        }
    }

    /** Mirrors ConnectFourGame's own AI heuristic (win, else block, else center-ish) from the player's side. */
    private fun chooseConnectFourColumn(): Int {
        val validColumns = (0 until CONNECT_FOUR_COLUMN_COUNT).filter { connectFourLowestEmptyRow(it) != null }
        validColumns.firstOrNull { connectFourWouldWin(it, ConnectFourTestSlot.PLAYER) }?.let { return it }
        validColumns.firstOrNull { connectFourWouldWin(it, ConnectFourTestSlot.OPPONENT) }?.let { return it }
        val center = (CONNECT_FOUR_COLUMN_COUNT - 1) / 2
        return validColumns.minBy { kotlin.math.abs(it - center) }
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
