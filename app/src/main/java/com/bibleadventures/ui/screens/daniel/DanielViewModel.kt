package com.bibleadventures.ui.screens.daniel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bibleadventures.audio.AudioController
import com.bibleadventures.audio.SoundEffect
import com.bibleadventures.domain.model.ChapterId
import com.bibleadventures.domain.model.CharacterCustomization
import com.bibleadventures.domain.repository.PlayerProfileRepository
import com.bibleadventures.game.puzzles.gridmaze.Direction
import com.bibleadventures.game.puzzles.gridmaze.GridMazeGame
import com.bibleadventures.game.puzzles.gridmaze.GridMazeState
import com.bibleadventures.game.puzzles.gridmaze.GridPosition
import com.bibleadventures.game.puzzles.gridmaze.GridTileType
import com.bibleadventures.game.puzzles.decisionpath.DecisionOutcome
import com.bibleadventures.game.puzzles.decisionpath.DecisionPathGame
import com.bibleadventures.game.puzzles.decisionpath.DecisionPathGameState
import com.bibleadventures.game.puzzles.decisionpath.DecisionStep
import com.bibleadventures.game.puzzles.rhythmlane.RhythmLaneGame
import com.bibleadventures.game.puzzles.rhythmlane.RhythmLaneGameState
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

/** Only screen-level movement math needs this — the engine only ever sees caller-supplied bounds, never a lane count. */
private const val HURRY_TO_PRAY_LANE_COUNT = 3

data class DanielUiState(
    val hurryToPrayState: RhythmLaneGameState = RhythmLaneGameState(
        chart = DanielContent.hurryToPrayChart,
        requiredHits = DanielContent.HURRY_TO_PRAY_REQUIRED_AVOIDS,
    ),
    /** Which of the 3 lanes Daniel currently stands in — moved one lane at a time via [DanielViewModel.onHurryToPrayLaneMoved]. Starts centered so both edges are one move away. */
    val characterLane: Int = 1,
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

    /** Moves Daniel by [deltaLane] (-1 left, +1 right), clamped to the 3 lanes — never a no-op-that-looks-broken, it just stops at the edge. */
    fun onHurryToPrayLaneMoved(deltaLane: Int) {
        _uiState.update { current ->
            current.copy(characterLane = (current.characterLane + deltaLane).coerceIn(0, HURRY_TO_PRAY_LANE_COUNT - 1))
        }
    }

    /**
     * Same role as every other `rhythmlane` screen's per-frame time-advance
     * tick (marks a fully-passed official MISSED, feedback only), plus the
     * actual avoid judgment via `RhythmLaneGame.onLaneAvoided` — a literal
     * reskin of `DavidGoliathViewModel.onCrossingValleyTimeAdvanced`.
     */
    fun onHurryToPrayTimeAdvanced(nowMs: Long) {
        _uiState.update { current ->
            val afterMisses = RhythmLaneGame.onTimeAdvanced(current.hurryToPrayState, nowMs)
            val afterAvoid = RhythmLaneGame.onLaneAvoided(afterMisses, current.characterLane, nowMs)
            if (afterAvoid.hits > current.hurryToPrayState.hits) {
                audioController.playSfx(SoundEffect.OBSTACLE_DODGED)
            }
            current.copy(hurryToPrayState = afterAvoid)
        }
    }

    fun onChoiceSelected(choiceId: String) {
        _uiState.update { it.copy(selectedChoiceId = choiceId) }
    }

    /**
     * A wrong tap just re-prompts the same problem, up to a point: after
     * [DecisionPathGame.WRONG_ATTEMPTS_BEFORE_NEW_STEP] wrong taps, the last
     * remaining choice would be a guaranteed-correct guess by elimination,
     * so a fresh problem replaces it instead (same id, so the screen's
     * `problems.first { it.id == step.id }` lookup keeps working unchanged).
     */
    fun onLionsDenAnswerTapped(choiceValue: Int) {
        _uiState.update { current ->
            val afterTap = DecisionPathGame.onOptionTapped(current.lionsDenState, choiceValue.toString())
            when (afterTap.lastOutcome) {
                DecisionOutcome.CORRECT, DecisionOutcome.COMPLETE -> audioController.playSfx(SoundEffect.ITEM_COLLECTED)
                else -> Unit
            }
            if (afterTap.wrongAttemptsOnCurrentStep >= DecisionPathGame.WRONG_ATTEMPTS_BEFORE_NEW_STEP) {
                val newProblem = newLionsDenProblem(problemNumber = afterTap.currentStepIndex + 1)
                val newStep = DecisionStep(
                    id = newProblem.id,
                    correctOptionId = newProblem.correctValue.toString(),
                    optionIds = newProblem.choiceValues.map { it.toString() },
                )
                current.copy(
                    lionsDenState = DecisionPathGame.replaceCurrentStep(afterTap, newStep),
                    lionsDenProblems = current.lionsDenProblems.map { if (it.id == newProblem.id) newProblem else it },
                )
            } else {
                current.copy(lionsDenState = afterTap)
            }
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
     * 7+ audience). Operands are 1-99 (confirmed with the user — was
     * previously up to 3 digits, e.g. "812 + 947", tuned down to keep it a
     * two-digit-or-less problem like "19 + 7"). Subtraction always draws the
     * larger operand first so the result is never negative; the two
     * distractor choices are near-misses (a small and a larger offset from
     * the true answer) so the correct one isn't obvious by magnitude alone.
     */
    private fun newLionsDenProblems(random: Random = Random.Default): List<MathProblem> =
        (1..DanielContent.LIONS_DEN_PROBLEM_COUNT).map { problemNumber -> newLionsDenProblem(problemNumber, random) }

    private fun newLionsDenProblem(problemNumber: Int, random: Random = Random.Default): MathProblem {
        val operator = if (random.nextBoolean()) MathOperator.ADD else MathOperator.SUBTRACT
        val (operandA, operandB) = if (operator == MathOperator.SUBTRACT) {
            // a in [2, 99], b in [1, a-1] — guarantees 1 <= a-b <= 98, never negative or zero.
            val a = random.nextInt(2, 100)
            val b = random.nextInt(1, a)
            a to b
        } else {
            random.nextInt(1, 100) to random.nextInt(1, 100)
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

        return MathProblem(
            id = "problem_$problemNumber",
            operandA = operandA,
            operandB = operandB,
            operator = operator,
            choiceValues = (distractors + correctValue).shuffled(random),
        )
    }
}
