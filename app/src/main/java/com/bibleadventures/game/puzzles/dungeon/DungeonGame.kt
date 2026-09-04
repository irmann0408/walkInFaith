package com.bibleadventures.game.puzzles.dungeon

import kotlin.math.hypot
import kotlin.math.min
import kotlin.random.Random

/**
 * Pure real-time movement/collision/encounter logic for Good Samaritan's
 * "mini dungeon" — no Compose/Android dependency, no notion of wall-clock
 * time (the screen owns the frame clock and passes an explicit
 * [tick]-to-[tick] [deltaSeconds], the same contract
 * [com.bibleadventures.game.puzzles.rhythmlane.RhythmLaneGame] already
 * uses, so an instrumented test can drive this deterministically under a
 * frozen `mainClock`).
 */
object DungeonGame {

    /** Fraction of one cell the player occupies, for wall collision — a point-vs-expanded-wall check (the standard Minkowski-sum trick), not a true circle/AABB clip. */
    const val PLAYER_RADIUS = 0.32f

    /** Shared by every proximity check except bandit detection (supply/checkpoint/goal, and by [DungeonGameState.isComplete]) — a real touch, not a sighting. See [BANDIT_DETECTION_RADIUS] for how a bandit spots the player instead. */
    const val TRIGGER_RADIUS = 0.4f

    /** Below this, a joystick reading is dead-zone noise, not intent — mirrors [com.bibleadventures.game.puzzles.slingshot.SlingshotGame.MIN_PULL_DISTANCE]'s "too small a gesture changes nothing" precedent. */
    const val MIN_JOYSTICK_MAGNITUDE = 0.15f

    const val PLAYER_SPEED_CELLS_PER_SECOND = 2.0f

    /** "Easy fight only" — a small, fixed number of hits needed. */
    const val BANDIT_INITIAL_TOUGHNESS = 2

    /** The player's own throw — high, not guaranteed: still "easy," but a real throw can miss. */
    const val PLAYER_HIT_CHANCE = 0.85f

    /** The bandit's counter-attack — deliberately lower than [PLAYER_HIT_CHANCE], so the player is favored overall even though both sides now roll. */
    const val BANDIT_STEAL_CHANCE = 0.3f

    /** Awarded once a bandit is scared off — a guaranteed amount (not rolled), makes a fight a net-positive trade for supplies rather than just a toughness sink, which is what actually offsets the steal risk. */
    const val BANDIT_DEFEAT_SUPPLY_REWARD = 2

    /** A map supply pickup grants a random amount in this range (50/50) instead of a flat 1 — rolled off the [Random] passed to [tick]. */
    const val SUPPLY_PICKUP_MIN = 1
    const val SUPPLY_PICKUP_MAX = 2

    const val CHECKPOINT_SUPPLY_COST = 1

    /** How fast a patrolling bandit (see [DungeonTrap.patrolWaypoints]) cycles between its waypoints — deliberately slower than [PLAYER_SPEED_CELLS_PER_SECOND] so a careful, attentive player can route around one, matching the "avoid it if you can, fight it if you're caught" design. */
    const val BANDIT_PATROL_SPEED_CELLS_PER_SECOND = 1.0f

    /**
     * How far a bandit can spot the player from — wider than [TRIGGER_RADIUS]
     * (an actual bump-into), so "seen" is a real, distinct thing from
     * "caught": omnidirectional (no facing/line-of-sight check, deliberately
     * simple), and being spotted starts the fight immediately, same as a
     * direct collision always has. Faster than this radius divided by the
     * closing speed and a careful player can still peel away before contact
     * — [PLAYER_SPEED_CELLS_PER_SECOND] is double [BANDIT_PATROL_SPEED_CELLS_PER_SECOND],
     * so a spotted player who immediately retreats is never guaranteed to
     * be caught.
     */
    const val BANDIT_DETECTION_RADIUS = 1.2f

