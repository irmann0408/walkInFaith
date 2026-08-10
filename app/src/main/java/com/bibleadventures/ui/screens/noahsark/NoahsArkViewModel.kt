package com.bibleadventures.ui.screens.noahsark

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bibleadventures.audio.AudioController
import com.bibleadventures.audio.SoundEffect
import com.bibleadventures.domain.model.ChapterId
import com.bibleadventures.domain.model.CharacterCustomization
import com.bibleadventures.domain.repository.PlayerProfileRepository
import com.bibleadventures.game.puzzles.dragsort.DragSortGame
import com.bibleadventures.game.puzzles.dragsort.DragSortGameState
import com.bibleadventures.game.puzzles.dragsort.SortCategory
import com.bibleadventures.game.puzzles.dragsort.SortableItem
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

data class NoahsArkRewardResult(val stars: Int)

/** Whether the last tap in a scene landed on a decoy item that doesn't belong. */
enum class DecoyTapOutcome { NONE, DECOY_TAPPED }

data class NoahsArkUiState(
    val foundAnimalIds: Set<String> = emptySet(),
    val matchingState: MatchingGameState,
    val collectedSupplyIds: Set<String> = emptySet(),
    val dragSortState: DragSortGameState,
    val hiddenObjectState: HiddenObjectGameState,
    val reward: NoahsArkRewardResult? = null,
    val lastFindAnimalsDecoyOutcome: DecoyTapOutcome = DecoyTapOutcome.NONE,
    val lastGatherSuppliesDecoyOutcome: DecoyTapOutcome = DecoyTapOutcome.NONE,
    /** Ids (real + decoy), shuffled once per fresh game so the layout isn't the same every time. */
    val findAnimalsOrder: List<String> = emptyList(),
    val gatherSuppliesOrder: List<String> = emptyList(),
)

class NoahsArkViewModel(
    private val progressionService: ProgressionService,
    profileRepository: PlayerProfileRepository,
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

    fun onAnimalFound(animalId: String) {
        _uiState.update { it.copy(foundAnimalIds = it.foundAnimalIds + animalId) }
    }

    /** Never penalized, never blocks progress — the decoy just stays tappable. */
    fun onFindAnimalsDecoyTapped() {
        _uiState.update { it.copy(lastFindAnimalsDecoyOutcome = DecoyTapOutcome.DECOY_TAPPED) }
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

    fun onSupplyCollected(supplyId: String) {
        _uiState.update { it.copy(collectedSupplyIds = it.collectedSupplyIds + supplyId) }
    }

    /** Never penalized, never blocks progress — the decoy just stays tappable. */
    fun onGatherSuppliesDecoyTapped() {
        _uiState.update { it.copy(lastGatherSuppliesDecoyOutcome = DecoyTapOutcome.DECOY_TAPPED) }
    }

    fun onSortItemDropped(itemId: String, categoryKey: String) {
        _uiState.update { current ->
            current.copy(dragSortState = DragSortGame.onItemDroppedOnCategory(current.dragSortState, itemId, categoryKey))
        }
    }

    fun onHiddenItemTapped(itemId: String) {
        _uiState.update { current ->
            current.copy(hiddenObjectState = HiddenObjectGame.onItemTapped(current.hiddenObjectState, itemId))
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
                scriptureCardId = NoahsArkReward.scriptureCard.id,
            )
            audioController.playSfx(SoundEffect.REWARD_CELEBRATION)
            _uiState.update { it.copy(reward = NoahsArkRewardResult(stars = stars)) }
        }
    }

    private fun createInitialState(): NoahsArkUiState {
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

        // Shuffled once per fresh game (like matchItems above), not on every
        // recomposition — order stays put for the rest of that playthrough.
        val sortableItems = NoahsArkContent.sortableItems.shuffled().map {
            SortableItem(id = it.id, iconRes = it.iconRes, contentDescriptionRes = it.nameRes, categoryKey = it.categoryKey)
        }
        val sortCategories = NoahsArkContent.sortCategories.map { SortCategory(key = it.key, labelRes = it.labelRes) }

        // Positions are hand-placed to fit the background and avoid overlap, so only
        // which item lands on which position is shuffled, not the positions themselves.
        val shuffledHiddenPositions = NoahsArkContent.hiddenItems.map { it.position }.shuffled()
        val hiddenItems = NoahsArkContent.hiddenItems.mapIndexed { index, def ->
            HiddenItem(id = def.id, position = shuffledHiddenPositions[index], iconRes = def.iconRes, contentDescriptionRes = def.nameRes)
        }

        val findAnimalsOrder = (NoahsArkContent.animals.map { it.id } + NoahsArkContent.findAnimalsDecoys.map { it.id }).shuffled()
        val gatherSuppliesOrder = (NoahsArkContent.supplies.map { it.id } + NoahsArkContent.gatherSuppliesDecoys.map { it.id }).shuffled()

        return NoahsArkUiState(
            matchingState = MatchingGameState(items = matchItems),
            dragSortState = DragSortGameState(items = sortableItems, categories = sortCategories),
            hiddenObjectState = HiddenObjectGameState(items = hiddenItems),
            findAnimalsOrder = findAnimalsOrder,
            gatherSuppliesOrder = gatherSuppliesOrder,
        )
    }
}
