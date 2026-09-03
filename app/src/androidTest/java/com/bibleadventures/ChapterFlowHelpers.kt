package com.bibleadventures

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.bibleadventures.game.puzzles.gridmaze.Direction
import com.bibleadventures.game.puzzles.roadblock.Direction as RoadblockDirection
import com.bibleadventures.game.stories.DavidGoliathContent
import com.bibleadventures.game.stories.GoodSamaritanContent
import com.bibleadventures.game.stories.NoahsArkContent
import com.bibleadventures.ui.screens.davidgoliath.slingpractice.ANCHOR_X_FRACTION
import com.bibleadventures.ui.screens.davidgoliath.slingpractice.ANCHOR_Y_FRACTION
import com.bibleadventures.ui.screens.davidgoliath.slingpractice.FLIGHT_DURATION_MS
import com.bibleadventures.ui.screens.davidgoliath.slingpractice.RatElapsedMsKey
import com.bibleadventures.ui.screens.davidgoliath.slingpractice.ratXFractionAt
import com.bibleadventures.ui.screens.davidgoliath.slingpractice.ratYFractionAt

/**
 * Shared prerequisite-completion helpers for `connectedAndroidTest` flow
 * tests. Every chapter after the first depends on its predecessors being
 * completed first to unlock, and previously each chapter's own flow test
 * file carried its own private copy of `completeNoahsArk()`/
 * `completeDavidGoliath()` — 9 and 6 copies respectively. When Noah's Ark
 * and David & Goliath were restructured into video-narrated chapters this
 * session, only the two chapters' own "thorough" flow test files were
 * updated; the other 7-8 duplicate copies silently went stale and broke
 * compilation. Centralizing them here means a future content change only
 * needs one update, not a search-and-fix across every chapter's test file.
 *
 * `NoahsArkFlowTest`/`DavidGoliathFlowTest` still keep their own richer,
 * from-scratch walkthroughs (with chapter-specific assertions on rewards,
 * badges, feedback text, etc.) rather than calling these — same
 * established "thorough test vs. shared prerequisite helper" split already
 * used for chapters like Esther/Jericho ("see XFlowTest for the thorough
 * walkthrough ... this only needs to clear it as a prerequisite").
 */
typealias FlowTestRule = AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>

/** The World Map is a `LazyColumn` — scrolling to a chapter that's already visible is a harmless no-op. */
private fun FlowTestRule.scrollToChapterOnWorldMap(title: String) {
    onNodeWithTag("world_map_chapter_list").performScrollToNode(hasText(title))
}

/**
 * Walks Noah's Ark end to end: World Map -> Intro video -> Find the Tools ->
 * Building the Ark video -> Animal Matching -> Animals Entering video ->
 * Load the Ark -> Great Flood video -> Dove and Land video -> Rainbow
 * Promise video -> Lesson video -> Reward -> back to the World Map. Assumes
 * the caller is already on the World Map screen.
 */
