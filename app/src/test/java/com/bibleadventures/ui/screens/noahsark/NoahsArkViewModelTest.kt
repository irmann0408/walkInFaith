package com.bibleadventures.ui.screens.noahsark

import com.bibleadventures.FakeAudioController
import com.bibleadventures.FakePlayerProfileRepository
import com.bibleadventures.MainDispatcherRule
import com.bibleadventures.audio.SoundEffect
import com.bibleadventures.domain.model.ChapterId
import com.bibleadventures.game.puzzles.matching.MatchOutcome
import com.bibleadventures.game.stories.NoahsArkContent
import com.bibleadventures.progress.ProgressionService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NoahsArkViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun createViewModel(
        repository: FakePlayerProfileRepository = FakePlayerProfileRepository(),
        audioController: FakeAudioController = FakeAudioController(),
    ) = NoahsArkViewModel(ProgressionService(repository), repository, audioController)

    @Test
    fun `initial state has one matching pair per animal, load-ark baskets solvable per deck, and all tool hotspots`() {
        val viewModel = createViewModel()
        val state = viewModel.uiState.value

        assertEquals(16, state.matchingState.items.size)
        assertEquals(NoahsArkContent.loadArkDeckTargets, state.groupFillState.circleTargets)
        assertEquals(10, state.hiddenObjectState.items.size)
    }

    @Test
    fun `every load-ark basket sums exactly to its own deck's target, and every basket has a supply kind`() {
        val state = createViewModel().uiState.value

        NoahsArkContent.loadArkDeckTargets.forEachIndexed { deckIndex, target ->
            val deckBaskets = state.groupFillState.families.filter { it.id.startsWith("basket_${deckIndex}_") }
            assertEquals(target, deckBaskets.sumOf { it.headcount })
        }
        val basketIds = state.groupFillState.families.map { it.id }.toSet()
        assertEquals(basketIds, state.loadArkBasketSupplyKinds.keys)
        assertTrue(state.loadArkBasketSupplyKinds.values.all { kindId -> NoahsArkContent.loadArkSupplyKinds.any { it.id == kindId } })
    }

    @Test
    fun `onBasketDropped places a basket that fits its own deck, and plays a collected sound`() {
        val audioController = FakeAudioController()
        val viewModel = createViewModel(audioController = audioController)
        val firstBasketId = viewModel.uiState.value.groupFillState.remainingFamilyIds.first()
        val deckIndex = firstBasketId.substringAfter("basket_").substringBefore("_").toInt()

        viewModel.onBasketDropped(firstBasketId, deckIndex)

        assertTrue(firstBasketId in viewModel.uiState.value.groupFillState.placedFamilyIds)
        assertEquals(listOf(SoundEffect.ITEM_COLLECTED), audioController.playedEffects)
    }

    @Test
    fun `hiddenObjectState items are the fixed find-tools hotspots, positions and ids unchanged`() {
        val state = createViewModel().uiState.value

        val expectedIds = NoahsArkContent.findToolsHotspots.map { it.id }.toSet()
        val expectedPositions = NoahsArkContent.findToolsHotspots.map { it.position }.toSet()

        assertEquals(expectedIds, state.hiddenObjectState.items.map { it.id }.toSet())
        assertEquals(expectedPositions, state.hiddenObjectState.items.map { it.position }.toSet())
    }

    @Test
    fun `onHiddenItemTapped marks the tapped tool hotspot as found`() {
        val viewModel = createViewModel()
        val firstHotspotId = viewModel.uiState.value.hiddenObjectState.items.first().id

        viewModel.onHiddenItemTapped(firstHotspotId)

        assertTrue(firstHotspotId in viewModel.uiState.value.hiddenObjectState.foundIds)
    }

    @Test
    fun `onFindToolsBackgroundTapped sets the wrong-tap outcome without marking any tool found`() {
        val viewModel = createViewModel()

        viewModel.onFindToolsBackgroundTapped()

        assertEquals(DecoyTapOutcome.DECOY_TAPPED, viewModel.uiState.value.lastFindToolsWrongTapOutcome)
        assertTrue(viewModel.uiState.value.hiddenObjectState.foundIds.isEmpty())
    }

    @Test
    fun `onSceneCompleted marks the scene as a completed activity for Noah's Ark`() = runTest {
        val repository = FakePlayerProfileRepository()
        val viewModel = createViewModel(repository = repository)

        viewModel.onSceneCompleted("intro")
        advanceUntilIdle()

        val progress = repository.current().progressByChapter.getValue(ChapterId.NOAHS_ARK)
        assertTrue("intro" in progress.completedActivities)
        assertTrue(!progress.completed)
    }

    @Test
    fun `onMatchItemTapped plays a success sound only on a correct match`() {
        val audioController = FakeAudioController()
        val viewModel = createViewModel(audioController = audioController)
        val items = viewModel.uiState.value.matchingState.items
        val firstPair = items.groupBy { it.pairKey }.values.first()

        viewModel.onMatchItemTapped(firstPair[0].id)
        viewModel.onMatchItemTapped(firstPair[1].id)

        assertEquals(MatchOutcome.CORRECT, viewModel.uiState.value.matchingState.lastOutcome)
        assertEquals(listOf(SoundEffect.MATCH_SUCCESS), audioController.playedEffects)
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
        assertTrue(ChapterId.NOAHS_ARK in repository.current().completedChapters)
        assertEquals(3, repository.current().stars)

        // Calling it again (e.g. a stale re-entry into the Reward screen) must not
        // double-award stars or re-complete the chapter (spec section 20).
        viewModel.onChapterFinished()
        advanceUntilIdle()

        assertEquals(3, repository.current().stars)

        job.cancel()
    }
}
