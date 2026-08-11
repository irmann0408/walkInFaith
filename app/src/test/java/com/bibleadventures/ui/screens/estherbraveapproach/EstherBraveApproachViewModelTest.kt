package com.bibleadventures.ui.screens.estherbraveapproach

import com.bibleadventures.FakeAudioController
import com.bibleadventures.FakePlayerProfileRepository
import com.bibleadventures.MainDispatcherRule
import com.bibleadventures.audio.SoundEffect
import com.bibleadventures.domain.model.ChapterId
import com.bibleadventures.game.puzzles.meter.TapPrecision
import com.bibleadventures.game.stories.EstherBraveApproachContent
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
class EstherBraveApproachViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun createViewModel(
        repository: FakePlayerProfileRepository = FakePlayerProfileRepository(),
        audioController: FakeAudioController = FakeAudioController(),
    ) = EstherBraveApproachViewModel(ProgressionService(repository), repository, audioController)

    @Test
    fun `onChoiceSelected records the selected choice`() {
        val viewModel = createViewModel()

        viewModel.onChoiceSelected("if_i_perish")

        assertEquals("if_i_perish", viewModel.uiState.value.selectedChoiceId)
    }

    @Test
    fun `an EARLY_OR_LATE tap still makes progress, never a failure`() {
        val viewModel = createViewModel()

        viewModel.onCorridorTapped(TapPrecision.EARLY_OR_LATE)

        assertTrue(viewModel.uiState.value.meterState.progress > 0)
        assertFalse(viewModel.uiState.value.meterState.isComplete)
    }

    @Test
    fun `tapping enough times completes the corridor and plays a sound exactly once`() {
        val audioController = FakeAudioController()
        val viewModel = createViewModel(audioController = audioController)

        repeat(EstherBraveApproachContent.CORRIDOR_REQUIRED_PROGRESS) {
            viewModel.onCorridorTapped(TapPrecision.EARLY_OR_LATE)
        }

        assertTrue(viewModel.uiState.value.meterState.isComplete)
        assertEquals(listOf(SoundEffect.ITEM_COLLECTED), audioController.playedEffects)
    }

    @Test
    fun `onSceneCompleted marks the scene as a completed activity for Esther Brave Approach`() = runTest {
        val repository = FakePlayerProfileRepository()
        val viewModel = createViewModel(repository = repository)

        viewModel.onSceneCompleted("intro")
        advanceUntilIdle()

        val progress = repository.current().progressByChapter.getValue(ChapterId.ESTHER_BRAVE_APPROACH)
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
        assertTrue(ChapterId.ESTHER_BRAVE_APPROACH in repository.current().completedChapters)
        assertEquals(3, repository.current().stars)

        viewModel.onChapterFinished()
        advanceUntilIdle()

        assertEquals(3, repository.current().stars)

        job.cancel()
    }
}
