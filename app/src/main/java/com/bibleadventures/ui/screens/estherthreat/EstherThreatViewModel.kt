package com.bibleadventures.ui.screens.estherthreat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bibleadventures.audio.AudioController
import com.bibleadventures.audio.SoundEffect
import com.bibleadventures.domain.model.ChapterId
import com.bibleadventures.domain.model.CharacterCustomization
import com.bibleadventures.domain.repository.PlayerProfileRepository
import com.bibleadventures.game.puzzles.sudoku.SudokuGame
import com.bibleadventures.game.puzzles.sudoku.SudokuGameState
import com.bibleadventures.game.puzzles.sudoku.SudokuOutcome
import com.bibleadventures.game.rewards.EstherThreatReward
import com.bibleadventures.game.rewards.RewardCalculator
import com.bibleadventures.game.stories.EstherThreatContent
import com.bibleadventures.progress.ProgressionService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EstherThreatRewardResult(val stars: Int)

data class EstherThreatUiState(
    val sudokuState: SudokuGameState,
    /** Which empty cell is currently selected, awaiting an icon tap — pure UI state, not engine state. */
    val selectedCell: Pair<Int, Int>? = null,
    val reward: EstherThreatRewardResult? = null,
)

class EstherThreatViewModel(
    private val progressionService: ProgressionService,
    private val profileRepository: PlayerProfileRepository,
    private val audioController: AudioController,
) : ViewModel() {

    private val _uiState = MutableStateFlow(createInitialState())
    val uiState: StateFlow<EstherThreatUiState> = _uiState.asStateFlow()

    val characterCustomization: StateFlow<CharacterCustomization> = profileRepository.profile
        .map { it.character }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = CharacterCustomization(),
        )

    /** Scene ids already completed on a prior playthrough — lets a puzzle's Continue button skip past re-solving it. */
    val previouslyCompletedSceneIds: StateFlow<Set<String>> = profileRepository.profile
        .map { it.progressByChapter[ChapterId.ESTHER_THREAT]?.completedActivities ?: emptySet() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = emptySet(),
        )

    fun onCellSelected(row: Int, col: Int) {
        if (_uiState.value.sudokuState.givens.containsKey(row to col)) return
        _uiState.update { it.copy(selectedCell = row to col) }
    }

    fun onIconTapped(icon: String) {
        val cell = _uiState.value.selectedCell ?: return
        _uiState.update { current ->
            val next = SudokuGame.onCellFilled(current.sudokuState, cell.first, cell.second, icon)
            when (next.lastOutcome) {
                SudokuOutcome.ROW_COMPLETE, SudokuOutcome.COMPLETE -> audioController.playSfx(SoundEffect.ITEM_COLLECTED)
                else -> Unit
            }
            val stillSelected = next.lastOutcome == SudokuOutcome.CONFLICT
            current.copy(sudokuState = next, selectedCell = if (stillSelected) cell else null)
        }
    }

    /** Records mid-adventure progress so "Continue Adventure" and a future resume can see it. */
    fun onSceneCompleted(sceneId: String) {
        viewModelScope.launch {
            profileRepository.markSceneCompleted(ChapterId.ESTHER_THREAT, sceneId)
        }
    }

    /** Idempotent so rapid double-taps on the final CONTINUE never double-award (spec section 20). */
    fun onChapterFinished() {
        if (_uiState.value.reward != null) return
        viewModelScope.launch {
            val stars = RewardCalculator.calculateStars(chapterCompleted = true)
            progressionService.completeChapter(
                chapterId = ChapterId.ESTHER_THREAT,
                stars = stars,
                badgeId = EstherThreatReward.badge.id,
                scriptureCardId = EstherThreatReward.scriptureCard.id,
            )
            audioController.playSfx(SoundEffect.REWARD_CELEBRATION)
            _uiState.update { it.copy(reward = EstherThreatRewardResult(stars = stars)) }
        }
    }

    private fun createInitialState(): EstherThreatUiState = EstherThreatUiState(
        sudokuState = SudokuGameState(size = 5, givens = EstherThreatContent.sudokuGivens),
    )
}
