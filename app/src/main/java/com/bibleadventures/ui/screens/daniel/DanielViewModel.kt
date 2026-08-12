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
import com.bibleadventures.game.puzzles.decisionpath.DecisionOutcome
import com.bibleadventures.game.puzzles.decisionpath.DecisionPathGame
import com.bibleadventures.game.puzzles.decisionpath.DecisionPathGameState
import com.bibleadventures.game.puzzles.decisionpath.DecisionStep
import com.bibleadventures.game.rewards.DanielReward
import com.bibleadventures.game.rewards.RewardCalculator
import com.bibleadventures.game.stories.DanielContent
import com.bibleadventures.game.stories.MathOperator
import com.bibleadventures.game.stories.MathProblem
import com.bibleadventures.progress.ProgressionService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

data class DanielRewardResult(val stars: Int)

data class DanielUiState(
    val dodgeState: DodgeGameState = DodgeGameState(beats = DanielContent.stealthBeats),
    val selectedChoiceId: String? = null,
    val lionsDenState: DecisionPathGameState,
    val lionsDenProblems: List<MathProblem>,
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

    fun onLionsDenAnswerTapped(choiceValue: Int) {
        _uiState.update { current ->
            val next = DecisionPathGame.onOptionTapped(current.lionsDenState, choiceValue.toString())
            when (next.lastOutcome) {
                DecisionOutcome.CORRECT, DecisionOutcome.COMPLETE -> audioController.playSfx(SoundEffect.ITEM_COLLECTED)
                else -> Unit
            }
            current.copy(lionsDenState = next)
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

        val lionsDenProblems = newLionsDenProblems()
        return DanielUiState(
            lionsDenState = DecisionPathGameState(
                steps = lionsDenProblems.map { problem ->
                    DecisionStep(
                        id = problem.id,
                        correctOptionId = problem.correctValue.toString(),
                        optionIds = problem.choiceValues.map { it.toString() },
                    )
                },
            ),
            lionsDenProblems = lionsDenProblems,
            gridMazeState = GridMazeState(grid = grid, playerPosition = GridPosition(startRow, startCol)),
        )
    }

    /**
     * Randomly generated fresh every playthrough (confirmed with the user —
     * rounding operands to multiples of 10 would make this too easy for a
     * 7+ audience). Subtraction always draws the larger operand first so the
     * result is never negative; the two distractor choices are near-misses
     * (a small and a larger offset from the true answer) so the correct one
     * isn't obvious by magnitude alone.
     */
    private fun newLionsDenProblems(random: Random = Random.Default): List<MathProblem> {
        return (1..DanielContent.LIONS_DEN_PROBLEM_COUNT).map { index ->
            val operator = if (random.nextBoolean()) MathOperator.ADD else MathOperator.SUBTRACT
            val (operandA, operandB) = if (operator == MathOperator.SUBTRACT) {
                // a in [2, 999], b in [1, a-1] — guarantees 1 <= a-b <= 998, never negative or zero.
                val a = random.nextInt(2, 1000)
                val b = random.nextInt(1, a)
                a to b
            } else {
                random.nextInt(1, 1000) to random.nextInt(1, 1000)
            }
            val correctValue = if (operator == MathOperator.ADD) operandA + operandB else operandA - operandB

            val distractors = mutableSetOf<Int>()
            while (distractors.size < 2) {
                val offset = listOf(-1, 1).random(random) * (if (distractors.isEmpty()) random.nextInt(1, 21) else random.nextInt(20, 151))
                val candidate = correctValue + offset
                if (candidate >= 0 && candidate != correctValue && candidate !in distractors) {
                    distractors += candidate
                }
            }

            MathProblem(
                id = "problem_$index",
                operandA = operandA,
                operandB = operandB,
                operator = operator,
                choiceValues = (distractors + correctValue).shuffled(random),
            )
        }
    }
}
