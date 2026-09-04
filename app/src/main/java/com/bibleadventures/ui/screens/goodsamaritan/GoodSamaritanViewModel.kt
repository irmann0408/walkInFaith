package com.bibleadventures.ui.screens.goodsamaritan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bibleadventures.audio.AudioController
import com.bibleadventures.audio.SoundEffect
import com.bibleadventures.domain.model.ChapterId
import com.bibleadventures.domain.model.CharacterCustomization
import com.bibleadventures.domain.repository.PlayerProfileRepository
import com.bibleadventures.game.puzzles.dungeon.DungeonGame
import com.bibleadventures.game.puzzles.dungeon.DungeonGameState
import com.bibleadventures.game.puzzles.dungeon.DungeonOutcome
import com.bibleadventures.game.puzzles.dungeon.Vector2
import com.bibleadventures.game.puzzles.roadblock.RoadblockGame
import com.bibleadventures.game.puzzles.roadblock.RoadblockGameState
import com.bibleadventures.game.puzzles.roadblock.Direction as RoadblockDirection
import com.bibleadventures.game.rewards.GoodSamaritanReward
import com.bibleadventures.game.rewards.RewardCalculator
import com.bibleadventures.game.stories.GoodSamaritanContent
import com.bibleadventures.progress.ProgressionService
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GoodSamaritanRewardResult(val stars: Int)

data class GoodSamaritanUiState(
    val dungeonState: DungeonGameState,
    val roadblockState: RoadblockGameState,
    /** Whether the player has dismissed the "helping" story beat shown once the traveler is treated. */
    val helpingBeatAcknowledged: Boolean = false,
    val reward: GoodSamaritanRewardResult? = null,
)