fun FlowTestRule.completeNoahsArk() {
    val activity = this.activity
    val nextPageLabel = activity.getString(R.string.action_next_page)

    scrollToChapterOnWorldMap(activity.getString(R.string.chapter_noahs_ark_title))
    onNodeWithText(activity.getString(R.string.chapter_noahs_ark_title)).performClick()

    onNodeWithText(nextPageLabel).performClick() // Intro video -> Find the Tools

    // Each tool hotspot appears twice (see NoahsArkContent.findToolsHotspots);
    // its semantics node stays in the tree (just disabled) once found, so
    // indexing [0] then [1] addresses the two distinct instances rather than
    // re-tapping the same one twice.
    NoahsArkContent.findToolsHotspots.map { it.nameRes }.distinct().forEach { nameRes ->
        val name = activity.getString(nameRes)
        onAllNodesWithContentDescription(name)[0].performClick()
        onAllNodesWithContentDescription(name)[1].performClick()
    }
    onNodeWithText(nextPageLabel).performClick() // Find the Tools -> Building the Ark video
    onNodeWithText(nextPageLabel).performClick() // Building the Ark video -> Animal Matching

    NoahsArkContent.animals.forEach { animal ->
        val name = activity.getString(animal.nameRes)
        onAllNodesWithContentDescription(name)[0].performClick()
        onAllNodesWithContentDescription(name)[1].performClick()
    }
    onNodeWithText(nextPageLabel).performClick() // Animal Matching -> Animals Entering video
    onNodeWithText(nextPageLabel).performClick() // Animals Entering video -> Load the Ark

    completeLoadTheArk()
    onNodeWithText(nextPageLabel).performClick() // Load the Ark -> Great Flood video
    onNodeWithText(nextPageLabel).performClick() // Great Flood video -> Dove and Land video
    onNodeWithText(nextPageLabel).performClick() // Dove and Land video -> Rainbow Promise video
    onNodeWithText(nextPageLabel).performClick() // Rainbow Promise video -> Lesson video
    onNodeWithText(nextPageLabel).performClick() // Lesson video -> Reward

    onNodeWithText(activity.getString(R.string.reward_title)).assertExists()
    onNodeWithText(activity.getString(R.string.action_return_to_map)).performClick()
}

/**
 * "Load the Ark" (`game/puzzles/groupfill`) randomly partitions each of
 * [NoahsArkContent.loadArkDeckTargets] into a handful of numbered baskets
 * every run, so there's no fixed drag sequence — reads the full remaining
 * basket-headcount multiset straight off the screen and solves a real
 * exact-bin-fill assignment (same discipline as Feeding the 5,000's
 * Gathering the Crowd, which reuses the same engine), then drags one
 * basket of each assigned headcount onto its assigned deck. Unlike
 * Gathering the Crowd's numbered circles, Noah's Ark's 3 decks have fixed
 * text labels (Lower/Middle/Upper), not an index-based content
 * description.
 */
private fun FlowTestRule.completeLoadTheArk() {
    val activity = this.activity
    val deckTargets = NoahsArkContent.loadArkDeckTargets
    val deckLabels = listOf(
        activity.getString(R.string.noahs_ark_load_ark_deck_lower),
        activity.getString(R.string.noahs_ark_load_ark_deck_middle),
        activity.getString(R.string.noahs_ark_load_ark_deck_upper),
    )

    val remainingHeadcounts = (1..deckTargets.max()).flatMap { headcount ->
        val label = activity.getString(R.string.noahs_ark_load_ark_basket_content_description, headcount)
        val count = onAllNodesWithContentDescription(label).fetchSemanticsNodes().size
        List(count) { headcount }
    }.sortedDescending()

    val assignment = solveExactBinFillAssignment(remainingHeadcounts, deckTargets)

    remainingHeadcounts.forEachIndexed { index, headcount ->
        val label = activity.getString(R.string.noahs_ark_load_ark_basket_content_description, headcount)
        val itemNode = onAllNodesWithContentDescription(label)[0]
        dragOntoContentDescription(itemNode = itemNode, targetContentDescription = deckLabels[assignment[index]])
        waitForIdle()
    }
}

/**
 * Exact bin-fill backtracking: assigns each of [values] to a bin index
 * (into [targets]) so every bin's assigned values sum exactly to its
 * target. Both `groupfill`-based puzzles in this app (Noah's Ark's Load
 * the Ark, Feeding the 5,000's Gathering the Crowd) are solvable by
 * construction — each target was itself partitioned into the pooled
 * values in the first place — so a valid assignment always exists.
 * Sorting [values] descending before calling this keeps the search fast;
 * with at most ~15 items this finishes instantly regardless.
 */
