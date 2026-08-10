package com.bibleadventures.ui.screens.badges

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bibleadventures.domain.model.Badge
import com.bibleadventures.domain.repository.PlayerProfileRepository
import com.bibleadventures.game.rewards.RewardCatalog
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class BadgeUiState(val badge: Badge, val earned: Boolean)

data class BadgesUiState(val badges: List<BadgeUiState> = emptyList())

class BadgesViewModel(
    repository: PlayerProfileRepository,
    catalog: List<Badge> = RewardCatalog.badges,
) : ViewModel() {

    val uiState: StateFlow<BadgesUiState> = repository.profile
        .map { profile ->
            BadgesUiState(
                badges = catalog.map { badge -> BadgeUiState(badge = badge, earned = badge.id in profile.badges) },
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = BadgesUiState(),
        )
}
