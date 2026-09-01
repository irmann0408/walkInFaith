package com.bibleadventures.ui.screens.feeding5000

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bibleadventures.R
import com.bibleadventures.audio.AudioController
import com.bibleadventures.audio.SoundEffect
import com.bibleadventures.domain.model.ChapterId
import com.bibleadventures.domain.model.CharacterCustomization
import com.bibleadventures.domain.repository.PlayerProfileRepository
import com.bibleadventures.game.puzzles.decisionpath.DecisionOutcome
import com.bibleadventures.game.puzzles.decisionpath.DecisionPathGame
import com.bibleadventures.game.puzzles.decisionpath.DecisionPathGameState
import com.bibleadventures.game.puzzles.decisionpath.DecisionStep
import com.bibleadventures.game.puzzles.gridmaze.Direction
import com.bibleadventures.game.puzzles.gridmaze.GridMazeGame
import com.bibleadventures.game.puzzles.gridmaze.GridMazeOutcome
import com.bibleadventures.game.puzzles.gridmaze.GridMazeState
import com.bibleadventures.game.puzzles.gridmaze.GridPosition
import com.bibleadventures.game.puzzles.gridmaze.GridTileType
import com.bibleadventures.game.puzzles.groupfill.FamilyGroup
import com.bibleadventures.game.puzzles.groupfill.GroupFillGame
import com.bibleadventures.game.puzzles.groupfill.GroupFillGameState
import com.bibleadventures.game.puzzles.hiddenobject.HiddenItem
import com.bibleadventures.game.puzzles.hiddenobject.HiddenObjectGame
import com.bibleadventures.game.puzzles.hiddenobject.HiddenObjectGameState
import com.bibleadventures.game.puzzles.rhythmlane.RhythmLaneGame
import com.bibleadventures.game.puzzles.rhythmlane.RhythmLaneGameState
import com.bibleadventures.game.rewards.Feeding5000Reward
import com.bibleadventures.game.rewards.RewardCalculator
import com.bibleadventures.game.stories.Feeding5000Content
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

data class Feeding5000RewardResult(val stars: Int)

/** Matches Catching's 3-lane `rhythmlane` chart — not derived from the chart itself since the basket's lane range is a screen/UI concept, not an engine one. */
private const val CATCHING_LANE_COUNT = 3

/**
 * Bounds for the boy's randomized position in Searching for Food — same
 * grass-band-safe fractional range (y >= ~0.58) already established for
 * [Feeding5000Content.searchingForFoodDecoys] on `bg_feeding_hillside.xml`,
 * so a random placement can never land him floating in the sky.
 */
private const val BOY_POSITION_MIN_X = 0.08f
private const val BOY_POSITION_MAX_X = 0.90f
private const val BOY_POSITION_MIN_Y = 0.58f
private const val BOY_POSITION_MAX_Y = 0.88f

/** Minimum center-to-center distance from every crowd decoy, so the boy never lands stacked directly on top of one. */
private const val BOY_DECOY_MIN_DISTANCE = 0.05f

data class Feeding5000UiState(
    val selectedChoiceId: String? = null,
    val groupFillState: GroupFillGameState,
    val searchingState: HiddenObjectGameState,
    val boysGiftState: HiddenObjectGameState,
    val miracleState: DecisionPathGameState,
    val miracleProblems: List<MathProblem>,
    val servingState: GridMazeState,
    val catchingState: RhythmLaneGameState = RhythmLaneGameState(
        chart = Feeding5000Content.catchingChart,
        requiredHits = Feeding5000Content.CATCHING_REQUIRED_HITS,
    ),
    /** Which of the 3 catch lanes the single basket currently sits in — moved one lane at a time via [Feeding5000ViewModel.onCatchingBasketMoved]. Starts centered so both edges are one move away. */
    val catchingBasketLane: Int = 1,
    val reward: Feeding5000RewardResult? = null,
)

/**
 * Feeding the 5,000, built from 6 real mini-puzzles: gathering the crowd
 * into exact-sum seating groups (`groupfill`), searching for the boy with
 * the loaves and fish (`hiddenobject`), finding exactly what's in his
 * basket among decoys (`hiddenobject` again), the miracle of multiplication
 * as real arithmetic (`decisionpath`), walking out to serve all 7 groups
 * (`gridmaze`, same D-pad engine as Good Samaritan's/Daniel's mazes — a
 * standing-still catch mechanic read as *receiving* food, not giving it
 * away), and gathering the twelve leftover baskets (`rhythmlane`) —
 * replacing an external blueprint's "tap the basket to multiply" gimmick
 * and punishing patience timer, neither of which fit this app's rules.
 */
