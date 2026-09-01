package com.bibleadventures.ui.screens.noahsark

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bibleadventures.audio.AudioController
import com.bibleadventures.audio.SoundEffect
import com.bibleadventures.domain.model.ChapterId
import com.bibleadventures.domain.model.CharacterCustomization
import com.bibleadventures.domain.repository.PlayerProfileRepository
import com.bibleadventures.game.puzzles.groupfill.FamilyGroup
import com.bibleadventures.game.puzzles.groupfill.GroupFillGame
import com.bibleadventures.game.puzzles.groupfill.GroupFillGameState
import com.bibleadventures.game.puzzles.hiddenobject.HiddenItem
import com.bibleadventures.game.puzzles.hiddenobject.HiddenObjectGame
import com.bibleadventures.game.puzzles.hiddenobject.HiddenObjectGameState
import com.bibleadventures.game.puzzles.matching.MatchItem
import com.bibleadventures.game.puzzles.matching.MatchOutcome
import com.bibleadventures.game.puzzles.matching.MatchingGame
import com.bibleadventures.game.puzzles.matching.MatchingGameState
import com.bibleadventures.game.rewards.NoahsArkReward
import com.bibleadventures.game.rewards.RewardCalculator
import com.bibleadventures.game.stories.NoahsArkContent
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

data class NoahsArkRewardResult(val stars: Int)

/** Whether the last tap in a scene landed on a decoy item that doesn't belong. */
enum class DecoyTapOutcome { NONE, DECOY_TAPPED }

data class NoahsArkUiState(
    val matchingState: MatchingGameState,
    /** "Load the Ark" — drag numbered supply baskets onto 3 decks until each hits its exact capacity. */
    val groupFillState: GroupFillGameState,
    /** Cosmetic only (basket id -> supply kind id): which icon a basket renders, unrelated to the arithmetic. */
    val loadArkBasketSupplyKinds: Map<String, String>,
    /** "Find the Tools" — 10 fixed tap targets baked into one background scene image. */
    val hiddenObjectState: HiddenObjectGameState,
    val reward: NoahsArkRewardResult? = null,
    /** Last tap on "Find the Tools" that landed outside every tool hotspot. */
    val lastFindToolsWrongTapOutcome: DecoyTapOutcome = DecoyTapOutcome.NONE,
)

