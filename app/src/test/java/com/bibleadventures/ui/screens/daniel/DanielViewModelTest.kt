package com.bibleadventures.ui.screens.daniel

import com.bibleadventures.FakeAudioController
import com.bibleadventures.FakePlayerProfileRepository
import com.bibleadventures.MainDispatcherRule
import com.bibleadventures.audio.SoundEffect
import com.bibleadventures.domain.model.ChapterId
import com.bibleadventures.game.puzzles.gridmaze.Direction
import com.bibleadventures.game.puzzles.gridmaze.GridPosition
import com.bibleadventures.game.puzzles.gridmaze.GridTileType
import com.bibleadventures.game.puzzles.slideout.SlideOutOutcome
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

    // --- Open the Window (slideout: tap a latch, it releases or gets stuck) ---

    @Test
    fun `tapping a latch with a clear path releases it and plays a sound`() {
        val audioController = FakeAudioController()
        val viewModel = createViewModel(audioController = audioController)
        val outerLatchId = DanielContent.windowLatchSolutionOrder.first().id

        viewModel.onLatchTapped(outerLatchId)

        assertEquals(SlideOutOutcome.RELEASED, viewModel.uiState.value.windowLatchState.lastOutcome)
        assertTrue(viewModel.uiState.value.windowLatchState.blocks.none { it.id == outerLatchId })
        assertEquals(listOf(SoundEffect.OBSTACLE_DODGED), audioController.playedEffects)
    }

    @Test
    fun `tapping a blocked latch changes nothing but the outcome, and plays no sound`() {
        val audioController = FakeAudioController()
        val viewModel = createViewModel(audioController = audioController)
        val innerLatchId = DanielContent.windowLatchSolutionOrder.last().id
        val blocksBefore = viewModel.uiState.value.windowLatchState.blocks

        viewModel.onLatchTapped(innerLatchId)

        assertEquals(SlideOutOutcome.BLOCKED, viewModel.uiState.value.windowLatchState.lastOutcome)
        assertEquals(innerLatchId, viewModel.uiState.value.windowLatchState.lastBlockedId)
        assertEquals(blocksBefore, viewModel.uiState.value.windowLatchState.blocks)
        assertTrue(audioController.playedEffects.isEmpty())
    }

    @Test
    fun `releasing every latch in windowLatchSolutionOrder completes Open the Window`() {
        val viewModel = createViewModel()

        DanielContent.windowLatchSolutionOrder.forEach { latch -> viewModel.onLatchTapped(latch.id) }

        assertTrue(viewModel.uiState.value.windowLatchState.isComplete)
        assertEquals(SlideOutOutcome.COMPLETE, viewModel.uiState.value.windowLatchState.lastOutcome)
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
    fun `two wrong answers on the same problem replace it with a fresh one, still not advanced`() {
        val audioController = FakeAudioController()
        val viewModel = createViewModel(audioController = audioController)
        val step = viewModel.uiState.value.lionsDenState.currentStep!!
        val originalProblem = viewModel.uiState.value.lionsDenProblems.first { it.id == step.id }
        val wrongValue = step.optionIds.map { it.toInt() }.first { it.toString() != step.correctOptionId }

        viewModel.onLionsDenAnswerTapped(wrongValue) // 1st wrong: same problem, just re-prompts
        assertEquals(0, viewModel.uiState.value.lionsDenState.currentStepIndex)
        assertEquals(originalProblem, viewModel.uiState.value.lionsDenProblems.first { it.id == originalProblem.id })

        viewModel.onLionsDenAnswerTapped(wrongValue) // 2nd wrong: replaced with a fresh problem
        val afterSecondWrong = viewModel.uiState.value
        assertEquals(0, afterSecondWrong.lionsDenState.currentStepIndex)
        assertTrue(audioController.playedEffects.isEmpty())

        val newProblem = afterSecondWrong.lionsDenProblems.first { it.id == originalProblem.id }
        assertTrue("expected a different problem after 2 wrong answers", newProblem != originalProblem)
        assertEquals(
            newProblem.choiceValues.map { it.toString() }.toSet(),
            afterSecondWrong.lionsDenState.currentStep!!.optionIds.toSet(),
        )
    }

    @Test
    fun `after a problem is replaced, its new correct answer still advances the step`() {
        val viewModel = createViewModel()
        val step = viewModel.uiState.value.lionsDenState.currentStep!!
        val wrongValue = step.optionIds.map { it.toInt() }.first { it.toString() != step.correctOptionId }

        viewModel.onLionsDenAnswerTapped(wrongValue)
        viewModel.onLionsDenAnswerTapped(wrongValue) // replaces the problem

        val newCorrectValue = viewModel.uiState.value.lionsDenState.currentStep!!.correctOptionId.toInt()
        viewModel.onLionsDenAnswerTapped(newCorrectValue)

        assertEquals(1, viewModel.uiState.value.lionsDenState.currentStepIndex)
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
                assertTrue("operandA out of range: $problem", problem.operandA in 1..99)
                assertTrue("operandB out of range: $problem", problem.operandB in 1..99)
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
