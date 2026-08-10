package com.bibleadventures.ui.screens.davidgoliath

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bibleadventures.audio.AudioController
import com.bibleadventures.audio.SoundEffect
import com.bibleadventures.domain.model.ChapterId
import com.bibleadventures.domain.model.CharacterCustomization
import com.bibleadventures.domain.repository.PlayerProfileRepository
import com.bibleadventures.game.puzzles.hiddenobject.HiddenItem
import com.bibleadventures.game.puzzles.hiddenobject.HiddenObjectGame
import com.bibleadventures.game.puzzles.hiddenobject.HiddenObjectGameState
import com.bibleadventures.game.puzzles.slingshot.SlingshotGame
import com.bibleadventures.game.puzzles.slingshot.SlingshotGameState
import com.bibleadventures.game.puzzles.slingshot.SlingshotOutcome
import com.bibleadventures.game.rewards.DavidGoliathReward
import com.bibleadventures.game.rewards.RewardCalculator
import com.bibleadventures.game.stories.DavidGoliathContent
import com.bibleadventures.progress.ProgressionService
import com.bibleadventures.ui.screens.noahsark.DecoyTapOutcome
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
    val hiddenObjectState: HiddenObjectGameState,
    /** Shuffled in with the stones each fresh game, same as the stones' own positions. */
    val riverbedDecoyPosition: Offset = Offset.Zero,
    val slingshotState: SlingshotGameState = SlingshotGameState(),
    val selectedChoiceId: String? = null,
    val reward: DavidGoliathRewardResult? = null,
    val lastRiverbedDecoyOutcome: DecoyTapOutcome = DecoyTapOutcome.NONE,
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

    fun onStoneFound(stoneId: String) {
        _uiState.update { current ->
            current.copy(hiddenObjectState = HiddenObjectGame.onItemTapped(current.hiddenObjectState, stoneId))
        }
    }

    /** Never penalized, never blocks progress — the decoy just stays tappable. */
    fun onRiverbedDecoyTapped() {
        _uiState.update { it.copy(lastRiverbedDecoyOutcome = DecoyTapOutcome.DECOY_TAPPED) }
    }

    fun onChoiceSelected(choiceId: String) {
        _uiState.update { it.copy(selectedChoiceId = choiceId) }
    }

    fun onStoneReleased(aimedPosition: Float, markPosition: Float) {
        _uiState.update { current ->
            val next = SlingshotGame.onStoneReleased(current.slingshotState, aimedPosition, markPosition)
            if (next.lastOutcome == SlingshotOutcome.HIT) {
                audioController.playSfx(SoundEffect.TARGET_HIT)
            }
            current.copy(slingshotState = next)
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
                scriptureCardId = DavidGoliathReward.scriptureCard.id,
            )
            audioController.playSfx(SoundEffect.REWARD_CELEBRATION)
            _uiState.update { it.copy(reward = DavidGoliathRewardResult(stars = stars)) }
        }
    }

    private fun createInitialState(): DavidGoliathUiState {
        // The 5 stones + the riverbed decoy share one shuffled pool of hand-placed
        // positions, so the decoy doesn't always land in the same spot either —
        // same technique NoahsArkViewModel uses for its hidden items.
        val allSpots = DavidGoliathContent.stones + DavidGoliathContent.riverbedDecoy
        val shuffledPositions = allSpots.map { it.position }.shuffled()

        val hiddenItems = DavidGoliathContent.stones.mapIndexed { index, def ->
            HiddenItem(id = def.id, position = shuffledPositions[index], iconRes = def.iconRes, contentDescriptionRes = def.nameRes)
        }
        val decoyPosition = shuffledPositions[DavidGoliathContent.stones.size]

        return DavidGoliathUiState(
            hiddenObjectState = HiddenObjectGameState(items = hiddenItems),
            riverbedDecoyPosition = decoyPosition,
        )
    }
}
