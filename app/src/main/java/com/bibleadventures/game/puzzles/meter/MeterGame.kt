package com.bibleadventures.game.puzzles.meter

/**
 * Mirrors [com.bibleadventures.game.puzzles.slingshot.SlingshotGame]'s
 * split: the screen owns the live, real-time beat animation and classifies
 * each tap's timing; this engine only turns that classification into
 * meter progress. Every [TapPrecision] value contributes a positive amount,
 * so the meter can only fill faster or slower depending on timing — never
 * reset, never run out, no failure state to design around.
 */
object MeterGame {

    fun onTapped(state: MeterGameState, precision: TapPrecision): MeterGameState {
        if (state.isComplete) return state
        return state.copy(
            progress = (state.progress + precision.progressAmount).coerceAtMost(state.requiredProgress),
            lastPrecision = precision,
        )
    }
}
