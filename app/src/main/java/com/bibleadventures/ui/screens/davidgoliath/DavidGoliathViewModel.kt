package com.bibleadventures.ui.screens.davidgoliath

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bibleadventures.audio.AudioController
import com.bibleadventures.audio.SoundEffect
import com.bibleadventures.domain.model.ChapterId
import com.bibleadventures.domain.model.CharacterCustomization
import com.bibleadventures.domain.repository.PlayerProfileRepository
import com.bibleadventures.game.puzzles.connectfour.ConnectFourGame
import com.bibleadventures.game.puzzles.connectfour.ConnectFourGameState
import com.bibleadventures.game.puzzles.connectfour.ConnectFourOutcome
import com.bibleadventures.game.puzzles.matching.MatchItem
import com.bibleadventures.game.puzzles.matching.MatchOutcome
import com.bibleadventures.game.puzzles.matching.MatchingGame
import com.bibleadventures.game.puzzles.matching.MatchingGameState
import com.bibleadventures.game.puzzles.rhythmlane.RhythmLaneGame
import com.bibleadventures.game.puzzles.rhythmlane.RhythmLaneGameState
import com.bibleadventures.game.puzzles.slingshot.SlingshotGame
import com.bibleadventures.game.puzzles.slingshot.SlingshotGameState
import com.bibleadventures.game.puzzles.slingshot.SlingshotOutcome
import com.bibleadventures.game.rewards.DavidGoliathReward
import com.bibleadventures.game.rewards.RewardCalculator
import com.bibleadventures.game.stories.DavidGoliathContent
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

data class DavidGoliathRewardResult(val stars: Int)

/** Only screen-level movement math needs this — the engine only ever sees caller-supplied bounds, never a lane count. */
private const val CROSSING_VALLEY_LANE_COUNT = 3

/**
 * Screen-geometry concept, not engine state — mirrors how the shield's
 * fractional bounds were already a caller-supplied concept
 * ([SlingshotGame.onStoneReleased] never knows "where" the shield is, only
 * whether a release matches it). The engine only counts hits; this decides
 * where the shield practice target relocates to after each one.
 */
enum class ShieldZone { LEFT, MIDDLE, RIGHT }

data class DavidGoliathUiState(
    /** "Choose the Stones" — David already has 1 stone; find 4 more by connecting 4 in a row. */
    val connectFourState: ConnectFourGameState = ConnectFourGame.newGame(),
    val slingshotState: SlingshotGameState = SlingshotGameState(),
    val selectedChoiceId: String? = null,
    val reward: DavidGoliathRewardResult? = null,
    val sheepCountingState: MatchingGameState,
    val crossingValleyState: RhythmLaneGameState = RhythmLaneGameState(
        chart = DavidGoliathContent.crossingValleyChart,
        requiredHits = DavidGoliathContent.CROSSING_VALLEY_REQUIRED_AVOIDS,
    ),
    /** Which of the 3 lanes David currently stands in — moved one lane at a time via [DavidGoliathViewModel.onCrossingValleyLaneMoved]. Starts centered so both edges are one move away. */
    val characterLane: Int = 1,
    /** Where the practice shield currently sits — relocated to a random *different* zone after each hit, so every hit produces a visible move, never a repeat in place. */
    val shieldZone: ShieldZone = ShieldZone.LEFT,
)