class GoodSamaritanViewModel(
    private val progressionService: ProgressionService,
    private val profileRepository: PlayerProfileRepository,
    private val audioController: AudioController,
    /** Injectable so tests can force deterministic hit/steal rolls (see [DungeonGame.onSupplyThrown]/[DungeonGame.onBanditAttack]) instead of depending on real randomness. */
    private val random: Random = Random.Default,
) : ViewModel() {

    private val _uiState = MutableStateFlow(createInitialState())
    val uiState: StateFlow<GoodSamaritanUiState> = _uiState.asStateFlow()

    val characterCustomization: StateFlow<CharacterCustomization> = profileRepository.profile
        .map { it.character }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = CharacterCustomization(),
        )

    /** Scene ids already completed on a prior playthrough — lets a puzzle's Continue button skip past re-solving it. */
    val previouslyCompletedSceneIds: StateFlow<Set<String>> = profileRepository.profile
        .map { it.progressByChapter[ChapterId.GOOD_SAMARITAN]?.completedActivities ?: emptySet() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = emptySet(),
        )

    /**
     * One frame of joystick-driven movement — see [DungeonGame.tick] for
     * why the screen, not this ViewModel, owns [deltaSeconds]. [DungeonOutcome]
     * is sticky (an ordinary movement frame never resets it — see
     * [com.bibleadventures.game.puzzles.dungeon.DungeonGameState]'s own doc
     * comment), so the SFX dispatch below must compare against the
     * *previous* outcome, not just switch on the new one — otherwise every
     * subsequent movement-only frame after a real pickup would keep
     * replaying that pickup's sound for as long as nothing else happens.
     */
    fun onDungeonTick(joystickInput: Vector2, deltaSeconds: Float) {
        _uiState.update { current ->
            val previousOutcome = current.dungeonState.lastOutcome
            val next = DungeonGame.tick(current.dungeonState, joystickInput, deltaSeconds, random)
            if (next.lastOutcome != previousOutcome) {
                when (next.lastOutcome) {
                    DungeonOutcome.SUPPLY_COLLECTED, DungeonOutcome.CHECKPOINT_ACTIVATED ->
                        audioController.playSfx(SoundEffect.ITEM_COLLECTED)
                    else -> Unit
                }
            }
            current.copy(dungeonState = next)
        }
    }

    /** Tapping the player's own character throws one medical supply at the bandit — a real roll now, favored but not guaranteed (see [DungeonGame.onSupplyThrown]). Same sticky-[DungeonOutcome] comparison as [onDungeonTick]. */
    fun onSupplyThrown() {
        _uiState.update { current ->
            val previousOutcome = current.dungeonState.lastOutcome
            val next = DungeonGame.onSupplyThrown(current.dungeonState, random)
            if (next.lastOutcome != previousOutcome) {
                when (next.lastOutcome) {
                    DungeonOutcome.BANDIT_HIT -> audioController.playSfx(SoundEffect.TARGET_HIT)
                    DungeonOutcome.BANDIT_SCARED_OFF -> audioController.playSfx(SoundEffect.OBSTACLE_DODGED)
                    else -> Unit
                }
            }
            current.copy(dungeonState = next)
        }
    }

    /** The bandit's own melee counter-attack, triggered by the screen after a hit's throw animation lands (see [DungeonGame.onBanditAttack]) — never hurts the player, just a chance to steal a supply back. */
    fun onBanditAttack() {
        _uiState.update { current ->
            val previousOutcome = current.dungeonState.lastOutcome
            val next = DungeonGame.onBanditAttack(current.dungeonState, random)
            if (next.lastOutcome != previousOutcome) {
                when (next.lastOutcome) {
                    DungeonOutcome.BANDIT_ATTACK_MISSED -> audioController.playSfx(SoundEffect.OBSTACLE_DODGED)
                    else -> Unit
                }
            }
            current.copy(dungeonState = next)
        }
    }

    /** Leaves an unwinnable-right-now bandit fight without losing anything but the supplies already spent — see [DungeonGame.onRetreat]. */
    fun onRetreat() {
        _uiState.update { it.copy(dungeonState = DungeonGame.onRetreat(it.dungeonState)) }
    }

    /**
     * "Passing By": no celebratory SFX on [com.bibleadventures.game.puzzles.roadblock.RoadblockOutcome.EXITED] —
     * unlike every other puzzle's completion, this one isn't a moment to
     * celebrate (see [com.bibleadventures.ui.screens.goodsamaritan.passingby.GoodSamaritanPassingByScreen]
     * for the character's own non-celebratory completion message).
     */
    fun onSlideAttempted(blockId: String, direction: RoadblockDirection, cellsAttempted: Int) {
        _uiState.update { current ->
            current.copy(roadblockState = RoadblockGame.onSlideAttempted(current.roadblockState, blockId, direction, cellsAttempted))
        }
    }

    /** Dismisses the "helping" story beat overlay once the player has read it. */
    fun onHelpingBeatAcknowledged() {
        _uiState.update { it.copy(helpingBeatAcknowledged = true) }
    }

    /** Records mid-adventure progress so "Continue Adventure" and a future resume can see it. */
    fun onSceneCompleted(sceneId: String) {
        viewModelScope.launch {
            profileRepository.markSceneCompleted(ChapterId.GOOD_SAMARITAN, sceneId)
        }
    }

    /** Idempotent so rapid double-taps on the final CONTINUE never double-award (spec section 20). */
    fun onChapterFinished() {
        if (_uiState.value.reward != null) return
        viewModelScope.launch {
            val stars = RewardCalculator.calculateStars(chapterCompleted = true)
            progressionService.completeChapter(
                chapterId = ChapterId.GOOD_SAMARITAN,
                stars = stars,
                badgeId = GoodSamaritanReward.badge.id,
                scriptureCardIds = listOf(GoodSamaritanReward.scriptureCard.id),
            )
            audioController.playSfx(SoundEffect.REWARD_CELEBRATION)
            _uiState.update { it.copy(reward = GoodSamaritanRewardResult(stars = stars)) }
        }
    }

    private fun createInitialState(): GoodSamaritanUiState {
        val roadblockState = RoadblockGame.fromLayout(
            layout = GoodSamaritanContent.passingByLayout,
            blockSpecs = GoodSamaritanContent.passingByBlockSpecs,
            protagonistId = GoodSamaritanContent.passingByProtagonistId,
            exitColumns = GoodSamaritanContent.passingByExitColumns,
        )

        return GoodSamaritanUiState(
            dungeonState = DungeonGame.fromLayout(GoodSamaritanContent.mapLayout, GoodSamaritanContent.banditPatrols),
            roadblockState = roadblockState,
        )
    }
}
