package com.bibleadventures.ui.screens.jesuscalmsstorm

import com.bibleadventures.FakeAudioController
import com.bibleadventures.FakePlayerProfileRepository
import com.bibleadventures.MainDispatcherRule
import com.bibleadventures.audio.SoundEffect
import com.bibleadventures.domain.model.ChapterId
import com.bibleadventures.game.puzzles.gridmaze.GridPosition
import com.bibleadventures.game.puzzles.rhythmlane.RhythmLaneChart
import com.bibleadventures.game.puzzles.stackbuild.StackBuildOutcome
import com.bibleadventures.game.stories.JesusCalmsStormContent
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
class JesusCalmsStormViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun createViewModel(
        repository: FakePlayerProfileRepository = FakePlayerProfileRepository(),
        audioController: FakeAudioController = FakeAudioController(),
    ) = JesusCalmsStormViewModel(ProgressionService(repository), repository, audioController)

    // --- Loading the Boat (stackbuild, descending weight) ---

    @Test
    fun `initial loading state holds all 6 boat items, none placed`() {
        val state = createViewModel().uiState.value.loadingState

        assertEquals(JesusCalmsStormContent.boatItemIds.size, state.itemIds.size)
        assertTrue(state.placedOrder.isEmpty())
    }

    @Test
    fun `initial boat item weights are 6 distinct numbers 1-99`() {
        val weights = createViewModel().uiState.value.boatItemWeights

        assertEquals(JesusCalmsStormContent.boatItemIds.toSet(), weights.keys)
        assertEquals(weights.size, weights.values.toSet().size)
        assertTrue(weights.values.all { it in 1..99 })
    }

    @Test
    fun `loading state's required order is descending by item weight`() {
        val state = createViewModel().uiState.value

        val expectedOrder = state.boatItemWeights.entries.sortedByDescending { it.value }.map { it.key }
        assertEquals(expectedOrder, state.loadingState.itemIds)
    }

    @Test
    fun `boat tray order is a permutation of the same 6 item ids`() {
        val state = createViewModel().uiState.value

        assertEquals(JesusCalmsStormContent.boatItemIds.toSet(), state.boatTrayOrder.toSet())
    }

    @Test
    fun `onBoatItemPlaced with the heaviest item advances and plays a sound`() {
        val audioController = FakeAudioController()
        val viewModel = createViewModel(audioController = audioController)
        val heaviestItemId = viewModel.uiState.value.loadingState.nextExpectedId!!

        viewModel.onBoatItemPlaced(heaviestItemId)

        assertEquals(listOf(heaviestItemId), viewModel.uiState.value.loadingState.placedOrder)
        assertEquals(listOf(SoundEffect.ITEM_COLLECTED), audioController.playedEffects)
    }

    @Test
    fun `onBoatItemPlaced out of order does not advance and never fails`() {
        val viewModel = createViewModel()
        val state = viewModel.uiState.value.loadingState
        val outOfOrderItemId = state.itemIds.last()

        viewModel.onBoatItemPlaced(outOfOrderItemId)

        assertTrue(viewModel.uiState.value.loadingState.placedOrder.isEmpty())
        assertEquals(StackBuildOutcome.WRONG_ORDER, viewModel.uiState.value.loadingState.lastOutcome)
    }

    @Test
    fun `placing every boat item in the required order completes loading`() {
        val viewModel = createViewModel()
        val requiredOrder = viewModel.uiState.value.loadingState.itemIds

        requiredOrder.forEach { viewModel.onBoatItemPlaced(it) }

        assertEquals(requiredOrder, viewModel.uiState.value.loadingState.placedOrder)
        assertTrue(viewModel.uiState.value.loadingState.isComplete)
    }

    // --- Bailing the Boat (rhythmlane, catch semantics) ---

    @Test
    fun `initial bailing state holds the densest chart, not yet complete`() {
        val state = createViewModel().uiState.value.bailingState

        assertEquals(JesusCalmsStormContent.bailingChart, state.chart)
        assertEquals(JesusCalmsStormContent.BAILING_REQUIRED_HITS, state.requiredHits)
        assertFalse(state.isComplete)
    }

    @Test
    fun `onBailingLaneMoved clamps to the 3 lanes, never a failure`() {
        val viewModel = createViewModel()

        repeat(5) { viewModel.onBailingLaneMoved(-1) }
        assertEquals(0, viewModel.uiState.value.bailingLane)

        repeat(5) { viewModel.onBailingLaneMoved(1) }
        assertEquals(2, viewModel.uiState.value.bailingLane)
    }

    @Test
    fun `moving into a wave's lane before it lands auto-bails, never a failure`() {
        val viewModel = createViewModel()
        val note = JesusCalmsStormContent.bailingChart.notes.first()

        moveBailingLaneTo(viewModel, note.lane)
        viewModel.onBailingTimeAdvanced(note.hitTimeMs)

        assertEquals(1, viewModel.uiState.value.bailingState.hits)
        assertFalse(viewModel.uiState.value.bailingState.isComplete)
    }

    @Test
    fun `advancing time to a wave while in the wrong lane does not register a bail`() {
        val viewModel = createViewModel()
        val note = JesusCalmsStormContent.bailingChart.notes.first { it.lane != viewModel.uiState.value.bailingLane }

        viewModel.onBailingTimeAdvanced(note.hitTimeMs)

        assertEquals(0, viewModel.uiState.value.bailingState.hits)
    }

    @Test
    fun `completing all required bails plays a sound exactly once`() {
        val audioController = FakeAudioController()
        val viewModel = createViewModel(audioController = audioController)

        completeBailingChart(viewModel, JesusCalmsStormContent.bailingChart, JesusCalmsStormContent.BAILING_REQUIRED_HITS)

        assertTrue(viewModel.uiState.value.bailingState.isComplete)
        assertEquals(1, audioController.playedEffects.count { it == SoundEffect.ITEM_COLLECTED })
    }

    private fun completeBailingChart(viewModel: JesusCalmsStormViewModel, chart: RhythmLaneChart, requiredHits: Int) {
        var hits = 0
        var loopIndex = 0L
        while (hits < requiredHits) {
            chart.notes.forEach { note ->
                if (hits < requiredHits) {
                    moveBailingLaneTo(viewModel, note.lane)
                    viewModel.onBailingTimeAdvanced(loopIndex * chart.loopDurationMs + note.hitTimeMs)
                    hits++
                }
            }
            loopIndex++
        }
    }

    private fun moveBailingLaneTo(viewModel: JesusCalmsStormViewModel, targetLane: Int) {
        while (viewModel.uiState.value.bailingLane != targetLane) {
            val delta = if (viewModel.uiState.value.bailingLane < targetLane) 1 else -1
            viewModel.onBailingLaneMoved(delta)
        }
    }

    // --- Choice / scene tracking / reward ---

    @Test
    fun `onChoiceSelected records the chosen option id`() {
        val viewModel = createViewModel()
        val optionId = JesusCalmsStormContent.choiceOptions.first().id

        viewModel.onChoiceSelected(optionId)

        assertEquals(optionId, viewModel.uiState.value.selectedChoiceId)
    }

    // --- Reaching Jesus (gridmaze) ---

    @Test
    fun `initial grid parses reachingJesusMapLayout into the correct dimensions and start position`() {
        val state = createViewModel().uiState.value.gridMazeState

        assertEquals(JesusCalmsStormContent.reachingJesusMapLayout.size, state.grid.size)
        assertEquals(JesusCalmsStormContent.reachingJesusMapLayout[0].length, state.grid[0].size)
        assertFalse(state.isComplete)
    }

    @Test
    fun `onReachingJesusDirectionPressed moves the player through the maze`() {
        val viewModel = createViewModel()

        viewModel.onReachingJesusDirectionPressed(JesusCalmsStormContent.reachingJesusSolutionPath[0])

        assertEquals(GridPosition(0, 1), viewModel.uiState.value.gridMazeState.playerPosition)
    }

    @Test
    fun `following the hand-verified solution path reaches Jesus`() {
        val viewModel = createViewModel()

        JesusCalmsStormContent.reachingJesusSolutionPath.forEach { direction ->
            viewModel.onReachingJesusDirectionPressed(direction)
        }

        assertTrue(viewModel.uiState.value.gridMazeState.isComplete)
    }

    // --- Peace, Be Still (rhythmlane, tap semantics, static word lanes) ---

    @Test
    fun `initial peaceBeStill state requires 3 hits, not yet complete`() {
        val state = createViewModel().uiState.value.peaceBeStillState

        assertEquals(JesusCalmsStormContent.peaceBeStillChart, state.chart)
        assertEquals(3, state.requiredHits)
        assertFalse(state.isComplete)
    }

    @Test
    fun `tapping the right word at the right moment advances and plays a sound`() {
        val audioController = FakeAudioController()
        val viewModel = createViewModel(audioController = audioController)
        val note = JesusCalmsStormContent.peaceBeStillChart.notes.first()

        viewModel.onPeaceBeStillWordTapped(note.lane, note.hitTimeMs)

        assertEquals(1, viewModel.uiState.value.peaceBeStillState.hits)
        assertEquals(listOf(SoundEffect.ITEM_COLLECTED), audioController.playedEffects)
    }

    @Test
    fun `tapping the wrong word does not advance`() {
        val viewModel = createViewModel()
        val note = JesusCalmsStormContent.peaceBeStillChart.notes.first()
        val wrongLane = (0..2).first { it != note.lane }

        viewModel.onPeaceBeStillWordTapped(wrongLane, note.hitTimeMs)

        assertEquals(0, viewModel.uiState.value.peaceBeStillState.hits)
    }

    @Test
    fun `tapping a later word out of order before earlier words are said does not advance`() {
        val viewModel = createViewModel()
        val stillNote = JesusCalmsStormContent.peaceBeStillChart.notes[2]

        viewModel.onPeaceBeStillWordTapped(stillNote.lane, stillNote.hitTimeMs)

        assertEquals(0, viewModel.uiState.value.peaceBeStillState.hits)
    }

    @Test
    fun `tapping words out of order across the whole loop never advances, only in-order does`() {
        val viewModel = createViewModel()
        val notes = JesusCalmsStormContent.peaceBeStillChart.notes

        // Try "be" and "still" first, at their own correct hit times: both must no-op.
        viewModel.onPeaceBeStillWordTapped(notes[1].lane, notes[1].hitTimeMs)
        viewModel.onPeaceBeStillWordTapped(notes[2].lane, notes[2].hitTimeMs)
        assertEquals(0, viewModel.uiState.value.peaceBeStillState.hits)

        // Now say them in the required order, starting a fresh loop.
        val loopDurationMs = JesusCalmsStormContent.peaceBeStillChart.loopDurationMs
        notes.forEachIndexed { index, note ->
            viewModel.onPeaceBeStillWordTapped(note.lane, loopDurationMs + note.hitTimeMs)
            assertEquals(index + 1, viewModel.uiState.value.peaceBeStillState.hits)
        }
        assertTrue(viewModel.uiState.value.peaceBeStillState.isComplete)
    }

    @Test
    fun `speaking peace be still in order completes the puzzle`() {
        val viewModel = createViewModel()

        JesusCalmsStormContent.peaceBeStillChart.notes.forEach { note ->
            viewModel.onPeaceBeStillWordTapped(note.lane, note.hitTimeMs)
        }

        assertTrue(viewModel.uiState.value.peaceBeStillState.isComplete)
    }

    // --- Scene tracking / reward ---

    @Test
    fun `onSceneCompleted marks the scene as a completed activity for Jesus Calms the Storm`() = runTest {
        val repository = FakePlayerProfileRepository()
        val viewModel = createViewModel(repository = repository)

        viewModel.onSceneCompleted("intro")
        advanceUntilIdle()

        val progress = repository.current().progressByChapter.getValue(ChapterId.JESUS_CALMS_STORM)
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
        assertTrue(ChapterId.JESUS_CALMS_STORM in repository.current().completedChapters)
        assertEquals(3, repository.current().stars)

        // Calling it again (e.g. a stale re-entry into the Reward screen) must not
        // double-award stars or re-complete the chapter (spec section 20).
        viewModel.onChapterFinished()
        advanceUntilIdle()

        assertEquals(3, repository.current().stars)

        job.cancel()
    }
}
