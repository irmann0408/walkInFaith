package com.bibleadventures.ui.screens.feeding5000

import com.bibleadventures.FakeAudioController
import com.bibleadventures.FakePlayerProfileRepository
import com.bibleadventures.MainDispatcherRule
import com.bibleadventures.audio.SoundEffect
import com.bibleadventures.domain.model.ChapterId
import com.bibleadventures.game.puzzles.gridmaze.Direction
import com.bibleadventures.game.puzzles.gridmaze.GridPosition
import com.bibleadventures.game.puzzles.gridmaze.GridTileType
import com.bibleadventures.game.puzzles.groupfill.GroupFillOutcome
import com.bibleadventures.game.puzzles.rhythmlane.RhythmLaneChart
import com.bibleadventures.game.stories.Feeding5000Content
import com.bibleadventures.game.stories.MathOperator
import com.bibleadventures.progress.ProgressionService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class Feeding5000ViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun createViewModel(
        repository: FakePlayerProfileRepository = FakePlayerProfileRepository(),
        audioController: FakeAudioController = FakeAudioController(),
    ) = Feeding5000ViewModel(ProgressionService(repository), repository, audioController)

    /** The generated family id encodes which circle it was originally partitioned for (`family_<circleIndex>_<n>`) — ground truth for tests, even though the engine itself never uses it. */
    private fun originCircleIndex(familyId: String): Int = familyId.substringAfter("family_").substringBefore("_").toInt()

    // --- Gathering the Crowd (groupfill) ---

    @Test
    fun `initial groupFill state holds all 3 circle targets, none placed`() {
        val state = createViewModel().uiState.value.groupFillState

        assertEquals(listOf(50, 50, 100), state.circleTargets)
        assertTrue(state.placedFamilyIds.isEmpty())
        assertFalse(state.isComplete)
    }

    @Test
    fun `generated group-fill families always partition exactly into the circle targets`() {
        // Constructed 100 times to exercise many random draws (Random.Default,
        // unseeded) — same "check invariants across many random instances, not
        // just one lucky run" discipline as SlidingPuzzleGameTest's shuffle tests.
        repeat(100) {
            val state = createViewModel().uiState.value.groupFillState

            assertEquals(state.families.size, state.families.map { it.id }.toSet().size) // no duplicate ids
            assertTrue(state.families.all { it.headcount > 0 })

            val sumsByOriginCircle = state.families.groupBy { originCircleIndex(it.id) }
            state.circleTargets.indices.forEach { index ->
                assertEquals("circle $index doesn't sum to its target", state.circleTargets[index], sumsByOriginCircle.getValue(index).sumOf { it.headcount })
            }
        }
    }

    @Test
    fun `onFamilyDropped with a fitting family advances and plays a sound`() {
        val audioController = FakeAudioController()
        val viewModel = createViewModel(audioController = audioController)
        val family = viewModel.uiState.value.groupFillState.families.first { originCircleIndex(it.id) == 0 }

        viewModel.onFamilyDropped(family.id, 0)

        assertTrue(family.id in viewModel.uiState.value.groupFillState.placedFamilyIds)
        assertEquals(listOf(SoundEffect.ITEM_COLLECTED), audioController.playedEffects)
    }

    @Test
    fun `dropping a family into an already-full circle is rejected without failing`() {
        val viewModel = createViewModel()
        val state = viewModel.uiState.value.groupFillState
        val circle0Families = state.families.filter { originCircleIndex(it.id) == 0 }
        circle0Families.forEach { viewModel.onFamilyDropped(it.id, 0) } // fills circle 0 exactly
        assertTrue(viewModel.uiState.value.groupFillState.isCircleComplete(0))

        val extraFamily = state.families.first { originCircleIndex(it.id) == 1 }
        viewModel.onFamilyDropped(extraFamily.id, 0) // circle 0 is already full — must overshoot

        assertEquals(GroupFillOutcome.REJECTED_OVERSHOOT, viewModel.uiState.value.groupFillState.lastOutcome)
        assertTrue(extraFamily.id !in viewModel.uiState.value.groupFillState.placedFamilyIds)
    }

    @Test
    fun `dropping every family into its originally-generated circle completes gathering the crowd`() {
        val viewModel = createViewModel()
        val families = viewModel.uiState.value.groupFillState.families

        families.forEach { family -> viewModel.onFamilyDropped(family.id, originCircleIndex(family.id)) }

        assertTrue(viewModel.uiState.value.groupFillState.isComplete)
    }

    // --- Searching for Food (hiddenobject) ---

    @Test
    fun `initial searching state holds the boy, not yet found`() {
        val state = createViewModel().uiState.value.searchingState

        assertEquals(1, state.items.size)
        assertFalse(state.isComplete)
    }

    @Test
    fun `the boy's position is randomized within bounds and clear of every decoy, across 100 constructions`() {
        repeat(100) {
            val boy = createViewModel().uiState.value.searchingState.items.single { it.id == "boy" }

            assertTrue("boy x out of bounds: ${boy.position.x}", boy.position.x in 0.08f..0.90f)
            assertTrue("boy y out of bounds: ${boy.position.y}", boy.position.y in 0.58f..0.88f)
            Feeding5000Content.searchingForFoodDecoys.forEach { decoy ->
                val distance = (boy.position - decoy.position).getDistance()
                assertTrue("boy landed on top of ${decoy.id}: distance=$distance", distance >= 0.05f)
            }
        }
    }

    @Test
    fun `onBoyFound marks the boy found and plays a sound only once`() {
        val audioController = FakeAudioController()
        val viewModel = createViewModel(audioController = audioController)

        viewModel.onBoyFound("boy")
        viewModel.onBoyFound("boy") // already found, re-tap is a no-op

        assertTrue("boy" in viewModel.uiState.value.searchingState.foundIds)
        assertEquals(listOf(SoundEffect.ITEM_COLLECTED), audioController.playedEffects)
    }

    // --- The Boy's Gift (hiddenobject, with decoys) ---

    @Test
    fun `initial boysGift state holds exactly 5 loaves and 2 fish, none found`() {
        val state = createViewModel().uiState.value.boysGiftState

        assertEquals(7, state.items.size)
        assertEquals(5, state.items.count { it.id.startsWith("loaf_") })
        assertEquals(2, state.items.count { it.id.startsWith("fish_") })
        assertFalse(state.isComplete)
    }

    @Test
    fun `finding all 7 real items completes the boys gift scene`() {
        val viewModel = createViewModel()

        Feeding5000ViewModel.boysGiftRealItems.forEach { viewModel.onBoysGiftItemTapped(it.id) }

        assertTrue(viewModel.uiState.value.boysGiftState.isComplete)
    }

    // --- The Miracle Multiplication (decisionpath) ---

    @Test
    fun `onMiracleAnswerTapped with the correct value advances the step and plays a sound`() {
        val audioController = FakeAudioController()
        val viewModel = createViewModel(audioController = audioController)
        val correctValue = viewModel.uiState.value.miracleState.currentStep!!.correctOptionId.toInt()

        viewModel.onMiracleAnswerTapped(correctValue)

        assertEquals(1, viewModel.uiState.value.miracleState.currentStepIndex)
        assertEquals(listOf(SoundEffect.ITEM_COLLECTED), audioController.playedEffects)
    }

    @Test
    fun `onMiracleAnswerTapped with a wrong value does not advance and never fails`() {
        val viewModel = createViewModel()
        val step = viewModel.uiState.value.miracleState.currentStep!!
        val wrongValue = step.optionIds.map { it.toInt() }.first { it.toString() != step.correctOptionId }

        viewModel.onMiracleAnswerTapped(wrongValue)

        assertEquals(0, viewModel.uiState.value.miracleState.currentStepIndex)
    }

    @Test
    fun `answering every miracle problem correctly completes it`() {
        val viewModel = createViewModel()

        repeat(Feeding5000Content.MIRACLE_PROBLEM_COUNT) {
            val correctValue = viewModel.uiState.value.miracleState.currentStep!!.correctOptionId.toInt()
            viewModel.onMiracleAnswerTapped(correctValue)
        }

        assertTrue(viewModel.uiState.value.miracleState.isComplete)
    }

    @Test
    fun `generated miracle problems are always well-formed`() {
        repeat(100) {
            val problems = createViewModel().uiState.value.miracleProblems

            assertEquals(Feeding5000Content.MIRACLE_PROBLEM_COUNT, problems.size)
            problems.forEach { problem ->
                assertEquals(MathOperator.MULTIPLY, problem.operator)
                assertTrue("multiplicand not from the pool: $problem", problem.operandA in Feeding5000Content.miracleMultiplicandPool)
                assertTrue("multiplier should be single-digit: $problem", problem.operandB in 1..9)
                assertEquals("choices weren't 3 distinct values: $problem", 3, problem.choiceValues.toSet().size)
                assertTrue("correct value missing from choices: $problem", problem.correctValue in problem.choiceValues)
                assertTrue("a choice was negative: $problem", problem.choiceValues.all { it >= 0 })
            }
        }
    }

    // --- Serving the Crowd (gridmaze) ---

    @Test
    fun `initial serving state parses servingMapLayout into the correct dimensions and start position`() {
        val state = createViewModel().uiState.value.servingState

        assertEquals(Feeding5000Content.servingMapLayout.size, state.grid.size)
        assertEquals(Feeding5000Content.servingMapLayout[0].length, state.grid[0].size)
        assertEquals(GridPosition(0, 0), state.playerPosition)
        assertEquals(GridTileType.COLLECTIBLE, state.grid[2][3])
        assertEquals(GridTileType.WALL, state.grid[0][3])
        assertFalse(state.isComplete)
    }

    @Test
    fun `onServingDirectionPressed plays a sound on serving a group, not on a blocked move`() {
        val audioController = FakeAudioController()
        val viewModel = createViewModel(audioController = audioController)

        viewModel.onServingDirectionPressed(Direction.UP) // blocked: (0,0) is the top row, out of bounds
        assertTrue(audioController.playedEffects.isEmpty())

        Feeding5000Content.servingSolutionPath.take(4).forEach { viewModel.onServingDirectionPressed(it) } // reaches the first group at (4,0)

        assertEquals(listOf(SoundEffect.ITEM_COLLECTED), audioController.playedEffects)
    }

    @Test
    fun `walking the full solution path serves all 7 groups and plays a sound exactly 7 times`() {
        val audioController = FakeAudioController()
        val viewModel = createViewModel(audioController = audioController)

        Feeding5000Content.servingSolutionPath.forEach { viewModel.onServingDirectionPressed(it) }

        assertTrue(viewModel.uiState.value.servingState.isComplete)
        assertEquals(Feeding5000Content.SERVING_GROUP_COUNT, viewModel.uiState.value.servingState.collectedPositions.size)
        assertEquals(7, audioController.playedEffects.count { it == SoundEffect.ITEM_COLLECTED })
    }

    // --- Gathering the Leftovers (rhythmlane) ---

    @Test
    fun `onCatchingBasketMoved clamps to the 3 lanes, never a failure`() {
        val viewModel = createViewModel()

        repeat(5) { viewModel.onCatchingBasketMoved(-1) }
        assertEquals(0, viewModel.uiState.value.catchingBasketLane)

        repeat(5) { viewModel.onCatchingBasketMoved(1) }
        assertEquals(2, viewModel.uiState.value.catchingBasketLane)
    }

    @Test
    fun `moving the basket into a note's lane before its hit time auto-catches, never a failure`() {
        val viewModel = createViewModel()
        val note = Feeding5000Content.catchingChart.notes.first()

        moveCatchingBasketTo(viewModel, note.lane)
        viewModel.onCatchingTimeAdvanced(note.hitTimeMs)

        assertEquals(1, viewModel.uiState.value.catchingState.hits)
        assertFalse(viewModel.uiState.value.catchingState.isComplete)
    }

    @Test
    fun `advancing time to a beat while the basket is in the wrong lane does not register a catch`() {
        val viewModel = createViewModel()
        val note = Feeding5000Content.catchingChart.notes.first { it.lane != viewModel.uiState.value.catchingBasketLane }

        viewModel.onCatchingTimeAdvanced(note.hitTimeMs)

        assertEquals(0, viewModel.uiState.value.catchingState.hits)
    }

    @Test
    fun `completing all 12 required catches plays a sound exactly once`() {
        val audioController = FakeAudioController()
        val viewModel = createViewModel(audioController = audioController)

        completeCatchingChart(viewModel, Feeding5000Content.catchingChart, Feeding5000Content.CATCHING_REQUIRED_HITS)

        assertTrue(viewModel.uiState.value.catchingState.isComplete)
        assertEquals(12, Feeding5000Content.CATCHING_REQUIRED_HITS) // John 6:13's twelve baskets, exactly
        assertEquals(1, audioController.playedEffects.count { it == SoundEffect.ITEM_COLLECTED })
    }

    /**
     * Loops [chart] as many times as needed to reach [requiredHits] — for
     * the single-basket Catching mechanic: steer the basket into each
     * note's lane (reading the *actual* current lane back from state, not
     * assuming a starting value) before advancing time to that note's exact
     * hit moment, so the auto-catch in [Feeding5000ViewModel.onCatchingTimeAdvanced]
     * fires.
     */
    private fun completeCatchingChart(viewModel: Feeding5000ViewModel, chart: RhythmLaneChart, requiredHits: Int) {
        var hits = 0
        var loopIndex = 0L
        while (hits < requiredHits) {
            chart.notes.forEach { note ->
                if (hits < requiredHits) {
                    moveCatchingBasketTo(viewModel, note.lane)
                    viewModel.onCatchingTimeAdvanced(loopIndex * chart.loopDurationMs + note.hitTimeMs)
                    hits++
                }
            }
            loopIndex++
        }
    }

    private fun moveCatchingBasketTo(viewModel: Feeding5000ViewModel, targetLane: Int) {
        while (viewModel.uiState.value.catchingBasketLane != targetLane) {
            val delta = if (viewModel.uiState.value.catchingBasketLane < targetLane) 1 else -1
            viewModel.onCatchingBasketMoved(delta)
        }
    }

    // --- Choice / scene tracking / reward ---

    @Test
    fun `onChoiceSelected records the selected choice`() {
        val viewModel = createViewModel()

        viewModel.onChoiceSelected("share_fully")

        assertEquals("share_fully", viewModel.uiState.value.selectedChoiceId)
    }

    @Test
    fun `onSceneCompleted marks the scene as a completed activity for Feeding the 5,000`() = runTest {
        val repository = FakePlayerProfileRepository()
        val viewModel = createViewModel(repository = repository)

        viewModel.onSceneCompleted("intro")
        advanceUntilIdle()

        val progress = repository.current().progressByChapter.getValue(ChapterId.FEEDING_5000)
        assertTrue("intro" in progress.completedActivities)
        assertFalse(progress.completed)
    }

    @Test
    fun `onChapterFinished awards full stars and completes the chapter exactly once`() = runTest {
        val repository = FakePlayerProfileRepository()
        val viewModel = createViewModel(repository = repository)
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.reward)
        viewModel.onChapterFinished()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.reward)
        assertEquals(3, viewModel.uiState.value.reward?.stars)
        assertTrue(ChapterId.FEEDING_5000 in repository.current().completedChapters)
        assertEquals(3, repository.current().stars)

        viewModel.onChapterFinished()
        advanceUntilIdle()

        assertEquals(3, repository.current().stars)

        job.cancel()
    }
}