private fun solveExactBinFillAssignment(values: List<Int>, targets: List<Int>): List<Int> {
    val assignment = IntArray(values.size) { -1 }
    val remaining = targets.toIntArray()

    fun backtrack(index: Int): Boolean {
        if (index == values.size) return remaining.all { it == 0 }
        val value = values[index]
        for (bin in remaining.indices) {
            if (remaining[bin] >= value) {
                remaining[bin] -= value
                assignment[index] = bin
                if (backtrack(index + 1)) return true
                remaining[bin] += value
            }
        }
        return false
    }

    check(backtrack(0)) { "No valid exact-bin-fill assignment found for $values into $targets" }
    return assignment.toList()
}

/**
 * Walks David & Goliath end to end: World Map -> Intro video -> Sheep
 * Counting -> Giant's Challenge video -> David Arrives video -> Choice ->
 * Heavy Armor video -> Choose the Stones -> Five Smooth Stones video ->
 * Sling Practice -> Victory video -> Lesson video -> Reward -> back to the
 * World Map. Assumes the caller is already on the World Map screen. See
 * `DavidGoliathFlowTest` for the thorough walkthrough that also asserts
 * reward/badge details.
 */
fun FlowTestRule.completeDavidGoliath() {
    val activity = this.activity
    val nextPageLabel = activity.getString(R.string.action_next_page)

    scrollToChapterOnWorldMap(activity.getString(R.string.chapter_david_goliath_title))
    onNodeWithText(activity.getString(R.string.chapter_david_goliath_title)).performClick()

    onNodeWithText(nextPageLabel).performClick() // Intro video -> Sheep Counting

    DavidGoliathContent.sheepCounts.forEach { count ->
        val name = activity.getString(count.nameRes)
        onAllNodesWithContentDescription(name)[0].performClick()
        onAllNodesWithContentDescription(name)[1].performClick()
    }
    onNodeWithText(nextPageLabel).performClick() // Sheep Counting -> Giant's Challenge video
    onNodeWithText(nextPageLabel).performClick() // Giant's Challenge video -> David Arrives video
    onNodeWithText(nextPageLabel).performClick() // David Arrives video -> Choice

    onNodeWithText(activity.getString(R.string.david_goliath_choice_option_1)).performClick()
    onNodeWithText(nextPageLabel).performClick() // Choice -> Heavy Armor video
    onNodeWithText(nextPageLabel).performClick() // Heavy Armor video -> Choose the Stones

    completeChooseStones()
    onNodeWithText(nextPageLabel).performClick() // Choose the Stones -> Five Smooth Stones video
    onNodeWithText(nextPageLabel).performClick() // Five Smooth Stones video -> Sling Practice

    completeSlingPractice()
    onNodeWithText(nextPageLabel).performClick() // Sling Practice -> Victory video

    onNodeWithText(nextPageLabel).performClick() // Victory video -> Lesson video
    onNodeWithText(nextPageLabel).performClick() // Lesson video -> Reward

    onNodeWithText(activity.getString(R.string.reward_title)).assertExists()
    onNodeWithText(activity.getString(R.string.action_return_to_map)).performClick()
}

/**
 * "Choose the Stones" is the one puzzle in the app with a real loss
 * condition (an AI opponent) — a board-blind fixed column-tap script loses
 * to it every time (verified offline: 0% win rate over 2000 simulated
 * matches, since the AI reliably blocks any predictable line before it
 * completes). Reading each cell's own live content description (see
 * `ConnectFourCell` in `DavidGoliathChooseStonesScreen.kt`) lets this
 * mirror the AI's own win-then-block-then-center heuristic from the
 * player's side, which wins roughly 60% of matches in isolation — retried
 * here across up to [maxAttempts] full matches (a loss or draw leaves the
 * board on screen until "Try Again" is tapped, never auto-resetting) to
 * drive the chance of never winning to effectively zero.
 */
