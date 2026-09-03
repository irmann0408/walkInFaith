package com.bibleadventures.ui.screens.goodsamaritan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bibleadventures.audio.AudioController
import com.bibleadventures.audio.SoundEffect
import com.bibleadventures.domain.model.ChapterId
import com.bibleadventures.domain.model.CharacterCustomization
import com.bibleadventures.domain.repository.PlayerProfileRepository
import com.bibleadventures.game.puzzles.gridmaze.Direction
import com.bibleadventures.game.puzzles.gridmaze.GridMazeGame
import com.bibleadventures.game.puzzles.gridmaze.GridMazeOutcome
import com.bibleadventures.game.puzzles.gridmaze.GridMazeState
import com.bibleadventures.game.puzzles.gridmaze.GridPosition
import com.bibleadventures.game.puzzles.gridmaze.GridTileType
import com.bibleadventures.game.puzzles.roadblock.RoadblockGame
import com.bibleadventures.game.puzzles.roadblock.RoadblockGameState
import com.bibleadventures.game.puzzles.roadblock.Direction as RoadblockDirection
import com.bibleadventures.game.rewards.GoodSamaritanReward
import com.bibleadventures.game.rewards.RewardCalculator
import com.bibleadventures.game.stories.GoodSamaritanContent
import com.bibleadventures.progress.ProgressionService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GoodSamaritanRewardResult(val stars: Int)

data class GoodSamaritanUiState(
    val gridMazeState: GridMazeState,
    val roadblockState: RoadblockGameState,
    /** Whether the player has dismissed the "helping" story beat shown once the traveler is treated. */
    val helpingBeatAcknowledged: Boolean = false,
    val reward: GoodSamaritanRewardResult? = null,
)

class GoodSamaritanViewModel(
    private val progressionService: ProgressionService,
    private val profileRepository: PlayerProfileRepository,
    private val audioController: AudioController,
) : ViewModel() {

    private val _uiState = MutableStateFlow(createInitialState())
    val uiState: StateFlow<GoodSamaritanUiState> = _uiState.asStateFlow()

    val characterCustomization: StateFlow<CharacterCustomization> = profileRepository.profile
        .map { it.character }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = CharacterCustomization(),
        )

    /** Scene ids already completed on a prior playthrough — lets a puzzle's Continue button skip past re-solving it. */
    val previouslyCompletedSceneIds: StateFlow<Set<String>> = profileRepository.profile
        .map { it.progressByChapter[ChapterId.GOOD_SAMARITAN]?.completedActivities ?: emptySet() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = emptySet(),
        )

    fun onDirectionPressed(direction: Direction) {
        _uiState.update { current ->
            val next = GridMazeGame.onDirectionPressed(current.gridMazeState, direction)
            when (next.lastOutcome) {
                GridMazeOutcome.COLLECTED, GridMazeOutcome.CHECKPOINT_ACTIVATED ->
                    audioController.playSfx(SoundEffect.ITEM_COLLECTED)
                else -> Unit
            }
            current.copy(gridMazeState = next)
        }
    }

    /**
     * "Passing By": no celebratory SFX on [com.bibleadventures.game.puzzles.roadblock.RoadblockOutcome.EXITED] —
     * unlike every other puzzle's completion, this one isn't a moment to
     * celebrate (see [com.bibleadventures.ui.screens.goodsamaritan.passingby.GoodSamaritanPassingByScreen]
     * for the character's own non-celebratory completion message).
     */
    fun onSlideAttempted(blockId: String, direction: RoadblockDirection, cellsAttempted: Int) {
        _uiState.update { current ->
            current.copy(roadblockState = RoadblockGame.onSlideAttempted(current.roadblockState, blockId, direction, cellsAttempted))
        }
    }

    /** Dismisses the "helping" story beat overlay once the player has read it. */
    fun onHelpingBeatAcknowledged() {
        _uiState.update { it.copy(helpingBeatAcknowledged = true) }
    }

    /** Records mid-adventure progress so "Continue Adventure" and a future resume can see it. */
    fun onSceneCompleted(sceneId: String) {
        viewModelScope.launch {
            profileRepository.markSceneCompleted(ChapterId.GOOD_SAMARITAN, sceneId)
        }
    }

    /** Idempotent so rapid double-taps on the final CONTINUE never double-award (spec section 20). */
    fun onChapterFinished() {
        if (_uiState.value.reward != null) return
        viewModelScope.launch {
            val stars = RewardCalculator.calculateStars(chapterCompleted = true)
            progressionService.completeChapter(
                chapterId = ChapterId.GOOD_SAMARITAN,
                stars = stars,
                badgeId = GoodSamaritanReward.badge.id,
                scriptureCardIds = listOf(GoodSamaritanReward.scriptureCard.id),
            )
            audioController.playSfx(SoundEffect.REWARD_CELEBRATION)
            _uiState.update { it.copy(reward = GoodSamaritanRewardResult(stars = stars)) }
        }
    }

    private fun createInitialState(): GoodSamaritanUiState {
        val grid = GoodSamaritanContent.mapLayout.map { row ->
            row.map { cell ->
                when (cell) {
                    '#', 'X' -> GridTileType.WALL
                    'M' -> GridTileType.COLLECTIBLE
                    'T' -> GridTileType.CHECKPOINT
                    'I' -> GridTileType.GOAL
                    else -> GridTileType.PATH
                }
            }
        }
        val startRow = GoodSamaritanContent.mapLayout.indexOfFirst { it.contains('S') }
        val startCol = GoodSamaritanContent.mapLayout[startRow].indexOf('S')

        val roadblockState = RoadblockGame.fromLayout(
            layout = GoodSamaritanContent.passingByLayout,
            blockSpecs = GoodSamaritanContent.passingByBlockSpecs,
            protagonistId = GoodSamaritanContent.passingByProtagonistId,
            exitColumns = GoodSamaritanContent.passingByExitColumns,
        )

        return GoodSamaritanUiState(
            gridMazeState = GridMazeState(grid = grid, playerPosition = GridPosition(startRow, startCol)),
            roadblockState = roadblockState,
        )
    }
}
