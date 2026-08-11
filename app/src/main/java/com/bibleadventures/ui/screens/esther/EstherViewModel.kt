package com.bibleadventures.ui.screens.esther

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bibleadventures.audio.AudioController
import com.bibleadventures.audio.SoundEffect
import com.bibleadventures.domain.model.ChapterId
import com.bibleadventures.domain.model.CharacterCustomization
import com.bibleadventures.domain.repository.PlayerProfileRepository
import com.bibleadventures.game.puzzles.gridmaze.Direction
import com.bibleadventures.game.puzzles.gridmaze.GridPosition
import com.bibleadventures.game.puzzles.hiddenobject.HiddenItem
import com.bibleadventures.game.puzzles.hiddenobject.HiddenObjectGame
import com.bibleadventures.game.puzzles.hiddenobject.HiddenObjectGameState
import com.bibleadventures.game.puzzles.rhythmlane.RhythmLaneGame
import com.bibleadventures.game.puzzles.rhythmlane.RhythmLaneGameState
import com.bibleadventures.game.puzzles.stealth.StealthGame
import com.bibleadventures.game.puzzles.stealth.StealthGameState
import com.bibleadventures.game.puzzles.stealth.StealthOutcome
import com.bibleadventures.game.puzzles.stealth.StealthTileType
import com.bibleadventures.game.puzzles.sudoku.SudokuGame
import com.bibleadventures.game.puzzles.sudoku.SudokuGameState
import com.bibleadventures.game.puzzles.sudoku.SudokuOutcome
import com.bibleadventures.game.rewards.EstherReward
import com.bibleadventures.game.rewards.RewardCalculator
import com.bibleadventures.game.stories.EstherContent
import com.bibleadventures.progress.ProgressionService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EstherRewardResult(val stars: Int)

data class EstherUiState(
    val hiddenObjectState: HiddenObjectGameState,
    val selectedGreetingChoiceId: String? = null,
    val stealthState: StealthGameState,
    val sudokuState: SudokuGameState,
    /** Which empty sudoku cell is currently selected, awaiting an icon tap — pure UI state, not engine state. */
    val selectedSudokuCell: Pair<Int, Int>? = null,
    val selectedDecisionChoiceId: String? = null,
    val rhythmLaneState: RhythmLaneGameState = RhythmLaneGameState(
        chart = EstherContent.corridorChart,
        requiredHits = EstherContent.CORRIDOR_REQUIRED_HITS,
    ),
    val reward: EstherRewardResult? = null,
)

/**
 * One chapter, 4 sequential mini-puzzles: Royal Attire (hidden object),
 * Courtyard Stealth, Messenger Sudoku, and Corridor Courage Meter — merged
 * from what were briefly 5 separate chapters back into "Esther's Rescue of
 * Her People" per playtesting feedback. Awards one badge and every
 * scripture card earned along the way.
 */
