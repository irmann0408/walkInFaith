package com.bibleadventures.ui.screens.estherbanquetsrescue

import com.bibleadventures.FakeAudioController
import com.bibleadventures.FakePlayerProfileRepository
import com.bibleadventures.MainDispatcherRule
import com.bibleadventures.audio.SoundEffect
import com.bibleadventures.domain.model.ChapterId
import com.bibleadventures.game.stories.EstherBanquetsRescueContent
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
class EstherBanquetsRescueViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun createViewModel(
        repository: FakePlayerProfileRepository = FakePlayerProfileRepository(),
        audioController: FakeAudioController = FakeAudioController(),
    ) = EstherBanquetsRescueViewModel(ProgressionService(repository), repository, audioController)

    @Test
    fun `placing every food item on its own zone completes the jigsaw`() {
        val viewModel = createViewModel()

        EstherBanquetsRescueContent.foodItems.forEach { item ->
            viewModel.onFoodItemDropped(item.id, item.categoryKey!!)
        }

        assertTrue(viewModel.uiState.value.dragSortState.isComplete)
    }

    @Test
    fun `dropping a food item on the wrong zone does not place it`() {
        val viewModel = createViewModel()
        val bread = EstherBanquetsRescueContent.foodItems.first { it.id == "food_bread" }

        viewModel.onFoodItemDropped(bread.id, "zone_wine") // wrong zone

        assertFalse(bread.id in viewModel.uiState.value.dragSortState.placedItems.keys)
    }

    @Test
    fun `onRevealOptionTapped plays a sound only on the correct option, advancing through all 3 steps`() {
        val audioController = FakeAudioController()
        val viewModel = createViewModel(audioController = audioController)

        viewModel.onRevealOptionTapped("shout_angrily") // wrong
        assertTrue(audioController.playedEffects.isEmpty())

        viewModel.onRevealOptionTapped("speak_calmly")
        viewModel.onRevealOptionTapped("tell_truth")
        viewModel.onRevealOptionTapped("name_haman")

        assertTrue(viewModel.uiState.value.decisionPathState.isComplete)
        assertEquals(3, audioController.playedEffects.size)
    }

    @Test
    fun `onSceneCompleted marks the scene as a completed activity for Esther Banquets Rescue`() = runTest {
        val repository = FakePlayerProfileRepository()
        val viewModel = createViewModel(repository = repository)

        viewModel.onSceneCompleted("intro")
        advanceUntilIdle()

        val progress = repository.current().progressByChapter.getValue(ChapterId.ESTHER_BANQUETS_RESCUE)
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
        assertTrue(ChapterId.ESTHER_BANQUETS_RESCUE in repository.current().completedChapters)
        assertEquals(3, repository.current().stars)

        viewModel.onChapterFinished()
        advanceUntilIdle()

        assertEquals(3, repository.current().stars)

        job.cancel()
    }
}
