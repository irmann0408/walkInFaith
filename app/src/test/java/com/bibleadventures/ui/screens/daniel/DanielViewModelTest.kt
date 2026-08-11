package com.bibleadventures.ui.screens.daniel

import com.bibleadventures.FakePlayerProfileRepository
import com.bibleadventures.MainDispatcherRule
import com.bibleadventures.audio.AudioController
import com.bibleadventures.audio.MusicTrack
import com.bibleadventures.audio.SoundEffect
import com.bibleadventures.domain.model.ChapterId
import com.bibleadventures.game.puzzles.dodge.DodgeLane
import com.bibleadventures.game.puzzles.gridmaze.Direction
import com.bibleadventures.game.puzzles.gridmaze.GridPosition
import com.bibleadventures.game.puzzles.gridmaze.GridTileType
import com.bibleadventures.game.stories.DanielContent
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

private class RecordingAudioController : AudioController {
    val playedEffects = mutableListOf<SoundEffect>()
    override fun playMusic(track: MusicTrack) = Unit
    override fun stopMusic() = Unit
    override fun playSfx(effect: SoundEffect) {
        playedEffects += effect
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class DanielViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun createViewModel(
        repository: FakePlayerProfileRepository = FakePlayerProfileRepository(),
        audioController: RecordingAudioController = RecordingAudioController(),
    ) = DanielViewModel(ProgressionService(repository), repository, audioController)

    @Test
    fun `initial grid parses dariusMapLayout into the correct dimensions and start position`() {
        val state = createViewModel().uiState.value.gridMazeState

        assertEquals(DanielContent.dariusMapLayout.size, state.grid.size)
        assertEquals(DanielContent.dariusMapLayout[0].length, state.grid[0].size)
        assertEquals(GridPosition(0, 0), state.playerPosition)
        assertEquals(GridTileType.PATH, state.grid[0][0])
        assertEquals(GridTileType.WALL, state.grid[1][0])
        assertEquals(GridTileType.GOAL, state.grid[6][6])
    }

    @Test
    fun `onLaneTapped plays a sound when Daniel dodges, not on a wrong step`() {
        val audioController = RecordingAudioController()
        val viewModel = createViewModel(audioController = audioController)

        val hazardLane = DanielContent.stealthBeats[0].hazardLane
        viewModel.onLaneTapped(hazardLane) // wrong lane, TRY_AGAIN
        assertTrue(audioController.playedEffects.isEmpty())

        val safeLane = if (hazardLane == DodgeLane.LEFT) DodgeLane.RIGHT else DodgeLane.LEFT
        viewModel.onLaneTapped(safeLane)
        assertEquals(listOf(SoundEffect.OBSTACLE_DODGED), audioController.playedEffects)
    }

    @Test
    fun `onChoiceSelected records the selected choice`() {
        val viewModel = createViewModel()

        viewModel.onChoiceSelected("thankful")

        assertEquals("thankful", viewModel.uiState.value.selectedChoiceId)
    }

    @Test
    fun `onLightPointTapped connects points in order and plays a sound`() {
        val audioController = RecordingAudioController()
        val viewModel = createViewModel(audioController = audioController)
        val pointIds = DanielContent.lionsDenPointIds

        viewModel.onLightPointTapped(pointIds[0])

        assertEquals(listOf(pointIds[0]), viewModel.uiState.value.sequenceState.connectedIds)
        assertEquals(listOf(SoundEffect.ITEM_COLLECTED), audioController.playedEffects)
    }

    @Test
    fun `onLightPointTapped out of order does not undo prior progress`() {
        val viewModel = createViewModel()
        val pointIds = DanielContent.lionsDenPointIds

        viewModel.onLightPointTapped(pointIds[0])
        viewModel.onLightPointTapped(pointIds[2]) // out of order, expected pointIds[1]

        assertEquals(listOf(pointIds[0]), viewModel.uiState.value.sequenceState.connectedIds)
    }

    @Test
    fun `onDirectionPressed moves King Darius through the maze`() {
        val viewModel = createViewModel()

        viewModel.onDirectionPressed(DanielContent.dariusSolutionPath[0])

        assertEquals(GridPosition(0, 1), viewModel.uiState.value.gridMazeState.playerPosition)
    }

    @Test
    fun `onSceneCompleted marks the scene as a completed activity for Daniel`() = runTest {
        val repository = FakePlayerProfileRepository()
        val viewModel = createViewModel(repository = repository)

        viewModel.onSceneCompleted("intro")
        advanceUntilIdle()

        val progress = repository.current().progressByChapter.getValue(ChapterId.DANIEL)
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
        assertTrue(ChapterId.DANIEL in repository.current().completedChapters)
        assertEquals(3, repository.current().stars)

        viewModel.onChapterFinished()
        advanceUntilIdle()

        assertEquals(3, repository.current().stars)

        job.cancel()
    }
}
