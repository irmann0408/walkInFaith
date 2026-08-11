package com.bibleadventures.ui.screens.jericho

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bibleadventures.audio.AudioController
import com.bibleadventures.audio.SoundEffect
import com.bibleadventures.domain.model.ChapterId
import com.bibleadventures.domain.model.CharacterCustomization
import com.bibleadventures.domain.repository.PlayerProfileRepository
import com.bibleadventures.game.puzzles.rhythmlane.RhythmLaneGame
import com.bibleadventures.game.puzzles.rhythmlane.RhythmLaneGameState
import com.bibleadventures.game.puzzles.sequence.SequenceGame
import com.bibleadventures.game.puzzles.sequence.SequenceGameState
import com.bibleadventures.game.puzzles.sequence.SequenceOutcome
import com.bibleadventures.game.puzzles.slidingpuzzle.SlidingPuzzleGame
import com.bibleadventures.game.puzzles.slidingpuzzle.SlidingPuzzleGameState
import com.bibleadventures.game.puzzles.stackbuild.StackBuildGame
import com.bibleadventures.game.puzzles.stackbuild.StackBuildGameState
import com.bibleadventures.game.rewards.JerichoReward
import com.bibleadventures.game.rewards.RewardCalculator
import com.bibleadventures.game.stories.JerichoContent
import com.bibleadventures.game.stories.ShofarNotePlacement
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

data class JerichoRewardResult(val stars: Int)

data class JerichoUiState(
    val selectedChoiceId: String? = null,
    val spiesEscapeState: SlidingPuzzleGameState,
    val campState: StackBuildGameState,
    val sixDayMarchState: RhythmLaneGameState = RhythmLaneGameState(
        chart = JerichoContent.sixDayMarchChart,
        requiredHits = JerichoContent.SIX_DAY_MARCH_REQUIRED_HITS,
    ),
    val fastMarchState: RhythmLaneGameState = RhythmLaneGameState(
        chart = JerichoContent.fastMarchChart,
        requiredHits = JerichoContent.FAST_MARCH_REQUIRED_HITS,
    ),
    val shofarState: SequenceGameState,
    val shofarPlacements: List<ShofarNotePlacement>,
    val shoutTaps: Int = 0,
    val reward: JerichoRewardResult? = null,
) {
    val isShoutComplete: Boolean get() = shoutTaps >= JerichoContent.SHOUT_REQUIRED_TAPS
}

/**
 * The Battle of Jericho, rebuilt with 4 real mini-puzzles: the spies'
 * rope escape (a sliding-tile puzzle), setting up camp (12 memorial
 * stones), the six-day silent march, and the seventh-day fast
 * march/shofar/shout finale — replacing the old 4-flashcard "March and
 * the Shout," which had no real challenge.
 */
