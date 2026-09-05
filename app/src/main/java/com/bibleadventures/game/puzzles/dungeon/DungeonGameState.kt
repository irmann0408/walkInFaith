package com.bibleadventures.game.puzzles.dungeon

import kotlin.math.sqrt

/**
 * A plain 2D point in cell-unit coordinates (1.0 = one map cell) — not
 * `androidx.compose.ui.geometry.Offset`, since this package stays pure
 * Kotlin with no Compose/Android dependency. Also used, unnormalized, as a
 * raw joystick reading passed into [DungeonGame.tick].
 */
data class Vector2(val x: Float, val y: Float) {
    fun distanceTo(other: Vector2): Float {
        val dx = x - other.x
        val dy = y - other.y
        return sqrt(dx * dx + dy * dy)
    }
}

/**
 * A bandit ambush — proximity-triggered, one-shot per [id] until resolved
 * (added to [DungeonGameState.resolvedTrapIds]). [patrolWaypoints] empty
 * (the default) means a stationary trap, exactly as before; 2+ waypoints
 * means [position] instead cycles between them at
 * [DungeonGame.BANDIT_PATROL_SPEED_CELLS_PER_SECOND], [patrolTargetIndex]
 * tracking which waypoint it's currently heading toward — the player's own
 * proximity trigger still fires the same way regardless of whether the trap
 * walked into range or the player did (see [DungeonGame]'s moving-target
 * `crossedInto` overload).
 */
data class DungeonTrap(
    val id: String,
    val position: Vector2,
    val patrolWaypoints: List<Vector2> = emptyList(),
    val patrolTargetIndex: Int = 0,
)

/** A medical-supply pickup — proximity-triggered, one-shot per [id]. */
data class DungeonSupply(val id: String, val position: Vector2)

/** Present only while a bandit fight is in progress; [DungeonGame.tick] is a full no-op whenever this is non-null, so the player can't walk away mid-fight. */
data class DungeonCombatState(val trapId: String, val banditToughnessRemaining: Int)

/**
 * Deliberately not a hard failure state even where combat is involved: an
 * [OUT_OF_SUPPLIES] fight never ends the run — [DungeonGame.onRetreat] lets
 * the player leave, keep their position, and come back once they've found
 * more supplies elsewhere. This is a confirmed, explicit exception to this
 * app's normal "no combat / no failure states" rule (see
 * `docs/PROJECT_STATUS.md`'s Good Samaritan dungeon addendum), kept as
 * gentle as the one other exception in this codebase (David & Goliath's
 * Connect Four): retry is always free and unlimited, nothing is ever lost
 * except the supplies already spent on that attempt.
 */
enum class DungeonOutcome {
    NONE,
    SUPPLY_COLLECTED,
    TRAP_ENTERED,
    CHECKPOINT_NEEDS_SUPPLIES,
    CHECKPOINT_ACTIVATED,
    GOAL_REACHED,
    BANDIT_HIT,
    THROW_MISSED,
    /** The Good Samaritan's own melee strike connected — see [com.bibleadventures.game.puzzles.dungeon.DungeonGame.onSamaritanAttack]. Distinct from [BANDIT_HIT] (the player's own throw connecting) so the two turns get their own feedback text. */
    SAMARITAN_HIT,
    /** The Good Samaritan's own attack missed — distinct from [BANDIT_ATTACK_MISSED], which is the *bandit's* counter-attack missing, not the Samaritan's. */
    SAMARITAN_ATTACK_MISSED,
    BANDIT_SCARED_OFF,
    OUT_OF_SUPPLIES,
    RETREATED,
    SUPPLY_STOLEN,
    BANDIT_ATTACK_MISSED,
}

/**
 * Row-major wall grid; [walls]' own dimensions define the map size.
 * Movement is continuous ([playerPosition] a float [Vector2] in cell units),
 * unlike [com.bibleadventures.game.puzzles.gridmaze.GridMazeState]'s
 * discrete [com.bibleadventures.game.puzzles.gridmaze.GridPosition] — this
 * engine is Good-Samaritan-only (an analog-joystick "mini dungeon"), so it
 * doesn't need that engine's cross-chapter generality, and hardcodes exactly
 * one completion shape: reach the checkpoint (with enough supplies in
 * reserve), then reach the goal.
 *
 * [lastOutcome] is sticky: [DungeonGame.tick] only overwrites it on a real
 * event (a pickup, a trap trigger, reaching the checkpoint/goal) — an
 * ordinary movement-only frame leaves it untouched, so a screen's
 * `liveRegion` announcement (which only fires on a value *change*) never
 * fires on every frame, only on real events, with no separate "was this an
 * event frame" mechanism needed.
 */
data class DungeonGameState(
    val walls: List<List<Boolean>>,
    val playerPosition: Vector2,
    val traps: List<DungeonTrap>,
    val supplies: List<DungeonSupply>,
    val checkpointPosition: Vector2,
    val goalPosition: Vector2,
    val supplyCount: Int = 0,
    val collectedSupplyIds: Set<String> = emptySet(),
    val resolvedTrapIds: Set<String> = emptySet(),
    val checkpointActivated: Boolean = false,
    val combat: DungeonCombatState? = null,
    val lastOutcome: DungeonOutcome = DungeonOutcome.NONE,
) {
    val rows: Int get() = walls.size
    val cols: Int get() = walls[0].size

    /** Live-checked from the current position (mirrors [com.bibleadventures.game.puzzles.gridmaze.GridMazeState.isComplete]'s own "standing on the goal tile" check), not a sticky flag — so wandering away from the goal before treating the traveler never prematurely completes anything. */
    val isComplete: Boolean
        get() = checkpointActivated && playerPosition.distanceTo(goalPosition) <= DungeonGame.TRIGGER_RADIUS
}
