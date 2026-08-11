package com.bibleadventures.ui.screens.daniel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bibleadventures.audio.AudioController
import com.bibleadventures.audio.SoundEffect
import com.bibleadventures.domain.model.ChapterId
import com.bibleadventures.domain.model.CharacterCustomization
import com.bibleadventures.domain.repository.PlayerProfileRepository
import com.bibleadventures.game.puzzles.dodge.DodgeGame
import com.bibleadventures.game.puzzles.dodge.DodgeGameState
import com.bibleadventures.game.puzzles.dodge.DodgeLane
import com.bibleadventures.game.puzzles.dodge.DodgeOutcome
import com.bibleadventures.game.puzzles.gridmaze.Direction
import com.bibleadventures.game.puzzles.gridmaze.GridMazeGame
import com.bibleadventures.game.puzzles.gridmaze.GridMazeState
import com.bibleadventures.game.puzzles.gridmaze.GridPosition
import com.bibleadventures.game.puzzles.gridmaze.GridTileType
import com.bibleadventures.game.puzzles.sequence.SequenceGame
import com.bibleadventures.game.puzzles.sequence.SequenceGameState
import com.bibleadventures.game.puzzles.sequence.SequenceOutcome
import com.bibleadventures.game.rewards.DanielReward
import com.bibleadventures.game.rewards.RewardCalculator
import com.bibleadventures.game.stories.DanielContent
import com.bibleadventures.progress.ProgressionService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DanielRewardResult(val stars: Int)

data class DanielUiState(
    val dodgeState: DodgeGameState = DodgeGameState(beats = DanielContent.stealthBeats),
    val selectedChoiceId: String? = null,
    val sequenceState: SequenceGameState = SequenceGameState(pointIds = DanielContent.lionsDenPointIds),
    val gridMazeState: GridMazeState,
    val reward: DanielRewardResult? = null,
)

class DanielViewModel(
    private val progressionService: ProgressionService,
    private val profileRepository: PlayerProfileRepository,
    private val audioController: AudioController,
) : ViewModel() {

    private val _uiState = MutableStateFlow(createInitialState())
    val uiState: StateFlow<DanielUiState> = _uiState.asStateFlow()

    val characterCustomization: StateFlow<CharacterCustomization> = profileRepository.profile
        .map { it.character }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = CharacterCustomization(),
        )

    /** Scene ids already completed on a prior playthrough — lets a puzzle's Continue button skip past re-solving it. */
    val previouslyCompletedSceneIds: StateFlow<Set<String>> = profileRepository.profile
        .map { it.progressByChapter[ChapterId.DANIEL]?.completedActivities ?: emptySet() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = emptySet(),
        )

    fun onLaneTapped(lane: DodgeLane) {
        _uiState.update { current ->
            val next = DodgeGame.onLaneTapped(current.dodgeState, lane)
            if (next.lastOutcome == DodgeOutcome.DODGED) {
                audioController.playSfx(SoundEffect.OBSTACLE_DODGED)
            }
            current.copy(dodgeState = next)
        }
    }

    fun onChoiceSelected(choiceId: String) {
        _uiState.update { it.copy(selectedChoiceId = choiceId) }
    }

    fun onLightPointTapped(pointId: String) {
        _uiState.update { current ->
            val next = SequenceGame.onPointTapped(current.sequenceState, pointId)
            when (next.lastOutcome) {
                SequenceOutcome.POINT_CONNECTED, SequenceOutcome.COMPLETE ->
                    audioController.playSfx(SoundEffect.ITEM_COLLECTED)
                else -> Unit
            }
            current.copy(sequenceState = next)
        }
    }

    fun onDirectionPressed(direction: Direction) {
        _uiState.update { current -> current.copy(gridMazeState = GridMazeGame.onDirectionPressed(current.gridMazeState, direction)) }
    }

    /** Records mid-adventure progress so "Continue Adventure" and a future resume can see it. */
    fun onSceneCompleted(sceneId: String) {
        viewModelScope.launch {
            profileRepository.markSceneCompleted(ChapterId.DANIEL, sceneId)
        }
    }

    /** Idempotent so rapid double-taps on the final CONTINUE never double-award (spec section 20). */
    fun onChapterFinished() {
        if (_uiState.value.reward != null) return
        viewModelScope.launch {
            val stars = RewardCalculator.calculateStars(chapterCompleted = true)
            progressionService.completeChapter(
                chapterId = ChapterId.DANIEL,
                stars = stars,
                badgeId = DanielReward.badge.id,
                scriptureCardIds = listOf(DanielReward.scriptureCard.id),
            )
            audioController.playSfx(SoundEffect.REWARD_CELEBRATION)
            _uiState.update { it.copy(reward = DanielRewardResult(stars = stars)) }
        }
    }

    private fun createInitialState(): DanielUiState {
        val grid = DanielContent.dariusMapLayout.map { row ->
            row.map { cell ->
                when (cell) {
                    '#' -> GridTileType.WALL
                    'D' -> GridTileType.GOAL
                    else -> GridTileType.PATH
                }
            }
        }
        val startRow = DanielContent.dariusMapLayout.indexOfFirst { it.contains('S') }
        val startCol = DanielContent.dariusMapLayout[startRow].indexOf('S')

        return DanielUiState(
            gridMazeState = GridMazeState(grid = grid, playerPosition = GridPosition(startRow, startCol)),
        )
    }
}
