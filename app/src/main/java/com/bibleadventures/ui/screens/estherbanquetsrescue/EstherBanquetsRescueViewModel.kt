package com.bibleadventures.ui.screens.estherbanquetsrescue

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
import com.bibleadventures.game.puzzles.dragsort.DragSortGame
import com.bibleadventures.game.puzzles.dragsort.DragSortGameState
import com.bibleadventures.game.puzzles.dragsort.SortCategory
import com.bibleadventures.game.puzzles.dragsort.SortableItem
import com.bibleadventures.game.rewards.EstherBanquetsRescueReward
import com.bibleadventures.game.rewards.RewardCalculator
import com.bibleadventures.game.stories.EstherBanquetsRescueContent
import com.bibleadventures.progress.ProgressionService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EstherBanquetsRescueRewardResult(val stars: Int)

data class EstherBanquetsRescueUiState(
    val dragSortState: DragSortGameState,
    val decisionPathState: DecisionPathGameState = DecisionPathGameState(steps = EstherBanquetsRescueContent.revealSteps),
    val reward: EstherBanquetsRescueRewardResult? = null,
)

class EstherBanquetsRescueViewModel(
    private val progressionService: ProgressionService,
    private val profileRepository: PlayerProfileRepository,
    private val audioController: AudioController,
) : ViewModel() {

    private val _uiState = MutableStateFlow(createInitialState())
    val uiState: StateFlow<EstherBanquetsRescueUiState> = _uiState.asStateFlow()

    val characterCustomization: StateFlow<CharacterCustomization> = profileRepository.profile
        .map { it.character }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = CharacterCustomization(),
        )

    /** Scene ids already completed on a prior playthrough — lets a puzzle's Continue button skip past re-solving it. */
    val previouslyCompletedSceneIds: StateFlow<Set<String>> = profileRepository.profile
        .map { it.progressByChapter[ChapterId.ESTHER_BANQUETS_RESCUE]?.completedActivities ?: emptySet() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = emptySet(),
        )

    fun onFoodItemDropped(itemId: String, categoryKey: String) {
        _uiState.update { current ->
            val next = DragSortGame.onItemDroppedOnCategory(current.dragSortState, itemId, categoryKey)
            current.copy(dragSortState = next)
        }
    }

    fun onRevealOptionTapped(optionId: String) {
        _uiState.update { current ->
            val next = DecisionPathGame.onOptionTapped(current.decisionPathState, optionId)
            when (next.lastOutcome) {
                DecisionOutcome.CORRECT, DecisionOutcome.COMPLETE -> audioController.playSfx(SoundEffect.ITEM_COLLECTED)
                else -> Unit
            }
            current.copy(decisionPathState = next)
        }
    }

    /** Records mid-adventure progress so "Continue Adventure" and a future resume can see it. */
    fun onSceneCompleted(sceneId: String) {
        viewModelScope.launch {
            profileRepository.markSceneCompleted(ChapterId.ESTHER_BANQUETS_RESCUE, sceneId)
        }
    }

    /** Idempotent so rapid double-taps on the final CONTINUE never double-award (spec section 20). */
    fun onChapterFinished() {
        if (_uiState.value.reward != null) return
        viewModelScope.launch {
            val stars = RewardCalculator.calculateStars(chapterCompleted = true)
            progressionService.completeChapter(
                chapterId = ChapterId.ESTHER_BANQUETS_RESCUE,
                stars = stars,
                badgeId = EstherBanquetsRescueReward.badge.id,
                scriptureCardId = EstherBanquetsRescueReward.scriptureCard.id,
            )
            audioController.playSfx(SoundEffect.REWARD_CELEBRATION)
            _uiState.update { it.copy(reward = EstherBanquetsRescueRewardResult(stars = stars)) }
        }
    }

    private fun createInitialState(): EstherBanquetsRescueUiState {
        val items = EstherBanquetsRescueContent.foodItems.map {
            SortableItem(id = it.id, iconRes = it.iconRes, contentDescriptionRes = it.nameRes, categoryKey = it.categoryKey)
        }
        val categories = EstherBanquetsRescueContent.zoneCategories.map { SortCategory(key = it.key, labelRes = it.labelRes) }
        return EstherBanquetsRescueUiState(
            dragSortState = DragSortGameState(items = items, categories = categories),
        )
    }
}
