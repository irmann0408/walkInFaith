package com.bibleadventures.game.puzzles.decisionpath

/**
 * Pure transition logic for the decision-path mini-game — no Compose/Android
 * dependency, directly unit-testable. Tapping the wrong option is never a
 * failure state: the step just re-prompts with INCORRECT feedback, fully
 * retriable, same as every other engine's "wrong tap" case in this codebase.
 */
object DecisionPathGame {

    fun onOptionTapped(state: DecisionPathGameState, optionId: String): DecisionPathGameState {
        val step = state.currentStep ?: return state

        return if (optionId == step.correctOptionId) {
            val nextIndex = state.currentStepIndex + 1
            val outcome = if (nextIndex >= state.steps.size) DecisionOutcome.COMPLETE else DecisionOutcome.CORRECT
            state.copy(currentStepIndex = nextIndex, lastOutcome = outcome)
        } else {
            state.copy(lastOutcome = DecisionOutcome.INCORRECT)
        }
    }
}
