package com.bibleadventures.ui.screens.mainmenu

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class MainMenuUiState(
    // No progress persistence exists yet (arrives with the progression
    // repository in Milestone 5), so there is nothing to continue.
    val hasAdventureInProgress: Boolean = false,
)

class MainMenuViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(MainMenuUiState())
    val uiState: StateFlow<MainMenuUiState> = _uiState
}
