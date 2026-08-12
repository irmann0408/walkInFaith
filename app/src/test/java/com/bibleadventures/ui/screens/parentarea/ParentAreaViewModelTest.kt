package com.bibleadventures.ui.screens.parentarea

import com.bibleadventures.FakePlayerProfileRepository
import com.bibleadventures.MainDispatcherRule
import com.bibleadventures.domain.model.AudioSettings
import com.bibleadventures.domain.model.Appearance
import com.bibleadventures.domain.model.CharacterCustomization
import com.bibleadventures.domain.model.ChapterId
import com.bibleadventures.domain.model.PlayerProfile
import com.bibleadventures.game.rewards.RewardCatalog
import com.bibleadventures.game.stories.ChapterCatalog
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ParentAreaViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `initial state totals mirror the reward and chapter catalogs`() = runTest {
        val viewModel = ParentAreaViewModel(FakePlayerProfileRepository())
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(0, state.chaptersCompleted)
        assertEquals(ChapterCatalog.all.size, state.totalChapters)
        assertEquals(0, state.stars)
        assertEquals(0, state.badgesEarned)
        assertEquals(RewardCatalog.badges.size, state.totalBadges)
        assertEquals(0, state.scriptureCardsEarned)
        assertEquals(RewardCatalog.scriptureCards.size, state.totalScriptureCards)
        assertEquals(0L, state.totalPlayTimeMillis)

        job.cancel()
    }

    @Test
    fun `uiState reflects earned progress and play time from the profile`() = runTest {
        val profile = PlayerProfile.DEFAULT.copy(
            completedChapters = setOf(ChapterId.NOAHS_ARK, ChapterId.DAVID_GOLIATH),
            stars = 5,
            badges = setOf("ARK_BUILDER", "BRAVE_HEART"),
            scriptureCards = setOf("GENESIS_6_22"),
            totalPlayTimeMillis = 90_000L,
        )
        val viewModel = ParentAreaViewModel(FakePlayerProfileRepository(initial = profile))
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.chaptersCompleted)
        assertEquals(5, state.stars)
        assertEquals(2, state.badgesEarned)
        assertEquals(1, state.scriptureCardsEarned)
        assertEquals(90_000L, state.totalPlayTimeMillis)

        job.cancel()
    }

    @Test
    fun `onResetProgressConfirmed clears progress but leaves character, audio and play time untouched`() = runTest {
        val customCharacter = CharacterCustomization(appearance = Appearance.GIRL)
        val customAudio = AudioSettings(musicEnabled = false)
        val profile = PlayerProfile.DEFAULT.copy(
            character = customCharacter,
            audioSettings = customAudio,
            totalPlayTimeMillis = 60_000L,
            completedChapters = setOf(ChapterId.NOAHS_ARK),
            stars = 3,
            badges = setOf("ARK_BUILDER"),
            scriptureCards = setOf("GENESIS_6_22"),
        )
        val repository = FakePlayerProfileRepository(initial = profile)
        val viewModel = ParentAreaViewModel(repository)
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onResetProgressConfirmed()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(0, state.chaptersCompleted)
        assertEquals(0, state.stars)
        assertEquals(0, state.badgesEarned)
        assertEquals(0, state.scriptureCardsEarned)
        assertEquals(60_000L, state.totalPlayTimeMillis)
        assertEquals(customCharacter, repository.current().character)
        assertEquals(customAudio, repository.current().audioSettings)

        job.cancel()
    }
}
