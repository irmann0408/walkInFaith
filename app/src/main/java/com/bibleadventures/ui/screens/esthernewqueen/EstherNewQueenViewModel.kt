package com.bibleadventures.ui.screens.esthernewqueen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bibleadventures.audio.AudioController
import com.bibleadventures.audio.SoundEffect
import com.bibleadventures.domain.model.ChapterId
import com.bibleadventures.domain.model.CharacterCustomization
import com.bibleadventures.domain.repository.PlayerProfileRepository
import com.bibleadventures.game.puzzles.hiddenobject.HiddenItem
import com.bibleadventures.game.puzzles.hiddenobject.HiddenObjectGame
import com.bibleadventures.game.puzzles.hiddenobject.HiddenObjectGameState
import com.bibleadventures.game.rewards.EstherNewQueenReward
import com.bibleadventures.game.rewards.RewardCalculator
import com.bibleadventures.game.stories.EstherNewQueenContent
import com.bibleadventures.progress.ProgressionService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EstherNewQueenRewardResult(val stars: Int)

data class EstherNewQueenUiState(
    val hiddenObjectState: HiddenObjectGameState,
    val selectedChoiceId: String? = null,
    val reward: EstherNewQueenRewardResult? = null,
)

class EstherNewQueenViewModel(
    private val progressionService: ProgressionService,
    private val profileRepository: PlayerProfileRepository,
    private val audioController: AudioController,
) : ViewModel() {

    private val _uiState = MutableStateFlow(createInitialState())
    val uiState: StateFlow<EstherNewQueenUiState> = _uiState.asStateFlow()

    val characterCustomization: StateFlow<CharacterCustomization> = profileRepository.profile
        .map { it.character }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = CharacterCustomization(),
        )

    /** Scene ids already completed on a prior playthrough — lets a puzzle's Continue button skip past re-solving it. */
    val previouslyCompletedSceneIds: StateFlow<Set<String>> = profileRepository.profile
        .map { it.progressByChapter[ChapterId.ESTHER_NEW_QUEEN]?.completedActivities ?: emptySet() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = emptySet(),
        )

    fun onAttireItemTapped(itemId: String) {
        _uiState.update { current ->
            val next = HiddenObjectGame.onItemTapped(current.hiddenObjectState, itemId)
            if (next.foundIds.size > current.hiddenObjectState.foundIds.size) {
                audioController.playSfx(SoundEffect.ITEM_COLLECTED)
            }
            current.copy(hiddenObjectState = next)
        }
    }

    fun onChoiceSelected(choiceId: String) {
        _uiState.update { it.copy(selectedChoiceId = choiceId) }
    }

    /** Records mid-adventure progress so "Continue Adventure" and a future resume can see it. */
    fun onSceneCompleted(sceneId: String) {
        viewModelScope.launch {
            profileRepository.markSceneCompleted(ChapterId.ESTHER_NEW_QUEEN, sceneId)
        }
    }

    /** Idempotent so rapid double-taps on the final CONTINUE never double-award (spec section 20). */
    fun onChapterFinished() {
        if (_uiState.value.reward != null) return
        viewModelScope.launch {
            val stars = RewardCalculator.calculateStars(chapterCompleted = true)
            progressionService.completeChapter(
                chapterId = ChapterId.ESTHER_NEW_QUEEN,
                stars = stars,
                badgeId = EstherNewQueenReward.badge.id,
                scriptureCardId = EstherNewQueenReward.scriptureCard.id,
            )
            audioController.playSfx(SoundEffect.REWARD_CELEBRATION)
            _uiState.update { it.copy(reward = EstherNewQueenRewardResult(stars = stars)) }
        }
    }

    private fun createInitialState(): EstherNewQueenUiState {
        val items = EstherNewQueenContent.royalAttireItems.map { def ->
            HiddenItem(id = def.id, position = def.position, iconRes = def.iconRes, contentDescriptionRes = def.nameRes)
        }
        return EstherNewQueenUiState(hiddenObjectState = HiddenObjectGameState(items = items))
    }
}
