package com.bibleadventures.game.puzzles.slingshot

import kotlin.math.abs

/**
 * Pure hit-test logic — no Compose/Android dependency, no notion of time. The
 * UI layer owns the drag gesture, the trajectory rendering, and the moving
 * target's animation; it calls this once it knows where the player aimed and
 * where the target's mark was at the moment of release (mirrors DragSortGame's
 * "UI resolves the geometry, engine judges the outcome" split).
 */
object SlingshotGame {

    /** Fractional (0..1 track) half-width tolerance — generous for young players. */
    const val HIT_TOLERANCE = 0.12f

    fun onStoneReleased(state: SlingshotGameState, aimedPosition: Float, markPosition: Float): SlingshotGameState {
        if (state.isHit) return state

        val hit = abs(aimedPosition - markPosition) <= HIT_TOLERANCE
        return state.copy(
            lastOutcome = if (hit) SlingshotOutcome.HIT else SlingshotOutcome.MISS,
            isHit = state.isHit || hit,
        )
    }
}
