package com.bibleadventures.ui.screens.esthersecretplot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bibleadventures.audio.AudioController
import com.bibleadventures.audio.SoundEffect
import com.bibleadventures.domain.model.ChapterId
import com.bibleadventures.domain.model.CharacterCustomization
import com.bibleadventures.domain.repository.PlayerProfileRepository
import com.bibleadventures.game.puzzles.gridmaze.Direction
import com.bibleadventures.game.puzzles.gridmaze.GridPosition
import com.bibleadventures.game.puzzles.stealth.StealthGame
import com.bibleadventures.game.puzzles.stealth.StealthGameState
import com.bibleadventures.game.puzzles.stealth.StealthOutcome
import com.bibleadventures.game.puzzles.stealth.StealthTileType
import com.bibleadventures.game.rewards.EstherSecretPlotReward
import com.bibleadventures.game.rewards.RewardCalculator
import com.bibleadventures.game.stories.EstherSecretPlotContent
import com.bibleadventures.progress.ProgressionService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EstherSecretPlotRewardResult(val stars: Int)

data class EstherSecretPlotUiState(
    val stealthState: StealthGameState,
    val reward: EstherSecretPlotRewardResult? = null,
)

class EstherSecretPlotViewModel(
    private val progressionService: ProgressionService,
    private val profileRepository: PlayerProfileRepository,
    private val audioController: AudioController,
) : ViewModel() {

    private val _uiState = MutableStateFlow(createInitialState())
    val uiState: StateFlow<EstherSecretPlotUiState> = _uiState.asStateFlow()

    val characterCustomization: StateFlow<CharacterCustomization> = profileRepository.profile
        .map { it.character }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = CharacterCustomization(),
        )

    /** Scene ids already completed on a prior playthrough — lets a puzzle's Continue button skip past re-solving it. */
    val previouslyCompletedSceneIds: StateFlow<Set<String>> = profileRepository.profile
        .map { it.progressByChapter[ChapterId.ESTHER_SECRET_PLOT]?.completedActivities ?: emptySet() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = emptySet(),
        )

    fun onDirectionPressed(direction: Direction) {
        _uiState.update { current ->
            val next = StealthGame.onDirectionPressed(current.stealthState, direction)
            when (next.lastOutcome) {
                StealthOutcome.COMPLETE -> audioController.playSfx(SoundEffect.ITEM_COLLECTED)
                else -> Unit
            }
            current.copy(stealthState = next)
        }
    }

    /** Records mid-adventure progress so "Continue Adventure" and a future resume can see it. */
    fun onSceneCompleted(sceneId: String) {
        viewModelScope.launch {
            profileRepository.markSceneCompleted(ChapterId.ESTHER_SECRET_PLOT, sceneId)
        }
    }

    /** Idempotent so rapid double-taps on the final CONTINUE never double-award (spec section 20). */
    fun onChapterFinished() {
        if (_uiState.value.reward != null) return
        viewModelScope.launch {
            val stars = RewardCalculator.calculateStars(chapterCompleted = true)
            progressionService.completeChapter(
                chapterId = ChapterId.ESTHER_SECRET_PLOT,
                stars = stars,
                badgeId = EstherSecretPlotReward.badge.id,
                scriptureCardId = EstherSecretPlotReward.scriptureCard.id,
            )
            audioController.playSfx(SoundEffect.REWARD_CELEBRATION)
            _uiState.update { it.copy(reward = EstherSecretPlotRewardResult(stars = stars)) }
        }
    }

    private fun createInitialState(): EstherSecretPlotUiState {
        val grid = EstherSecretPlotContent.courtyardMapLayout.map { row ->
            row.map { cell ->
                when (cell) {
                    '#' -> StealthTileType.WALL
                    'G' -> StealthTileType.GOAL
                    else -> StealthTileType.PATH
                }
            }
        }
        val startRow = EstherSecretPlotContent.courtyardMapLayout.indexOfFirst { it.contains('S') }
        val startCol = EstherSecretPlotContent.courtyardMapLayout[startRow].indexOf('S')
        val start = GridPosition(startRow, startCol)

        return EstherSecretPlotUiState(
            stealthState = StealthGameState(
                grid = grid,
                startPosition = start,
                playerPosition = start,
                guards = EstherSecretPlotContent.courtyardGuards,
            ),
        )
    }
}
