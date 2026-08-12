package com.bibleadventures.game.puzzles.decisionpath

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DecisionPathGameTest {

    private val steps = listOf(
        DecisionStep("step_1", correctOptionId = "march_quietly", optionIds = listOf("march_quietly", "attack_gate")),
        DecisionStep("step_2", correctOptionId = "stay_silent", optionIds = listOf("stay_silent", "shout_now")),
        DecisionStep("step_3", correctOptionId = "blow_horns_and_shout", optionIds = listOf("blow_horns_and_shout", "stay_silent")),
    )

    @Test
    fun `tapping the wrong option reports INCORRECT and never advances`() {
        val state = DecisionPathGame.onOptionTapped(DecisionPathGameState(steps), optionId = "attack_gate")

        assertEquals(DecisionOutcome.INCORRECT, state.lastOutcome)
        assertEquals(0, state.currentStepIndex)
        assertFalse(state.isComplete)
    }

    @Test
    fun `tapping the correct option reports CORRECT and advances to the next step`() {
        val state = DecisionPathGame.onOptionTapped(DecisionPathGameState(steps), optionId = "march_quietly")

        assertEquals(DecisionOutcome.CORRECT, state.lastOutcome)
        assertEquals(1, state.currentStepIndex)
        assertEquals(steps[1], state.currentStep)
    }

    @Test
    fun `a wrong pick does not block a later correct one`() {
        var state = DecisionPathGameState(steps)
        state = DecisionPathGame.onOptionTapped(state, optionId = "attack_gate") // wrong, INCORRECT
        state = DecisionPathGame.onOptionTapped(state, optionId = "march_quietly") // correct

        assertEquals(DecisionOutcome.CORRECT, state.lastOutcome)
        assertEquals(1, state.currentStepIndex)
    }

    @Test
    fun `the same option id can be correct at one step and wrong at another`() {
        var state = DecisionPathGameState(steps)
        state = DecisionPathGame.onOptionTapped(state, optionId = "march_quietly") // step 1 correct
        state = DecisionPathGame.onOptionTapped(state, optionId = "stay_silent") // step 2 correct

        // "stay_silent" was correct at step 2; at step 3 it's the wrong option.
        state = DecisionPathGame.onOptionTapped(state, optionId = "stay_silent")
        assertEquals(DecisionOutcome.INCORRECT, state.lastOutcome)
        assertEquals(2, state.currentStepIndex)
    }

    @Test
    fun `tapping through every step completes the path`() {
        var state = DecisionPathGameState(steps)
        state = DecisionPathGame.onOptionTapped(state, optionId = "march_quietly")
        state = DecisionPathGame.onOptionTapped(state, optionId = "stay_silent")
        state = DecisionPathGame.onOptionTapped(state, optionId = "blow_horns_and_shout")

        assertEquals(DecisionOutcome.COMPLETE, state.lastOutcome)
        assertTrue(state.isComplete)
        assertEquals(null, state.currentStep)
    }

    @Test
    fun `once complete, further taps are a no-op`() {
        var state = DecisionPathGameState(listOf(DecisionStep("only", "yes", listOf("yes", "no"))))
        state = DecisionPathGame.onOptionTapped(state, optionId = "yes")
        val afterComplete = state

        state = DecisionPathGame.onOptionTapped(state, optionId = "yes")

        assertEquals(afterComplete, state)
    }

    @Test
    fun `wrongAttemptsOnCurrentStep increments per wrong tap and resets on advance`() {
        var state = DecisionPathGameState(steps)
        state = DecisionPathGame.onOptionTapped(state, optionId = "attack_gate") // wrong
        assertEquals(1, state.wrongAttemptsOnCurrentStep)

        state = DecisionPathGame.onOptionTapped(state, optionId = "attack_gate") // wrong again
        assertEquals(2, state.wrongAttemptsOnCurrentStep)

        state = DecisionPathGame.onOptionTapped(state, optionId = "march_quietly") // correct
        assertEquals(0, state.wrongAttemptsOnCurrentStep)
    }

    @Test
    fun `replaceCurrentStep swaps only the current step and clears the wrong-attempt count`() {
        var state = DecisionPathGameState(steps)
        state = DecisionPathGame.onOptionTapped(state, optionId = "attack_gate")
        state = DecisionPathGame.onOptionTapped(state, optionId = "attack_gate")
        assertEquals(2, state.wrongAttemptsOnCurrentStep)

        val newStep = DecisionStep("step_1_retry", correctOptionId = "hide_quietly", optionIds = listOf("hide_quietly", "run_away"))
        state = DecisionPathGame.replaceCurrentStep(state, newStep)

        assertEquals(newStep, state.currentStep)
        assertEquals(0, state.wrongAttemptsOnCurrentStep)
        assertEquals(0, state.currentStepIndex)
        assertEquals(steps[1], state.steps[1]) // untouched
        assertEquals(steps[2], state.steps[2]) // untouched
    }

    @Test
    fun `replaceCurrentStep on a completed path is a no-op`() {
        var state = DecisionPathGameState(listOf(DecisionStep("only", "yes", listOf("yes", "no"))))
        state = DecisionPathGame.onOptionTapped(state, optionId = "yes")
        assertTrue(state.isComplete)

        val afterComplete = state
        state = DecisionPathGame.replaceCurrentStep(state, DecisionStep("other", "no", listOf("yes", "no")))

        assertEquals(afterComplete, state)
    }
}
