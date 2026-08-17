package com.bibleadventures.game.puzzles.groupfill

/**
 * Pure transition logic — no Compose/Android dependency. An overshoot is
 * never a failure state: the family just doesn't fit *this* circle, the
 * player is free to try it in another one or bring a different family
 * here instead, no progress is ever lost.
 */
object GroupFillGame {
    /**
     * Whether dropping [familyId] into [circleIndex] would actually be
     * accepted by [onFamilyDropped] — lets the screen check *before*
     * committing to its optimistic snap-into-circle drop animation, instead
     * of discovering the rejection only after the animation already played
     * (which left the dragged tile stranded near the circle with no reset).
     */
    fun canAccept(state: GroupFillGameState, familyId: String, circleIndex: Int): Boolean {
        if (state.isComplete || familyId in state.placedFamilyIds) return false
        val family = state.families.firstOrNull { it.id == familyId } ?: return false

        val wouldBeSum = state.circleSum(circleIndex) + family.headcount
        if (wouldBeSum > state.circleTargets[circleIndex]) return false

        val nextContents = state.circleContents.toMutableList().also {
            it[circleIndex] = it[circleIndex] + familyId
        }
        return state.copy(circleContents = nextContents).isEveryCircleStillReachable()
    }

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

        // A drop that fits *this* circle can still strand another one — e.g. the
        // last family left is a 20 but some other circle only has a 15-wide gap
        // left. Reject before that dead end happens, same as an overshoot: no
        // state change, just different feedback.
        if (!next.isEveryCircleStillReachable()) {
            return state.copy(lastOutcome = GroupFillOutcome.REJECTED_UNREACHABLE)
        }

        val outcome = when {
            next.isComplete -> GroupFillOutcome.ALL_COMPLETE
            next.isCircleComplete(circleIndex) -> GroupFillOutcome.CIRCLE_COMPLETE
            else -> GroupFillOutcome.ADDED
        }
        return next.copy(lastOutcome = outcome)
    }
}
