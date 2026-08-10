package com.bibleadventures.game.puzzles.dodge

enum class DodgeLane { LEFT, RIGHT }

/** Never FAILED — a wrong step just prompts another try (spec section 9). */
enum class DodgeOutcome { NONE, DODGED, TRY_AGAIN }

data class DodgeBeat(val id: String, val hazardLane: DodgeLane)

/**
 * A fixed sequence of discrete, self-paced obstacle "beats." Each beat shows
 * a hazard resting in one lane; the player steps to the other lane whenever
 * ready — nothing is time-gated, so there's no reflex/timing pressure.
 */
data class DodgeGameState(
    val beats: List<DodgeBeat>,
    val currentBeatIndex: Int = 0,
    val lastOutcome: DodgeOutcome = DodgeOutcome.NONE,
) {
    val isComplete: Boolean get() = currentBeatIndex >= beats.size
    val currentBeat: DodgeBeat? get() = beats.getOrNull(currentBeatIndex)
}
