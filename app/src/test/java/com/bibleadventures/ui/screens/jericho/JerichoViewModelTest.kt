package com.bibleadventures.ui.screens.jericho

import com.bibleadventures.FakeAudioController
import com.bibleadventures.FakePlayerProfileRepository
import com.bibleadventures.MainDispatcherRule
import com.bibleadventures.audio.SoundEffect
import com.bibleadventures.domain.model.ChapterId
import com.bibleadventures.game.stories.JerichoContent
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
class JerichoViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun createViewModel(
        repository: FakePlayerProfileRepository = FakePlayerProfileRepository(),
        audioController: FakeAudioController = FakeAudioController(),
    ) = JerichoViewModel(ProgressionService(repository), repository, audioController)

    @Test
    fun `initial decision path state holds all configured steps, not yet complete`() {
        val state = createViewModel().uiState.value.decisionPathState

        assertEquals(JerichoContent.marchSteps, state.steps)
        assertEquals(0, state.currentStepIndex)
        assertFalse(state.isComplete)
    }

    @Test
    fun `onChoiceSelected records the selected choice`() {
        val viewModel = createViewModel()

        viewModel.onChoiceSelected("trust_god")

        assertEquals("trust_god", viewModel.uiState.value.selectedChoiceId)
    }

    @Test
    fun `onMarchOptionTapped plays no sound on a wrong, force-based option`() {
        val audioController = FakeAudioController()
        val viewModel = createViewModel(audioController = audioController)

        viewModel.onMarchOptionTapped("attack_gate")

        assertTrue(audioController.playedEffects.isEmpty())
        assertEquals(0, viewModel.uiState.value.decisionPathState.currentStepIndex)
    }

    @Test
    fun `onMarchOptionTapped plays the trumpet fanfare only on the final correct step`() {
        val audioController = FakeAudioController()
        val viewModel = createViewModel(audioController = audioController)

        viewModel.onMarchOptionTapped("march_quietly")
        viewModel.onMarchOptionTapped("stay_silent")
        viewModel.onMarchOptionTapped("march_seven_times")
        assertEquals(listOf(SoundEffect.ITEM_COLLECTED, SoundEffect.ITEM_COLLECTED, SoundEffect.ITEM_COLLECTED), audioController.playedEffects)

        viewModel.onMarchOptionTapped("blow_horns_and_shout")

        assertEquals(
            listOf(SoundEffect.ITEM_COLLECTED, SoundEffect.ITEM_COLLECTED, SoundEffect.ITEM_COLLECTED, SoundEffect.TRUMPET_FANFARE),
            audioController.playedEffects,
        )
        assertTrue(viewModel.uiState.value.decisionPathState.isComplete)
    }

    @Test
    fun `the same option id that was correct earlier can be wrong at the final step`() {
        val viewModel = createViewModel()
        viewModel.onMarchOptionTapped("march_quietly")
        viewModel.onMarchOptionTapped("stay_silent") // correct at step 2
        viewModel.onMarchOptionTapped("march_seven_times")

        // "stay_silent" is the wrong option at the final step.
        viewModel.onMarchOptionTapped("stay_silent")

        assertFalse(viewModel.uiState.value.decisionPathState.isComplete)
        assertEquals(3, viewModel.uiState.value.decisionPathState.currentStepIndex)
    }

    @Test
    fun `onSceneCompleted marks the scene as a completed activity for Jericho`() = runTest {
        val repository = FakePlayerProfileRepository()
        val viewModel = createViewModel(repository = repository)

        viewModel.onSceneCompleted("intro")
        advanceUntilIdle()

        val progress = repository.current().progressByChapter.getValue(ChapterId.JERICHO)
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
        assertTrue(ChapterId.JERICHO in repository.current().completedChapters)
        assertEquals(3, repository.current().stars)

        viewModel.onChapterFinished()
        advanceUntilIdle()

        assertEquals(3, repository.current().stars)

        job.cancel()
    }
}
