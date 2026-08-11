package com.bibleadventures.ui.screens.esthernewqueen

import com.bibleadventures.FakeAudioController
import com.bibleadventures.FakePlayerProfileRepository
import com.bibleadventures.MainDispatcherRule
import com.bibleadventures.audio.SoundEffect
import com.bibleadventures.domain.model.ChapterId
import com.bibleadventures.game.stories.EstherNewQueenContent
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
class EstherNewQueenViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun createViewModel(
        repository: FakePlayerProfileRepository = FakePlayerProfileRepository(),
        audioController: FakeAudioController = FakeAudioController(),
    ) = EstherNewQueenViewModel(ProgressionService(repository), repository, audioController)

    @Test
    fun `initial hidden object state holds every royal attire item, none found`() {
        val state = createViewModel().uiState.value.hiddenObjectState

        assertEquals(EstherNewQueenContent.royalAttireItems.size, state.items.size)
        assertTrue(state.foundIds.isEmpty())
        assertFalse(state.isComplete)
    }

    @Test
    fun `onAttireItemTapped finds an item and plays a sound only once per item`() {
        val audioController = FakeAudioController()
        val viewModel = createViewModel(audioController = audioController)
        val itemId = EstherNewQueenContent.royalAttireItems[0].id

        viewModel.onAttireItemTapped(itemId)
        viewModel.onAttireItemTapped(itemId) // already found, re-tap is a no-op

        assertTrue(itemId in viewModel.uiState.value.hiddenObjectState.foundIds)
        assertEquals(listOf(SoundEffect.ITEM_COLLECTED), audioController.playedEffects)
    }

    @Test
    fun `finding every item completes the scene`() {
        val viewModel = createViewModel()

        EstherNewQueenContent.royalAttireItems.forEach { viewModel.onAttireItemTapped(it.id) }

        assertTrue(viewModel.uiState.value.hiddenObjectState.isComplete)
    }

    @Test
    fun `onChoiceSelected records the selected choice`() {
        val viewModel = createViewModel()

        viewModel.onChoiceSelected("kindly")

        assertEquals("kindly", viewModel.uiState.value.selectedChoiceId)
    }

    @Test
    fun `onSceneCompleted marks the scene as a completed activity for Esther New Queen`() = runTest {
        val repository = FakePlayerProfileRepository()
        val viewModel = createViewModel(repository = repository)

        viewModel.onSceneCompleted("intro")
        advanceUntilIdle()

        val progress = repository.current().progressByChapter.getValue(ChapterId.ESTHER_NEW_QUEEN)
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
        assertTrue(ChapterId.ESTHER_NEW_QUEEN in repository.current().completedChapters)
        assertEquals(3, repository.current().stars)

        viewModel.onChapterFinished()
        advanceUntilIdle()

        assertEquals(3, repository.current().stars)

        job.cancel()
    }
}