class Feeding5000ViewModel(
    private val progressionService: ProgressionService,
    private val profileRepository: PlayerProfileRepository,
    private val audioController: AudioController,
) : ViewModel() {

    private val _uiState = MutableStateFlow(createInitialState())
    val uiState: StateFlow<Feeding5000UiState> = _uiState.asStateFlow()

    val characterCustomization: StateFlow<CharacterCustomization> = profileRepository.profile
        .map { it.character }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = CharacterCustomization(),
        )

    /** Scene ids already completed on a prior playthrough — lets a puzzle's Continue button skip past re-solving it. */
    val previouslyCompletedSceneIds: StateFlow<Set<String>> = profileRepository.profile
        .map { it.progressByChapter[ChapterId.FEEDING_5000]?.completedActivities ?: emptySet() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = emptySet(),
        )

    fun onChoiceSelected(choiceId: String) {
        _uiState.update { it.copy(selectedChoiceId = choiceId) }
    }

    /** Called only once the screen has confirmed a drag ended over a given circle — not on every drag. */
    fun onFamilyDropped(familyId: String, circleIndex: Int) {
        _uiState.update { current ->
            val next = GroupFillGame.onFamilyDropped(current.groupFillState, familyId, circleIndex)
            if (next.placedFamilyIds.size > current.groupFillState.placedFamilyIds.size) {
                audioController.playSfx(SoundEffect.ITEM_COLLECTED)
            }
            current.copy(groupFillState = next)
        }
    }

    fun onBoyFound(itemId: String) {
        _uiState.update { current ->
            val next = HiddenObjectGame.onItemTapped(current.searchingState, itemId)
            if (next.foundIds.size > current.searchingState.foundIds.size) {
                audioController.playSfx(SoundEffect.ITEM_COLLECTED)
            }
            current.copy(searchingState = next)
        }
    }

    fun onBoysGiftItemTapped(itemId: String) {
        _uiState.update { current ->
            val next = HiddenObjectGame.onItemTapped(current.boysGiftState, itemId)
            if (next.foundIds.size > current.boysGiftState.foundIds.size) {
                audioController.playSfx(SoundEffect.ITEM_COLLECTED)
            }
            current.copy(boysGiftState = next)
        }
    }

    /**
     * A wrong tap just re-prompts the same problem, up to a point: after
     * [DecisionPathGame.WRONG_ATTEMPTS_BEFORE_NEW_STEP] wrong taps, the last
     * remaining choice would be a guaranteed-correct guess by elimination,
     * so a fresh problem replaces it instead (same id, so the screen's
     * `problems.first { it.id == step.id }` lookup keeps working unchanged).
     */
    fun onMiracleAnswerTapped(choiceValue: Int) {
        _uiState.update { current ->
            val afterTap = DecisionPathGame.onOptionTapped(current.miracleState, choiceValue.toString())
            when (afterTap.lastOutcome) {
                DecisionOutcome.CORRECT, DecisionOutcome.COMPLETE -> audioController.playSfx(SoundEffect.ITEM_COLLECTED)
                else -> Unit
            }
            if (afterTap.wrongAttemptsOnCurrentStep >= DecisionPathGame.WRONG_ATTEMPTS_BEFORE_NEW_STEP) {
                val newProblem = newMiracleProblem(problemNumber = afterTap.currentStepIndex + 1)
                val newStep = DecisionStep(
                    id = newProblem.id,
                    correctOptionId = newProblem.correctValue.toString(),
                    optionIds = newProblem.choiceValues.map { it.toString() },
                )
                current.copy(
                    miracleState = DecisionPathGame.replaceCurrentStep(afterTap, newStep),
                    miracleProblems = current.miracleProblems.map { if (it.id == newProblem.id) newProblem else it },
                )
            } else {
                current.copy(miracleState = afterTap)
            }
        }
    }

    fun onServingDirectionPressed(direction: Direction) {
        _uiState.update { current ->
            val next = GridMazeGame.onDirectionPressed(current.servingState, direction)
            if (next.lastOutcome == GridMazeOutcome.COLLECTED) {
                audioController.playSfx(SoundEffect.ITEM_COLLECTED)
            }
            current.copy(servingState = next)
        }
    }

    /** Moves the single basket by [deltaLane] (-1 left, +1 right), clamped to the 3 lanes — never a no-op-that-looks-broken, it just stops at the edge. */
    fun onCatchingBasketMoved(deltaLane: Int) {
        _uiState.update { current ->
            current.copy(catchingBasketLane = (current.catchingBasketLane + deltaLane).coerceIn(0, CATCHING_LANE_COUNT - 1))
        }
    }

    /**
     * Same role as [onServingTimeAdvanced] (marks a fully-passed note MISSED,
     * feedback only), plus the actual catch judgment: unlike Serving's 3
     * independently-tapped lanes, there's only one basket here, so a catch
     * is judged automatically against whichever lane [Feeding5000UiState.catchingBasketLane]
     * is currently in, at this exact moment — reusing [RhythmLaneGame.onLaneTapped]
     * completely unchanged (it's already idempotent per note via
     * `judgedNoteKeys`, so calling it every frame while the basket sits in
     * the right lane is safe; it only ever registers once). The real
     * challenge is moving the basket into position *before* a note reaches
     * its hit window, not reacting to it.
     */
    fun onCatchingTimeAdvanced(nowMs: Long) {
        _uiState.update { current ->
            val afterMisses = RhythmLaneGame.onTimeAdvanced(current.catchingState, nowMs)
            val afterCatch = RhythmLaneGame.onLaneTapped(afterMisses, current.catchingBasketLane, nowMs)
            if (afterCatch.hits > current.catchingState.hits) {
                audioController.playSfx(SoundEffect.TARGET_HIT)
            }
            if (afterCatch.isComplete && !current.catchingState.isComplete) {
                audioController.playSfx(SoundEffect.ITEM_COLLECTED)
            }
            current.copy(catchingState = afterCatch)
        }
    }

    /** Records mid-adventure progress so "Continue Adventure" and a future resume can see it. */
    fun onSceneCompleted(sceneId: String) {
        viewModelScope.launch {
            profileRepository.markSceneCompleted(ChapterId.FEEDING_5000, sceneId)
        }
    }

    /** Idempotent so rapid double-taps on the final CONTINUE never double-award (spec section 20). */
    fun onChapterFinished() {
        if (_uiState.value.reward != null) return
        viewModelScope.launch {
            val stars = RewardCalculator.calculateStars(chapterCompleted = true)
            progressionService.completeChapter(
                chapterId = ChapterId.FEEDING_5000,
                stars = stars,
                badgeId = Feeding5000Reward.badge.id,
                scriptureCardIds = listOf(Feeding5000Reward.scriptureCard.id),
            )
            audioController.playSfx(SoundEffect.REWARD_CELEBRATION)
            _uiState.update { it.copy(reward = Feeding5000RewardResult(stars = stars)) }
        }
    }

    private fun createInitialState(): Feeding5000UiState {
        val random = Random.Default
        val miracleProblems = newMiracleProblems(random)

        return Feeding5000UiState(
            groupFillState = GroupFillGameState(
                families = newGroupFillFamilies(random),
                circleTargets = Feeding5000Content.groupFillCircleTargets,
            ),
            searchingState = HiddenObjectGameState(
                items = listOf(
                    HiddenItem(
                        id = "boy",
                        position = newBoyPosition(random),
                        iconRes = R.drawable.ic_boy_with_basket,
                        contentDescriptionRes = R.string.feeding_5000_searching_for_food_boy_content_description,
                    ),
                ),
            ),
            boysGiftState = HiddenObjectGameState(items = boysGiftRealItems),
            miracleState = DecisionPathGameState(
                steps = miracleProblems.map { problem ->
                    DecisionStep(
                        id = problem.id,
                        correctOptionId = problem.correctValue.toString(),
                        optionIds = problem.choiceValues.map { it.toString() },
                    )
                },
            ),
            miracleProblems = miracleProblems,
            servingState = newServingState(),
        )
    }

    /** Parses `Feeding5000Content.servingMapLayout` into a grid, same mapping convention as `GoodSamaritanViewModel`/`DanielViewModel`. */
    private fun newServingState(): GridMazeState {
        val grid = Feeding5000Content.servingMapLayout.map { row ->
            row.map { cell ->
                when (cell) {
                    '#', 'B' -> GridTileType.WALL
                    'C' -> GridTileType.COLLECTIBLE
                    else -> GridTileType.PATH
                }
            }
        }
        val startRow = Feeding5000Content.servingMapLayout.indexOfFirst { it.contains('S') }
        val startCol = Feeding5000Content.servingMapLayout[startRow].indexOf('S')
        return GridMazeState(grid = grid, playerPosition = GridPosition(startRow, startCol))
    }

    /**
     * Randomized fresh every playthrough, so the boy isn't always in the
     * same spot once a player has seen the scene before. Rejection-sampled
     * within the same grass-safe bounds the crowd decoys use, retried until
     * a candidate lands far enough from every decoy that he never appears
     * stacked directly on top of one — bounded at 200 attempts as a defensive
     * fallback (never actually needed: 20 decoys, each excluding a small
     * circle, cover well under half of the sampling area).
     */
    private fun newBoyPosition(random: Random = Random.Default): Offset {
        repeat(200) {
            val candidate = Offset(
                random.nextFloat() * (BOY_POSITION_MAX_X - BOY_POSITION_MIN_X) + BOY_POSITION_MIN_X,
                random.nextFloat() * (BOY_POSITION_MAX_Y - BOY_POSITION_MIN_Y) + BOY_POSITION_MIN_Y,
            )
            if (Feeding5000Content.searchingForFoodDecoys.none { (candidate - it.position).getDistance() < BOY_DECOY_MIN_DISTANCE }) {
                return candidate
            }
        }
        return Offset((BOY_POSITION_MIN_X + BOY_POSITION_MAX_X) / 2f, (BOY_POSITION_MIN_Y + BOY_POSITION_MAX_Y) / 2f)
    }

    /**
     * Randomly generated fresh every playthrough, solvable by construction:
     * each seating circle's target is split into 3-5 random positive
     * headcounts (same "build the puzzle from its own solution" principle
     * as `SlidingPuzzleGame.newShuffled`), then every circle's families are
     * pooled and shuffled once — this pooled-and-shuffled list *is* the
     * tray display order, so it never hints which family belongs where.
     */
    private fun newGroupFillFamilies(random: Random = Random.Default): List<FamilyGroup> {
        val families = mutableListOf<FamilyGroup>()
        Feeding5000Content.groupFillCircleTargets.forEachIndexed { circleIndex, target ->
            GroupFillGame.randomSolvablePartition(target, minParts = 3, maxParts = 5, random).forEachIndexed { partIndex, headcount ->
                families += FamilyGroup(id = "family_${circleIndex}_$partIndex", headcount = headcount)
            }
        }
        return families.shuffled(random)
    }

    /**
     * Randomly generated fresh every playthrough — multiplicand drawn from
     * the miracle's own numbers (5 loaves, 2 fish, then round numbers as it
     * visibly scales up), multiplier always single-digit, same tuning
     * lesson Jericho's Blow the Shofar needed a playtest pass to learn,
     * applied here from the start.
     */
    private fun newMiracleProblems(random: Random = Random.Default): List<MathProblem> =
        (1..Feeding5000Content.MIRACLE_PROBLEM_COUNT).map { problemNumber -> newMiracleProblem(problemNumber, random) }

    private fun newMiracleProblem(problemNumber: Int, random: Random = Random.Default): MathProblem {
        val multiplicand = Feeding5000Content.miracleMultiplicandPool.random(random)
        val multiplier = random.nextInt(1, 10)
        val correctValue = multiplicand * multiplier

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
            operandA = multiplicand,
            operandB = multiplier,
            operator = MathOperator.MULTIPLY,
            choiceValues = (distractors + correctValue).shuffled(random),
        )
    }

    companion object {
        /**
         * Fixed content: 5 barley loaves + 2 fish, the only items that count
         * toward completion. Decoys (a few stones, a couple of frogs) are
         * purely screen-level visuals never wired to [onBoysGiftItemTapped] —
         * tapping one is a harmless no-op by construction, not because the
         * engine defends against it.
         */
        val boysGiftRealItems: List<HiddenItem> = listOf(
            HiddenItem("loaf_1", Offset(0.2f, 0.3f), R.drawable.ic_supply_bread, R.string.feeding_5000_boys_gift_loaf_content_description),
            HiddenItem("loaf_2", Offset(0.5f, 0.2f), R.drawable.ic_supply_bread, R.string.feeding_5000_boys_gift_loaf_content_description),
            HiddenItem("loaf_3", Offset(0.78f, 0.35f), R.drawable.ic_supply_bread, R.string.feeding_5000_boys_gift_loaf_content_description),
            HiddenItem("loaf_4", Offset(0.3f, 0.62f), R.drawable.ic_supply_bread, R.string.feeding_5000_boys_gift_loaf_content_description),
            HiddenItem("loaf_5", Offset(0.6f, 0.55f), R.drawable.ic_supply_bread, R.string.feeding_5000_boys_gift_loaf_content_description),
            HiddenItem("fish_1", Offset(0.15f, 0.55f), R.drawable.ic_supply_fish, R.string.feeding_5000_boys_gift_fish_content_description),
            HiddenItem("fish_2", Offset(0.82f, 0.6f), R.drawable.ic_supply_fish, R.string.feeding_5000_boys_gift_fish_content_description),
        )
    }
}
