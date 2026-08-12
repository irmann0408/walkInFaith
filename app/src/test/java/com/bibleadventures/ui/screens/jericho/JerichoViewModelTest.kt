package com.bibleadventures.ui.screens.jericho

import com.bibleadventures.FakeAudioController
import com.bibleadventures.FakePlayerProfileRepository
import com.bibleadventures.MainDispatcherRule
import com.bibleadventures.audio.SoundEffect
import com.bibleadventures.domain.model.ChapterId
import com.bibleadventures.game.puzzles.stackbuild.StackBuildOutcome
import com.bibleadventures.game.stories.JerichoContent
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
class JerichoViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun createViewModel(
        repository: FakePlayerProfileRepository = FakePlayerProfileRepository(),
        audioController: FakeAudioController = FakeAudioController(),
    ) = JerichoViewModel(ProgressionService(repository), repository, audioController)

    @Test
    fun `initial spies escape state is a solvable, not-yet-complete 3x3 puzzle`() {
        val state = createViewModel().uiState.value.spiesEscapeState

        assertEquals(3, state.size)
        assertEquals((0..8).toSet(), state.tiles.toSet())
    }

    @Test
    fun `initial camp state holds all 12 stones, none placed`() {
        val state = createViewModel().uiState.value.campState

        assertEquals(JerichoContent.campStoneIds.size, state.itemIds.size)
        assertTrue(state.placedOrder.isEmpty())
        assertFalse(state.isComplete)
    }

    @Test
    fun `initial camp stone values are 12 distinct numbers 1-99`() {
        val values = createViewModel().uiState.value.campStoneValues

        assertEquals(JerichoContent.campStoneIds.toSet(), values.keys)
        assertEquals(12, values.values.toSet().size)
        assertTrue(values.values.all { it in 1..99 })
    }

    @Test
    fun `camp state's required order is ascending by stone value`() {
        val viewModel = createViewModel()
        val state = viewModel.uiState.value

        val expectedOrder = state.campStoneValues.entries.sortedBy { it.value }.map { it.key }
        assertEquals(expectedOrder, state.campState.itemIds)
    }

    @Test
    fun `onChoiceSelected records the selected choice`() {
        val viewModel = createViewModel()

        viewModel.onChoiceSelected("trust_god")

        assertEquals("trust_god", viewModel.uiState.value.selectedChoiceId)
    }

    @Test
    fun `onSpiesEscapeTileTapped sliding a tile adjacent to the empty slot moves it`() {
        val viewModel = createViewModel()
        val before = viewModel.uiState.value.spiesEscapeState
        val emptyIndex = before.tiles.indexOf(0)
        val adjacentIndex = if (emptyIndex % 3 != 0) emptyIndex - 1 else emptyIndex + 1

        viewModel.onSpiesEscapeTileTapped(adjacentIndex)

        assertEquals(before.tiles[adjacentIndex], viewModel.uiState.value.spiesEscapeState.tiles[emptyIndex])
    }

    @Test
    fun `onCampStonePlaced with the lowest-value stone advances and plays a sound`() {
        val audioController = FakeAudioController()
        val viewModel = createViewModel(audioController = audioController)
        val lowestStoneId = viewModel.uiState.value.campState.nextExpectedId!!

        viewModel.onCampStonePlaced(lowestStoneId)

        assertEquals(listOf(lowestStoneId), viewModel.uiState.value.campState.placedOrder)
        assertEquals(listOf(SoundEffect.ITEM_COLLECTED), audioController.playedEffects)
    }

    @Test
    fun `onCampStonePlaced out of order does not advance and never fails`() {
        val viewModel = createViewModel()
        val state = viewModel.uiState.value.campState
        val outOfOrderStoneId = state.itemIds.last() // the highest value, never correct first

        viewModel.onCampStonePlaced(outOfOrderStoneId)

        assertTrue(viewModel.uiState.value.campState.placedOrder.isEmpty())
        assertEquals(StackBuildOutcome.WRONG_ORDER, viewModel.uiState.value.campState.lastOutcome)
    }

    @Test
    fun `stones must be placed in ascending order to complete the monument`() {
        val viewModel = createViewModel()
        val requiredOrder = viewModel.uiState.value.campState.itemIds

        requiredOrder.forEach { viewModel.onCampStonePlaced(it) }

        assertEquals(requiredOrder, viewModel.uiState.value.campState.placedOrder)
        assertTrue(viewModel.uiState.value.campState.isComplete)
    }

    @Test
    fun `onSixDayMarchTapped on the beat increases hits, never a failure`() {
        val viewModel = createViewModel()
        val note = JerichoContent.sixDayMarchChart.notes.first()

        viewModel.onSixDayMarchTapped(note.lane, note.hitTimeMs)

        assertEquals(1, viewModel.uiState.value.sixDayMarchState.hits)
        assertFalse(viewModel.uiState.value.sixDayMarchState.isComplete)
    }

    @Test
    fun `tapping all six notes across all three lanes completes the six-day march and plays a sound exactly once`() {
        val audioController = FakeAudioController()
        val viewModel = createViewModel(audioController = audioController)

        JerichoContent.sixDayMarchChart.notes.forEach { note ->
            viewModel.onSixDayMarchTapped(note.lane, note.hitTimeMs)
        }

        assertTrue(viewModel.uiState.value.sixDayMarchState.isComplete)
        assertEquals(1, audioController.playedEffects.count { it == SoundEffect.ITEM_COLLECTED })
    }

    @Test
    fun `tapping all seven notes across all three lanes completes the fast march`() {
        val viewModel = createViewModel()

        JerichoContent.fastMarchChart.notes.forEach { note ->
            viewModel.onFastMarchTapped(note.lane, note.hitTimeMs)
        }

        assertTrue(viewModel.uiState.value.fastMarchState.isComplete)
    }

    @Test
    fun `onShofarAnswerTapped with the correct value advances the step and plays a sound`() {
        val audioController = FakeAudioController()
        val viewModel = createViewModel(audioController = audioController)
        val correctValue = viewModel.uiState.value.shofarState.currentStep!!.correctOptionId.toInt()

        viewModel.onShofarAnswerTapped(correctValue)

        assertEquals(1, viewModel.uiState.value.shofarState.currentStepIndex)
        assertEquals(listOf(SoundEffect.ITEM_COLLECTED), audioController.playedEffects)
    }

    @Test
    fun `onShofarAnswerTapped with a wrong value does not advance and never fails`() {
        val viewModel = createViewModel()
        val step = viewModel.uiState.value.shofarState.currentStep!!
        val wrongValue = step.optionIds.map { it.toInt() }.first { it.toString() != step.correctOptionId }

        viewModel.onShofarAnswerTapped(wrongValue)

        assertEquals(0, viewModel.uiState.value.shofarState.currentStepIndex)
    }

    @Test
    fun `two wrong answers on the same problem replace it with a fresh one, still not advanced`() {
        val audioController = FakeAudioController()
        val viewModel = createViewModel(audioController = audioController)
        val step = viewModel.uiState.value.shofarState.currentStep!!
        val originalProblem = viewModel.uiState.value.shofarProblems.first { it.id == step.id }
        val wrongValue = step.optionIds.map { it.toInt() }.first { it.toString() != step.correctOptionId }

        viewModel.onShofarAnswerTapped(wrongValue) // 1st wrong: same problem, just re-prompts
        assertEquals(0, viewModel.uiState.value.shofarState.currentStepIndex)
        assertEquals(originalProblem, viewModel.uiState.value.shofarProblems.first { it.id == originalProblem.id })

        viewModel.onShofarAnswerTapped(wrongValue) // 2nd wrong: replaced with a fresh problem
        val afterSecondWrong = viewModel.uiState.value
        assertEquals(0, afterSecondWrong.shofarState.currentStepIndex)
        assertTrue(audioController.playedEffects.isEmpty())

        val newProblem = afterSecondWrong.shofarProblems.first { it.id == originalProblem.id }
        assertTrue("expected a different problem after 2 wrong answers", newProblem != originalProblem)
        assertEquals(
            newProblem.choiceValues.map { it.toString() }.toSet(),
            afterSecondWrong.shofarState.currentStep!!.optionIds.toSet(),
        )
    }

    @Test
    fun `after a problem is replaced, its new correct answer still advances the step`() {
        val viewModel = createViewModel()
        val step = viewModel.uiState.value.shofarState.currentStep!!
        val wrongValue = step.optionIds.map { it.toInt() }.first { it.toString() != step.correctOptionId }

        viewModel.onShofarAnswerTapped(wrongValue)
        viewModel.onShofarAnswerTapped(wrongValue) // replaces the problem

        val newCorrectValue = viewModel.uiState.value.shofarState.currentStep!!.correctOptionId.toInt()
        viewModel.onShofarAnswerTapped(newCorrectValue)

        assertEquals(1, viewModel.uiState.value.shofarState.currentStepIndex)
    }

    @Test
    fun `answering all problems correctly sounds every note`() {
        val viewModel = createViewModel()

        repeat(JerichoContent.shofarNoteIds.size) {
            val correctValue = viewModel.uiState.value.shofarState.currentStep!!.correctOptionId.toInt()
            viewModel.onShofarAnswerTapped(correctValue)
        }

        assertTrue(viewModel.uiState.value.shofarState.isComplete)
    }

    @Test
    fun `generated Blow the Shofar problems are always well-formed`() {
        // Constructed 100 times to exercise many random draws (Random.Default,
        // unseeded) — same "check invariants across many random instances, not
        // just one lucky run" discipline as SlidingPuzzleGameTest's shuffle tests.
        repeat(100) {
            val problems = createViewModel().uiState.value.shofarProblems

            assertEquals(JerichoContent.shofarNoteIds.size, problems.size)
            problems.forEach { problem ->
                assertTrue("operator should be multiply or divide: $problem", problem.operator == MathOperator.MULTIPLY || problem.operator == MathOperator.DIVIDE)
                // Kept easy for a 7+ audience: multiplicand/dividend is 1-2 digits, multiplier/divisor is always single-digit.
                assertTrue("operandA (multiplicand/dividend) out of range: $problem", problem.operandA in 1..99)
                assertTrue("operandB (multiplier/divisor) should be single-digit: $problem", problem.operandB in 1..9)
                if (problem.operator == MathOperator.DIVIDE) {
                    assertEquals("division should be exact: $problem", 0, problem.operandA % problem.operandB)
                }
                assertEquals("choices weren't 3 distinct values: $problem", 3, problem.choiceValues.toSet().size)
                assertTrue("correct value missing from choices: $problem", problem.correctValue in problem.choiceValues)
                assertTrue("a choice was negative: $problem", problem.choiceValues.all { it >= 0 })
            }
        }
    }

    @Test
    fun `onShoutTapped increases the shout count and never regresses`() {
        val viewModel = createViewModel()

        viewModel.onShoutTapped()

        assertEquals(1, viewModel.uiState.value.shoutTaps)
        assertFalse(viewModel.uiState.value.isShoutComplete)
    }

    @Test
    fun `enough shout taps completes it and plays the trumpet fanfare exactly once`() {
        val audioController = FakeAudioController()
        val viewModel = createViewModel(audioController = audioController)

        repeat(JerichoContent.SHOUT_REQUIRED_TAPS) { viewModel.onShoutTapped() }

        assertTrue(viewModel.uiState.value.isShoutComplete)
        assertEquals(listOf(SoundEffect.TRUMPET_FANFARE), audioController.playedEffects)

        // Once complete, further taps are a no-op — never double-plays the fanfare.
        viewModel.onShoutTapped()
        assertEquals(JerichoContent.SHOUT_REQUIRED_TAPS, viewModel.uiState.value.shoutTaps)
        assertEquals(listOf(SoundEffect.TRUMPET_FANFARE), audioController.playedEffects)
    }

    @Test
    fun `onSceneCompleted marks the scene as a completed activity for Jericho`() = runTest {
        val repository = FakePlayerProfileRepository()
        val viewModel = createViewModel(repository = repository)

        viewModel.onSceneCompleted("intro")
        advanceUntilIdle()

        val progress = repository.current().progressByChapter.getValue(ChapterId.JERICHO)
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
        assertTrue(ChapterId.JERICHO in repository.current().completedChapters)
        assertEquals(3, repository.current().stars)

        viewModel.onChapterFinished()
        advanceUntilIdle()

        assertEquals(3, repository.current().stars)

        job.cancel()
    }
}
