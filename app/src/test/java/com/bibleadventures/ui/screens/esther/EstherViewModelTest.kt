package com.bibleadventures.ui.screens.esther

import com.bibleadventures.FakeAudioController
import com.bibleadventures.FakePlayerProfileRepository
import com.bibleadventures.MainDispatcherRule
import com.bibleadventures.audio.SoundEffect
import com.bibleadventures.domain.model.ChapterId
import com.bibleadventures.game.puzzles.gridmaze.Direction
import com.bibleadventures.game.puzzles.meter.TapPrecision
import com.bibleadventures.game.puzzles.sudoku.SudokuOutcome
import com.bibleadventures.game.rewards.EstherReward
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
    fun `initial hidden object state holds every royal attire item, none found`() {
        val state = createViewModel().uiState.value.hiddenObjectState

        assertEquals(EstherContent.royalAttireItems.size, state.items.size)
        assertTrue(state.foundIds.isEmpty())
        assertFalse(state.isComplete)
    }

    @Test
    fun `onAttireItemTapped finds an item and plays a sound only once per item`() {
        val audioController = FakeAudioController()
        val viewModel = createViewModel(audioController = audioController)
        val itemId = EstherContent.royalAttireItems[0].id

        viewModel.onAttireItemTapped(itemId)
        viewModel.onAttireItemTapped(itemId) // already found, re-tap is a no-op

        assertTrue(itemId in viewModel.uiState.value.hiddenObjectState.foundIds)
        assertEquals(listOf(SoundEffect.ITEM_COLLECTED), audioController.playedEffects)
    }

    @Test
    fun `finding every royal attire item completes the scene`() {
        val viewModel = createViewModel()

        EstherContent.royalAttireItems.forEach { viewModel.onAttireItemTapped(it.id) }

        assertTrue(viewModel.uiState.value.hiddenObjectState.isComplete)
    }

    @Test
    fun `onGreetingChoiceSelected records the selected choice`() {
        val viewModel = createViewModel()

        viewModel.onGreetingChoiceSelected("kindly")

        assertEquals("kindly", viewModel.uiState.value.selectedGreetingChoiceId)
    }

    @Test
    fun `initial stealth grid parses courtyardMapLayout into the correct dimensions and start position`() {
        val state = createViewModel().uiState.value.stealthState

        assertEquals(EstherContent.courtyardMapLayout.size, state.grid.size)
        assertEquals(EstherContent.courtyardMapLayout[0].length, state.grid[0].size)
        assertFalse(state.isComplete)
    }

    @Test
    fun `following the hand-verified courtyard solution path reaches the goal without ever being spotted`() {
        val viewModel = createViewModel()

        EstherContent.courtyardSolutionPath.forEach { direction ->
            viewModel.onCourtyardDirectionPressed(direction)
        }

        assertTrue(viewModel.uiState.value.stealthState.isComplete)
    }

    @Test
    fun `completing the courtyard plays a sound`() {
        val audioController = FakeAudioController()
        val viewModel = createViewModel(audioController = audioController)

        EstherContent.courtyardSolutionPath.forEach { direction ->
            viewModel.onCourtyardDirectionPressed(direction)
        }

        assertEquals(listOf(SoundEffect.ITEM_COLLECTED), audioController.playedEffects)
    }

    @Test
    fun `walking into a wall never advances the guard patrol`() {
        val viewModel = createViewModel()

        // (4,1) start; UP is blocked by the wall directly above.
        viewModel.onCourtyardDirectionPressed(Direction.UP)

        assertEquals(0, viewModel.uiState.value.stealthState.turnIndex)
    }

    @Test
    fun `onSudokuCellSelected on a given cell is ignored`() {
        val viewModel = createViewModel()

        viewModel.onSudokuCellSelected(0, 0) // (0,0) is a given per EstherContent

        assertNull(viewModel.uiState.value.selectedSudokuCell)
    }

    @Test
    fun `selecting an empty sudoku cell then tapping a valid icon fills it and clears the selection`() {
        val viewModel = createViewModel()

        viewModel.onSudokuCellSelected(0, 2) // empty per EstherContent's givens
        viewModel.onSudokuIconTapped("sun")

        assertEquals("sun", viewModel.uiState.value.sudokuState.filled[0 to 2])
        assertNull(viewModel.uiState.value.selectedSudokuCell)
    }

    @Test
    fun `tapping a conflicting sudoku icon keeps the cell selected for another try`() {
        val viewModel = createViewModel()

        viewModel.onSudokuCellSelected(0, 2)
        viewModel.onSudokuIconTapped("star") // row 0 already has "star" at (0,0)

        assertEquals(SudokuOutcome.CONFLICT, viewModel.uiState.value.sudokuState.lastOutcome)
        assertEquals(0 to 2, viewModel.uiState.value.selectedSudokuCell)
    }

    @Test
    fun `completing a sudoku row plays a sound`() {
        val audioController = FakeAudioController()
        val viewModel = createViewModel(audioController = audioController)

        // Row 0 givens: (0,0)=star (0,1)=moon (0,4)=leaf; empty: (0,2), (0,3).
        viewModel.onSudokuCellSelected(0, 2)
        viewModel.onSudokuIconTapped("sun")
        viewModel.onSudokuCellSelected(0, 3)
        viewModel.onSudokuIconTapped("drop")

        assertEquals(SudokuOutcome.ROW_COMPLETE, viewModel.uiState.value.sudokuState.lastOutcome)
        assertEquals(setOf(0), viewModel.uiState.value.sudokuState.completedRows)
        assertEquals(listOf(SoundEffect.ITEM_COLLECTED), audioController.playedEffects)
    }

    @Test
    fun `onDecisionChoiceSelected records the selected choice`() {
        val viewModel = createViewModel()

        viewModel.onDecisionChoiceSelected("if_i_perish")

        assertEquals("if_i_perish", viewModel.uiState.value.selectedDecisionChoiceId)
    }

    @Test
    fun `an EARLY_OR_LATE corridor tap still makes progress, never a failure`() {
        val viewModel = createViewModel()

        viewModel.onCorridorTapped(TapPrecision.EARLY_OR_LATE)

        assertTrue(viewModel.uiState.value.meterState.progress > 0)
        assertFalse(viewModel.uiState.value.meterState.isComplete)
    }

    @Test
    fun `tapping the corridor enough times completes it and plays a sound exactly once`() {
        val audioController = FakeAudioController()
        val viewModel = createViewModel(audioController = audioController)

        repeat(EstherContent.CORRIDOR_REQUIRED_PROGRESS) {
            viewModel.onCorridorTapped(TapPrecision.EARLY_OR_LATE)
        }

        assertTrue(viewModel.uiState.value.meterState.isComplete)
        assertEquals(listOf(SoundEffect.ITEM_COLLECTED), audioController.playedEffects)
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
    fun `onChapterFinished awards full stars, one badge, all 5 scripture cards, and completes the chapter exactly once`() = runTest {
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
        assertTrue(EstherReward.badge.id in repository.current().badges)
        EstherReward.scriptureCards.forEach { card ->
            assertTrue(card.id in repository.current().scriptureCards)
        }

        viewModel.onChapterFinished()
        advanceUntilIdle()

        assertEquals(3, repository.current().stars)

        job.cancel()
    }
}