class NoahsArkViewModel(
    private val progressionService: ProgressionService,
    private val profileRepository: PlayerProfileRepository,
    private val audioController: AudioController,
) : ViewModel() {

    private val _uiState = MutableStateFlow(createInitialState())
    val uiState: StateFlow<NoahsArkUiState> = _uiState.asStateFlow()

    val characterCustomization: StateFlow<CharacterCustomization> = profileRepository.profile
        .map { it.character }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = CharacterCustomization(),
        )

    /** Scene ids already completed on a prior playthrough — lets a puzzle's Continue button skip past re-solving it. */
    val previouslyCompletedSceneIds: StateFlow<Set<String>> = profileRepository.profile
        .map { it.progressByChapter[ChapterId.NOAHS_ARK]?.completedActivities ?: emptySet() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = emptySet(),
        )

    /** A tap on "Find the Tools" that landed outside every tool hotspot — never penalized, never blocks progress. */
    fun onFindToolsBackgroundTapped() {
        _uiState.update { it.copy(lastFindToolsWrongTapOutcome = DecoyTapOutcome.DECOY_TAPPED) }
    }

    fun onMatchItemTapped(itemId: String) {
        _uiState.update { current ->
            val newMatchingState = MatchingGame.onItemTapped(current.matchingState, itemId)
            if (newMatchingState.lastOutcome == MatchOutcome.CORRECT) {
                audioController.playSfx(SoundEffect.MATCH_SUCCESS)
            }
            current.copy(matchingState = newMatchingState)
        }
    }

    /** Called only once the screen has confirmed a drag ended over a given deck — not on every drag. */
    fun onBasketDropped(basketId: String, deckIndex: Int) {
        _uiState.update { current ->
            val next = GroupFillGame.onFamilyDropped(current.groupFillState, basketId, deckIndex)
            if (next.placedFamilyIds.size > current.groupFillState.placedFamilyIds.size) {
                audioController.playSfx(SoundEffect.ITEM_COLLECTED)
            }
            current.copy(groupFillState = next)
        }
    }

    fun onHiddenItemTapped(itemId: String) {
        _uiState.update { current ->
            current.copy(
                hiddenObjectState = HiddenObjectGame.onItemTapped(current.hiddenObjectState, itemId),
                // Clears any lingering "That's not a tool!" bubble from an earlier
                // wrong tap — a correct tap right after should never leave stale
                // wrong-answer feedback on screen.
                lastFindToolsWrongTapOutcome = DecoyTapOutcome.NONE,
            )
        }
    }

    /** Records mid-adventure progress so "Continue Adventure" and a future resume can see it. */
    fun onSceneCompleted(sceneId: String) {
        viewModelScope.launch {
            profileRepository.markSceneCompleted(ChapterId.NOAHS_ARK, sceneId)
        }
    }

    /** Idempotent so rapid double-taps on the final CONTINUE never double-award (spec section 20). */
    fun onChapterFinished() {
        if (_uiState.value.reward != null) return
        viewModelScope.launch {
            val stars = RewardCalculator.calculateStars(chapterCompleted = true)
            progressionService.completeChapter(
                chapterId = ChapterId.NOAHS_ARK,
                stars = stars,
                badgeId = NoahsArkReward.badge.id,
                scriptureCardIds = listOf(NoahsArkReward.scriptureCard.id),
            )
            audioController.playSfx(SoundEffect.REWARD_CELEBRATION)
            _uiState.update { it.copy(reward = NoahsArkRewardResult(stars = stars)) }
        }
    }

    private fun createInitialState(): NoahsArkUiState {
        val random = Random.Default
        val matchItems = NoahsArkContent.animals.flatMap { animal ->
            listOf(
                MatchItem(
                    id = "${animal.id}_a",
                    iconRes = animal.iconRes,
                    contentDescriptionRes = animal.nameRes,
                    pairKey = animal.id,
                ),
                MatchItem(
                    id = "${animal.id}_b",
                    iconRes = animal.iconRes,
                    contentDescriptionRes = animal.nameRes,
                    pairKey = animal.id,
                ),
            )
        }.shuffled()

        val (loadArkBaskets, loadArkBasketSupplyKinds) = newLoadArkBaskets(random)

        // Unlike a typical hidden-object scene, these positions are NOT interchangeable —
        // each tool is baked into one fixed spot in the background art itself, so nothing
        // here is shuffled.
        val findToolsItems = NoahsArkContent.findToolsHotspots.map {
            HiddenItem(id = it.id, position = it.position, iconRes = it.iconRes, contentDescriptionRes = it.nameRes)
        }

        return NoahsArkUiState(
            matchingState = MatchingGameState(items = matchItems),
            groupFillState = GroupFillGameState(
                families = loadArkBaskets,
                circleTargets = NoahsArkContent.loadArkDeckTargets,
            ),
            loadArkBasketSupplyKinds = loadArkBasketSupplyKinds,
            hiddenObjectState = HiddenObjectGameState(items = findToolsItems),
        )
    }

    /**
     * Splits each of [NoahsArkContent.loadArkDeckTargets] into 3-5 random
     * baskets that sum exactly to it (same "build the puzzle from its own
     * solution" generator Feeding the 5000 uses for its seating circles —
     * see [GroupFillGame.randomSolvablePartition]), pools and shuffles them
     * across decks so the tray order never hints which deck a basket
     * belongs to, and independently assigns each basket one of
     * [NoahsArkContent.loadArkSupplyKinds] for its icon — purely cosmetic,
     * unrelated to the arithmetic the engine actually checks.
     */
    private fun newLoadArkBaskets(random: Random): Pair<List<FamilyGroup>, Map<String, String>> {
        val baskets = mutableListOf<FamilyGroup>()
        NoahsArkContent.loadArkDeckTargets.forEachIndexed { deckIndex, target ->
            GroupFillGame.randomSolvablePartition(target, minParts = 3, maxParts = 5, random).forEachIndexed { partIndex, count ->
                baskets += FamilyGroup(id = "basket_${deckIndex}_$partIndex", headcount = count)
            }
        }
        val shuffledBaskets = baskets.shuffled(random)
        val supplyKinds = NoahsArkContent.loadArkSupplyKinds.map { it.id }
        val basketSupplyKinds = shuffledBaskets.mapIndexed { index, basket ->
            basket.id to supplyKinds[index % supplyKinds.size]
        }.toMap()
        return shuffledBaskets to basketSupplyKinds
    }
}