private fun FlowTestRule.completeChooseStones(maxAttempts: Int = 20, maxTurnsPerAttempt: Int = 30) {
    val activity = this.activity
    val playerWonLabel = activity.getString(R.string.david_goliath_choose_stones_player_won)
    val opponentWonLabel = activity.getString(R.string.david_goliath_choose_stones_opponent_won)
    val drawLabel = activity.getString(R.string.david_goliath_choose_stones_draw)
    val yourTurnLabel = activity.getString(R.string.david_goliath_choose_stones_your_turn)
    val tryAgainLabel = activity.getString(R.string.david_goliath_choose_stones_try_again)

    for (attempt in 0 until maxAttempts) {
        turns@ for (turn in 0 until maxTurnsPerAttempt) {
            if (onAllNodesWithText(playerWonLabel).fetchSemanticsNodes().isNotEmpty()) return
            if (onAllNodesWithText(opponentWonLabel).fetchSemanticsNodes().isNotEmpty() ||
                onAllNodesWithText(drawLabel).fetchSemanticsNodes().isNotEmpty()
            ) {
                onNodeWithText(tryAgainLabel).performClick()
                break@turns
            }
            if (onAllNodesWithText(yourTurnLabel).fetchSemanticsNodes().isNotEmpty()) {
                val column = chooseConnectFourColumn()
                val columnLabel = activity.getString(R.string.david_goliath_choose_stones_column_content_description, column + 1)
                onNodeWithContentDescription(columnLabel).performClick()
            }
            waitForIdle()
        }
    }
    onNodeWithText(playerWonLabel).assertExists()
}

private const val CONNECT_FOUR_COLUMN_COUNT = 7
private const val CONNECT_FOUR_ROW_COUNT = 6

private enum class ConnectFourTestSlot { EMPTY, PLAYER, OPPONENT }

private fun FlowTestRule.connectFourCell(column: Int, row: Int): ConnectFourTestSlot {
    val activity = this.activity
    val emptyLabel = activity.getString(
        R.string.david_goliath_choose_stones_cell_content_description,
        column + 1, row + 1, activity.getString(R.string.david_goliath_choose_stones_cell_empty),
    )
    if (onAllNodesWithContentDescription(emptyLabel).fetchSemanticsNodes().isNotEmpty()) return ConnectFourTestSlot.EMPTY
    val playerLabel = activity.getString(
        R.string.david_goliath_choose_stones_cell_content_description,
        column + 1, row + 1, activity.getString(R.string.david_goliath_choose_stones_cell_player),
    )
    if (onAllNodesWithContentDescription(playerLabel).fetchSemanticsNodes().isNotEmpty()) return ConnectFourTestSlot.PLAYER
    return ConnectFourTestSlot.OPPONENT
}

private fun FlowTestRule.connectFourLowestEmptyRow(column: Int): Int? =
    (0 until CONNECT_FOUR_ROW_COUNT).firstOrNull { row -> connectFourCell(column, row) == ConnectFourTestSlot.EMPTY }

