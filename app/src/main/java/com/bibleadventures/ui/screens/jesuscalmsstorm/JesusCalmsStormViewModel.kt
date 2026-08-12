package com.bibleadventures.ui.screens.jesuscalmsstorm

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
import com.bibleadventures.game.puzzles.rhythmlane.RhythmLaneGame
import com.bibleadventures.game.puzzles.rhythmlane.RhythmLaneGameState
import com.bibleadventures.game.puzzles.stackbuild.StackBuildGame
import com.bibleadventures.game.puzzles.stackbuild.StackBuildGameState
import com.bibleadventures.game.rewards.JesusCalmsStormReward
import com.bibleadventures.game.rewards.RewardCalculator
import com.bibleadventures.game.stories.JesusCalmsStormContent
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

data class JesusCalmsStormRewardResult(val stars: Int)

/** Only screen-level movement math needs this — the engine only ever sees caller-supplied lane numbers, never a lane count. */
private const val BAILING_LANE_COUNT = 3

data class JesusCalmsStormUiState(
    val loadingState: StackBuildGameState,
    /** itemId -> its randomly assigned 1-99 weight, distinct, fresh every playthrough. */
    val boatItemWeights: Map<String, Int>,
    /** Fixed, shuffled *display* order for the tray — independent of [loadingState]'s required (descending-weight) placement order, so the tray never gives away the answer. */
    val boatTrayOrder: List<String>,
    val bailingState: RhythmLaneGameState = RhythmLaneGameState(
        chart = JesusCalmsStormContent.bailingChart,
        requiredHits = JesusCalmsStormContent.BAILING_REQUIRED_HITS,
    ),
    /** Which of the 3 bailing spots the disciple currently stands in — moved one lane at a time via [JesusCalmsStormViewModel.onBailingLaneMoved]. Starts centered so both edges are one move away. */
    val bailingLane: Int = 1,
    val selectedChoiceId: String? = null,
    val gridMazeState: GridMazeState,
    val peaceBeStillState: RhythmLaneGameState = RhythmLaneGameState(
        chart = JesusCalmsStormContent.peaceBeStillChart,
        requiredHits = JesusCalmsStormContent.PEACE_BE_STILL_REQUIRED_HITS,
    ),
    val reward: JesusCalmsStormRewardResult? = null,
)

/**
 * Jesus Calms the Storm (Mark 4:35-41) — the last chapter in the chain,
 * built from 4 real mini-puzzles, all reused from this app's existing
 * moderate-to-hardest engine tier (no new engine): loading the boat in the
 * correct heaviest-first order (`stackbuild`), bailing the boat against
 * the densest `rhythmlane` chart in the app, reaching Jesus through a
 * genuine dead-end-laden maze (`gridmaze`), and speaking "Peace, be still"
 * to the storm at exactly the right moment (`rhythmlane` again, static
 * always-visible word lanes instead of a steered object).
 */
