package com.bibleadventures.game.puzzles.groupfill

/**
 * Pure transition logic — no Compose/Android dependency. An overshoot is
 * never a failure state: the family just doesn't fit *this* circle, the
 * player is free to try it in another one or bring a different family
 * here instead, no progress is ever lost.
 */
object GroupFillGame {
    fun onFamilyDropped(state: GroupFillGameState, familyId: String, circleIndex: Int): GroupFillGameState {
        if (state.isComplete || familyId in state.placedFamilyIds) return state
        val family = state.families.firstOrNull { it.id == familyId } ?: return state

        val wouldBeSum = state.circleSum(circleIndex) + family.headcount
        if (wouldBeSum > state.circleTargets[circleIndex]) {
            return state.copy(lastOutcome = GroupFillOutcome.REJECTED_OVERSHOOT)
        }

        val nextContents = state.circleContents.toMutableList().also {
            it[circleIndex] = it[circleIndex] + familyId
        }
        val next = state.copy(circleContents = nextContents)
        val outcome = when {
            next.isComplete -> GroupFillOutcome.ALL_COMPLETE
            next.isCircleComplete(circleIndex) -> GroupFillOutcome.CIRCLE_COMPLETE
            else -> GroupFillOutcome.ADDED
        }
        return next.copy(lastOutcome = outcome)
    }
}