    /**
     * `.` path, `#` wall, `X` bandit trap (on a walkable cell, not a wall),
     * `M` medical-supply pickup, `T` traveler (checkpoint), `I` Inn (goal),
     * `S` start. Ids are deterministic from cell position (`"trap_${row}_$col"`
     * etc.), for stable identity across recompositions and in tests — which
     * [banditPatrols] relies on to attach a patrol route to the right `X`
     * by id; an `X` with no matching entry stays stationary (the original,
     * still-supported behavior).
     */
    fun fromLayout(layout: List<String>, banditPatrols: Map<String, List<Vector2>> = emptyMap()): DungeonGameState {
        val walls = layout.map { row -> row.map { it == '#' } }
        val traps = mutableListOf<DungeonTrap>()
        val supplies = mutableListOf<DungeonSupply>()
        var startPosition: Vector2? = null
        var checkpointPosition: Vector2? = null
        var goalPosition: Vector2? = null

        layout.forEachIndexed { row, line ->
            line.forEachIndexed { col, tileChar ->
                val center = Vector2(col + 0.5f, row + 0.5f)
                when (tileChar) {
                    'X' -> {
                        val id = "trap_${row}_$col"
                        traps += DungeonTrap(id = id, position = center, patrolWaypoints = banditPatrols[id] ?: emptyList())
                    }
                    'M' -> supplies += DungeonSupply(id = "supply_${row}_$col", position = center)
                    'S' -> startPosition = center
                    'T' -> checkpointPosition = center
                    'I' -> goalPosition = center
                }
            }
        }

        return DungeonGameState(
            walls = walls,
            playerPosition = requireNotNull(startPosition) { "Dungeon layout is missing a start ('S') cell" },
            traps = traps,
            supplies = supplies,
            checkpointPosition = requireNotNull(checkpointPosition) { "Dungeon layout is missing a checkpoint ('T') cell" },
            goalPosition = requireNotNull(goalPosition) { "Dungeon layout is missing a goal ('I') cell" },
        )
    }

    /**
     * One frame of movement + collision + proximity checks. [joystickInput]
     * is the raw (not pre-normalized) knob vector — its magnitude (0..1)
     * scales speed for real analog feel (a half-pushed stick moves at half
     * speed), its direction sets heading. [deltaSeconds] is real elapsed
     * wall-time since the *previous* [tick] call, computed by the screen
     * from consecutive `withFrameNanos` timestamps — this function never
     * touches a clock itself. A full no-op while a bandit fight is active
     * ([DungeonGameState.combat] non-null) or the state is already complete.
     * [random] rolls a supply pickup's amount ([SUPPLY_PICKUP_MIN]..[SUPPLY_PICKUP_MAX]);
     * defaults to [Random.Default] in real play, tests pass their own.
     */
    fun tick(state: DungeonGameState, joystickInput: Vector2, deltaSeconds: Float, random: Random = Random.Default): DungeonGameState {
        if (state.isComplete || state.combat != null) return state

        val oldPosition = state.playerPosition
        val magnitude = hypot(joystickInput.x, joystickInput.y)
        // Below the dead zone, the player themselves doesn't move — but a
        // patrolling bandit still does (see below): standing still is not a
        // way to pause the world, matching "if you're caught, you're caught"
        // even for a player who never touches the joystick.
        val newPosition = if (magnitude < MIN_JOYSTICK_MAGNITUDE) {
            oldPosition
        } else {
            val dirX = joystickInput.x / magnitude
            val dirY = joystickInput.y / magnitude
            val speed = PLAYER_SPEED_CELLS_PER_SECOND * min(magnitude, 1f)
            val candidateX = oldPosition.x + dirX * speed * deltaSeconds
            val candidateY = oldPosition.y + dirY * speed * deltaSeconds

            // Per-axis sweep: resolve X first (Y held at its old value), then Y
            // using the already-resolved X. This is what makes a diagonal push
            // into a wall slide along it instead of stopping dead, without
            // needing exact edge-clamping math — a blocked axis simply keeps
            // its old value for this frame, and at 3 cells/sec and 60fps that's
            // at most ~0.05 cells of "stopping short" of the wall, imperceptible
            // in play.
            val resolvedX = if (collidesWithWall(state.walls, candidateX, oldPosition.y)) oldPosition.x else candidateX
            val resolvedY = if (collidesWithWall(state.walls, resolvedX, candidateY)) oldPosition.y else candidateY
            Vector2(resolvedX, resolvedY)
        }

        // Unresolved bandits keep patrolling every tick, independent of
        // whether the player moved at all this frame — a stationary trap
        // (patrolWaypoints empty) is unaffected, matching every existing map.
        val oldTraps = state.traps
        val movedTraps = oldTraps.map { trap -> if (trap.id in state.resolvedTrapIds) trap else advancePatrol(trap, deltaSeconds) }

        val newlyCollectedSupply = state.supplies.firstOrNull { supply ->
            supply.id !in state.collectedSupplyIds && crossedInto(oldPosition, newPosition, supply.position)
        }
        if (newlyCollectedSupply != null) {
            val pickupAmount = if (random.nextFloat() < 0.5f) SUPPLY_PICKUP_MIN else SUPPLY_PICKUP_MAX
            return state.copy(
                playerPosition = newPosition,
                traps = movedTraps,
                supplyCount = state.supplyCount + pickupAmount,
                collectedSupplyIds = state.collectedSupplyIds + newlyCollectedSupply.id,
                lastOutcome = DungeonOutcome.SUPPLY_COLLECTED,
            )
        }

        // Checked against each trap's OLD and NEW position (not just the
        // player's) so a bandit patrolling into a player who isn't moving
        // — or into one who's moving away, but not fast enough — still
        // triggers the encounter, not just the reverse. Uses
        // BANDIT_DETECTION_RADIUS rather than TRIGGER_RADIUS: "seen" starts
        // the fight exactly like "caught" does, just from farther away.
        val newlyTriggeredTrap = oldTraps.indices.firstOrNull { i ->
            oldTraps[i].id !in state.resolvedTrapIds &&
                crossedInto(oldPosition, newPosition, oldTraps[i].position, movedTraps[i].position, BANDIT_DETECTION_RADIUS)
        }?.let { movedTraps[it] }
        if (newlyTriggeredTrap != null) {
            return state.copy(
                playerPosition = newPosition,
                traps = movedTraps,
                combat = DungeonCombatState(trapId = newlyTriggeredTrap.id, banditToughnessRemaining = BANDIT_INITIAL_TOUGHNESS),
                lastOutcome = DungeonOutcome.TRAP_ENTERED,
            )
        }

        if (!state.checkpointActivated && crossedInto(oldPosition, newPosition, state.checkpointPosition)) {
            return if (state.supplyCount >= CHECKPOINT_SUPPLY_COST) {
                state.copy(
                    playerPosition = newPosition,
                    traps = movedTraps,
                    supplyCount = state.supplyCount - CHECKPOINT_SUPPLY_COST,
                    checkpointActivated = true,
                    lastOutcome = DungeonOutcome.CHECKPOINT_ACTIVATED,
                )
            } else {
                state.copy(playerPosition = newPosition, traps = movedTraps, lastOutcome = DungeonOutcome.CHECKPOINT_NEEDS_SUPPLIES)
            }
        }

        if (state.checkpointActivated && crossedInto(oldPosition, newPosition, state.goalPosition)) {
            return state.copy(playerPosition = newPosition, traps = movedTraps, lastOutcome = DungeonOutcome.GOAL_REACHED)
        }

        return state.copy(playerPosition = newPosition, traps = movedTraps)
    }