class JesusCalmsStormViewModel(
    private val progressionService: ProgressionService,
    private val profileRepository: PlayerProfileRepository,
    private val audioController: AudioController,
) : ViewModel() {

    private val _uiState = MutableStateFlow(createInitialState())
    val uiState: StateFlow<JesusCalmsStormUiState> = _uiState.asStateFlow()

    val characterCustomization: StateFlow<CharacterCustomization> = profileRepository.profile
        .map { it.character }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = CharacterCustomization(),
        )

    /** Scene ids already completed on a prior playthrough — lets a puzzle's Continue button skip past re-solving it. */
    val previouslyCompletedSceneIds: StateFlow<Set<String>> = profileRepository.profile
        .map { it.progressByChapter[ChapterId.JESUS_CALMS_STORM]?.completedActivities ?: emptySet() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = emptySet(),
        )

    /** Called only once the screen has confirmed a drag ended over the boat's drop zone — not on every drag. */
    fun onBoatItemPlaced(itemId: String) {
        _uiState.update { current ->
            val next = StackBuildGame.onItemPlaced(current.loadingState, itemId)
            if (next.placedOrder.size > current.loadingState.placedOrder.size) {
                audioController.playSfx(SoundEffect.ITEM_COLLECTED)
            }
            current.copy(loadingState = next)
        }
    }

    /** Moves the disciple by [deltaLane] (-1 left, +1 right), clamped to the 3 bailing spots — never a no-op-that-looks-broken, it just stops at the edge. */
    fun onBailingLaneMoved(deltaLane: Int) {
        _uiState.update { current ->
            current.copy(bailingLane = (current.bailingLane + deltaLane).coerceIn(0, BAILING_LANE_COUNT - 1))
        }
    }

    /**
     * Same role as every other `rhythmlane` screen's per-frame time-advance
     * tick (marks a fully-passed wave MISSED, feedback only), plus the
     * actual bail judgment: reuses [RhythmLaneGame.onLaneTapped] — the same
     * catch semantics as Gathering the Leftovers — checking
     * [JesusCalmsStormUiState.bailingLane] against whichever wave is
     * currently pouring in. The challenge is moving to where the water is
     * *before* it lands, not reacting to it once it has.
     */
    fun onBailingTimeAdvanced(nowMs: Long) {
        _uiState.update { current ->
            val afterMisses = RhythmLaneGame.onTimeAdvanced(current.bailingState, nowMs)
            val afterBail = RhythmLaneGame.onLaneTapped(afterMisses, current.bailingLane, nowMs)
            if (afterBail.hits > current.bailingState.hits) {
                audioController.playSfx(SoundEffect.TARGET_HIT)
            }
            if (afterBail.isComplete && !current.bailingState.isComplete) {
                audioController.playSfx(SoundEffect.ITEM_COLLECTED)
            }
            current.copy(bailingState = afterBail)
        }
    }

    fun onChoiceSelected(choiceId: String) {
        _uiState.update { it.copy(selectedChoiceId = choiceId) }
    }

    fun onReachingJesusDirectionPressed(direction: Direction) {
        _uiState.update { current -> current.copy(gridMazeState = GridMazeGame.onDirectionPressed(current.gridMazeState, direction)) }
    }

    /**
     * Directly taps one of the 3 always-visible word lanes (PEACE/BE/STILL)
     * — unlike Bailing the Boat's steered single object, there's no
     * "current lane" to check against; the player names which lane they're
     * attempting, same shape as Esther's Corridor / Jericho's marches.
     *
     * Unlike those, word order is meaningful here (Jesus's actual words), so
     * a tap only counts if it's for the next expected word — since
     * [JesusCalmsStormContent.peaceBeStillChart]'s notes are already listed
     * PEACE/BE/STILL in lane order, that's just `chart.notes[hits].lane`.
     * A tap on any other lane is a pure no-op, same as a mistimed tap.
     */
    fun onPeaceBeStillWordTapped(lane: Int, nowMs: Long) {
        _uiState.update { current ->
            val expectedLane = current.peaceBeStillState.chart.notes.getOrNull(current.peaceBeStillState.hits)?.lane
            if (lane != expectedLane) return@update current
            val next = RhythmLaneGame.onLaneTapped(current.peaceBeStillState, lane, nowMs)
            if (next.hits > current.peaceBeStillState.hits) {
                audioController.playSfx(SoundEffect.ITEM_COLLECTED)
            }
            current.copy(peaceBeStillState = next)
        }
    }

    fun onPeaceBeStillTimeAdvanced(nowMs: Long) {
        _uiState.update { current -> current.copy(peaceBeStillState = RhythmLaneGame.onTimeAdvanced(current.peaceBeStillState, nowMs)) }
    }

    /** Records mid-adventure progress so "Continue Adventure" and a future resume can see it. */
    fun onSceneCompleted(sceneId: String) {
        viewModelScope.launch {
            profileRepository.markSceneCompleted(ChapterId.JESUS_CALMS_STORM, sceneId)
        }
    }

    /** Idempotent so rapid double-taps on the final CONTINUE never double-award (spec section 20). */
    fun onChapterFinished() {
        if (_uiState.value.reward != null) return
        viewModelScope.launch {
            val stars = RewardCalculator.calculateStars(chapterCompleted = true)
            progressionService.completeChapter(
                chapterId = ChapterId.JESUS_CALMS_STORM,
                stars = stars,
                badgeId = JesusCalmsStormReward.badge.id,
                scriptureCardIds = listOf(JesusCalmsStormReward.scriptureCard.id),
            )
            audioController.playSfx(SoundEffect.REWARD_CELEBRATION)
            _uiState.update { it.copy(reward = JesusCalmsStormRewardResult(stars = stars)) }
        }
    }

    private fun createInitialState(): JesusCalmsStormUiState {
        val random = Random.Default
        val itemWeights = JesusCalmsStormContent.boatItemIds
            .zip((1..99).shuffled(random).take(JesusCalmsStormContent.boatItemIds.size))
            .toMap()

        return JesusCalmsStormUiState(
            loadingState = StackBuildGameState(itemIds = itemWeights.entries.sortedByDescending { it.value }.map { it.key }),
            boatItemWeights = itemWeights,
            boatTrayOrder = JesusCalmsStormContent.boatItemIds.shuffled(random),
            gridMazeState = newReachingJesusState(),
        )
    }

    /** Parses `JesusCalmsStormContent.reachingJesusMapLayout` into a grid, same mapping convention as `DanielViewModel`'s Darius maze. */
    private fun newReachingJesusState(): GridMazeState {
        val grid = JesusCalmsStormContent.reachingJesusMapLayout.map { row ->
            row.map { cell ->
                when (cell) {
                    '#' -> GridTileType.WALL
                    'D' -> GridTileType.GOAL
                    else -> GridTileType.PATH
                }
            }
        }
        val startRow = JesusCalmsStormContent.reachingJesusMapLayout.indexOfFirst { it.contains('S') }
        val startCol = JesusCalmsStormContent.reachingJesusMapLayout[startRow].indexOf('S')
        return GridMazeState(grid = grid, playerPosition = GridPosition(startRow, startCol))
    }
}
