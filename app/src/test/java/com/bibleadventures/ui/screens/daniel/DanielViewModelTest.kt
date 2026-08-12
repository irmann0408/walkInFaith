package com.bibleadventures.ui.screens.daniel

import com.bibleadventures.FakeAudioController
import com.bibleadventures.FakePlayerProfileRepository
import com.bibleadventures.MainDispatcherRule
import com.bibleadventures.audio.SoundEffect
import com.bibleadventures.domain.model.ChapterId
import com.bibleadventures.game.puzzles.gridmaze.Direction
import com.bibleadventures.game.puzzles.gridmaze.GridPosition
import com.bibleadventures.game.puzzles.gridmaze.GridTileType
import com.bibleadventures.game.puzzles.rhythmlane.RhythmLaneChart
import com.bibleadventures.game.stories.DanielContent
import com.bibleadventures.game.stories.MathOperator
import com.bibleadventures.progress.ProgressionService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DanielViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun createViewModel(
        repository: FakePlayerProfileRepository = FakePlayerProfileRepository(),
        audioController: FakeAudioController = FakeAudioController(),
    ) = DanielViewModel(ProgressionService(repository), repository, audioController)

    @Test
    fun `initial grid parses dariusMapLayout into the correct dimensions and start position`() {
        val state = createViewModel().uiState.value.gridMazeState

        assertEquals(DanielContent.dariusMapLayout.size, state.grid.size)
        assertEquals(DanielContent.dariusMapLayout[0].length, state.grid[0].size)
        assertEquals(GridPosition(0, 0), state.playerPosition)
        assertEquals(GridTileType.PATH, state.grid[0][0])
        assertEquals(GridTileType.WALL, state.grid[1][0])
        assertEquals(GridTileType.GOAL, state.grid[6][6])
    }

    // --- Hurrying to Pray (rhythmlane, avoid semantics) ---

    @Test
    fun `onHurryToPrayLaneMoved clamps to the 3 lanes, never a failure`() {
        val viewModel = createViewModel()

        repeat(5) { viewModel.onHurryToPrayLaneMoved(-1) }
        assertEquals(0, viewModel.uiState.value.characterLane)

        repeat(5) { viewModel.onHurryToPrayLaneMoved(1) }
        assertEquals(2, viewModel.uiState.value.characterLane)
    }

    @Test
    fun `moving out of an official's lane before they arrive registers an avoid and plays a sound`() {
        val audioController = FakeAudioController()
        val viewModel = createViewModel(audioController = audioController)
        val note = DanielContent.hurryToPrayChart.notes.first()
        val safeLane = (0..2).first { it != note.lane }

        moveHurryToPrayLaneTo(viewModel, safeLane)
        viewModel.onHurryToPrayTimeAdvanced(note.hitTimeMs)

        assertEquals(1, viewModel.uiState.value.hurryToPrayState.hits)
        assertFalse(viewModel.uiState.value.hurryToPrayState.isComplete)
        assertEquals(listOf(SoundEffect.OBSTACLE_DODGED), audioController.playedEffects)
    }

    @Test
    fun `staying in an official's own lane when they arrive does not register an avoid`() {
        val viewModel = createViewModel()
        val note = DanielContent.hurryToPrayChart.notes.first()

        moveHurryToPrayLaneTo(viewModel, note.lane)
        viewModel.onHurryToPrayTimeAdvanced(note.hitTimeMs)

        assertEquals(0, viewModel.uiState.value.hurryToPrayState.hits)
    }

    @Test
    fun `completing all 3 required avoids marks Hurrying to Pray complete`() {
        val viewModel = createViewModel()

        completeHurryToPray(viewModel, DanielContent.hurryToPrayChart, DanielContent.HURRY_TO_PRAY_REQUIRED_AVOIDS)

        assertTrue(viewModel.uiState.value.hurryToPrayState.isComplete)
    }

    /** Mirrors DavidGoliathViewModelTest's `completeCrossingValley` — loops the chart as many times as needed, parking Daniel out of each note's lane before its exact hit time. */
    private fun completeHurryToPray(viewModel: DanielViewModel, chart: RhythmLaneChart, requiredAvoids: Int) {
        var hits = 0
        var loopIndex = 0L
        while (hits < requiredAvoids) {
            chart.notes.forEach { note ->
                if (hits < requiredAvoids) {
                    val safeLane = (0..2).first { it != note.lane }
                    moveHurryToPrayLaneTo(viewModel, safeLane)
                    viewModel.onHurryToPrayTimeAdvanced(loopIndex * chart.loopDurationMs + note.hitTimeMs)
                    hits++
                }
            }
            loopIndex++
        }
    }

    private fun moveHurryToPrayLaneTo(viewModel: DanielViewModel, targetLane: Int) {
        while (viewModel.uiState.value.characterLane != targetLane) {
            val delta = if (viewModel.uiState.value.characterLane < targetLane) 1 else -1
            viewModel.onHurryToPrayLaneMoved(delta)
        }
    }

    @Test
    fun `onChoiceSelected records the selected choice`() {
        val viewModel = createViewModel()

        viewModel.onChoiceSelected("thankful")

        assertEquals("thankful", viewModel.uiState.value.selectedChoiceId)
    }

    @Test
    fun `onLionsDenAnswerTapped with the correct value advances the step and plays a sound`() {
        val audioController = FakeAudioController()
        val viewModel = createViewModel(audioController = audioController)
        val correctValue = viewModel.uiState.value.lionsDenState.currentStep!!.correctOptionId.toInt()

        viewModel.onLionsDenAnswerTapped(correctValue)

        assertEquals(1, viewModel.uiState.value.lionsDenState.currentStepIndex)
        assertEquals(listOf(SoundEffect.ITEM_COLLECTED), audioController.playedEffects)
    }

    @Test
    fun `onLionsDenAnswerTapped with a wrong value does not advance and never fails`() {
        val viewModel = createViewModel()
        val step = viewModel.uiState.value.lionsDenState.currentStep!!
        val wrongValue = step.optionIds.map { it.toInt() }.first { it.toString() != step.correctOptionId }

        viewModel.onLionsDenAnswerTapped(wrongValue)

        assertEquals(0, viewModel.uiState.value.lionsDenState.currentStepIndex)
    }

    @Test
    fun `answering all problems correctly completes the Angel's Shield`() {
        val viewModel = createViewModel()

        repeat(DanielContent.LIONS_DEN_PROBLEM_COUNT) {
            val correctValue = viewModel.uiState.value.lionsDenState.currentStep!!.correctOptionId.toInt()
            viewModel.onLionsDenAnswerTapped(correctValue)
        }

        assertTrue(viewModel.uiState.value.lionsDenState.isComplete)
    }

    @Test
    fun `generated Angel's Shield problems are always well-formed`() {
        // Constructed 100 times to exercise many random draws (Random.Default,
        // unseeded) — same "check invariants across many random instances, not
        // just one lucky run" discipline as SlidingPuzzleGameTest's shuffle tests.
        repeat(100) {
            val problems = createViewModel().uiState.value.lionsDenProblems

            assertEquals(DanielContent.LIONS_DEN_PROBLEM_COUNT, problems.size)
            problems.forEach { problem ->
                assertTrue("operandA out of range: $problem", problem.operandA in 1..999)
                assertTrue("operandB out of range: $problem", problem.operandB in 1..999)
                if (problem.operator == MathOperator.SUBTRACT) {
                    assertTrue("subtraction result not positive: $problem", problem.correctValue > 0)
                }
                assertEquals("choices weren't 3 distinct values: $problem", 3, problem.choiceValues.toSet().size)
                assertTrue("correct value missing from choices: $problem", problem.correctValue in problem.choiceValues)
                assertTrue("a choice was negative: $problem", problem.choiceValues.all { it >= 0 })
            }
        }
    }

    @Test
    fun `onDirectionPressed moves King Darius through the maze`() {
        val viewModel = createViewModel()

        viewModel.onDirectionPressed(DanielContent.dariusSolutionPath[0])

        assertEquals(GridPosition(0, 1), viewModel.uiState.value.gridMazeState.playerPosition)
    }

    @Test
    fun `onSceneCompleted marks the scene as a completed activity for Daniel`() = runTest {
        val repository = FakePlayerProfileRepository()
        val viewModel = createViewModel(repository = repository)

        viewModel.onSceneCompleted("intro")
        advanceUntilIdle()

        val progress = repository.current().progressByChapter.getValue(ChapterId.DANIEL)
        assertTrue("intro" in progress.completedActivities)
        assertFalse(progress.completed)
    }

    @Test
    fun `onChapterFinished awards full stars and completes the chapter exactly once`() = runTest {
        val repository = FakePlayerProfileRepository()
        val viewModel = createViewModel(repository = repository)
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.reward)
        viewModel.onChapterFinished()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.reward)
        assertEquals(3, viewModel.uiState.value.reward?.stars)
        assertTrue(ChapterId.DANIEL in repository.current().completedChapters)
        assertEquals(3, repository.current().stars)

        viewModel.onChapterFinished()
        advanceUntilIdle()

        assertEquals(3, repository.current().stars)

        job.cancel()
    }
}
