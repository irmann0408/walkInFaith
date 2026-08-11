package com.bibleadventures.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bibleadventures.domain.model.AudioSettings
import com.bibleadventures.domain.repository.PlayerProfileRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(val audioSettings: AudioSettings = AudioSettings())

class SettingsViewModel(private val repository: PlayerProfileRepository) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = repository.profile
        .map { SettingsUiState(audioSettings = it.audioSettings) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = SettingsUiState(),
        )

    fun onMusicToggled(enabled: Boolean) = updateAudioSettings { it.copy(musicEnabled = enabled) }
    fun onSoundEffectsToggled(enabled: Boolean) = updateAudioSettings { it.copy(soundEffectsEnabled = enabled) }
    fun onNarrationToggled(enabled: Boolean) = updateAudioSettings { it.copy(narrationEnabled = enabled) }

    private fun updateAudioSettings(transform: (AudioSettings) -> AudioSettings) {
        viewModelScope.launch {
            repository.updateAudioSettings(transform(uiState.value.audioSettings))
        }
    }
}
