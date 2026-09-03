package com.bibleadventures.ui.screens.davidgoliath

import com.bibleadventures.FakeAudioController
import com.bibleadventures.FakePlayerProfileRepository
import com.bibleadventures.MainDispatcherRule
import com.bibleadventures.audio.SoundEffect
import com.bibleadventures.domain.model.ChapterId
import com.bibleadventures.game.puzzles.connectfour.ConnectFourOutcome
import com.bibleadventures.game.puzzles.connectfour.Slot
import com.bibleadventures.game.puzzles.slingshot.Vector2
import com.bibleadventures.game.stories.DavidGoliathContent
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
class DavidGoliathViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun createViewModel(
        repository: FakePlayerProfileRepository = FakePlayerProfileRepository(),
        audioController: FakeAudioController = FakeAudioController(),
    ) = DavidGoliathViewModel(ProgressionService(repository), repository, audioController)

    // --- Choose the Stones (connectfour) ---

    @Test
    fun `initial connect four state is an empty 7x6 board with the player to move`() {
        val state = createViewModel().uiState.value.connectFourState

        assertEquals(7, state.columns)
        assertEquals(6, state.rows)
        assertTrue(state.grid.all { row -> row.all { it == Slot.EMPTY } })
        assertTrue(state.isPlayerTurn)
        assertEquals(ConnectFourOutcome.NONE, state.outcome)
    }

    @Test
    fun `onConnectFourColumnTapped drops a player stone and hands the turn to the opponent, without a sound`() {
        val audioController = FakeAudioController()
        val viewModel = createViewModel(audioController = audioController)

        viewModel.onConnectFourColumnTapped(0)

        val state = viewModel.uiState.value.connectFourState
        assertEquals(Slot.PLAYER, state.grid[0][0])
        assertFalse(state.isPlayerTurn)
        assertTrue(audioController.playedEffects.isEmpty())
    }

    @Test
    fun `onConnectFourColumnTapped is a no-op once it's the opponent's turn`() {
        val viewModel = createViewModel()
        viewModel.onConnectFourColumnTapped(0)
        val afterFirstDrop = viewModel.uiState.value.connectFourState

        viewModel.onConnectFourColumnTapped(1)

        assertEquals(afterFirstDrop, viewModel.uiState.value.connectFourState)
    }

    @Test
    fun `onConnectFourOpponentMove is a no-op while it's still the player's turn`() {
        val viewModel = createViewModel()
        val initialState = viewModel.uiState.value.connectFourState

        viewModel.onConnectFourOpponentMove()

        assertEquals(initialState, viewModel.uiState.value.connectFourState)
    }

    @Test
    fun `onConnectFourReset returns to a fresh board regardless of prior progress`() {
        val viewModel = createViewModel()
        viewModel.onConnectFourColumnTapped(0)
        viewModel.onConnectFourOpponentMove()

        viewModel.onConnectFourReset()

        val state = viewModel.uiState.value.connectFourState
        assertTrue(state.grid.all { row -> row.all { it == Slot.EMPTY } })
        assertTrue(state.isPlayerTurn)
        assertEquals(ConnectFourOutcome.NONE, state.outcome)
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

    @Test
    fun `onChoiceSelected records the chosen option id`() {
        val viewModel = createViewModel()
        val optionId = DavidGoliathContent.choiceOptions.first().id

        viewModel.onChoiceSelected(optionId)

        assertEquals(optionId, viewModel.uiState.value.selectedChoiceId)
    }

    // --- Sling Practice (slingshot, moving rat targets) ---

    private val anchor = Vector2(0.5f, 0.9f)
    private val pullDown = Vector2(0f, 0.3f) // pulled down -> launches straight up
    private val pullUp = Vector2(0f, -0.3f) // pulled up, toward the rat -> launches away from it

    @Test
    fun `onStoneReleased plays a sound only on a hit`() {
        val audioController = FakeAudioController()
        val viewModel = createViewModel(audioController = audioController)
        val rat = Vector2(0.5f, 0.2f)

        viewModel.onStoneReleased(anchor, pullUp, rat)
        assertTrue(audioController.playedEffects.isEmpty())

        viewModel.onStoneReleased(anchor, pullDown, rat)
        assertEquals(listOf(SoundEffect.TARGET_HIT), audioController.playedEffects)
    }

    @Test
    fun `onStoneReleased does not play a sound on a miss`() {
        val audioController = FakeAudioController()
        val viewModel = createViewModel(audioController = audioController)

        viewModel.onStoneReleased(anchor, pullUp, Vector2(0.5f, 0.2f))

        assertTrue(audioController.playedEffects.isEmpty())
    }

    @Test
    fun `slingshotState requires 5 real hits to complete — escaped rats are free and never count`() {
        val viewModel = createViewModel()
        val rat = Vector2(0.5f, 0.2f)

        viewModel.onStoneReleased(anchor, pullDown, rat)
        assertEquals(1, viewModel.uiState.value.slingshotState.hits)
        assertFalse(viewModel.uiState.value.slingshotState.isComplete)

        // Escapes and a miss don't advance the hit count at all — the same
        // 5th hit is still needed no matter how many rats got away first.
        viewModel.onRatEscaped()
        viewModel.onRatEscaped()
        viewModel.onStoneReleased(anchor, pullUp, rat) // a miss
        assertEquals(1, viewModel.uiState.value.slingshotState.hits)

        repeat(4) { viewModel.onStoneReleased(anchor, pullDown, rat) }

        assertEquals(5, viewModel.uiState.value.slingshotState.hits)
        assertTrue(viewModel.uiState.value.slingshotState.isComplete)
    }

    @Test
    fun `onRatEscaped never plays a sound`() {
        val audioController = FakeAudioController()
        val viewModel = createViewModel(audioController = audioController)

        viewModel.onRatEscaped()

        assertTrue(audioController.playedEffects.isEmpty())
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