    /**
     * Advances a patrolling bandit one step along [DungeonTrap.patrolWaypoints]
     * at [BANDIT_PATROL_SPEED_CELLS_PER_SECOND] — a no-op for a stationary
     * trap (empty waypoints). Snaps exactly onto a waypoint once within one
     * step's distance of it rather than overshooting, then advances to the
     * next one (wrapping back to the first after the last), so the patrol
     * is a smooth, endless loop.
     */
    private fun advancePatrol(trap: DungeonTrap, deltaSeconds: Float): DungeonTrap {
        if (trap.patrolWaypoints.isEmpty()) return trap
        val target = trap.patrolWaypoints[trap.patrolTargetIndex % trap.patrolWaypoints.size]
        val dx = target.x - trap.position.x
        val dy = target.y - trap.position.y
        val distance = hypot(dx, dy)
        val step = BANDIT_PATROL_SPEED_CELLS_PER_SECOND * deltaSeconds
        return if (distance <= step) {
            trap.copy(position = target, patrolTargetIndex = trap.patrolTargetIndex + 1)
        } else {
            trap.copy(position = Vector2(trap.position.x + dx / distance * step, trap.position.y + dy / distance * step))
        }
    }

    /**
     * The thrown supply is spent either way (hit or miss — you don't get an
     * unthrown supply back), rolled against [PLAYER_HIT_CHANCE]. A hit
     * reduces the active bandit's toughness by 1; reaching 0 permanently
     * resolves that trap id, ends combat, and awards
     * [BANDIT_DEFEAT_SUPPLY_REWARD] bonus supplies. A no-op (besides
     * reporting [DungeonOutcome.OUT_OF_SUPPLIES]) with 0 supplies, and a
     * full no-op if there's no active combat. [random] defaults to
     * [Random.Default] in real play; callers needing deterministic behavior
     * (tests) pass their own.
     */
    fun onSupplyThrown(state: DungeonGameState, random: Random = Random.Default): DungeonGameState {
        val combat = state.combat ?: return state
        if (state.supplyCount <= 0) return state.copy(lastOutcome = DungeonOutcome.OUT_OF_SUPPLIES)

        val newSupplyCount = state.supplyCount - 1
        if (random.nextFloat() >= PLAYER_HIT_CHANCE) {
            return state.copy(supplyCount = newSupplyCount, lastOutcome = DungeonOutcome.THROW_MISSED)
        }

        val remainingToughness = combat.banditToughnessRemaining - 1
        return if (remainingToughness <= 0) {
            state.copy(
                supplyCount = newSupplyCount + BANDIT_DEFEAT_SUPPLY_REWARD,
                combat = null,
                resolvedTrapIds = state.resolvedTrapIds + combat.trapId,
                lastOutcome = DungeonOutcome.BANDIT_SCARED_OFF,
            )
        } else {
            state.copy(
                supplyCount = newSupplyCount,
                combat = combat.copy(banditToughnessRemaining = remainingToughness),
                lastOutcome = DungeonOutcome.BANDIT_HIT,
            )
        }
    }