class EstherViewModel(
    private val progressionService: ProgressionService,
    private val profileRepository: PlayerProfileRepository,
    private val audioController: AudioController,
) : ViewModel() {

    private val _uiState = MutableStateFlow(createInitialState())
    val uiState: StateFlow<EstherUiState> = _uiState.asStateFlow()

    val characterCustomization: StateFlow<CharacterCustomization> = profileRepository.profile
        .map { it.character }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = CharacterCustomization(),
        )

    /** Scene ids already completed on a prior playthrough — lets a puzzle's Continue button skip past re-solving it. */
    val previouslyCompletedSceneIds: StateFlow<Set<String>> = profileRepository.profile
        .map { it.progressByChapter[ChapterId.ESTHER]?.completedActivities ?: emptySet() }
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

    fun onGreetingChoiceSelected(choiceId: String) {
        _uiState.update { it.copy(selectedGreetingChoiceId = choiceId) }
    }

    fun onCourtyardDirectionPressed(direction: Direction) {
        _uiState.update { current ->
            val next = StealthGame.onDirectionPressed(current.stealthState, direction)
            when (next.lastOutcome) {
                StealthOutcome.COMPLETE -> audioController.playSfx(SoundEffect.ITEM_COLLECTED)
                else -> Unit
            }
            current.copy(stealthState = next)
        }
    }

    fun onSudokuCellSelected(row: Int, col: Int) {
        if (_uiState.value.sudokuState.givens.containsKey(row to col)) return
        _uiState.update { it.copy(selectedSudokuCell = row to col) }
    }

    fun onSudokuIconTapped(icon: String) {
        val cell = _uiState.value.selectedSudokuCell ?: return
        _uiState.update { current ->
            val next = SudokuGame.onCellFilled(current.sudokuState, cell.first, cell.second, icon)
            when (next.lastOutcome) {
                SudokuOutcome.ROW_COMPLETE, SudokuOutcome.COMPLETE -> audioController.playSfx(SoundEffect.ITEM_COLLECTED)
                else -> Unit
            }
            val stillSelected = next.lastOutcome == SudokuOutcome.CONFLICT
            current.copy(sudokuState = next, selectedSudokuCell = if (stillSelected) cell else null)
        }
    }

    fun onDecisionChoiceSelected(choiceId: String) {
        _uiState.update { it.copy(selectedDecisionChoiceId = choiceId) }
    }

    fun onCorridorLaneTapped(lane: Int, nowMs: Long) {
        _uiState.update { current ->
            val next = RhythmLaneGame.onLaneTapped(current.rhythmLaneState, lane, nowMs)
            if (next.hits > current.rhythmLaneState.hits) {
                audioController.playSfx(SoundEffect.TARGET_HIT)
            }
            if (next.isComplete && !current.rhythmLaneState.isComplete) {
                audioController.playSfx(SoundEffect.ITEM_COLLECTED)
            }
            current.copy(rhythmLaneState = next)
        }
    }

    /** Called as the corridor's real-time clock advances, so notes nobody tapped in time get marked missed (feedback only, never a setback). */
    fun onCorridorTimeAdvanced(nowMs: Long) {
        _uiState.update { current -> current.copy(rhythmLaneState = RhythmLaneGame.onTimeAdvanced(current.rhythmLaneState, nowMs)) }
    }

    /** Records mid-adventure progress so "Continue Adventure" and a future resume can see it. */
    fun onSceneCompleted(sceneId: String) {
        viewModelScope.launch {
            profileRepository.markSceneCompleted(ChapterId.ESTHER, sceneId)
        }
    }

    /** Idempotent so rapid double-taps on the final CONTINUE never double-award (spec section 20). */
    fun onChapterFinished() {
        if (_uiState.value.reward != null) return
        viewModelScope.launch {
            val stars = RewardCalculator.calculateStars(chapterCompleted = true)
            progressionService.completeChapter(
                chapterId = ChapterId.ESTHER,
                stars = stars,
                badgeId = EstherReward.badge.id,
                scriptureCardIds = EstherReward.scriptureCards.map { it.id },
            )
            audioController.playSfx(SoundEffect.REWARD_CELEBRATION)
            _uiState.update { it.copy(reward = EstherRewardResult(stars = stars)) }
        }
    }

    private fun createInitialState(): EstherUiState {
        val attireItems = EstherContent.royalAttireItems.map { def ->
            HiddenItem(id = def.id, position = def.position, iconRes = def.iconRes, contentDescriptionRes = def.nameRes)
        }

        val grid = EstherContent.courtyardMapLayout.map { row ->
            row.map { cell ->
                when (cell) {
                    '#' -> StealthTileType.WALL
                    'G' -> StealthTileType.GOAL
                    else -> StealthTileType.PATH
                }
            }
        }
        val startRow = EstherContent.courtyardMapLayout.indexOfFirst { it.contains('S') }
        val startCol = EstherContent.courtyardMapLayout[startRow].indexOf('S')
        val start = GridPosition(startRow, startCol)

        return EstherUiState(
            hiddenObjectState = HiddenObjectGameState(items = attireItems),
            stealthState = StealthGameState(
                grid = grid,
                startPosition = start,
                playerPosition = start,
                guards = EstherContent.courtyardGuards,
            ),
            sudokuState = SudokuGameState(size = 5, givens = EstherContent.sudokuGivens),
        )
    }
}
