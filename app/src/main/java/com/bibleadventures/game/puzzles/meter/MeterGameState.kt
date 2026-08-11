package com.bibleadventures.game.puzzles.meter

/**
 * Every precision level still contributes positive progress — never zero,
 * never negative. There is no failure state here, only how many taps it
 * takes to fill the meter; mistimed taps just mean a few more taps.
 */
enum class TapPrecision(val progressAmount: Int) {
    PERFECT(2),
    GOOD(1),
    EARLY_OR_LATE(1),
}

data class MeterGameState(
    val requiredProgress: Int,
    val progress: Int = 0,
    val lastPrecision: TapPrecision? = null,
) {
    val progressFraction: Float
        get() = (progress.toFloat() / requiredProgress).coerceIn(0f, 1f)

    val isComplete: Boolean
        get() = progress >= requiredProgress
}
