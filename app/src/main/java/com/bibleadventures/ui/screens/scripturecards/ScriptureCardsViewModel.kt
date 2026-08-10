package com.bibleadventures.ui.screens.scripturecards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bibleadventures.domain.model.ScriptureCard
import com.bibleadventures.domain.repository.PlayerProfileRepository
import com.bibleadventures.game.rewards.RewardCatalog
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class ScriptureCardUiState(val card: ScriptureCard, val earned: Boolean)

data class ScriptureCardsUiState(val cards: List<ScriptureCardUiState> = emptyList())

class ScriptureCardsViewModel(
    repository: PlayerProfileRepository,
    catalog: List<ScriptureCard> = RewardCatalog.scriptureCards,
) : ViewModel() {

    val uiState: StateFlow<ScriptureCardsUiState> = repository.profile
        .map { profile ->
            ScriptureCardsUiState(
                cards = catalog.map { card -> ScriptureCardUiState(card = card, earned = card.id in profile.scriptureCards) },
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = ScriptureCardsUiState(),
        )
}