    /**
     * The bandit's own melee counter-attack: never hurts the player
     * (no HP system, no failure state) — a successful roll (against
     * [BANDIT_STEAL_CHANCE]) just steals 1 supply. A no-op besides
     * reporting [DungeonOutcome.BANDIT_ATTACK_MISSED] with 0 supplies
     * (nothing to steal) or no active combat. [random] defaults to
     * [Random.Default] in real play; callers needing deterministic
     * behavior (tests) pass their own.
     */
    fun onBanditAttack(state: DungeonGameState, random: Random = Random.Default): DungeonGameState {
        if (state.combat == null) return state
        if (state.supplyCount <= 0) return state.copy(lastOutcome = DungeonOutcome.BANDIT_ATTACK_MISSED)

        return if (random.nextFloat() < BANDIT_STEAL_CHANCE) {
            state.copy(supplyCount = state.supplyCount - 1, lastOutcome = DungeonOutcome.SUPPLY_STOLEN)
        } else {
            state.copy(lastOutcome = DungeonOutcome.BANDIT_ATTACK_MISSED)
        }
    }

    /**
     * Ends the active encounter WITHOUT resolving it: the trap is not added
     * to [DungeonGameState.resolvedTrapIds], so it's exactly as live as
     * before — player position and supply count are untouched, no
     * punishment beyond the supplies already spent this attempt. A no-op
     * if there's no active combat.
     */
    fun onRetreat(state: DungeonGameState): DungeonGameState {
        if (state.combat == null) return state
        return state.copy(combat = null, lastOutcome = DungeonOutcome.RETREATED)
    }

    /** True only on the frame the player's position crosses from outside [radius] of [target] to inside it — not every frame spent standing inside, so idling near an unresolved trigger doesn't spam outcomes, and returning from a fight (during which position never moved) doesn't instantly re-trigger the same trap. Defaults to [TRIGGER_RADIUS]; a stationary-target convenience wrapper over the two-target overload below. */
    private fun crossedInto(oldPosition: Vector2, newPosition: Vector2, target: Vector2, radius: Float = TRIGGER_RADIUS): Boolean =
        crossedInto(oldPosition, newPosition, target, target, radius)

    /**
     * As above, but for a target that can itself move between frames (a
     * patrolling bandit) — true the moment the gap between player and
     * target crosses from outside [radius] to inside it, whichever of the
     * two did the moving. This is what lets a bandit walking into a
     * *stationary* player still register a catch: with a single fixed
     * target, "old distance > R, new distance <= R" can never be true for
     * an unmoving player, since old and new position are identical.
     * Defaults to [TRIGGER_RADIUS]; the trap-detection call site in [tick]
     * passes [BANDIT_DETECTION_RADIUS] instead, so "seen from a distance"
     * uses a wider radius than every other proximity check in this file.
     */
    private fun crossedInto(oldPosition: Vector2, newPosition: Vector2, oldTarget: Vector2, newTarget: Vector2, radius: Float = TRIGGER_RADIUS): Boolean =
        oldPosition.distanceTo(oldTarget) > radius && newPosition.distanceTo(newTarget) <= radius

    /** Point-vs-every-wall-cell overlap, each wall cell's unit square expanded by [PLAYER_RADIUS] on all sides; map edges count as walls too. Simple O(rows*cols) scan — fine at this map's 10x10 scale, even at 60fps. */
    private fun collidesWithWall(walls: List<List<Boolean>>, x: Float, y: Float): Boolean {
        val rows = walls.size
        val cols = walls[0].size
        if (x - PLAYER_RADIUS < 0f || x + PLAYER_RADIUS > cols || y - PLAYER_RADIUS < 0f || y + PLAYER_RADIUS > rows) return true

        for (row in 0 until rows) {
            for (col in 0 until cols) {
                if (!walls[row][col]) continue
                val withinX = x >= col - PLAYER_RADIUS && x <= col + 1 + PLAYER_RADIUS
                val withinY = y >= row - PLAYER_RADIUS && y <= row + 1 + PLAYER_RADIUS
                if (withinX && withinY) return true
            }
        }
        return false
    }
}
