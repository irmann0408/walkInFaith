package com.bibleadventures.ui.screens.settings

import com.bibleadventures.FakePlayerProfileRepository
import com.bibleadventures.MainDispatcherRule
import com.bibleadventures.domain.model.AudioSettings
import com.bibleadventures.domain.model.PlayerProfile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `uiState reflects the repository's current audio settings`() = runTest {
        val startingSettings = AudioSettings(musicEnabled = false)
        val repository = FakePlayerProfileRepository(PlayerProfile.DEFAULT.copy(audioSettings = startingSettings))
        val viewModel = SettingsViewModel(repository)
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertEquals(startingSettings, viewModel.uiState.value.audioSettings)

        job.cancel()
    }

    @Test
    fun `each toggle updates uiState and persists through the repository independently`() = runTest {
        val repository = FakePlayerProfileRepository()
        val viewModel = SettingsViewModel(repository)
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onMusicToggled(false)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.audioSettings.musicEnabled)
        assertTrue(viewModel.uiState.value.audioSettings.soundEffectsEnabled)
        assertTrue(viewModel.uiState.value.audioSettings.narrationEnabled)

        viewModel.onSoundEffectsToggled(false)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.audioSettings.soundEffectsEnabled)

        viewModel.onNarrationToggled(false)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.audioSettings.narrationEnabled)

        assertEquals(AudioSettings(false, false, false), repository.current().audioSettings)

        job.cancel()
    }
}
