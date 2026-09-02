package com.bibleadventures.daniel

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
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
import com.bibleadventures.completeNoahsArk
import com.bibleadventures.game.puzzles.gridmaze.Direction
import com.bibleadventures.game.puzzles.rhythmlane.RhythmLaneChart
import com.bibleadventures.game.puzzles.slingshot.SlingshotGameState
import com.bibleadventures.game.stories.DanielContent
import com.bibleadventures.game.stories.GoodSamaritanContent
import org.junit.Rule
import org.junit.Test

// ic_goliath_shield.xml's silhouette is narrower than its own bounding box —
// its visible top edge (where the mark's line sits, and what the hit-test
// actually checks) spans x=12..52 of a 64-wide viewport, mirroring
// DavidGoliathSlingPracticeScreen.kt's own SHIELD_TOP_EDGE_*_RATIO constants.
private const val SLING_SHIELD_TOP_EDGE_LEFT_RATIO = 12f / 64f
private const val SLING_SHIELD_TOP_EDGE_RIGHT_RATIO = 52f / 64f

/**
 * Walks the full Daniel and the Lions adventure end to end. It's locked
 * until Noah's Ark, David and Goliath, and Good Samaritan are completed —
 * and this device's save data persists real state across test runs — so
 * this test completes all three prerequisites itself rather than assuming
 * they're already done, to stay deterministic regardless of what ran before
 * it (same pattern as GoodSamaritanFlowTest).
 */
class DanielFlowTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun completingDaniel_awardsStarsAndUnlocksEstherOnTheWorldMap() {
        val activity = composeTestRule.activity
        val continueLabel = activity.getString(R.string.action_continue)
        val nextPageLabel = activity.getString(R.string.action_next_page)

        composeTestRule.onNodeWithText(activity.getString(R.string.menu_adventures)).performClick()
        composeTestRule.completeNoahsArk()
        composeTestRule.completeDavidGoliath()
        completeGoodSamaritan(continueLabel)

        // World Map -> Daniel and the Lions (now unlocked).
        composeTestRule.onNodeWithText(activity.getString(R.string.chapter_daniel_title)).performClick()

        // Scene 1: Intro.
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // Scene 1b: Hurrying to Pray context card.
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // Scene 2: Hurrying to Pray — steer Daniel out of each official's
        // lane before they arrive (rhythmlane avoid semantics, 3 avoids).
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

        // Scene 3: Choice — Daniel's prayer, flavor-only.
        composeTestRule.onNodeWithText(activity.getString(R.string.daniel_choice_option_1)).performClick()
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // Scene 3b: Into the Lions' Den context card.
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // Scene 4: The Angel's Shield — 5 random math problems, one per
        // light. Two wrong answers in a row now replace the problem instead
        // of leaving the last choice a guaranteed-correct guess (see
        // DecisionPathGame.WRONG_ATTEMPTS_BEFORE_NEW_STEP), so the old
        // "just try each of the 3 choices" trick no longer reliably solves
        // it — compute the real answer from the displayed problem instead.
        repeat(DanielContent.LIONS_DEN_PROBLEM_COUNT) {
            solveLionsDenProblem()
        }
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // Scene 4b: Darius's Long Night context card.
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // Scene 5: Darius's Maze — solve with a hand-verified move sequence.
        val upLabel = activity.getString(R.string.daniel_darius_direction_up)
        val downLabel = activity.getString(R.string.daniel_darius_direction_down)
        val mazeLeftLabel = activity.getString(R.string.daniel_darius_direction_left)
        val mazeRightLabel = activity.getString(R.string.daniel_darius_direction_right)

        // The start tile (0,0) is the map's top-left corner, so pressing Up
        // walks off the edge — a deliberate, harmless BLOCKED move (doesn't
        // change position) confirming the maze's new accessibility feedback
        // text actually renders, not just that the engine reports it.
        composeTestRule.onNodeWithContentDescription(upLabel).performClick()
        composeTestRule.onNodeWithText(activity.getString(R.string.grid_maze_feedback_blocked)).assertExists()

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

