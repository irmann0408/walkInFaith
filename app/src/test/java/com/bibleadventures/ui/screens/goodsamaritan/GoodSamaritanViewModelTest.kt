package com.bibleadventures.ui.screens.goodsamaritan

import com.bibleadventures.FakeAudioController
import com.bibleadventures.FakePlayerProfileRepository
import com.bibleadventures.MainDispatcherRule
import com.bibleadventures.audio.SoundEffect
import com.bibleadventures.domain.model.ChapterId
import com.bibleadventures.game.puzzles.gridmaze.Direction
import com.bibleadventures.game.puzzles.gridmaze.GridPosition
import com.bibleadventures.game.puzzles.gridmaze.GridTileType
import com.bibleadventures.game.stories.GoodSamaritanContent
import com.bibleadventures.progress.ProgressionService
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

@OptIn(ExperimentalCoroutinesApi::class)
class GoodSamaritanViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun createViewModel(
        repository: FakePlayerProfileRepository = FakePlayerProfileRepository(),
        audioController: FakeAudioController = FakeAudioController(),
    ) = GoodSamaritanViewModel(ProgressionService(repository), repository, audioController)

    @Test
    fun `initial grid parses mapLayout into the correct dimensions and start position`() {
        val state = createViewModel().uiState.value.gridMazeState

        assertEquals(GoodSamaritanContent.mapLayout.size, state.grid.size)
        assertEquals(GoodSamaritanContent.mapLayout[0].length, state.grid[0].size)
        assertEquals(GridPosition(0, 0), state.playerPosition)
        assertEquals(GridTileType.PATH, state.grid[0][0])
        assertEquals(GridTileType.COLLECTIBLE, state.grid[0][2])
        assertEquals(GridTileType.CHECKPOINT, state.grid[2][9])
        assertEquals(GridTileType.GOAL, state.grid[9][9])
    }

    @Test
    fun `onDirectionPressed plays a sound on collecting medicine and treating the traveler, not on blocked moves`() {
        val audioController = FakeAudioController()
        val viewModel = createViewModel(audioController = audioController)

        viewModel.onDirectionPressed(Direction.UP) // blocked: (0,0) is the top row, out of bounds
        assertTrue(audioController.playedEffects.isEmpty())

        viewModel.onDirectionPressed(Direction.RIGHT) // (0,1)
        viewModel.onDirectionPressed(Direction.RIGHT) // (0,2) medicine
        assertEquals(listOf(SoundEffect.ITEM_COLLECTED), audioController.playedEffects)
    }

    @Test
    fun `onHelpingBeatAcknowledged flips the flag`() {
        val viewModel = createViewModel()

        viewModel.onHelpingBeatAcknowledged()

        assertTrue(viewModel.uiState.value.helpingBeatAcknowledged)
    }

    @Test
    fun `onSceneCompleted marks the scene as a completed activity for the Good Samaritan`() = runTest {
        val repository = FakePlayerProfileRepository()
        val viewModel = createViewModel(repository = repository)

        viewModel.onSceneCompleted("intro")
        advanceUntilIdle()

        val progress = repository.current().progressByChapter.getValue(ChapterId.GOOD_SAMARITAN)
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
        assertTrue(ChapterId.GOOD_SAMARITAN in repository.current().completedChapters)
        assertEquals(3, repository.current().stars)

        viewModel.onChapterFinished()
        advanceUntilIdle()

        assertEquals(3, repository.current().stars)

        job.cancel()
    }
}
