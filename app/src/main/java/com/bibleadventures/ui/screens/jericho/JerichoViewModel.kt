package com.bibleadventures.ui.screens.jericho

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bibleadventures.audio.AudioController
import com.bibleadventures.audio.SoundEffect
import com.bibleadventures.domain.model.ChapterId
import com.bibleadventures.domain.model.CharacterCustomization
import com.bibleadventures.domain.repository.PlayerProfileRepository
import com.bibleadventures.game.puzzles.decisionpath.DecisionOutcome
import com.bibleadventures.game.puzzles.decisionpath.DecisionPathGame
import com.bibleadventures.game.puzzles.decisionpath.DecisionPathGameState
import com.bibleadventures.game.rewards.JerichoReward
import com.bibleadventures.game.rewards.RewardCalculator
import com.bibleadventures.game.stories.JerichoContent
import com.bibleadventures.progress.ProgressionService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class JerichoRewardResult(val stars: Int)

data class JerichoUiState(
    val selectedChoiceId: String? = null,
    val decisionPathState: DecisionPathGameState = DecisionPathGameState(steps = JerichoContent.marchSteps),
    val reward: JerichoRewardResult? = null,
)

class JerichoViewModel(
    private val progressionService: ProgressionService,
    private val profileRepository: PlayerProfileRepository,
    private val audioController: AudioController,
) : ViewModel() {

    private val _uiState = MutableStateFlow(JerichoUiState())
    val uiState: StateFlow<JerichoUiState> = _uiState.asStateFlow()

    val characterCustomization: StateFlow<CharacterCustomization> = profileRepository.profile
        .map { it.character }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = CharacterCustomization(),
        )

    /** Scene ids already completed on a prior playthrough — lets a puzzle's Continue button skip past re-solving it. */
    val previouslyCompletedSceneIds: StateFlow<Set<String>> = profileRepository.profile
        .map { it.progressByChapter[ChapterId.JERICHO]?.completedActivities ?: emptySet() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = emptySet(),
        )

    fun onChoiceSelected(choiceId: String) {
        _uiState.update { it.copy(selectedChoiceId = choiceId) }
    }

    /** The trumpet fanfare plays exactly once, on the final correct tap that brings down the wall. */
    fun onMarchOptionTapped(optionId: String) {
        _uiState.update { current ->
            val next = DecisionPathGame.onOptionTapped(current.decisionPathState, optionId)
            when (next.lastOutcome) {
                DecisionOutcome.COMPLETE -> audioController.playSfx(SoundEffect.TRUMPET_FANFARE)
                DecisionOutcome.CORRECT -> audioController.playSfx(SoundEffect.ITEM_COLLECTED)
                else -> Unit
            }
            current.copy(decisionPathState = next)
        }
    }

    /** Records mid-adventure progress so "Continue Adventure" and a future resume can see it. */
    fun onSceneCompleted(sceneId: String) {
        viewModelScope.launch {
            profileRepository.markSceneCompleted(ChapterId.JERICHO, sceneId)
        }
    }

    /** Idempotent so rapid double-taps on the final CONTINUE never double-award (spec section 20). */
    fun onChapterFinished() {
        if (_uiState.value.reward != null) return
        viewModelScope.launch {
            val stars = RewardCalculator.calculateStars(chapterCompleted = true)
            progressionService.completeChapter(
                chapterId = ChapterId.JERICHO,
                stars = stars,
                badgeId = JerichoReward.badge.id,
                scriptureCardIds = listOf(JerichoReward.scriptureCard.id),
            )
            audioController.playSfx(SoundEffect.REWARD_CELEBRATION)
            _uiState.update { it.copy(reward = JerichoRewardResult(stars = stars)) }
        }
    }
}
