package com.bibleadventures.ui.screens.character

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bibleadventures.domain.model.Appearance
import com.bibleadventures.domain.model.CharacterCustomization
import com.bibleadventures.domain.model.ClothingColor
import com.bibleadventures.domain.model.Hairstyle
import com.bibleadventures.domain.repository.PlayerProfileRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CharacterUiState(val customization: CharacterCustomization = CharacterCustomization())

class CharacterViewModel(private val repository: PlayerProfileRepository) : ViewModel() {

    val uiState: StateFlow<CharacterUiState> = repository.profile
        .map { CharacterUiState(customization = it.character) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = CharacterUiState(),
        )

    fun onAppearanceSelected(value: Appearance) = updateCustomization { it.copy(appearance = value) }
    fun onHairstyleSelected(value: Hairstyle) = updateCustomization { it.copy(hairstyle = value) }
    fun onClothingSelected(value: ClothingColor) = updateCustomization { it.copy(clothing = value) }

    private fun updateCustomization(transform: (CharacterCustomization) -> CharacterCustomization) {
        viewModelScope.launch {
            repository.updateCharacter(transform(uiState.value.customization))
        }
    }
}
