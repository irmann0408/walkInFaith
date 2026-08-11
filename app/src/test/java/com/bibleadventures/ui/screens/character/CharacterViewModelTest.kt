package com.bibleadventures.ui.screens.character

import com.bibleadventures.MainDispatcherRule
import com.bibleadventures.domain.model.Appearance
import com.bibleadventures.domain.model.AudioSettings
import com.bibleadventures.domain.model.ChapterId
import com.bibleadventures.domain.model.Clothing
import com.bibleadventures.domain.model.CharacterCustomization
import com.bibleadventures.domain.model.Hairstyle
import com.bibleadventures.domain.model.PlayerProfile
import com.bibleadventures.domain.model.SkinTone
import com.bibleadventures.domain.repository.PlayerProfileRepository
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

    override suspend fun updateAudioSettings(audioSettings: AudioSettings) {
        state.value = state.value.copy(audioSettings = audioSettings)
    }

    override suspend fun markSceneCompleted(chapterId: ChapterId, sceneId: String) = Unit

    override suspend fun completeChapter(
        chapterId: ChapterId,
        stars: Int,
        badgeId: String,
        scriptureCardIds: List<String>,
    ) = Unit
}

@OptIn(ExperimentalCoroutinesApi::class)
class CharacterViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `uiState reflects the repository's current character`() = runTest {
        val startingCharacter = CharacterCustomization(appearance = Appearance.GIRL)
        val repository = FakePlayerProfileRepository(PlayerProfile.DEFAULT.copy(character = startingCharacter))
        val viewModel = CharacterViewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertEquals(startingCharacter, viewModel.uiState.value.customization)
    }

    @Test
    fun `selecting an option updates uiState and persists through the repository`() = runTest {
        val repository = FakePlayerProfileRepository()
        val viewModel = CharacterViewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onAppearanceSelected(Appearance.GIRL)
        advanceUntilIdle()
        assertEquals(Appearance.GIRL, viewModel.uiState.value.customization.appearance)

        viewModel.onHairstyleSelected(Hairstyle.BRAIDED)
        advanceUntilIdle()
        assertEquals(Hairstyle.BRAIDED, viewModel.uiState.value.customization.hairstyle)

        viewModel.onSkinToneSelected(SkinTone.TONE_3)
        advanceUntilIdle()
        assertEquals(SkinTone.TONE_3, viewModel.uiState.value.customization.skinTone)

        viewModel.onClothingSelected(Clothing.ROBE_RED)
        advanceUntilIdle()
        assertEquals(Clothing.ROBE_RED, viewModel.uiState.value.customization.clothing)
    }
}
