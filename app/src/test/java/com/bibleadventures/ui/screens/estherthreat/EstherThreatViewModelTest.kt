package com.bibleadventures.ui.screens.estherthreat

import com.bibleadventures.FakeAudioController
import com.bibleadventures.FakePlayerProfileRepository
import com.bibleadventures.MainDispatcherRule
import com.bibleadventures.audio.SoundEffect
import com.bibleadventures.domain.model.ChapterId
import com.bibleadventures.game.puzzles.sudoku.SudokuOutcome
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
class EstherThreatViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun createViewModel(
        repository: FakePlayerProfileRepository = FakePlayerProfileRepository(),
        audioController: FakeAudioController = FakeAudioController(),
    ) = EstherThreatViewModel(ProgressionService(repository), repository, audioController)

    @Test
    fun `onCellSelected on a given cell is ignored`() {
        val viewModel = createViewModel()

        viewModel.onCellSelected(0, 0) // (0,0) is a given per EstherThreatContent

        assertNull(viewModel.uiState.value.selectedCell)
    }

    @Test
    fun `selecting an empty cell then tapping a valid icon fills it and clears the selection`() {
        val viewModel = createViewModel()

        viewModel.onCellSelected(0, 2) // empty per EstherThreatContent's givens
        viewModel.onIconTapped("sun")

        assertEquals("sun", viewModel.uiState.value.sudokuState.filled[0 to 2])
        assertNull(viewModel.uiState.value.selectedCell)
    }

    @Test
    fun `tapping a conflicting icon keeps the cell selected for another try`() {
        val viewModel = createViewModel()

        viewModel.onCellSelected(0, 2)
        viewModel.onIconTapped("star") // row 0 already has "star" at (0,0)

        assertEquals(SudokuOutcome.CONFLICT, viewModel.uiState.value.sudokuState.lastOutcome)
        assertEquals(0 to 2, viewModel.uiState.value.selectedCell)
    }

    @Test
    fun `completing a row plays a sound`() {
        val audioController = FakeAudioController()
        val viewModel = createViewModel(audioController = audioController)

        // Row 0 givens: (0,0)=star (0,1)=moon (0,4)=leaf; empty: (0,2), (0,3).
        viewModel.onCellSelected(0, 2)
        viewModel.onIconTapped("sun")
        viewModel.onCellSelected(0, 3)
        viewModel.onIconTapped("drop")

        assertEquals(SudokuOutcome.ROW_COMPLETE, viewModel.uiState.value.sudokuState.lastOutcome)
        assertEquals(setOf(0), viewModel.uiState.value.sudokuState.completedRows)
        assertEquals(listOf(SoundEffect.ITEM_COLLECTED), audioController.playedEffects)
    }

    @Test
    fun `onSceneCompleted marks the scene as a completed activity for Esther Threat`() = runTest {
        val repository = FakePlayerProfileRepository()
        val viewModel = createViewModel(repository = repository)

        viewModel.onSceneCompleted("intro")
        advanceUntilIdle()

        val progress = repository.current().progressByChapter.getValue(ChapterId.ESTHER_THREAT)
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
        assertTrue(ChapterId.ESTHER_THREAT in repository.current().completedChapters)
        assertEquals(3, repository.current().stars)

        viewModel.onChapterFinished()
        advanceUntilIdle()

        assertEquals(3, repository.current().stars)

        job.cancel()
    }
}
