package com.bibleadventures.ui.screens.goodsamaritan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bibleadventures.audio.AudioController
import com.bibleadventures.audio.CharacterVoiceLine
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
    /** Which of [GoodSamaritanContent.passingByLevels] [roadblockState] currently holds — advances via [GoodSamaritanViewModel.onPassingByNextLevel] once a level's own [RoadblockGameState.isComplete] is true. */
    val passingByLevelIndex: Int = 0,
    /** Whether the player has dismissed the "helping" story beat shown once the traveler is treated. */
    val helpingBeatAcknowledged: Boolean = false,
    /**
     * Whether the player has already seen the medical-supply/bandit
     * explainer popup — shown once automatically the first time the player
     * ever collects a supply or is ever ambushed (see
     * [com.bibleadventures.ui.screens.goodsamaritan.explore.GoodSamaritanExploreScreen]),
     * and any time afterward the player deliberately taps that item's map
     * icon. Tapping early sets this flag too, so the automatic version
     * never redundantly repeats something the player already asked to see.
     */
    val medicalSupplyPreviewAcknowledged: Boolean = false,
    val banditPreviewAcknowledged: Boolean = false,
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

    /** The Good Samaritan's own melee turn, triggered by the screen right after the player's own throw resolves (see [DungeonGame.onSamaritanAttack]) — a second real attack each round, at no supply cost. */
    fun onSamaritanAttack() {
        _uiState.update { current ->
            val previousOutcome = current.dungeonState.lastOutcome
            val next = DungeonGame.onSamaritanAttack(current.dungeonState, random)
            if (next.lastOutcome != previousOutcome) {
                when (next.lastOutcome) {
                    DungeonOutcome.SAMARITAN_HIT -> audioController.playSfx(SoundEffect.TARGET_HIT)
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
     * for the character's own non-celebratory completion message). The
     * character's own recorded line for that moment — "well done, tap for
     * the next puzzle" on an earlier level, or the parable's moral on the
     * last one — plays exactly once, right on the false-to-true completion
     * edge, so it never re-fires on an unrelated recomposition.
     */
    fun onSlideAttempted(blockId: String, direction: RoadblockDirection, cellsAttempted: Int) {
        _uiState.update { current ->
            val newRoadblockState = RoadblockGame.onSlideAttempted(current.roadblockState, blockId, direction, cellsAttempted)
            if (newRoadblockState.isComplete && !current.roadblockState.isComplete) {
                val isLastLevel = current.passingByLevelIndex == GoodSamaritanContent.passingByLevels.lastIndex
                audioController.playCharacterLine(
                    if (isLastLevel) CharacterVoiceLine.GOOD_SAMARITAN_PASSING_BY_MORAL else CharacterVoiceLine.GOOD_SAMARITAN_PASSING_BY_LEVEL_COMPLETE,
                )
            }
            current.copy(roadblockState = newRoadblockState)
        }
    }

    /**
     * Advances from one solved [com.bibleadventures.game.stories.GoodSamaritanContent.PassingByLevel]
     * to the next — a fresh [RoadblockGameState] built from that level's own
     * layout, same "solve step N, move to N+1 in place" shape as Daniel's
     * Lions Den math sequence. A no-op if the current level isn't actually
     * solved yet, or there's no next level to advance to (the screen's own
     * "Continue" button calls [onSceneCompleted] instead once
     * [GoodSamaritanContent.passingByLevels]'s last level is solved).
     */
    fun onPassingByNextLevel() {
        _uiState.update { current ->
            val nextIndex = current.passingByLevelIndex + 1
            if (!current.roadblockState.isComplete || nextIndex !in GoodSamaritanContent.passingByLevels.indices) return@update current
            current.copy(roadblockState = buildRoadblockState(nextIndex), passingByLevelIndex = nextIndex)
        }
    }

    /** Dismisses the "helping" story beat overlay once the player has read it. */
    fun onHelpingBeatAcknowledged() {
        _uiState.update { it.copy(helpingBeatAcknowledged = true) }
    }

    /** Dismisses the medical-supply explainer popup, whether it showed automatically (first-ever pickup) or from a deliberate tap on its map icon. */
    fun onMedicalSupplyPreviewAcknowledged() {
        _uiState.update { it.copy(medicalSupplyPreviewAcknowledged = true) }
    }

    /** Dismisses the bandit explainer popup, whether it showed automatically (first-ever ambush) or from a deliberate tap on a bandit's map icon. */
    fun onBanditPreviewAcknowledged() {
        _uiState.update { it.copy(banditPreviewAcknowledged = true) }
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

    private fun buildRoadblockState(levelIndex: Int): RoadblockGameState {
        val level = GoodSamaritanContent.passingByLevels[levelIndex]
        return RoadblockGame.fromLayout(
            layout = level.layout,
            blockSpecs = level.blockSpecs,
            protagonistId = GoodSamaritanContent.passingByProtagonistId,
            exitColumns = level.exitColumns,
        )
    }

    private fun createInitialState(): GoodSamaritanUiState {
        return GoodSamaritanUiState(
            dungeonState = DungeonGame.fromLayout(GoodSamaritanContent.mapLayout, GoodSamaritanContent.banditPatrols),
            roadblockState = buildRoadblockState(levelIndex = 0),
        )
    }
}
