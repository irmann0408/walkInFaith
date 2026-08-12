package com.bibleadventures.game.puzzles.decisionpath

/**
 * Pure transition logic for the decision-path mini-game — no Compose/Android
 * dependency, directly unit-testable. Tapping the wrong option is never a
 * failure state: the step just re-prompts with INCORRECT feedback, fully
 * retriable, same as every other engine's "wrong tap" case in this codebase.
 */
object DecisionPathGame {

    /** After this many wrong taps on the same step, the caller should replace it via [replaceCurrentStep] rather than let the last option be picked by elimination. */
    const val WRONG_ATTEMPTS_BEFORE_NEW_STEP = 2

    fun onOptionTapped(state: DecisionPathGameState, optionId: String): DecisionPathGameState {
        val step = state.currentStep ?: return state

        return if (optionId == step.correctOptionId) {
            val nextIndex = state.currentStepIndex + 1
            val outcome = if (nextIndex >= state.steps.size) DecisionOutcome.COMPLETE else DecisionOutcome.CORRECT
            state.copy(currentStepIndex = nextIndex, lastOutcome = outcome, wrongAttemptsOnCurrentStep = 0)
        } else {
            state.copy(lastOutcome = DecisionOutcome.INCORRECT, wrongAttemptsOnCurrentStep = state.wrongAttemptsOnCurrentStep + 1)
        }
    }

    /**
     * Swaps the current step for [newStep] and clears the wrong-attempt
     * counter — call once [DecisionPathGameState.wrongAttemptsOnCurrentStep]
     * reaches [WRONG_ATTEMPTS_BEFORE_NEW_STEP], so a 3-choice question can no
     * longer be solved by elimination after two wrong taps. This engine has
     * no content of its own to generate a replacement step from (see this
     * state's own doc comment), so the caller supplies [newStep].
     */
    fun replaceCurrentStep(state: DecisionPathGameState, newStep: DecisionStep): DecisionPathGameState {
        if (state.isComplete) return state
        val steps = state.steps.toMutableList().apply { set(state.currentStepIndex, newStep) }
        return state.copy(steps = steps, wrongAttemptsOnCurrentStep = 0)
    }
}
