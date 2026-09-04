package com.bibleadventures.ui.screens.davidgoliath

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bibleadventures.audio.AudioController
import com.bibleadventures.audio.CharacterVoiceLine
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
import com.bibleadventures.game.puzzles.slingshot.SlingshotGame
import com.bibleadventures.game.puzzles.slingshot.SlingshotGameState
import com.bibleadventures.game.puzzles.slingshot.SlingshotOutcome
import com.bibleadventures.game.puzzles.slingshot.Vector2
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

data class DavidGoliathRewardResult(val stars: Int)

data class DavidGoliathUiState(
    /** "Choose the Stones" — David already has 1 stone; find 4 more by connecting 4 in a row. */
    val connectFourState: ConnectFourGameState = ConnectFourGame.newGame(),
    /** "Sling Practice" — 5 rats, one at a time; hit as many as you can before each one reaches the bottom. */
    val slingshotState: SlingshotGameState = SlingshotGameState(),
    val selectedChoiceId: String? = null,
    val reward: DavidGoliathRewardResult? = null,
    val sheepCountingState: MatchingGameState,
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
            when (next.lastOutcome) {
                MatchOutcome.CORRECT -> {
                    audioController.playSfx(SoundEffect.MATCH_SUCCESS)
                    audioController.playCharacterLine(CharacterVoiceLine.FEEDBACK_GREAT_JOB)
                }
                MatchOutcome.TRY_AGAIN -> audioController.playCharacterLine(CharacterVoiceLine.FEEDBACK_TRY_ANOTHER_ONE)
                MatchOutcome.NONE -> Unit
            }
            current.copy(sheepCountingState = next)
        }
    }

    fun onStoneReleased(anchor: Vector2, pull: Vector2, ratPosition: Vector2) {
        _uiState.update { current ->
            val next = SlingshotGame.onStoneReleased(current.slingshotState, anchor, pull, ratPosition)
            if (next.hits > current.slingshotState.hits) {
                audioController.playSfx(SoundEffect.TARGET_HIT)
            }
            when (next.lastOutcome) {
                SlingshotOutcome.HIT -> audioController.playCharacterLine(CharacterVoiceLine.FEEDBACK_GREAT_JOB)
                SlingshotOutcome.MISS -> audioController.playCharacterLine(CharacterVoiceLine.FEEDBACK_TRY_ANOTHER_ONE)
                SlingshotOutcome.ESCAPED, SlingshotOutcome.NONE -> Unit
            }
            current.copy(slingshotState = next)
        }
    }

    /** Called by the screen once the current rat's fall duration elapses without being hit — never punished, the next rat simply appears. */
    fun onRatEscaped() {
        _uiState.update { current ->
            audioController.playCharacterLine(CharacterVoiceLine.DAVID_SLING_ESCAPED)
            current.copy(slingshotState = SlingshotGame.onRatEscaped(current.slingshotState))
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
