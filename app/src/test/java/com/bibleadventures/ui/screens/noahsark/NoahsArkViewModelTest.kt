package com.bibleadventures.ui.screens.noahsark

import com.bibleadventures.MainDispatcherRule
import com.bibleadventures.audio.AudioController
import com.bibleadventures.audio.SoundEffect
import com.bibleadventures.audio.MusicTrack
import com.bibleadventures.domain.model.ChapterId
import com.bibleadventures.domain.model.CharacterCustomization
import com.bibleadventures.domain.model.AdventureProgress
import com.bibleadventures.domain.model.PlayerProfile
import com.bibleadventures.domain.repository.PlayerProfileRepository
import com.bibleadventures.game.puzzles.matching.MatchOutcome
import com.bibleadventures.progress.ProgressionService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

private class FakePlayerProfileRepository(
    initial: PlayerProfile = PlayerProfile.DEFAULT,
) : PlayerProfileRepository {
    private val state = MutableStateFlow(initial)
    override val profile: Flow<PlayerProfile> = state

    override suspend fun updateCharacter(customization: CharacterCustomization) {
        state.value = state.value.copy(character = customization)
    }

    override suspend fun markSceneCompleted(chapterId: ChapterId, sceneId: String) = Unit

    override suspend fun completeChapter(
        chapterId: ChapterId,
        stars: Int,
        badgeId: String,
        scriptureCardId: String,
    ) {
        state.value = state.value.let { current ->
            val progress = (current.progressByChapter[chapterId] ?: AdventureProgress(chapterId = chapterId))
                .copy(completed = true, stars = stars)
            current.copy(
                progressByChapter = current.progressByChapter + (chapterId to progress),
                completedChapters = current.completedChapters + chapterId,
                stars = current.stars + stars,
                badges = current.badges + badgeId,
                scriptureCards = current.scriptureCards + scriptureCardId,
            )
        }
    }

    fun current(): PlayerProfile = state.value
}

private class RecordingAudioController : AudioController {
    val playedEffects = mutableListOf<SoundEffect>()
    override fun playMusic(track: MusicTrack) = Unit
    override fun stopMusic() = Unit
    override fun playSfx(effect: SoundEffect) {
        playedEffects += effect
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class NoahsArkViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun createViewModel(
        repository: FakePlayerProfileRepository = FakePlayerProfileRepository(),
        audioController: RecordingAudioController = RecordingAudioController(),
    ) = NoahsArkViewModel(ProgressionService(repository), repository, audioController)

    @Test
    fun `initial state has all 6 animal pairs, all sortable items, and all hidden items`() {
        val viewModel = createViewModel()
        val state = viewModel.uiState.value

        assertEquals(12, state.matchingState.items.size)
        assertEquals(6, state.dragSortState.items.size)
        assertEquals(4, state.hiddenObjectState.items.size)
        assertTrue(state.foundAnimalIds.isEmpty())
        assertTrue(state.collectedSupplyIds.isEmpty())
    }

    @Test
    fun `onAnimalFound adds the animal to foundAnimalIds`() {
        val viewModel = createViewModel()

        viewModel.onAnimalFound("lion")

        assertTrue("lion" in viewModel.uiState.value.foundAnimalIds)
    }

    @Test
    fun `onSupplyCollected adds the supply to collectedSupplyIds`() {
        val viewModel = createViewModel()

        viewModel.onSupplyCollected("bread")

        assertTrue("bread" in viewModel.uiState.value.collectedSupplyIds)
    }

    @Test
    fun `onMatchItemTapped plays a success sound only on a correct match`() {
        val audioController = RecordingAudioController()
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
