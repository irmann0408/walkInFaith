package com.bibleadventures.ui.screens.worldmap

import com.bibleadventures.MainDispatcherRule
import com.bibleadventures.domain.model.AdventureProgress
import com.bibleadventures.domain.model.ChapterId
import com.bibleadventures.domain.model.CharacterCustomization
import com.bibleadventures.domain.model.ChapterStatus
import com.bibleadventures.domain.model.PlayerProfile
import com.bibleadventures.domain.repository.PlayerProfileRepository
import com.bibleadventures.progress.ProgressionService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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
                badges = current.badges + badgeId,
                scriptureCards = current.scriptureCards + scriptureCardId,
            )
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class WorldMapViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `only Noah's Ark starts unlocked, with no stars anywhere`() = runTest {
        val repository = FakePlayerProfileRepository()
        val viewModel = WorldMapViewModel(ProgressionService(repository), repository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val nodes = viewModel.uiState.value.nodes.associateBy { it.chapter.id }
        assertEquals(ChapterStatus.UNLOCKED, nodes.getValue(ChapterId.NOAHS_ARK).status)
        assertEquals(ChapterStatus.LOCKED, nodes.getValue(ChapterId.DAVID_GOLIATH).status)
        assertEquals(0, nodes.getValue(ChapterId.NOAHS_ARK).stars)
    }

    @Test
    fun `completing a chapter updates its status stars and unlocks the next`() = runTest {
        val repository = FakePlayerProfileRepository()
        val viewModel = WorldMapViewModel(ProgressionService(repository), repository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        repository.completeChapter(ChapterId.NOAHS_ARK, stars = 3, badgeId = "ARK_BUILDER", scriptureCardId = "GENESIS_6_22")
        advanceUntilIdle()

        val nodes = viewModel.uiState.value.nodes.associateBy { it.chapter.id }
        assertEquals(ChapterStatus.COMPLETED, nodes.getValue(ChapterId.NOAHS_ARK).status)
        assertEquals(3, nodes.getValue(ChapterId.NOAHS_ARK).stars)
        assertEquals(ChapterStatus.UNLOCKED, nodes.getValue(ChapterId.DAVID_GOLIATH).status)
    }
}
