package com.bibleadventures.davidgoliath

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
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
import com.bibleadventures.game.puzzles.slingshot.SlingshotGameState
import com.bibleadventures.game.stories.DavidGoliathContent
import com.bibleadventures.ui.screens.davidgoliath.slingpractice.ANCHOR_X_FRACTION
import com.bibleadventures.ui.screens.davidgoliath.slingpractice.ANCHOR_Y_FRACTION
import com.bibleadventures.ui.screens.davidgoliath.slingpractice.FLIGHT_DURATION_MS
import com.bibleadventures.ui.screens.davidgoliath.slingpractice.RatElapsedMsKey
import com.bibleadventures.ui.screens.davidgoliath.slingpractice.ratXFractionAt
import com.bibleadventures.ui.screens.davidgoliath.slingpractice.ratYFractionAt
import org.junit.Rule
import org.junit.Test

private const val CONNECT_FOUR_COLUMN_COUNT = 7
private const val CONNECT_FOUR_ROW_COUNT = 6

/** Comfortably longer than the screen's own (private) flight-animation duration, so the deferred state update always lands before the next check. */
private const val SLING_FLIGHT_SETTLE_MS = 500L

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

        // Scene 8: Sling Practice — 5 rats, one at a time; hit as many as
        // possible before each one reaches the bottom.
        completeSlingPractice()
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // Scene 9: Victory video.
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // Scene 10: Lesson video (Glory to God).
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // Scene 11: Reward.
        composeTestRule.onNodeWithText(activity.getString(R.string.reward_title)).assertExists()
        composeTestRule.onNodeWithText(activity.getString(R.string.badge_brave_heart_title)).assertExists()
        composeTestRule.onNodeWithText(activity.getString(R.string.action_return_to_map)).performClick()

        // Back on the World Map: David & Goliath completed, Good Samaritan unlocked.
        composeTestRule.onNodeWithText(activity.getString(R.string.world_map_title)).assertExists()
        composeTestRule.onNodeWithText(activity.getString(R.string.chapter_good_samaritan_title)).assertExists()
    }

    /**
     * `SlingshotGame` now launches the stone *opposite* the pull (pull
     * southwest, it flies northeast) — the rat itself is still the only
     * moving reference the hit-test cares about, but hitting it means
     * pulling the stone to the *mirror image* of the rat's position
     * through the sling's anchor, not dragging onto the rat directly. The
     * stone always rests exactly on that anchor point when not being
     * dragged, so reading the stone's own resting bounds gives the anchor
     * for free. Freezes the clock as the very first thing this function
     * does (once already safely on this screen via an ordinary, un-frozen
     * navigating click) so `elapsedMs` never advances mid-gesture, then
     * computes and drags to that mirror point each time. The actual
     * hit/miss isn't committed to game state until the screen's own
     * cosmetic flight animation finishes (so a hit visibly lands on the
     * rat, not just flies off in the right direction) —
     * [SLING_FLIGHT_SETTLE_MS] advances the clock past that animation
     * before checking progress again, and dragging is a no-op while a shot
     * is still resolving. An escaped rat doesn't count toward completion
     * at all (free practice), so this loop only stops once every required
     * hit has actually landed — it never deliberately lets a rat escape,
     * so that path is only covered by the unit tests.
     */
    private fun completeSlingPractice() {
        val activity = composeTestRule.activity
        val ratDescription = activity.getString(R.string.david_goliath_sling_rat_content_description)
        val stoneDescription = activity.getString(R.string.david_goliath_sling_stone_content_description)
        val requiredHits = SlingshotGameState().requiredHits

        composeTestRule.mainClock.autoAdvance = false
        // One explicit frame to let this screen's first composition (and
        // its progress label) land before any query — freezing the clock
        // doesn't itself wait for anything to compose.
        composeTestRule.mainClock.advanceTimeByFrame()

        var safetySteps = 0
        while (currentSlingHits(requiredHits) < requiredHits) {
            check(safetySteps++ < 200) { "Sling Practice didn't reach $requiredHits hits after 200 clock steps — stuck at ${currentSlingHits(requiredHits)}" }

            if (composeTestRule.onAllNodesWithContentDescription(ratDescription).fetchSemanticsNodes().isEmpty()) {
                // Between one rat resolving and the next rat's first frame landing.
                composeTestRule.mainClock.advanceTimeByFrame()
                continue
            }
            dragStoneOppositeOfRat(stoneDescription, ratDescription)
            composeTestRule.mainClock.advanceTimeBy(SLING_FLIGHT_SETTLE_MS)
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
     * The rat keeps moving while a stone is in flight, so the game itself
     * leads the shot — it resolves against the rat's *projected* position
     * at `elapsedMs + FLIGHT_DURATION_MS`, not where it stood at release
     * (see `DavidGoliathSlingPracticeScreen.kt`'s `onDragEnd`). This
     * mirrors that exactly: reads the rat's live `elapsedMs` off its own
     * semantics (exposed test-only via `RatElapsedMsKey`, never read
     * aloud), computes the same projected fractional position via the
     * screen's own `ratXFractionAt`/`ratYFractionAt` (made `internal`
     * specifically so a test can reuse the identical math), converts that
     * into root pixel coordinates using the rat's own current
     * (position, fraction) pair as a scale reference, and pulls the stone
     * to the *mirror image* of that projected point through the stone's
     * own resting point (the sling's anchor) — since the launch direction
     * is the pull, reversed, this lines the eventual shot up on the rat.
     */
    private fun dragStoneOppositeOfRat(stoneDescription: String, ratDescription: String) {
        val ratNode = composeTestRule.onNodeWithContentDescription(ratDescription).fetchSemanticsNode()
        val currentElapsedMs = ratNode.config[RatElapsedMsKey]
        val impactElapsedMs = currentElapsedMs + FLIGHT_DURATION_MS

        val currentRatFraction = Offset(ratXFractionAt(currentElapsedMs), ratYFractionAt(currentElapsedMs))
        val projectedRatFraction = Offset(ratXFractionAt(impactElapsedMs), ratYFractionAt(impactElapsedMs))
        val ratPixelCenter = ratNode.boundsInRoot.center

        val stoneNode = composeTestRule.onNodeWithContentDescription(stoneDescription)
        val stoneBounds = stoneNode.fetchSemanticsNode().boundsInRoot
        val anchorCenter = stoneBounds.center // the stone rests exactly on the sling's anchor when not being dragged
        val anchorFraction = Offset(ANCHOR_X_FRACTION, ANCHOR_Y_FRACTION)

        // The track is square (AspectRatioFitBox ratio = 1f), so one scale
        // (derived from the axis less likely to sit exactly on the
        // anchor's own fraction, avoiding a near-zero divide) applies to
        // both.
        val pixelsPerFraction = (anchorCenter.y - ratPixelCenter.y) / (anchorFraction.y - currentRatFraction.y)
        val projectedRatPixelCenter = Offset(
            anchorCenter.x + (projectedRatFraction.x - anchorFraction.x) * pixelsPerFraction,
            anchorCenter.y + (projectedRatFraction.y - anchorFraction.y) * pixelsPerFraction,
        )

        val targetGlobalCenter = Offset(2 * anchorCenter.x - projectedRatPixelCenter.x, 2 * anchorCenter.y - projectedRatPixelCenter.y)
        val localEnd = Offset(targetGlobalCenter.x - stoneBounds.left, targetGlobalCenter.y - stoneBounds.top)

        stoneNode.performTouchInput {
            swipe(start = center, end = localEnd, durationMillis = 200)
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

}
