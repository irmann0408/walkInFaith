package com.bibleadventures.game.puzzles.decisionpath

/** Never FAILED — an incorrect option just re-prompts the same step, all prior progress kept. */
enum class DecisionOutcome { NONE, CORRECT, INCORRECT, COMPLETE }

/** One decision point: [correctOptionId] must be one of [optionIds]. */
data class DecisionStep(val id: String, val correctOptionId: String, val optionIds: List<String>)

/**
 * A fixed sequence of decision points, each offering a couple of options
 * with exactly one correct answer per step. No position/visual data — this
 * engine is pure and chapter-agnostic, like
 * [com.bibleadventures.game.puzzles.gridmaze.GridMazeState]; step content
 * and framing live in each chapter's own content object under `game/stories`.
 */
data class DecisionPathGameState(
    val steps: List<DecisionStep>,
    val currentStepIndex: Int = 0,
    val lastOutcome: DecisionOutcome = DecisionOutcome.NONE,
    /** Wrong taps on the current step only — reset to 0 by any advance or by [DecisionPathGame.replaceCurrentStep]. */
    val wrongAttemptsOnCurrentStep: Int = 0,
) {
    val isComplete: Boolean get() = currentStepIndex >= steps.size
    val currentStep: DecisionStep? get() = steps.getOrNull(currentStepIndex)
}
