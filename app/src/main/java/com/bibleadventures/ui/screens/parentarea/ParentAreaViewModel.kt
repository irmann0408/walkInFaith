package com.bibleadventures.ui.screens.parentarea

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bibleadventures.domain.repository.PlayerProfileRepository
import com.bibleadventures.game.rewards.RewardCatalog
import com.bibleadventures.game.stories.ChapterCatalog
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ParentAreaUiState(
    val chaptersCompleted: Int = 0,
    val totalChapters: Int = ChapterCatalog.all.size,
    val stars: Int = 0,
    val badgesEarned: Int = 0,
    val totalBadges: Int = RewardCatalog.badges.size,
    val scriptureCardsEarned: Int = 0,
    val totalScriptureCards: Int = RewardCatalog.scriptureCards.size,
    val totalPlayTimeMillis: Long = 0L,
)

class ParentAreaViewModel(private val repository: PlayerProfileRepository) : ViewModel() {

    val uiState: StateFlow<ParentAreaUiState> = repository.profile
        .map { profile ->
            ParentAreaUiState(
                chaptersCompleted = profile.completedChapters.size,
                stars = profile.stars,
                badgesEarned = profile.badges.size,
                scriptureCardsEarned = profile.scriptureCards.size,
                totalPlayTimeMillis = profile.totalPlayTimeMillis,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = ParentAreaUiState(),
        )

    fun onResetProgressConfirmed() {
        viewModelScope.launch { repository.resetProgress() }
    }
}
