package com.bibleadventures.game.puzzles.slingshot

import kotlin.math.sqrt

/**
 * Pure hit-test logic — no Compose/Android dependency, no notion of time.
 * The UI layer owns the drag gesture, the projectile's flight animation,
 * and the rat's own row-stepping movement; it calls [onStoneReleased] once
 * it knows the sling's fixed [anchor] point, how far and which direction
 * the player pulled the stone back ([pull], relative to the anchor), and
 * where the rat's own (moving) position was at the moment of release.
 *
 * Mirrors a real sling: the stone launches in the direction *opposite*
 * [pull] (pull southwest, the stone flies northeast), so the hit-test
 * checks whether that launch ray passes close enough to the rat, not
 * whether the release position matches it directly. [wouldHit] exposes
 * that same check without committing it, so the screen can predict the
 * outcome up front — e.g. to aim a flight animation's endpoint exactly at
 * the rat when it's about to land, rather than deciding that only after
 * the animation already started.
 */
object SlingshotGame {

    /** Fractional (0..1 track) tolerance, as a perpendicular distance from the launch ray — generous for young players. */
    const val HIT_TOLERANCE = 0.12f

    /** A pull shorter than this is treated as no real shot at all — not a miss, so it changes nothing. */
    const val MIN_PULL_DISTANCE = 0.03f

    /** Whether releasing right now, with this [pull], would hit the rat — same math [onStoneReleased] commits, without mutating anything. */
    fun wouldHit(anchor: Vector2, pull: Vector2, ratPosition: Vector2): Boolean {
        val pullDistance = sqrt(pull.x * pull.x + pull.y * pull.y)
        if (pullDistance < MIN_PULL_DISTANCE) return false

        // The launch direction is the pull, reversed and normalized — pull
        // southwest, the stone flies northeast.
        val direction = Vector2(-pull.x / pullDistance, -pull.y / pullDistance)

        val toRat = Vector2(ratPosition.x - anchor.x, ratPosition.y - anchor.y)
        val forwardDistance = toRat.x * direction.x + toRat.y * direction.y
        if (forwardDistance <= 0f) return false // the rat is behind the launch direction entirely

        val perpendicularX = toRat.x - direction.x * forwardDistance
        val perpendicularY = toRat.y - direction.y * forwardDistance
        return sqrt(perpendicularX * perpendicularX + perpendicularY * perpendicularY) <= HIT_TOLERANCE
    }

    /** A miss never ends the rat's turn — it keeps falling, more throws are always allowed. An escaped rat (see [onRatEscaped]) doesn't count toward or against [SlingshotGameState.requiredHits] either. */
    fun onStoneReleased(state: SlingshotGameState, anchor: Vector2, pull: Vector2, ratPosition: Vector2): SlingshotGameState {
        if (state.isComplete) return state

        val pullDistance = sqrt(pull.x * pull.x + pull.y * pull.y)
        if (pullDistance < MIN_PULL_DISTANCE) return state

        return if (wouldHit(anchor, pull, ratPosition)) {
            state.copy(hits = state.hits + 1, ratsSpawned = state.ratsSpawned + 1, lastOutcome = SlingshotOutcome.HIT)
        } else {
            state.copy(lastOutcome = SlingshotOutcome.MISS)
        }
    }

    /** Called by the screen once the current rat's fall duration elapses without being hit — free practice, never counted. */
    fun onRatEscaped(state: SlingshotGameState): SlingshotGameState {
        if (state.isComplete) return state
        return state.copy(ratsSpawned = state.ratsSpawned + 1, lastOutcome = SlingshotOutcome.ESCAPED)
    }
}