class DavidGoliathViewModel(
    private val progressionService: ProgressionService,
    private val profileRepository: PlayerProfileRepository,
    private val audioController: AudioController,
) : ViewModel() {

    private val _uiState = MutableStateFlow(createInitialState())
    val uiState: StateFlow<DavidGoliathUiState> = _uiState.asStateFlow()

    val characterCustomization: StateFlow<CharacterCustomization> = profileRepository.profile
        .map { it.character }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = CharacterCustomization(),
        )

    /** Scene ids already completed on a prior playthrough — lets a puzzle's Continue button skip past re-solving it. */
    val previouslyCompletedSceneIds: StateFlow<Set<String>> = profileRepository.profile
        .map { it.progressByChapter[ChapterId.DAVID_GOLIATH]?.completedActivities ?: emptySet() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = emptySet(),
        )

    fun onConnectFourColumnTapped(column: Int) {
        _uiState.update { current ->
            val next = ConnectFourGame.onPlayerColumnTapped(current.connectFourState, column)
            if (next.outcome == ConnectFourOutcome.PLAYER_WON) {
                audioController.playSfx(SoundEffect.ITEM_COLLECTED)
            }
            current.copy(connectFourState = next)
        }
    }

    /** Called by the screen after a short "thinking" delay once it's the opponent's turn. */
    fun onConnectFourOpponentMove() {
        _uiState.update { current -> current.copy(connectFourState = ConnectFourGame.onOpponentMove(current.connectFourState)) }
    }

    /**
     * A loss or a draw is never a dead end — the screen shows a gentle
     * message for a moment, then calls this to start a fresh round.
     * Winning is the only outcome that completes the scene.
     */
    fun onConnectFourReset() {
        _uiState.update { it.copy(connectFourState = ConnectFourGame.newGame()) }
    }

    fun onChoiceSelected(choiceId: String) {
        _uiState.update { it.copy(selectedChoiceId = choiceId) }
    }

    fun onSheepCountingItemTapped(itemId: String) {
        _uiState.update { current ->
            val next = MatchingGame.onItemTapped(current.sheepCountingState, itemId)
            if (next.lastOutcome == MatchOutcome.CORRECT) {
                audioController.playSfx(SoundEffect.MATCH_SUCCESS)
            }
            current.copy(sheepCountingState = next)
        }
    }

    /** Moves David by [deltaLane] (-1 left, +1 right), clamped to the 3 lanes — never a no-op-that-looks-broken, it just stops at the edge. */
    fun onCrossingValleyLaneMoved(deltaLane: Int) {
        _uiState.update { current ->
            current.copy(characterLane = (current.characterLane + deltaLane).coerceIn(0, CROSSING_VALLEY_LANE_COUNT - 1))
        }
    }

    /**
     * Same role as every other `rhythmlane` screen's per-frame time-advance
     * tick (marks a fully-passed rock MISSED, feedback only), plus the
     * actual avoid judgment: reuses [RhythmLaneGame.onLaneAvoided] — the
     * inverse of Gathering the Leftovers' catch semantics — checking
     * [DavidGoliathUiState.characterLane] against whichever rock is
     * currently landing. The challenge is moving out of a rock's lane
     * *before* it lands, not reacting to it once it has.
     */
    fun onCrossingValleyTimeAdvanced(nowMs: Long) {
        _uiState.update { current ->
            val afterMisses = RhythmLaneGame.onTimeAdvanced(current.crossingValleyState, nowMs)
            val afterAvoid = RhythmLaneGame.onLaneAvoided(afterMisses, current.characterLane, nowMs)
            if (afterAvoid.hits > current.crossingValleyState.hits) {
                audioController.playSfx(SoundEffect.OBSTACLE_DODGED)
            }
            current.copy(crossingValleyState = afterAvoid)
        }
    }

    fun onStoneReleased(aimedPosition: Float, markPosition: Float, shieldMinFraction: Float, shieldMaxFraction: Float) {
        _uiState.update { current ->
            val next = SlingshotGame.onStoneReleased(current.slingshotState, aimedPosition, markPosition, shieldMinFraction, shieldMaxFraction)
            val isNewHit = next.hits > current.slingshotState.hits
            if (isNewHit) {
                audioController.playSfx(SoundEffect.TARGET_HIT)
            }
            current.copy(
                slingshotState = next,
                shieldZone = if (isNewHit) nextRandomShieldZone(current.shieldZone) else current.shieldZone,
            )
        }
    }

    /** Records mid-adventure progress so "Continue Adventure" and a future resume can see it. */
    fun onSceneCompleted(sceneId: String) {
        viewModelScope.launch {
            profileRepository.markSceneCompleted(ChapterId.DAVID_GOLIATH, sceneId)
        }
    }

    /** Idempotent so rapid double-taps on the final CONTINUE never double-award (spec section 20). */
    fun onChapterFinished() {
        if (_uiState.value.reward != null) return
        viewModelScope.launch {
            val stars = RewardCalculator.calculateStars(chapterCompleted = true)
            progressionService.completeChapter(
                chapterId = ChapterId.DAVID_GOLIATH,
                stars = stars,
                badgeId = DavidGoliathReward.badge.id,
                scriptureCardIds = listOf(DavidGoliathReward.scriptureCard.id),
            )
            audioController.playSfx(SoundEffect.REWARD_CELEBRATION)
            _uiState.update { it.copy(reward = DavidGoliathRewardResult(stars = stars)) }
        }
    }

    /** Excludes [current] so every hit produces a visible relocation, never an occasional no-op-looking repeat. */
    private fun nextRandomShieldZone(current: ShieldZone): ShieldZone {
        val choices = ShieldZone.entries.filter { it != current }
        return choices[Random.Default.nextInt(choices.size)]
    }

    private fun createInitialState(): DavidGoliathUiState {
        val sheepCountingItems = DavidGoliathContent.sheepCounts.flatMap { def ->
            listOf(
                MatchItem(id = "numeral_${def.count}", iconRes = def.numeralIconRes, contentDescriptionRes = def.nameRes, pairKey = "${def.count}"),
                MatchItem(id = "group_${def.count}", iconRes = def.sheepGroupIconRes, contentDescriptionRes = def.nameRes, pairKey = "${def.count}"),
            )
        }.shuffled()

        return DavidGoliathUiState(
            sheepCountingState = MatchingGameState(items = sheepCountingItems),
        )
    }
}