private fun FlowTestRule.connectFourWouldWin(column: Int, slot: ConnectFourTestSlot): Boolean {
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
private fun FlowTestRule.chooseConnectFourColumn(): Int {
    val validColumns = (0 until CONNECT_FOUR_COLUMN_COUNT).filter { connectFourLowestEmptyRow(it) != null }
    validColumns.firstOrNull { connectFourWouldWin(it, ConnectFourTestSlot.PLAYER) }?.let { return it }
    validColumns.firstOrNull { connectFourWouldWin(it, ConnectFourTestSlot.OPPONENT) }?.let { return it }
    val center = (CONNECT_FOUR_COLUMN_COUNT - 1) / 2
    return validColumns.minBy { kotlin.math.abs(it - center) }
}

/**
 * `SlingshotGame` now launches the stone *opposite* the pull (pull
 * southwest, it flies northeast) — the rat itself is still the only
 * moving reference the hit-test cares about, but hitting it means pulling
 * the stone to the *mirror image* of the rat's position through the
 * sling's anchor, not dragging onto the rat directly. The stone always
 * rests exactly on that anchor point when not being dragged (confirmed by
 * `DavidGoliathSlingPracticeScreen.kt`'s own layout math), so reading the
 * stone's own resting bounds gives the anchor for free — no separate
 * anchor node needed. Freezes the clock as the very first thing this
 * function does (once already safely on this screen via an ordinary,
 * un-frozen navigating click) so `elapsedMs` never advances mid-gesture,
 * then computes and drags to that mirror point each time. The actual
 * hit/miss isn't committed to game state until the screen's own cosmetic
 * flight animation finishes (so a hit visibly lands on the rat, not just
 * flies off in the right direction) — `SLING_FLIGHT_SETTLE_MS` advances
 * the clock past that animation before checking progress again, and
 * dragging is a no-op while a shot is still resolving. An escaped rat
 * doesn't count toward completion at all (free practice), so this loop
 * only stops once every required hit has actually landed — it never
 * deliberately lets a rat escape, so that path is only covered by the
 * unit tests, not this flow test.
 */
private fun FlowTestRule.completeSlingPractice() {
    val activity = this.activity
    val ratDescription = activity.getString(R.string.david_goliath_sling_rat_content_description)
    val stoneDescription = activity.getString(R.string.david_goliath_sling_stone_content_description)
    val requiredHits = com.bibleadventures.game.puzzles.slingshot.SlingshotGameState().requiredHits

    mainClock.autoAdvance = false
    // One explicit frame to let this screen's first composition (and its
    // progress label) land before any query — freezing the clock doesn't
    // itself wait for anything to compose.
    mainClock.advanceTimeByFrame()

    var safetySteps = 0
    while (currentSlingHits(requiredHits) < requiredHits) {
        check(safetySteps++ < 200) { "Sling Practice didn't reach $requiredHits hits after 200 clock steps — stuck at ${currentSlingHits(requiredHits)}" }

        if (onAllNodesWithContentDescription(ratDescription).fetchSemanticsNodes().isEmpty()) {
            // Between one rat resolving and the next rat's first frame landing.
            mainClock.advanceTimeByFrame()
            continue
        }
        dragStoneOppositeOfRat(stoneDescription, ratDescription)
        mainClock.advanceTimeBy(SLING_FLIGHT_SETTLE_MS)
    }

    mainClock.autoAdvance = true
}

/** Comfortably longer than the screen's own (private) flight-animation duration, so the deferred state update always lands before the next check. */
private const val SLING_FLIGHT_SETTLE_MS = 500L

private fun FlowTestRule.currentSlingHits(requiredHits: Int): Int {
    val activity = this.activity
    return (0..requiredHits).first { candidateHits ->
        val label = activity.getString(R.string.david_goliath_sling_practice_progress_label, candidateHits, requiredHits)
        onAllNodesWithText(label).fetchSemanticsNodes().isNotEmpty()
    }
}

/**
 * The rat keeps moving while a stone is in flight, so the game itself
 * leads the shot — it resolves against the rat's *projected* position at
 * `elapsedMs + FLIGHT_DURATION_MS`, not where it stood at release (see
 * `DavidGoliathSlingPracticeScreen.kt`'s `onDragEnd`). This test mirrors
 * that exactly: reads the rat's live `elapsedMs` off its own semantics
 * (exposed test-only via `RatElapsedMsKey`, never read aloud), computes
 * the same projected fractional position via the screen's own
 * `ratXFractionAt`/`ratYFractionAt` (made `internal` specifically so a
 * test can reuse the identical math rather than re-deriving it), converts
 * that from the screen's 0..1 track space into root pixel coordinates
 * using the rat's own current (position, fraction) pair as a scale
 * reference, and finally pulls the stone to the *mirror image* of that
 * projected point through the stone's own resting point (the sling's
 * anchor) — since the launch direction is the pull, reversed, this lines
 * the eventual shot up on the rat.
 */
private fun FlowTestRule.dragStoneOppositeOfRat(stoneDescription: String, ratDescription: String) {
    val ratNode = onNodeWithContentDescription(ratDescription).fetchSemanticsNode()
    val currentElapsedMs = ratNode.config[RatElapsedMsKey]
    val impactElapsedMs = currentElapsedMs + FLIGHT_DURATION_MS

    val currentRatFraction = Offset(ratXFractionAt(currentElapsedMs), ratYFractionAt(currentElapsedMs))
    val projectedRatFraction = Offset(ratXFractionAt(impactElapsedMs), ratYFractionAt(impactElapsedMs))
    val ratPixelCenter = ratNode.boundsInRoot.center

    val stoneNode = onNodeWithContentDescription(stoneDescription)
    val stoneBounds = stoneNode.fetchSemanticsNode().boundsInRoot
    val anchorPixelCenter = stoneBounds.center // the stone rests exactly on the sling's anchor when not being dragged
    val anchorFraction = Offset(ANCHOR_X_FRACTION, ANCHOR_Y_FRACTION)

    // The track is square (AspectRatioFitBox ratio = 1f), so one scale
    // (derived from the axis less likely to sit exactly on the anchor's
    // own fraction, avoiding a near-zero divide) applies to both.
    val pixelsPerFraction = (anchorPixelCenter.y - ratPixelCenter.y) / (anchorFraction.y - currentRatFraction.y)
    val projectedRatPixelCenter = Offset(
        anchorPixelCenter.x + (projectedRatFraction.x - anchorFraction.x) * pixelsPerFraction,
        anchorPixelCenter.y + (projectedRatFraction.y - anchorFraction.y) * pixelsPerFraction,
    )

    val targetGlobalCenter = Offset(
        2 * anchorPixelCenter.x - projectedRatPixelCenter.x,
        2 * anchorPixelCenter.y - projectedRatPixelCenter.y,
    )
    val localEnd = Offset(targetGlobalCenter.x - stoneBounds.left, targetGlobalCenter.y - stoneBounds.top)

    stoneNode.performTouchInput {
        swipe(start = center, end = localEnd, durationMillis = 200)
    }
}

private fun FlowTestRule.dragOntoContentDescription(itemNode: SemanticsNodeInteraction, targetContentDescription: String) {
    val itemBounds = itemNode.fetchSemanticsNode().boundsInRoot
    val targetBounds = onNodeWithContentDescription(targetContentDescription).fetchSemanticsNode().boundsInRoot
    val targetGlobalCenter = targetBounds.center
    val localEnd = Offset(targetGlobalCenter.x - itemBounds.left, targetGlobalCenter.y - itemBounds.top)

    itemNode.performTouchInput {
        swipe(start = center, end = localEnd, durationMillis = 200)
    }
}

/**
 * Walks Good Samaritan end to end: World Map -> Dangerous Road video ->
 * Passing By -> The Priest video -> The Levite video -> Explore -> Samaritan
 * Arrives video -> Reward -> back to the World Map. Assumes the caller is
 * already on the World Map screen.
 *
 * This is the second time this exact chapter's flow went stale across every
 * OTHER chapter's own prerequisite copy of it (see this file's own top
 * comment — the first time was Noah's Ark/David & Goliath's video
 * restructuring): when the "Passing By" sliding-block puzzle was inserted
 * mid-chapter, only `GoodSamaritanFlowTest`'s own copy was updated, so
 * every later chapter's test silently carried a walkthrough that could
 * never get past it. Centralizing here for good this time.
 */
fun FlowTestRule.completeGoodSamaritan() {
    val activity = this.activity
    val continueLabel = activity.getString(R.string.action_continue)
    val nextPageLabel = activity.getString(R.string.action_next_page)

    scrollToChapterOnWorldMap(activity.getString(R.string.chapter_good_samaritan_title))
    onNodeWithText(activity.getString(R.string.chapter_good_samaritan_title)).performClick()

    onNodeWithText(nextPageLabel).performClick() // Dangerous Road video -> Passing By

    completePassingBy()
    onNodeWithText(nextPageLabel).performClick() // Passing By -> The Priest video
    onNodeWithText(nextPageLabel).performClick() // The Priest video -> The Levite video
    onNodeWithText(nextPageLabel).performClick() // The Levite video -> Explore

    // The helping-beat overlay appears automatically the instant the traveler is
    // treated (blocking further D-pad input underneath), so it's dismissed
    // inline the moment it's detected rather than at one hardcoded step index.
    val upLabel = activity.getString(R.string.good_samaritan_direction_up)
    val downLabel = activity.getString(R.string.good_samaritan_direction_down)
    val leftLabel = activity.getString(R.string.good_samaritan_direction_left)
    val rightLabel = activity.getString(R.string.good_samaritan_direction_right)
    val helpingBeatTitle = activity.getString(R.string.good_samaritan_helping_beat_title)

    GoodSamaritanContent.solutionPath.forEach { direction ->
        val label = when (direction) {
            Direction.UP -> upLabel
            Direction.DOWN -> downLabel
            Direction.LEFT -> leftLabel
            Direction.RIGHT -> rightLabel
        }
        onNodeWithContentDescription(label).performClick()

        val helpingBeatShown = onAllNodesWithText(helpingBeatTitle).fetchSemanticsNodes().isNotEmpty()
        if (helpingBeatShown) {
            // This "Continue" belongs to HelpingBeatOverlay, a full-screen dialog.
            onNodeWithText(continueLabel).performClick()
        }
    }

    onNodeWithText(nextPageLabel).performClick() // Explore -> Samaritan Arrives video
    onNodeWithText(nextPageLabel).performClick() // Samaritan Arrives video -> Reward

    onNodeWithText(activity.getString(R.string.reward_title)).assertExists()
    onNodeWithText(activity.getString(R.string.action_return_to_map)).performClick()
}

/**
 * Replays `GoodSamaritanContent.passingBySolution` as real drag gestures.
 * Each block's own content description (the protagonist's, or an excuse
 * block's own label) is unique on screen, and the exit gate's rendered
 * width — a single grid cell — gives the exact pixels-per-cell needed to
 * turn a hand-verified (direction, distance) move into a real swipe.
 */
private fun FlowTestRule.completePassingBy() {
    val activity = this.activity
    val gateBounds = onNodeWithContentDescription(activity.getString(R.string.good_samaritan_passing_by_exit_gate_content_description))
        .fetchSemanticsNode()
        .boundsInRoot
    val cellSizePx = gateBounds.width

    val blockContentDescriptions = mapOf(
        "religious_leader" to activity.getString(R.string.good_samaritan_passing_by_protagonist_content_description),
        "ritual_purity" to activity.getString(R.string.good_samaritan_passing_by_excuse_ritual_purity),
        "fear_of_ambush" to activity.getString(R.string.good_samaritan_passing_by_excuse_fear_of_ambush),
        "strict_schedule" to activity.getString(R.string.good_samaritan_passing_by_excuse_strict_schedule),
        "not_my_problem" to activity.getString(R.string.good_samaritan_passing_by_excuse_not_my_problem),
    )

    GoodSamaritanContent.passingBySolution.forEach { move ->
        val description = blockContentDescriptions.getValue(move.blockId)
        val magnitude = cellSizePx * move.distance
        val delta = when (move.direction) {
            RoadblockDirection.UP -> Offset(0f, -magnitude)
            RoadblockDirection.DOWN -> Offset(0f, magnitude)
            RoadblockDirection.LEFT -> Offset(-magnitude, 0f)
            RoadblockDirection.RIGHT -> Offset(magnitude, 0f)
        }
        onNodeWithContentDescription(description).performTouchInput {
            swipe(start = center, end = center + delta, durationMillis = 200)
        }
    }
}