class JerichoViewModel(
    private val progressionService: ProgressionService,
    private val profileRepository: PlayerProfileRepository,
    private val audioController: AudioController,
) : ViewModel() {

    private val _uiState = MutableStateFlow(createInitialState())
    val uiState: StateFlow<JerichoUiState> = _uiState.asStateFlow()

    val characterCustomization: StateFlow<CharacterCustomization> = profileRepository.profile
        .map { it.character }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = CharacterCustomization(),
        )

    /** Scene ids already completed on a prior playthrough — lets a puzzle's Continue button skip past re-solving it. */
    val previouslyCompletedSceneIds: StateFlow<Set<String>> = profileRepository.profile
        .map { it.progressByChapter[ChapterId.JERICHO]?.completedActivities ?: emptySet() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = emptySet(),
        )

    fun onChoiceSelected(choiceId: String) {
        _uiState.update { it.copy(selectedChoiceId = choiceId) }
    }

    fun onSpiesEscapeTileTapped(index: Int) {
        _uiState.update { current ->
            val next = SlidingPuzzleGame.onTileTapped(current.spiesEscapeState, index)
            if (next.isComplete && !current.spiesEscapeState.isComplete) {
                audioController.playSfx(SoundEffect.ITEM_COLLECTED)
            }
            current.copy(spiesEscapeState = next)
        }
    }

    /** Called only once the screen has confirmed a drag ended within the monument's snap radius — not on every drag. */
    fun onCampStonePlaced(stoneId: String) {
        _uiState.update { current ->
            val next = StackBuildGame.onItemPlaced(current.campState, stoneId)
            if (next.placedOrder.size > current.campState.placedOrder.size) {
                audioController.playSfx(SoundEffect.ITEM_COLLECTED)
            }
            current.copy(campState = next)
        }
    }

    fun onSixDayMarchTapped(lane: Int, nowMs: Long) {
        _uiState.update { current ->
            val next = RhythmLaneGame.onLaneTapped(current.sixDayMarchState, lane, nowMs)
            if (next.hits > current.sixDayMarchState.hits) {
                audioController.playSfx(SoundEffect.TARGET_HIT)
            }
            if (next.isComplete && !current.sixDayMarchState.isComplete) {
                audioController.playSfx(SoundEffect.ITEM_COLLECTED)
            }
            current.copy(sixDayMarchState = next)
        }
    }

    /** Called as the six-day march's real-time clock advances, so a beat nobody tapped in time gets marked missed (feedback only, never a setback). */
    fun onSixDayMarchTimeAdvanced(nowMs: Long) {
        _uiState.update { current -> current.copy(sixDayMarchState = RhythmLaneGame.onTimeAdvanced(current.sixDayMarchState, nowMs)) }
    }

    fun onFastMarchTapped(lane: Int, nowMs: Long) {
        _uiState.update { current ->
            val next = RhythmLaneGame.onLaneTapped(current.fastMarchState, lane, nowMs)
            if (next.hits > current.fastMarchState.hits) {
                audioController.playSfx(SoundEffect.TARGET_HIT)
            }
            if (next.isComplete && !current.fastMarchState.isComplete) {
                audioController.playSfx(SoundEffect.ITEM_COLLECTED)
            }
            current.copy(fastMarchState = next)
        }
    }

    /** Same role as [onSixDayMarchTimeAdvanced], for the faster seventh-day reprise. */
    fun onFastMarchTimeAdvanced(nowMs: Long) {
        _uiState.update { current -> current.copy(fastMarchState = RhythmLaneGame.onTimeAdvanced(current.fastMarchState, nowMs)) }
    }

    fun onShofarNoteTapped(noteId: String) {
        _uiState.update { current ->
            val next = SequenceGame.onPointTapped(current.shofarState, noteId)
            when (next.lastOutcome) {
                SequenceOutcome.POINT_CONNECTED, SequenceOutcome.COMPLETE -> audioController.playSfx(SoundEffect.ITEM_COLLECTED)
                else -> Unit
            }
            current.copy(shofarState = next)
        }
    }

    /** The trumpet fanfare plays exactly once, the moment enough shouts bring the wall down. */
    fun onShoutTapped() {
        _uiState.update { current ->
            if (current.isShoutComplete) return@update current
            val nextTaps = (current.shoutTaps + 1).coerceAtMost(JerichoContent.SHOUT_REQUIRED_TAPS)
            if (nextTaps == JerichoContent.SHOUT_REQUIRED_TAPS) {
                audioController.playSfx(SoundEffect.TRUMPET_FANFARE)
            }
            current.copy(shoutTaps = nextTaps)
        }
    }

    /** Records mid-adventure progress so "Continue Adventure" and a future resume can see it. */
    fun onSceneCompleted(sceneId: String) {
        viewModelScope.launch {
            profileRepository.markSceneCompleted(ChapterId.JERICHO, sceneId)
        }
    }

    /** Idempotent so rapid double-taps on the final CONTINUE never double-award (spec section 20). */
    fun onChapterFinished() {
        if (_uiState.value.reward != null) return
        viewModelScope.launch {
            val stars = RewardCalculator.calculateStars(chapterCompleted = true)
            progressionService.completeChapter(
                chapterId = ChapterId.JERICHO,
                stars = stars,
                badgeId = JerichoReward.badge.id,
                scriptureCardIds = listOf(JerichoReward.scriptureCard.id),
            )
            audioController.playSfx(SoundEffect.REWARD_CELEBRATION)
            _uiState.update { it.copy(reward = JerichoRewardResult(stars = stars)) }
        }
    }

    private fun createInitialState(): JerichoUiState {
        val random = Random.Default
        val shofarPlacements = newShofarPlacements(random)
        return JerichoUiState(
            spiesEscapeState = SlidingPuzzleGame.newShuffled(size = JerichoContent.SPIES_ESCAPE_GRID_SIZE),
            campState = StackBuildGameState(itemIds = JerichoContent.campStones.map { it.id }),
            shofarState = SequenceGameState(pointIds = JerichoContent.shofarNoteColors.map { it.id }.shuffled(random)),
            shofarPlacements = shofarPlacements,
        )
    }

    /** Shuffles which color lands at which screen slot — the required tap order is shuffled separately (see [createInitialState]), so neither layout nor order can be memorized across playthroughs. */
    private fun newShofarPlacements(random: Random = Random.Default): List<ShofarNotePlacement> {
        val shuffledPositions = JerichoContent.shofarNotePositionSlots.shuffled(random)
        return JerichoContent.shofarNoteColors.mapIndexed { index, def ->
            ShofarNotePlacement(id = def.id, nameRes = def.nameRes, position = shuffledPositions[index])
        }
    }
}
