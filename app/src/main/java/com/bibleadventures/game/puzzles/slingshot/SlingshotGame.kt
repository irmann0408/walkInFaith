package com.bibleadventures.game.puzzles.slingshot

import kotlin.math.abs

/**
 * Pure hit-test logic — no Compose/Android dependency, no notion of time. The
 * UI layer owns the drag gesture, the trajectory rendering, and the moving
 * target's animation; it calls this once it knows where the player aimed,
 * where the target's mark was at the moment of release, and where the
 * shield's own (fixed) fractional bounds are on the same 0..1 track (mirrors
 * DragSortGame's "UI resolves the geometry, engine judges the outcome"
 * split).
 */
object SlingshotGame {

    /** Fractional (0..1 track) half-width tolerance — generous for young players. */
    const val HIT_TOLERANCE = 0.12f

    /**
     * A hit requires both: the release was aimed close enough to the mark,
     * AND the mark itself was within the shield's bounds at that moment —
     * the mark spends much of its swing outside the shield entirely, so
     * timing the release to when it's actually over the shield is the real
     * challenge, not just matching its position.
     */
    fun onStoneReleased(
        state: SlingshotGameState,
        aimedPosition: Float,
        markPosition: Float,
        shieldMinFraction: Float,
        shieldMaxFraction: Float,
    ): SlingshotGameState {
        if (state.isComplete) return state

        val aimMatchesMark = abs(aimedPosition - markPosition) <= HIT_TOLERANCE
        val markWithinShield = markPosition in shieldMinFraction..shieldMaxFraction
        val hit = aimMatchesMark && markWithinShield
        return state.copy(
            hits = if (hit) (state.hits + 1).coerceAtMost(state.requiredHits) else state.hits,
            lastOutcome = if (hit) SlingshotOutcome.HIT else SlingshotOutcome.MISS,
        )
    }
}
