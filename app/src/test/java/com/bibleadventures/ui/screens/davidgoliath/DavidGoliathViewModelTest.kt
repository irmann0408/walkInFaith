package com.bibleadventures.ui.screens.davidgoliath

import com.bibleadventures.FakePlayerProfileRepository
import com.bibleadventures.MainDispatcherRule
import com.bibleadventures.audio.AudioController
import com.bibleadventures.audio.MusicTrack
import com.bibleadventures.audio.SoundEffect
import com.bibleadventures.domain.model.ChapterId
import com.bibleadventures.game.stories.DavidGoliathContent
import com.bibleadventures.progress.ProgressionService
import com.bibleadventures.ui.screens.noahsark.DecoyTapOutcome
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

private class RecordingAudioController : AudioController {
    val playedEffects = mutableListOf<SoundEffect>()
    override fun playMusic(track: MusicTrack) = Unit
    override fun stopMusic() = Unit
    override fun playSfx(effect: SoundEffect) {
        playedEffects += effect
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class DavidGoliathViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun createViewModel(
        repository: FakePlayerProfileRepository = FakePlayerProfileRepository(),
        audioController: RecordingAudioController = RecordingAudioController(),
    ) = DavidGoliathViewModel(ProgressionService(repository), repository, audioController)

    @Test
    fun `initial state has all 5 stones and a shuffled decoy position among the same hand-placed spots`() {
        val state = createViewModel().uiState.value

        assertEquals(5, state.hiddenObjectState.items.size)
        assertTrue(state.hiddenObjectState.foundIds.isEmpty())

        val expectedPositions = (DavidGoliathContent.stones.map { it.position } + DavidGoliathContent.riverbedDecoy.position).toSet()
        val actualPositions = (state.hiddenObjectState.items.map { it.position } + state.riverbedDecoyPosition).toSet()
        assertEquals(expectedPositions, actualPositions)
    }

    @Test
    fun `onStoneFound adds the stone to foundIds`() {
        val viewModel = createViewModel()

        viewModel.onStoneFound("stone_1")

        assertTrue("stone_1" in viewModel.uiState.value.hiddenObjectState.foundIds)
    }

    @Test
    fun `onRiverbedDecoyTapped sets the decoy outcome without touching found stones`() {
        val viewModel = createViewModel()

        viewModel.onRiverbedDecoyTapped()

        assertEquals(DecoyTapOutcome.DECOY_TAPPED, viewModel.uiState.value.lastRiverbedDecoyOutcome)
        assertTrue(viewModel.uiState.value.hiddenObjectState.foundIds.isEmpty())
    }

    @Test
    fun `onChoiceSelected records the chosen option id`() {
        val viewModel = createViewModel()
        val optionId = DavidGoliathContent.choiceOptions.first().id

        viewModel.onChoiceSelected(optionId)

        assertEquals(optionId, viewModel.uiState.value.selectedChoiceId)
    }

    @Test
    fun `onStoneReleased plays a sound only on a hit`() {
        val audioController = RecordingAudioController()
        val viewModel = createViewModel(audioController = audioController)

        viewModel.onStoneReleased(aimedPosition = 0.1f, markPosition = 0.9f)
        assertTrue(audioController.playedEffects.isEmpty())

        viewModel.onStoneReleased(aimedPosition = 0.5f, markPosition = 0.5f)
        assertEquals(listOf(SoundEffect.TARGET_HIT), audioController.playedEffects)
    }

    @Test
    fun `onSceneCompleted marks the scene as a completed activity for David and Goliath`() = runTest {
        val repository = FakePlayerProfileRepository()
        val viewModel = createViewModel(repository = repository)

        viewModel.onSceneCompleted("intro")
        advanceUntilIdle()

        val progress = repository.current().progressByChapter.getValue(ChapterId.DAVID_GOLIATH)
        assertTrue("intro" in progress.completedActivities)
        assertFalse(progress.completed)
    }

    @Test
    fun `onChapterFinished awards full stars and completes the chapter exactly once`() = runTest {
        val repository = FakePlayerProfileRepository()
        val viewModel = createViewModel(repository = repository)
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onChapterFinished()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.reward)
        assertEquals(3, viewModel.uiState.value.reward?.stars)
        assertTrue(ChapterId.DAVID_GOLIATH in repository.current().completedChapters)
        assertEquals(3, repository.current().stars)

        // Calling it again (e.g. a stale re-entry into the Reward screen) must not
        // double-award stars or re-complete the chapter (spec section 20).
        viewModel.onChapterFinished()
        advanceUntilIdle()

        assertEquals(3, repository.current().stars)

        job.cancel()
    }
}

