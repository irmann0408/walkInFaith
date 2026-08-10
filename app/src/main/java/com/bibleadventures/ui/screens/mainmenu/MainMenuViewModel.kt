package com.bibleadventures.ui.screens.mainmenu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bibleadventures.domain.repository.PlayerProfileRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class MainMenuUiState(
    val hasAdventureInProgress: Boolean = false,
)

class MainMenuViewModel(repository: PlayerProfileRepository) : ViewModel() {
    val uiState: StateFlow<MainMenuUiState> = repository.profile
        .map { profile ->
            MainMenuUiState(
                hasAdventureInProgress = profile.progressByChapter.values
                    .any { it.completedActivities.isNotEmpty() && !it.completed },
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = MainMenuUiState(),
        )
}
