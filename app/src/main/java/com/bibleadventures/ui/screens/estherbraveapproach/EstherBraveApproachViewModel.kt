package com.bibleadventures.ui.screens.estherbraveapproach

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bibleadventures.audio.AudioController
import com.bibleadventures.audio.SoundEffect
import com.bibleadventures.domain.model.ChapterId
import com.bibleadventures.domain.model.CharacterCustomization
import com.bibleadventures.domain.repository.PlayerProfileRepository
import com.bibleadventures.game.puzzles.meter.MeterGame
import com.bibleadventures.game.puzzles.meter.MeterGameState
import com.bibleadventures.game.puzzles.meter.TapPrecision
import com.bibleadventures.game.rewards.EstherBraveApproachReward
import com.bibleadventures.game.rewards.RewardCalculator
import com.bibleadventures.game.stories.EstherBraveApproachContent
import com.bibleadventures.progress.ProgressionService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EstherBraveApproachRewardResult(val stars: Int)

data class EstherBraveApproachUiState(
    val selectedChoiceId: String? = null,
    val meterState: MeterGameState = MeterGameState(requiredProgress = EstherBraveApproachContent.CORRIDOR_REQUIRED_PROGRESS),
    val reward: EstherBraveApproachRewardResult? = null,
)

class EstherBraveApproachViewModel(
    private val progressionService: ProgressionService,
    private val profileRepository: PlayerProfileRepository,
    private val audioController: AudioController,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EstherBraveApproachUiState())
    val uiState: StateFlow<EstherBraveApproachUiState> = _uiState.asStateFlow()

    val characterCustomization: StateFlow<CharacterCustomization> = profileRepository.profile
        .map { it.character }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = CharacterCustomization(),
        )

    /** Scene ids already completed on a prior playthrough — lets a puzzle's Continue button skip past re-solving it. */
    val previouslyCompletedSceneIds: StateFlow<Set<String>> = profileRepository.profile
        .map { it.progressByChapter[ChapterId.ESTHER_BRAVE_APPROACH]?.completedActivities ?: emptySet() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = emptySet(),
        )

    fun onChoiceSelected(choiceId: String) {
        _uiState.update { it.copy(selectedChoiceId = choiceId) }
    }

    fun onCorridorTapped(precision: TapPrecision) {
        _uiState.update { current ->
            val next = MeterGame.onTapped(current.meterState, precision)
            if (next.isComplete && !current.meterState.isComplete) {
                audioController.playSfx(SoundEffect.ITEM_COLLECTED)
            }
            current.copy(meterState = next)
        }
    }

    /** Records mid-adventure progress so "Continue Adventure" and a future resume can see it. */
    fun onSceneCompleted(sceneId: String) {
        viewModelScope.launch {
            profileRepository.markSceneCompleted(ChapterId.ESTHER_BRAVE_APPROACH, sceneId)
        }
    }

    /** Idempotent so rapid double-taps on the final CONTINUE never double-award (spec section 20). */
    fun onChapterFinished() {
        if (_uiState.value.reward != null) return
        viewModelScope.launch {
            val stars = RewardCalculator.calculateStars(chapterCompleted = true)
            progressionService.completeChapter(
                chapterId = ChapterId.ESTHER_BRAVE_APPROACH,
                stars = stars,
                badgeId = EstherBraveApproachReward.badge.id,
                scriptureCardId = EstherBraveApproachReward.scriptureCard.id,
            )
            audioController.playSfx(SoundEffect.REWARD_CELEBRATION)
            _uiState.update { it.copy(reward = EstherBraveApproachRewardResult(stars = stars)) }
        }
    }
}
