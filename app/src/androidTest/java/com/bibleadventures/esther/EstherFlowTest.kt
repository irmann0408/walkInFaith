package com.bibleadventures.esther

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import com.bibleadventures.MainActivity
import com.bibleadventures.R
import com.bibleadventures.completeDavidGoliath
import com.bibleadventures.completeGoodSamaritan
import com.bibleadventures.completeNoahsArk
import com.bibleadventures.game.puzzles.gridmaze.Direction
import com.bibleadventures.game.puzzles.rhythmlane.RhythmLaneChart
import com.bibleadventures.game.stories.DanielContent
import com.bibleadventures.game.stories.EstherContent
import org.junit.Rule
import org.junit.Test

/**
 * Walks the merged Esther's Rescue of Her People chapter end to end — one
 * chapter, 5 sequential mini-puzzles (Royal Attire, Courtyard Stealth,
 * Messenger Sudoku, Corridor Courage Meter, Reveal Haman's Plot). Replaces
 * the old EstherArcFlowTest, which walked this content as 5 separate
 * chapters before they were consolidated back into one per playtesting
 * feedback (splitting them felt disjointed, and the banquet jigsaw
 * mini-game — a repeat of `dragsort`, the same engine Organize the Ark
 * used at the time — was dropped for being both redundant and too easy).
 */
class EstherFlowTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    /**
     * The World Map is a LazyColumn — items far outside the composition
     * window don't exist in the semantics tree until scrolled into view, so
     * plain performScrollTo() (which requires the node to already exist)
     * isn't enough. Scrolling the tagged list itself via performScrollToNode
     * incrementally scrolls until a matching item is composed.
     */
    private fun scrollToChapterOnWorldMap(title: String) {
        composeTestRule.onNodeWithTag("world_map_chapter_list").performScrollToNode(hasText(title))
    }

    @Test
    fun completingEsther_awardsOneBadgeAndAllFiveScriptureCardsAndUnlocksJericho() {
        val activity = composeTestRule.activity
        val nextPageLabel = activity.getString(R.string.action_next_page)

        composeTestRule.onNodeWithText(activity.getString(R.string.menu_adventures)).performClick()
        composeTestRule.completeNoahsArk()
        composeTestRule.completeDavidGoliath()
        composeTestRule.completeGoodSamaritan()
        completeDaniel()

        scrollToChapterOnWorldMap(activity.getString(R.string.chapter_esther_title))
        composeTestRule.onNodeWithText(activity.getString(R.string.chapter_esther_title)).performClick()

        // Intro -> Chosen for the Palace context.
        composeTestRule.onNodeWithText(nextPageLabel).performClick()
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // Royal Attire (hidden object).
        EstherContent.royalAttireItems.forEach { item ->
            composeTestRule.onNodeWithContentDescription(activity.getString(item.nameRes)).performClick()
        }
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // Esther Becomes Queen context -> Greeting choice.
        composeTestRule.onNodeWithText(nextPageLabel).performClick()
        composeTestRule.onNodeWithText(activity.getString(R.string.esther_new_queen_choice_option_1)).performClick()
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // A Dangerous Secret context.
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // Courtyard Stealth.
        val upLabel = activity.getString(R.string.esther_secret_plot_direction_up)
        val downLabel = activity.getString(R.string.esther_secret_plot_direction_down)
        val stealthLeftLabel = activity.getString(R.string.esther_secret_plot_direction_left)
        val stealthRightLabel = activity.getString(R.string.esther_secret_plot_direction_right)
        EstherContent.courtyardSolutionPath.forEach { direction ->
            val label = when (direction) {
                Direction.UP -> upLabel
                Direction.DOWN -> downLabel
                Direction.LEFT -> stealthLeftLabel
                Direction.RIGHT -> stealthRightLabel
            }
            composeTestRule.onNodeWithContentDescription(label).performClick()
        }
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // The King is Warned context -> A Wicked Law context.
        composeTestRule.onNodeWithText(nextPageLabel).performClick()
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // Messenger Sudoku.
        val iconKeyToLabel = mapOf(
            "star" to R.string.esther_threat_icon_star,
            "moon" to R.string.esther_threat_icon_moon,
            "sun" to R.string.esther_threat_icon_sun,
            "drop" to R.string.esther_threat_icon_drop,
            "leaf" to R.string.esther_threat_icon_leaf,
        )
        val sudokuSolution = listOf(
            Triple(0, 2, "sun"), Triple(0, 3, "drop"),
            Triple(1, 1, "sun"), Triple(1, 3, "leaf"),
            Triple(2, 2, "leaf"), Triple(2, 4, "moon"),
            Triple(3, 0, "drop"), Triple(3, 3, "moon"),
            Triple(4, 1, "star"), Triple(4, 4, "drop"),
        )
        sudokuSolution.forEach { (row, col, iconKey) ->
            val cellLabel = activity.getString(R.string.esther_threat_sudoku_cell_content_description, row + 1, col + 1)
            composeTestRule.onNodeWithContentDescription(cellLabel, substring = true).performClick()
            composeTestRule.onNodeWithContentDescription(activity.getString(iconKeyToLabel.getValue(iconKey))).performClick()
        }
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // The City Mourns and Fasts context -> decision choice.
        composeTestRule.onNodeWithText(nextPageLabel).performClick()
        composeTestRule.onNodeWithText(activity.getString(R.string.esther_brave_approach_choice_option_1)).performClick()
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // Three Days of Fasting context.
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // Corridor Courage Meter — a 3-lane rhythm mini-game. Freeze the clock and
        // advance to each authored note's exact time so every tap lands; a
        // mistimed tap would just be a no-op (see RhythmLaneGame's no-failure design).
        // Leads straight into the Lesson now — Reveal Haman's Plot and its
        // surrounding context cards were dropped to tighten the chapter's tail end.
        completeCorridorRhythmLane()
        composeTestRule.onNodeWithText(nextPageLabel).performClick()
        composeTestRule.onNodeWithText(activity.getString(R.string.esther_brave_approach_lesson_title)).assertExists()
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // Reward — one badge, all 5 scripture cards.
        composeTestRule.onNodeWithText(activity.getString(R.string.reward_title)).assertExists()
        composeTestRule.onNodeWithText(activity.getString(R.string.badge_courageous_heart_title)).assertExists()
        composeTestRule.onNodeWithText("Esther 2:20").assertExists()
        composeTestRule.onNodeWithText("Esther 2:22").assertExists()
        composeTestRule.onNodeWithText("Esther 4:3").assertExists()
        composeTestRule.onNodeWithText("Esther 4:14").assertExists()
        composeTestRule.onNodeWithText("Esther 7:3").assertExists()
        // The Reward screen scrolls (5 scripture cards + badge won't fit one
        // screen), so Return to Map sits below the fold — scroll to it first.
        val returnToMapNode = composeTestRule.onNodeWithText(activity.getString(R.string.action_return_to_map))
        returnToMapNode.performScrollTo()
        returnToMapNode.performClick()

        // Closing the loop: completing Esther unlocks Jericho.
        composeTestRule.onNodeWithText(activity.getString(R.string.world_map_title)).assertExists()
        scrollToChapterOnWorldMap(activity.getString(R.string.chapter_jericho_title))
        composeTestRule.onNodeWithText(activity.getString(R.string.chapter_jericho_title)).assertExists()
    }

    /** Walks Daniel and the Lions end to end (mirrors DanielFlowTest) so Esther unlocks. */
    private fun completeDaniel() {
        val activity = composeTestRule.activity
        val nextPageLabel = activity.getString(R.string.action_next_page)

        scrollToChapterOnWorldMap(activity.getString(R.string.chapter_daniel_title))
        composeTestRule.onNodeWithText(activity.getString(R.string.chapter_daniel_title)).performClick()

        composeTestRule.onNodeWithText(nextPageLabel).performClick() // Intro
        composeTestRule.onNodeWithText(nextPageLabel).performClick() // Hurrying to Pray context

        completeLaneAvoid(
            chart = DanielContent.hurryToPrayChart,
            requiredAvoids = DanielContent.HURRY_TO_PRAY_REQUIRED_AVOIDS,
            titleRes = R.string.daniel_stealth_title,
            progressLabelRes = R.string.daniel_stealth_progress_label,
            characterContentDescriptionRes = R.string.daniel_stealth_character_content_description,
            moveLeftLabelRes = R.string.daniel_stealth_move_left_content_description,
            moveRightLabelRes = R.string.daniel_stealth_move_right_content_description,
        )
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        composeTestRule.onNodeWithText(activity.getString(R.string.daniel_choice_option_1)).performClick()
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        composeTestRule.onNodeWithText(nextPageLabel).performClick() // Into the Lions' Den context

        // The Angel's Shield — 5 random math problems. Two wrong answers in a
        // row now replace the problem instead of leaving the last choice a
        // guaranteed-correct guess, so compute the real answer instead of
        // trying all 3 choices blind.
        repeat(DanielContent.LIONS_DEN_PROBLEM_COUNT) {
            solveLionsDenProblem()
        }
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        composeTestRule.onNodeWithText(nextPageLabel).performClick() // Darius's Long Night context

        val upLabel = activity.getString(R.string.daniel_darius_direction_up)
        val downLabel = activity.getString(R.string.daniel_darius_direction_down)
        val mazeLeftLabel = activity.getString(R.string.daniel_darius_direction_left)
        val mazeRightLabel = activity.getString(R.string.daniel_darius_direction_right)
        DanielContent.dariusSolutionPath.forEach { direction ->
            val label = when (direction) {
                Direction.UP -> upLabel
                Direction.DOWN -> downLabel
                Direction.LEFT -> mazeLeftLabel
                Direction.RIGHT -> mazeRightLabel
            }
            composeTestRule.onNodeWithContentDescription(label).performClick()
        }
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        composeTestRule.onNodeWithText(activity.getString(R.string.daniel_lesson_title)).assertExists()
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        composeTestRule.onNodeWithText(activity.getString(R.string.reward_title)).assertExists()
        composeTestRule.onNodeWithText(activity.getString(R.string.action_return_to_map)).performClick()
    }

    /**
     * Reads the displayed "%d + %d = ?" / "%d − %d = ?" problem, computes
     * the real answer, and taps the matching choice by its content
     * description. Two wrong answers in a row now replace the problem
     * instead of leaving the last choice a guaranteed-correct guess (see
     * `DecisionPathGame.WRONG_ATTEMPTS_BEFORE_NEW_STEP`), so the old "try
     * each of the 3 choices" trick no longer reliably solves it.
     */
    private fun solveLionsDenProblem() {
        val problemText = composeTestRule.onNodeWithTag("lions_den_problem").fetchSemanticsNode()
            .config[SemanticsProperties.Text].joinToString(separator = "") { it.text }
        val operands = Regex("\\d+").findAll(problemText).map { it.value.toInt() }.toList()
        val correctValue = if ("−" in problemText) operands[0] - operands[1] else operands[0] + operands[1]
        composeTestRule.onNodeWithContentDescription(correctValue.toString()).performClick()
    }

    /**
     * Freezes the Compose test clock and advances it to each of the
     * corridor's authored notes' exact `hitTimeMs` in turn, tapping that
     * note's lane — fully deterministic, no timing luck. Loops through the
     * chart as many times as needed to reach `CORRIDOR_REQUIRED_HITS`. The
     * screen drives its scroll clock with a manual `withFrameNanos`
     * accumulator specifically so `mainClock.advanceTimeBy(...)` can
     * control it this way (unlike `rememberInfiniteTransition`, which
     * Sling Practice's tests found doesn't progress under a frozen clock).
     */
    private fun completeCorridorRhythmLane() {
        val activity = composeTestRule.activity
        val laneDescriptions = (1..3).map {
            activity.getString(R.string.esther_brave_approach_corridor_lane_content_description, it)
        }
        val chart = EstherContent.corridorChart

        composeTestRule.mainClock.autoAdvance = false
        var currentMs = 0L
        var hits = 0
        var loopIndex = 0L
        while (hits < EstherContent.CORRIDOR_REQUIRED_HITS) {
            chart.notes.forEach { note ->
                if (hits < EstherContent.CORRIDOR_REQUIRED_HITS) {
                    val targetMs = loopIndex * chart.loopDurationMs + note.hitTimeMs
                    composeTestRule.mainClock.advanceTimeBy(targetMs - currentMs)
                    currentMs = targetMs
                    composeTestRule.onNodeWithContentDescription(laneDescriptions[note.lane]).performClick()
                    hits++
                }
            }
            loopIndex++
        }
        composeTestRule.mainClock.autoAdvance = true
    }

    /**
     * Crossing the Valley / Hurrying to Pray's rhythmlane "avoid" mechanic
     * auto-judges every frame purely from the character's current lane (see
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
