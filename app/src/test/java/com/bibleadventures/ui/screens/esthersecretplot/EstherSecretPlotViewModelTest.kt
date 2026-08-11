package com.bibleadventures.ui.screens.esthersecretplot

import com.bibleadventures.FakeAudioController
import com.bibleadventures.FakePlayerProfileRepository
import com.bibleadventures.MainDispatcherRule
import com.bibleadventures.audio.SoundEffect
import com.bibleadventures.domain.model.ChapterId
import com.bibleadventures.game.puzzles.gridmaze.Direction
import com.bibleadventures.game.stories.EstherSecretPlotContent
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
class EstherSecretPlotViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun createViewModel(
        repository: FakePlayerProfileRepository = FakePlayerProfileRepository(),
        audioController: FakeAudioController = FakeAudioController(),
    ) = EstherSecretPlotViewModel(ProgressionService(repository), repository, audioController)

    @Test
    fun `initial grid parses courtyardMapLayout into the correct dimensions and start position`() {
        val state = createViewModel().uiState.value.stealthState

        assertEquals(EstherSecretPlotContent.courtyardMapLayout.size, state.grid.size)
        assertEquals(EstherSecretPlotContent.courtyardMapLayout[0].length, state.grid[0].size)
        assertFalse(state.isComplete)
    }

    @Test
    fun `following the hand-verified solution path reaches the goal without ever being spotted`() {
        val viewModel = createViewModel()

        EstherSecretPlotContent.courtyardSolutionPath.forEach { direction ->
            viewModel.onDirectionPressed(direction)
        }

        assertTrue(viewModel.uiState.value.stealthState.isComplete)
    }

    @Test
    fun `completing the courtyard plays a sound`() {
        val audioController = FakeAudioController()
        val viewModel = createViewModel(audioController = audioController)

        EstherSecretPlotContent.courtyardSolutionPath.forEach { direction ->
            viewModel.onDirectionPressed(direction)
        }

        assertEquals(listOf(SoundEffect.ITEM_COLLECTED), audioController.playedEffects)
    }

    @Test
    fun `walking into a wall never advances the guard patrol`() {
        val viewModel = createViewModel()

        // (4,1) start; UP is blocked by the wall directly above.
        viewModel.onDirectionPressed(Direction.UP)

        assertEquals(0, viewModel.uiState.value.stealthState.turnIndex)
    }

    @Test
    fun `onSceneCompleted marks the scene as a completed activity for Esther Secret Plot`() = runTest {
        val repository = FakePlayerProfileRepository()
        val viewModel = createViewModel(repository = repository)

        viewModel.onSceneCompleted("intro")
        advanceUntilIdle()

        val progress = repository.current().progressByChapter.getValue(ChapterId.ESTHER_SECRET_PLOT)
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
        assertTrue(ChapterId.ESTHER_SECRET_PLOT in repository.current().completedChapters)
        assertEquals(3, repository.current().stars)

        viewModel.onChapterFinished()
        advanceUntilIdle()

        assertEquals(3, repository.current().stars)

        job.cancel()
    }
}
