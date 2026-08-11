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
    fun `initial spies escape state is a solvable, not-yet-complete 3x3 puzzle`() {
        val state = createViewModel().uiState.value.spiesEscapeState

        assertEquals(3, state.size)
        assertEquals((0..8).toSet(), state.tiles.toSet())
    }

    @Test
    fun `initial camp state holds all 12 stones, none placed`() {
        val state = createViewModel().uiState.value.campState

        assertEquals(JerichoContent.campStones.size, state.itemIds.size)
        assertTrue(state.placedOrder.isEmpty())
        assertFalse(state.isComplete)
    }

    @Test
    fun `onChoiceSelected records the selected choice`() {
        val viewModel = createViewModel()

        viewModel.onChoiceSelected("trust_god")

        assertEquals("trust_god", viewModel.uiState.value.selectedChoiceId)
    }

    @Test
    fun `onSpiesEscapeTileTapped sliding a tile adjacent to the empty slot moves it`() {
        val viewModel = createViewModel()
        val before = viewModel.uiState.value.spiesEscapeState
        val emptyIndex = before.tiles.indexOf(0)
        val adjacentIndex = if (emptyIndex % 3 != 0) emptyIndex - 1 else emptyIndex + 1

        viewModel.onSpiesEscapeTileTapped(adjacentIndex)

        assertEquals(before.tiles[adjacentIndex], viewModel.uiState.value.spiesEscapeState.tiles[emptyIndex])
    }

    @Test
    fun `onCampStonePlaced stacks a stone and plays a sound only once per stone`() {
        val audioController = FakeAudioController()
        val viewModel = createViewModel(audioController = audioController)
        val stoneId = JerichoContent.campStones[0].id

        viewModel.onCampStonePlaced(stoneId)
        viewModel.onCampStonePlaced(stoneId) // already placed, re-placing is a no-op

        assertEquals(listOf(stoneId), viewModel.uiState.value.campState.placedOrder)
        assertEquals(listOf(SoundEffect.ITEM_COLLECTED), audioController.playedEffects)
    }

    @Test
    fun `stones can be placed in any order`() {
        val viewModel = createViewModel()
        val shuffledIds = JerichoContent.campStones.map { it.id }.reversed()

        shuffledIds.forEach { viewModel.onCampStonePlaced(it) }

        assertEquals(shuffledIds, viewModel.uiState.value.campState.placedOrder)
        assertTrue(viewModel.uiState.value.campState.isComplete)
    }

    @Test
    fun `onSixDayMarchTapped on the beat increases hits, never a failure`() {
        val viewModel = createViewModel()
        val note = JerichoContent.sixDayMarchChart.notes.first()

        viewModel.onSixDayMarchTapped(note.lane, note.hitTimeMs)

        assertEquals(1, viewModel.uiState.value.sixDayMarchState.hits)
        assertFalse(viewModel.uiState.value.sixDayMarchState.isComplete)
    }

    @Test
    fun `tapping all six notes across all three lanes completes the six-day march and plays a sound exactly once`() {
        val audioController = FakeAudioController()
        val viewModel = createViewModel(audioController = audioController)

        JerichoContent.sixDayMarchChart.notes.forEach { note ->
            viewModel.onSixDayMarchTapped(note.lane, note.hitTimeMs)
        }

        assertTrue(viewModel.uiState.value.sixDayMarchState.isComplete)
        assertEquals(1, audioController.playedEffects.count { it == SoundEffect.ITEM_COLLECTED })
    }

    @Test
    fun `tapping all seven notes across all three lanes completes the fast march`() {
        val viewModel = createViewModel()

        JerichoContent.fastMarchChart.notes.forEach { note ->
            viewModel.onFastMarchTapped(note.lane, note.hitTimeMs)
        }

        assertTrue(viewModel.uiState.value.fastMarchState.isComplete)
    }

    @Test
    fun `initial shofar arrangement covers all 5 colors at 5 distinct positions`() {
        val placements = createViewModel().uiState.value.shofarPlacements

        assertEquals(JerichoContent.shofarNoteColors.map { it.id }.toSet(), placements.map { it.id }.toSet())
        assertEquals(5, placements.map { it.position }.toSet().size)
    }

    @Test
    fun `onShofarNoteTapped plays a sound only on the correct next note, advancing through all 5`() {
        val audioController = FakeAudioController()
        val viewModel = createViewModel(audioController = audioController)
        val notesInOrder = viewModel.uiState.value.shofarState.pointIds

        viewModel.onShofarNoteTapped(notesInOrder[4]) // out of order — index 0 is expected first
        assertTrue(audioController.playedEffects.isEmpty())

        notesInOrder.forEach { viewModel.onShofarNoteTapped(it) }

        assertTrue(viewModel.uiState.value.shofarState.isComplete)
        assertEquals(5, audioController.playedEffects.size)
    }

    @Test
    fun `onShoutTapped increases the shout count and never regresses`() {
        val viewModel = createViewModel()

        viewModel.onShoutTapped()

        assertEquals(1, viewModel.uiState.value.shoutTaps)
        assertFalse(viewModel.uiState.value.isShoutComplete)
    }

    @Test
    fun `enough shout taps completes it and plays the trumpet fanfare exactly once`() {
        val audioController = FakeAudioController()
        val viewModel = createViewModel(audioController = audioController)

        repeat(JerichoContent.SHOUT_REQUIRED_TAPS) { viewModel.onShoutTapped() }

        assertTrue(viewModel.uiState.value.isShoutComplete)
        assertEquals(listOf(SoundEffect.TRUMPET_FANFARE), audioController.playedEffects)

        // Once complete, further taps are a no-op — never double-plays the fanfare.
        viewModel.onShoutTapped()
        assertEquals(JerichoContent.SHOUT_REQUIRED_TAPS, viewModel.uiState.value.shoutTaps)
        assertEquals(listOf(SoundEffect.TRUMPET_FANFARE), audioController.playedEffects)
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
