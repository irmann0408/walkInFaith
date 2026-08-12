package com.bibleadventures.ui.screens.davidgoliath

import com.bibleadventures.FakeAudioController
import com.bibleadventures.FakePlayerProfileRepository
import com.bibleadventures.MainDispatcherRule
import com.bibleadventures.audio.SoundEffect
import com.bibleadventures.domain.model.ChapterId
import com.bibleadventures.game.puzzles.rhythmlane.RhythmLaneChart
import com.bibleadventures.game.stories.DavidGoliathContent
import com.bibleadventures.progress.ProgressionService
import com.bibleadventures.ui.screens.noahsark.DecoyTapOutcome
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DavidGoliathViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun createViewModel(
        repository: FakePlayerProfileRepository = FakePlayerProfileRepository(),
        audioController: FakeAudioController = FakeAudioController(),
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
    fun `initial sheep counting state has 10 cards across 5 distinct pair keys, all face down`() {
        val state = createViewModel().uiState.value.sheepCountingState

        assertEquals(10, state.items.size)
        assertEquals(5, state.items.map { it.pairKey }.toSet().size)
        assertTrue(state.matchedIds.isEmpty())
        assertTrue(state.items.none { state.isFaceUp(it.id) })
    }

    @Test
    fun `onSheepCountingItemTapped plays a sound only on a correct pair`() {
        val audioController = FakeAudioController()
        val viewModel = createViewModel(audioController = audioController)
        val state = viewModel.uiState.value.sheepCountingState
        val pairKey = state.items.first().pairKey
        val samePair = state.items.filter { it.pairKey == pairKey }
        val mismatched = state.items.first { it.pairKey != pairKey }

        viewModel.onSheepCountingItemTapped(samePair[0].id)
        viewModel.onSheepCountingItemTapped(mismatched.id)
        assertTrue(audioController.playedEffects.isEmpty())

        // The next tap after a shown mismatch flips the pair back down and starts fresh.
        viewModel.onSheepCountingItemTapped(samePair[0].id)
        viewModel.onSheepCountingItemTapped(samePair[1].id)
        assertEquals(listOf(SoundEffect.MATCH_SUCCESS), audioController.playedEffects)
    }

    // --- Crossing the Valley (rhythmlane, avoid semantics) ---

    @Test
    fun `onCrossingValleyLaneMoved clamps to the 3 lanes, never a failure`() {
        val viewModel = createViewModel()

        repeat(5) { viewModel.onCrossingValleyLaneMoved(-1) }
        assertEquals(0, viewModel.uiState.value.characterLane)

        repeat(5) { viewModel.onCrossingValleyLaneMoved(1) }
        assertEquals(2, viewModel.uiState.value.characterLane)
    }

    @Test
    fun `moving out of a rock's lane before it lands registers an avoid and plays a sound`() {
        val audioController = FakeAudioController()
        val viewModel = createViewModel(audioController = audioController)
        val note = DavidGoliathContent.crossingValleyChart.notes.first()
        val safeLane = (0..2).first { it != note.lane }

        moveCrossingValleyLaneTo(viewModel, safeLane)
        viewModel.onCrossingValleyTimeAdvanced(note.hitTimeMs)

        assertEquals(1, viewModel.uiState.value.crossingValleyState.hits)
        assertFalse(viewModel.uiState.value.crossingValleyState.isComplete)
        assertEquals(listOf(SoundEffect.OBSTACLE_DODGED), audioController.playedEffects)
    }

    @Test
    fun `staying in a rock's own lane when it lands does not register an avoid`() {
        val viewModel = createViewModel()
        val note = DavidGoliathContent.crossingValleyChart.notes.first()

        moveCrossingValleyLaneTo(viewModel, note.lane)
        viewModel.onCrossingValleyTimeAdvanced(note.hitTimeMs)

        assertEquals(0, viewModel.uiState.value.crossingValleyState.hits)
    }

    @Test
    fun `completing all 3 required avoids marks Crossing the Valley complete`() {
        val viewModel = createViewModel()

        completeCrossingValley(viewModel, DavidGoliathContent.crossingValleyChart, DavidGoliathContent.CROSSING_VALLEY_REQUIRED_AVOIDS)

        assertTrue(viewModel.uiState.value.crossingValleyState.isComplete)
    }

    /** Mirrors Feeding5000ViewModelTest's `completeCatchingChart` — loops the chart as many times as needed, parking the character out of each note's lane before its exact hit time. */
    private fun completeCrossingValley(viewModel: DavidGoliathViewModel, chart: RhythmLaneChart, requiredAvoids: Int) {
        var hits = 0
        var loopIndex = 0L
        while (hits < requiredAvoids) {
            chart.notes.forEach { note ->
                if (hits < requiredAvoids) {
                    val safeLane = (0..2).first { it != note.lane }
                    moveCrossingValleyLaneTo(viewModel, safeLane)
                    viewModel.onCrossingValleyTimeAdvanced(loopIndex * chart.loopDurationMs + note.hitTimeMs)
                    hits++
                }
            }
            loopIndex++
        }
    }

    private fun moveCrossingValleyLaneTo(viewModel: DavidGoliathViewModel, targetLane: Int) {
        while (viewModel.uiState.value.characterLane != targetLane) {
            val delta = if (viewModel.uiState.value.characterLane < targetLane) 1 else -1
            viewModel.onCrossingValleyLaneMoved(delta)
        }
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
        val audioController = FakeAudioController()
        val viewModel = createViewModel(audioController = audioController)

        viewModel.onStoneReleased(aimedPosition = 0.1f, markPosition = 0.9f, shieldMinFraction = 0.35f, shieldMaxFraction = 0.65f)
        assertTrue(audioController.playedEffects.isEmpty())

        viewModel.onStoneReleased(aimedPosition = 0.5f, markPosition = 0.5f, shieldMinFraction = 0.35f, shieldMaxFraction = 0.65f)
        assertEquals(listOf(SoundEffect.TARGET_HIT), audioController.playedEffects)
    }

    @Test
    fun `onStoneReleased does not play a sound when the aim matches the mark outside the shield`() {
        val audioController = FakeAudioController()
        val viewModel = createViewModel(audioController = audioController)

        viewModel.onStoneReleased(aimedPosition = 0.15f, markPosition = 0.15f, shieldMinFraction = 0.35f, shieldMaxFraction = 0.65f)

        assertTrue(audioController.playedEffects.isEmpty())
    }

    @Test
    fun `slingshotState requires 3 hits to complete, a miss between hits does not reset progress`() {
        val viewModel = createViewModel()

        viewModel.onStoneReleased(aimedPosition = 0.5f, markPosition = 0.5f, shieldMinFraction = 0.35f, shieldMaxFraction = 0.65f)
        assertEquals(1, viewModel.uiState.value.slingshotState.hits)
        assertFalse(viewModel.uiState.value.slingshotState.isComplete)

        viewModel.onStoneReleased(aimedPosition = 0.1f, markPosition = 0.9f, shieldMinFraction = 0.35f, shieldMaxFraction = 0.65f)
        assertEquals(1, viewModel.uiState.value.slingshotState.hits)

        viewModel.onStoneReleased(aimedPosition = 0.5f, markPosition = 0.5f, shieldMinFraction = 0.35f, shieldMaxFraction = 0.65f)
        viewModel.onStoneReleased(aimedPosition = 0.5f, markPosition = 0.5f, shieldMinFraction = 0.35f, shieldMaxFraction = 0.65f)

        assertEquals(3, viewModel.uiState.value.slingshotState.hits)
        assertTrue(viewModel.uiState.value.slingshotState.isComplete)
    }

    @Test
    fun `a hit relocates the practice shield to a different zone than it was in`() {
        val viewModel = createViewModel()
        val startZone = viewModel.uiState.value.shieldZone

        viewModel.onStoneReleased(aimedPosition = 0.5f, markPosition = 0.5f, shieldMinFraction = 0.35f, shieldMaxFraction = 0.65f)

        assertEquals(1, viewModel.uiState.value.slingshotState.hits)
        assertNotEquals(startZone, viewModel.uiState.value.shieldZone)
    }

    @Test
    fun `a miss does not relocate the practice shield`() {
        val viewModel = createViewModel()
        val startZone = viewModel.uiState.value.shieldZone

        viewModel.onStoneReleased(aimedPosition = 0.1f, markPosition = 0.9f, shieldMinFraction = 0.35f, shieldMaxFraction = 0.65f)

        assertEquals(startZone, viewModel.uiState.value.shieldZone)
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