        // Scene 6: Lesson.
        composeTestRule.onNodeWithText(activity.getString(R.string.daniel_lesson_title)).assertExists()
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        // Scene 7: Reward.
        composeTestRule.onNodeWithText(activity.getString(R.string.reward_title)).assertExists()
        composeTestRule.onNodeWithText(activity.getString(R.string.badge_faithful_heart_title)).assertExists()
        composeTestRule.onNodeWithText(activity.getString(R.string.action_return_to_map)).performClick()

        // Back on the World Map: Daniel completed, Esther unlocked.
        composeTestRule.onNodeWithText(activity.getString(R.string.world_map_title)).assertExists()
        composeTestRule.onNodeWithText(activity.getString(R.string.chapter_esther_title)).assertExists()
    }

    /** Walks Good Samaritan end to end (mirrors GoodSamaritanFlowTest) so Daniel unlocks. */
    private fun completeGoodSamaritan(continueLabel: String) {
        val activity = composeTestRule.activity
        val nextPageLabel = activity.getString(R.string.action_next_page)

        composeTestRule.onNodeWithText(activity.getString(R.string.chapter_good_samaritan_title)).performClick()

        composeTestRule.onNodeWithText(nextPageLabel).performClick() // Intro
        composeTestRule.onNodeWithText(nextPageLabel).performClick() // The Road to Jericho context

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
            composeTestRule.onNodeWithContentDescription(label).performClick()

            val helpingBeatShown = composeTestRule.onAllNodesWithText(helpingBeatTitle).fetchSemanticsNodes().isNotEmpty()
            if (helpingBeatShown) {
                // This "Continue" belongs to HelpingBeatOverlay, a full-screen dialog.
                composeTestRule.onNodeWithText(continueLabel).performClick()
            }
        }

        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        composeTestRule.onNodeWithText(activity.getString(R.string.good_samaritan_lesson_title)).assertExists()
        composeTestRule.onNodeWithText(nextPageLabel).performClick()

        composeTestRule.onNodeWithText(activity.getString(R.string.reward_title)).assertExists()
        composeTestRule.onNodeWithText(activity.getString(R.string.action_return_to_map)).performClick()
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

    private fun dragOntoContentDescription(itemNode: SemanticsNodeInteraction, targetContentDescription: String) {
        val itemBounds = itemNode.fetchSemanticsNode().boundsInRoot
        val targetBounds = composeTestRule.onNodeWithContentDescription(targetContentDescription).fetchSemanticsNode().boundsInRoot
        val targetGlobalCenter = targetBounds.center
        val localEnd = Offset(targetGlobalCenter.x - itemBounds.left, targetGlobalCenter.y - itemBounds.top)

        itemNode.performTouchInput {
            swipe(start = center, end = localEnd, durationMillis = 200)
        }
    }

    /**
     * Reads the displayed "%d + %d = ?" / "%d − %d = ?" problem, computes
     * the real answer, and taps the matching choice by its content
     * description (each `AnswerChoice` exposes its own value as its content
     * description). Deterministic by construction, so it also stays correct
     * once wrong answers can replace the problem mid-attempt — unlike a
     * blind "try all 3 choices" trial, this never taps a wrong answer at all.
     */
    private fun solveLionsDenProblem() {
        val problemText = composeTestRule.onNodeWithTag("lions_den_problem").fetchSemanticsNode()
            .config[SemanticsProperties.Text].joinToString(separator = "") { it.text }
        val operands = Regex("\\d+").findAll(problemText).map { it.value.toInt() }.toList()
        val correctValue = if ("−" in problemText) operands[0] - operands[1] else operands[0] + operands[1]
        composeTestRule.onNodeWithContentDescription(correctValue.toString()).performClick()
    }
}
