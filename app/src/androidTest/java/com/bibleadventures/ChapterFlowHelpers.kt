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
import com.bibleadventures.game.stories.DavidGoliathContent
import com.bibleadventures.game.stories.NoahsArkContent

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
 * Cross the Valley -> Sling Practice -> Victory video -> Lesson video ->
 * Reward -> back to the World Map. Assumes the caller is already on the
 * World Map screen. See `DavidGoliathFlowTest` for the thorough walkthrough
 * that also asserts reward/badge details.
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
    onNodeWithText(nextPageLabel).performClick() // Five Smooth Stones video -> Cross the Valley

    completeLaneAvoid(
        chart = DavidGoliathContent.crossingValleyChart,
        requiredAvoids = DavidGoliathContent.CROSSING_VALLEY_REQUIRED_AVOIDS,
        titleRes = R.string.david_goliath_dodge_title,
        progressLabelRes = R.string.david_goliath_dodge_progress_label,
        characterContentDescriptionRes = R.string.david_goliath_dodge_character_content_description,
        moveLeftLabelRes = R.string.david_goliath_dodge_move_left_content_description,
        moveRightLabelRes = R.string.david_goliath_dodge_move_right_content_description,
    )
    onNodeWithText(nextPageLabel).performClick() // Cross the Valley -> Sling Practice

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
 * Freezes the Compose test clock and, for each lane a chart ever uses,
 * parks the character there and advances the clock by one full
 * `chart.loopDurationMs` — since every note recurs exactly once per loop, a
 * full-loop dwell in a lane is guaranteed to pass through (and avoid) every
 * note assigned to that lane exactly once, regardless of where in the loop
 * the clock actually started. Shared by every `rhythmlane` "avoid" scene
 * (David & Goliath's Crossing the Valley, Daniel's Hurrying to Pray).
 */
private fun FlowTestRule.completeLaneAvoid(
    chart: com.bibleadventures.game.puzzles.rhythmlane.RhythmLaneChart,
    requiredAvoids: Int,
    titleRes: Int,
    progressLabelRes: Int,
    characterContentDescriptionRes: Int,
    moveLeftLabelRes: Int,
    moveRightLabelRes: Int,
) {
    val activity = this.activity
    val lanes = chart.notes.map { it.lane }.distinct().sorted()

    // Let the screen fully compose (with the clock still auto-advancing)
    // before freezing it — freezing immediately after navigating can catch
    // the new screen before its first frame lands, so even static elements
    // like the progress label aren't in the semantics tree yet.
    onNodeWithText(activity.getString(titleRes)).assertExists()

    mainClock.autoAdvance = false
    var safetyRounds = 0
    while (currentLaneAvoidHits(progressLabelRes, requiredAvoids) < requiredAvoids) {
        check(safetyRounds++ < 20) { "Lane-avoid puzzle didn't reach $requiredAvoids avoids after 20 full sweep rounds — stuck at ${currentLaneAvoidHits(progressLabelRes, requiredAvoids)}" }
        lanes.forEach { lane ->
            if (currentLaneAvoidHits(progressLabelRes, requiredAvoids) < requiredAvoids) {
                moveCharacterToLane(lane, characterContentDescriptionRes, moveLeftLabelRes, moveRightLabelRes)
                mainClock.advanceTimeBy(chart.loopDurationMs)
            }
        }
    }
    mainClock.autoAdvance = true
}

private fun FlowTestRule.currentLaneAvoidHits(progressLabelRes: Int, requiredAvoids: Int): Int {
    val activity = this.activity
    return (0..requiredAvoids).first { candidateHits ->
        val label = activity.getString(progressLabelRes, candidateHits, requiredAvoids)
        onAllNodesWithText(label).fetchSemanticsNodes().isNotEmpty()
    }
}

private fun FlowTestRule.currentCharacterLane(characterContentDescriptionRes: Int): Int {
    val activity = this.activity
    return (1..3).first { candidateLane ->
        val label = activity.getString(characterContentDescriptionRes, candidateLane)
        onAllNodesWithContentDescription(label).fetchSemanticsNodes().isNotEmpty()
    } - 1
}

private fun FlowTestRule.moveCharacterToLane(targetLane: Int, characterContentDescriptionRes: Int, moveLeftLabelRes: Int, moveRightLabelRes: Int) {
    val activity = this.activity
    val moveLeftLabel = activity.getString(moveLeftLabelRes)
    val moveRightLabel = activity.getString(moveRightLabelRes)

    while (currentCharacterLane(characterContentDescriptionRes) != targetLane) {
        val label = if (currentCharacterLane(characterContentDescriptionRes) < targetLane) moveRightLabel else moveLeftLabel
        onNodeWithContentDescription(label).performClick()
    }
}

/**
 * Freezes the clock as the very first thing this function does (once
 * already safely on this screen via an ordinary, un-frozen navigating
 * click), then drives the mark forward in small deterministic steps,
 * reading its *actual* rendered position after each step and dragging the
 * stone onto it the moment it's within the shield's true (rendered, not
 * assumed) span. See `DavidGoliathSlingPracticeScreen.kt`'s own
 * `SHIELD_TOP_EDGE_*_RATIO` constants for why the true span isn't the
 * shield image's full bounding box.
 */
private fun FlowTestRule.completeSlingPractice() {
    val shieldTopEdgeLeftRatio = 12f / 64f
    val shieldTopEdgeRightRatio = 52f / 64f
    val activity = this.activity
    val markDescription = activity.getString(R.string.david_goliath_sling_target_mark_content_description)
    val stoneDescription = activity.getString(R.string.david_goliath_sling_stone_content_description)
    val shieldDescriptionPrefix = activity.getString(R.string.david_goliath_sling_shield_content_description, "")
    val requiredHits = com.bibleadventures.game.puzzles.slingshot.SlingshotGameState().requiredHits

    mainClock.autoAdvance = false
    // One explicit frame to let this screen's first composition (and its
    // progress label) land before any query — freezing the clock doesn't
    // itself wait for anything to compose.
    mainClock.advanceTimeByFrame()

    var safetySteps = 0
    while (currentSlingHits(requiredHits) < requiredHits) {
        check(safetySteps++ < 1500) { "Sling Practice didn't reach $requiredHits hits after 1500 clock steps — stuck at ${currentSlingHits(requiredHits)}" }

        val markBounds = onNodeWithContentDescription(markDescription).fetchSemanticsNode().boundsInRoot
        val shieldImageBounds = onNodeWithContentDescription(shieldDescriptionPrefix, substring = true).fetchSemanticsNode().boundsInRoot
        val shieldTrueLeft = shieldImageBounds.left + shieldTopEdgeLeftRatio * shieldImageBounds.width
        val shieldTrueRight = shieldImageBounds.left + shieldTopEdgeRightRatio * shieldImageBounds.width

        if (markBounds.center.x in shieldTrueLeft..shieldTrueRight) {
            val stoneNode = onNodeWithContentDescription(stoneDescription)
            dragOntoContentDescription(itemNode = stoneNode, targetContentDescription = markDescription)
        } else {
            mainClock.advanceTimeBy(50L)
        }
    }

    mainClock.autoAdvance = true
}

private fun FlowTestRule.currentSlingHits(requiredHits: Int): Int {
    val activity = this.activity
    return (0..requiredHits).first { candidateHits ->
        val label = activity.getString(R.string.david_goliath_sling_practice_progress_label, candidateHits, requiredHits)
        onAllNodesWithText(label).fetchSemanticsNodes().isNotEmpty()
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
