package com.bibleadventures.ui.screens.esther

import com.bibleadventures.FakeAudioController
import com.bibleadventures.FakePlayerProfileRepository
import com.bibleadventures.MainDispatcherRule
import com.bibleadventures.audio.SoundEffect
import com.bibleadventures.domain.model.ChapterId
import com.bibleadventures.game.stories.EstherContent
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
class EstherViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun createViewModel(
        repository: FakePlayerProfileRepository = FakePlayerProfileRepository(),
        audioController: FakeAudioController = FakeAudioController(),
    ) = EstherViewModel(ProgressionService(repository), repository, audioController)

    @Test
    fun `initial decision path state holds all configured steps, not yet complete`() {
        val state = createViewModel().uiState.value.decisionPathState

        assertEquals(EstherContent.banquetSteps, state.steps)
        assertEquals(0, state.currentStepIndex)
        assertFalse(state.isComplete)
    }

    @Test
    fun `onChoiceSelected records the selected choice`() {
        val viewModel = createViewModel()

        viewModel.onChoiceSelected("if_i_perish")

        assertEquals("if_i_perish", viewModel.uiState.value.selectedChoiceId)
    }

    @Test
    fun `onBanquetOptionTapped plays a sound only on the correct option, waiting through the first two steps`() {
        val audioController = FakeAudioController()
        val viewModel = createViewModel(audioController = audioController)

        viewModel.onBanquetOptionTapped("speak_now") // wrong at step 1, should wait
        assertTrue(audioController.playedEffects.isEmpty())
        assertEquals(0, viewModel.uiState.value.decisionPathState.currentStepIndex)

        viewModel.onBanquetOptionTapped("wait")
        assertEquals(listOf(SoundEffect.ITEM_COLLECTED), audioController.playedEffects)
        assertEquals(1, viewModel.uiState.value.decisionPathState.currentStepIndex)
    }

    @Test
    fun `completing all three banquet steps completes the decision path`() {
        val viewModel = createViewModel()

        viewModel.onBanquetOptionTapped("wait")
        viewModel.onBanquetOptionTapped("wait")
        viewModel.onBanquetOptionTapped("speak_now")

        assertTrue(viewModel.uiState.value.decisionPathState.isComplete)
    }

    @Test
    fun `onSceneCompleted marks the scene as a completed activity for Esther`() = runTest {
        val repository = FakePlayerProfileRepository()
        val viewModel = createViewModel(repository = repository)

        viewModel.onSceneCompleted("intro")
        advanceUntilIdle()

        val progress = repository.current().progressByChapter.getValue(ChapterId.ESTHER)
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
        assertTrue(ChapterId.ESTHER in repository.current().completedChapters)
        assertEquals(3, repository.current().stars)

        viewModel.onChapterFinished()
        advanceUntilIdle()

        assertEquals(3, repository.current().stars)

        job.cancel()
    }
}
