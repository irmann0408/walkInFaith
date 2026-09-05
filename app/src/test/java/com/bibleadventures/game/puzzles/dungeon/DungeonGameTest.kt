package com.bibleadventures.game.puzzles.dungeon

import com.bibleadventures.game.stories.GoodSamaritanContent
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DungeonGameTest {

    /** A [Random] whose `nextFloat()` always returns [value] — deterministic control over [DungeonGame.onSupplyThrown]/[DungeonGame.onBanditAttack]'s rolls, rather than relying on a seeded sequence (fragile to reason about across multiple calls in one test). */
    private fun fixedRandom(value: Float): Random = object : Random() {
        override fun nextBits(bitCount: Int): Int = 0
        override fun nextFloat(): Float = value
    }

    /** Comfortably below any hit/steal chance used in this file — a guaranteed success roll. */
    private val guaranteedSuccess = fixedRandom(0f)

    /** Comfortably above any hit/steal chance used in this file — a guaranteed failure roll. */
    private val guaranteedFailure = fixedRandom(0.999f)

    // A tiny test map, mirroring GridMazeGameTest's own small hand-built
    // fixture rather than the real 10x10 production map for most tests:
    //   S . M
    //   . # .
    //   X T .
    //   . . I
    private val testLayout = listOf(
        "S.M",
        ".#.",
        "XT.",
        "..I",
    )

    private fun initialState() = DungeonGame.fromLayout(testLayout)

    /**
     * Steps [DungeonGame.tick] one small frame at a time (mirroring real
     * ~60fps play, unlike one oversized jump) until a real event fires —
     * avoids hand-computing exact single-tick distances, which risks
     * overshooting a trigger radius or even a map edge in one jump.
     * Compares against [initial]'s own [DungeonOutcome], not a hardcoded
     * [DungeonOutcome.NONE]: [DungeonOutcome] is sticky (an ordinary
     * movement frame never resets it), so calling this a second time on a
     * state whose outcome is already non-[DungeonOutcome.NONE] from an
     * earlier event must still recognize only a *new* event, not the
     * leftover old one.
     */
    private fun tickUntilEvent(
        initial: DungeonGameState,
        direction: Vector2,
        maxSteps: Int = 300,
        deltaSeconds: Float = 1f / 60f,
        random: Random = Random.Default,
    ): DungeonGameState {
        var state = initial
        val startingOutcome = initial.lastOutcome
        repeat(maxSteps) {
            state = DungeonGame.tick(state, direction, deltaSeconds, random)
            if (state.lastOutcome != startingOutcome || state.combat != null) return state
        }
        return state
    }

    @Test
    fun `fromLayout parses walls, supplies, trap, checkpoint, goal, and the start position`() {
        val state = initialState()

        assertEquals(4, state.rows)
        assertEquals(3, state.cols)
        assertEquals(Vector2(0.5f, 0.5f), state.playerPosition)
        assertTrue(state.walls[1][1])
        assertFalse(state.walls[0][0])
        assertEquals(listOf(DungeonSupply(id = "supply_0_2", position = Vector2(2.5f, 0.5f))), state.supplies)
        assertEquals(listOf(DungeonTrap(id = "trap_2_0", position = Vector2(0.5f, 2.5f))), state.traps)
        assertEquals(Vector2(1.5f, 2.5f), state.checkpointPosition)
        assertEquals(Vector2(2.5f, 3.5f), state.goalPosition)
    }

    @Test
    fun `tick is a no-op below the dead zone`() {
        val state = initialState()

        val result = DungeonGame.tick(state, Vector2(0.05f, 0.05f), deltaSeconds = 1f)

        assertEquals(state, result)
    }

    @Test
    fun `tick moves the player toward the joystick direction, scaled by speed and delta time`() {
        val state = initialState()

        val result = DungeonGame.tick(state, Vector2(1f, 0f), deltaSeconds = 0.1f)

        val expectedX = 0.5f + DungeonGame.PLAYER_SPEED_CELLS_PER_SECOND * 0.1f
        assertEquals(expectedX, result.playerPosition.x, 0.001f)
        assertEquals(0.5f, result.playerPosition.y, 0.001f)
    }

    @Test
    fun `wall collision blocks the axis that would collide while leaving the other axis free to slide`() {
        // Approaching the wall at (row 1, col 1) diagonally from the open
        // corner (0.5, 0.5): the X candidate alone (paired with the old Y,
        // which is still safely outside the wall's expanded band) never
        // collides, but the Y candidate (paired with the now-resolved X,
        // which *is* inside the wall's expanded X band) does — so X should
        // still advance even though Y gets held back, which is what makes a
        // diagonal push into a wall slide along it instead of stopping dead.
        val state = initialState()

        val result = DungeonGame.tick(state, Vector2(1f, 1f), deltaSeconds = 0.15f)

        assertTrue("X should advance", result.playerPosition.x > state.playerPosition.x)
        assertEquals("Y should be blocked by the wall", state.playerPosition.y, result.playerPosition.y, 0.001f)
    }

    @Test
    fun `a candidate position that would cross a map edge is treated as a wall collision`() {
        val state = initialState().copy(playerPosition = Vector2(0.4f, 0.5f))

        val result = DungeonGame.tick(state, Vector2(-1f, 0f), deltaSeconds = 1f)

        assertEquals(state.playerPosition, result.playerPosition)
    }

    @Test
    fun `stepping onto a supply collects a randomized amount and is idempotent on revisit`() {
        // guaranteedSuccess's nextFloat() is a fixed 0f, which selects SUPPLY_PICKUP_MIN.
        var state = initialState()

        state = tickUntilEvent(state, Vector2(1f, 0f), random = guaranteedSuccess)

        assertEquals(DungeonOutcome.SUPPLY_COLLECTED, state.lastOutcome)
        assertEquals(DungeonGame.SUPPLY_PICKUP_MIN, state.supplyCount)
        assertEquals(setOf("supply_0_2"), state.collectedSupplyIds)

        // Walking away and back onto the same supply doesn't collect it again.
        state = tickUntilEvent(state, Vector2(-1f, 0f), random = guaranteedSuccess)
        state = tickUntilEvent(state, Vector2(1f, 0f), random = guaranteedSuccess)

        assertEquals(DungeonGame.SUPPLY_PICKUP_MIN, state.supplyCount)
    }

    @Test
    fun `stepping onto a supply on a favorable reward roll collects the maximum amount`() {
        var state = initialState()

        // 0.6f fails the pickup roll's "< 0.5f" check, selecting SUPPLY_PICKUP_MAX.
        state = tickUntilEvent(state, Vector2(1f, 0f), random = fixedRandom(0.6f))

        assertEquals(DungeonOutcome.SUPPLY_COLLECTED, state.lastOutcome)
        assertEquals(DungeonGame.SUPPLY_PICKUP_MAX, state.supplyCount)
    }

    @Test
    fun `entering a trap freezes movement and starts combat with full toughness`() {
        // Approach from directly above (x = 0.5 stays clear of the wall at
        // col 1 the whole way down), the only open corridor into the trap.
        var state = initialState().copy(playerPosition = Vector2(0.5f, 0.9f))

        state = tickUntilEvent(state, Vector2(0f, 1f))

        assertEquals(DungeonOutcome.TRAP_ENTERED, state.lastOutcome)
        assertEquals(DungeonCombatState(trapId = "trap_2_0", banditToughnessRemaining = DungeonGame.BANDIT_INITIAL_TOUGHNESS), state.combat)

        val beforeTick = state
        state = DungeonGame.tick(state, Vector2(0f, 1f), deltaSeconds = 1f)

        assertEquals("tick is a full no-op while combat is active", beforeTick, state)
    }

    @Test
    fun `onSupplyThrown on a favorable roll hits and resolves the trap once toughness reaches zero, awarding the defeat bonus`() {
        var state = initialState().copy(
            combat = DungeonCombatState(trapId = "trap_2_0", banditToughnessRemaining = 2),
            supplyCount = 5,
        )

        state = DungeonGame.onSupplyThrown(state, guaranteedSuccess)
        assertEquals(DungeonOutcome.BANDIT_HIT, state.lastOutcome)
        assertEquals(1, state.combat?.banditToughnessRemaining)
        assertEquals(4, state.supplyCount)

        state = DungeonGame.onSupplyThrown(state, guaranteedSuccess)
        assertEquals(DungeonOutcome.BANDIT_SCARED_OFF, state.lastOutcome)
        assertNull(state.combat)
        assertEquals(3 + DungeonGame.BANDIT_DEFEAT_SUPPLY_REWARD, state.supplyCount)
        assertTrue("trap_2_0" in state.resolvedTrapIds)
    }

    @Test
    fun `onSupplyThrown on an unfavorable roll still spends the supply but reports a miss without touching toughness`() {
        val state = initialState().copy(
            combat = DungeonCombatState(trapId = "trap_2_0", banditToughnessRemaining = 2),
            supplyCount = 3,
        )

        val result = DungeonGame.onSupplyThrown(state, guaranteedFailure)

        assertEquals(DungeonOutcome.THROW_MISSED, result.lastOutcome)
        assertEquals(2, result.supplyCount)
        assertEquals(2, result.combat?.banditToughnessRemaining)
    }

    @Test
    fun `onSupplyThrown with no supplies reports OUT_OF_SUPPLIES and changes nothing else`() {
        val state = initialState().copy(
            combat = DungeonCombatState(trapId = "trap_2_0", banditToughnessRemaining = 2),
            supplyCount = 0,
        )

        val result = DungeonGame.onSupplyThrown(state, guaranteedSuccess)

        assertEquals(DungeonOutcome.OUT_OF_SUPPLIES, result.lastOutcome)
        assertEquals(state.combat, result.combat)
        assertEquals(0, result.supplyCount)
    }

    @Test
    fun `onSupplyThrown with no active combat is a full no-op`() {
        val state = initialState().copy(supplyCount = 3)

        val result = DungeonGame.onSupplyThrown(state, guaranteedSuccess)

        assertEquals(state, result)
    }

    @Test
    fun `onSamaritanAttack on a favorable roll hits and resolves the trap once toughness reaches zero, without spending a supply`() {
        var state = initialState().copy(
            combat = DungeonCombatState(trapId = "trap_2_0", banditToughnessRemaining = 2),
            supplyCount = 5,
        )

        state = DungeonGame.onSamaritanAttack(state, guaranteedSuccess)
        assertEquals(DungeonOutcome.SAMARITAN_HIT, state.lastOutcome)
        assertEquals(1, state.combat?.banditToughnessRemaining)
        assertEquals("the Samaritan's melee turn costs no supply", 5, state.supplyCount)

        state = DungeonGame.onSamaritanAttack(state, guaranteedSuccess)
        assertEquals(DungeonOutcome.BANDIT_SCARED_OFF, state.lastOutcome)
        assertNull(state.combat)
        assertEquals(5 + DungeonGame.BANDIT_DEFEAT_SUPPLY_REWARD, state.supplyCount)
        assertTrue("trap_2_0" in state.resolvedTrapIds)
    }

    @Test
    fun `onSamaritanAttack on an unfavorable roll reports a miss without touching toughness or supplies`() {
        val state = initialState().copy(
            combat = DungeonCombatState(trapId = "trap_2_0", banditToughnessRemaining = 2),
            supplyCount = 3,
        )

        val result = DungeonGame.onSamaritanAttack(state, guaranteedFailure)

        assertEquals(DungeonOutcome.SAMARITAN_ATTACK_MISSED, result.lastOutcome)
        assertEquals(2, result.combat?.banditToughnessRemaining)
        assertEquals(3, result.supplyCount)
    }

    @Test
    fun `onSamaritanAttack with no active combat is a full no-op`() {
        val state = initialState().copy(supplyCount = 3)

        val result = DungeonGame.onSamaritanAttack(state, guaranteedSuccess)

        assertEquals(state, result)
    }

    @Test
    fun `onBanditAttack steals a supply on a favorable roll, leaving combat itself untouched`() {
        val state = initialState().copy(
            combat = DungeonCombatState(trapId = "trap_2_0", banditToughnessRemaining = 1),
            supplyCount = 3,
        )

        val result = DungeonGame.onBanditAttack(state, guaranteedSuccess)

        assertEquals(DungeonOutcome.SUPPLY_STOLEN, result.lastOutcome)
        assertEquals(2, result.supplyCount)
        assertEquals(state.combat, result.combat)
    }

    @Test
    fun `onBanditAttack misses on an unfavorable roll, changing nothing but the outcome`() {
        val state = initialState().copy(
            combat = DungeonCombatState(trapId = "trap_2_0", banditToughnessRemaining = 1),
            supplyCount = 3,
        )

        val result = DungeonGame.onBanditAttack(state, guaranteedFailure)

        assertEquals(DungeonOutcome.BANDIT_ATTACK_MISSED, result.lastOutcome)
        assertEquals(3, result.supplyCount)
    }

    @Test
    fun `onBanditAttack with no supplies to steal reports a miss regardless of the roll`() {
        val state = initialState().copy(
            combat = DungeonCombatState(trapId = "trap_2_0", banditToughnessRemaining = 1),
            supplyCount = 0,
        )

        val result = DungeonGame.onBanditAttack(state, guaranteedSuccess)

        assertEquals(DungeonOutcome.BANDIT_ATTACK_MISSED, result.lastOutcome)
        assertEquals(0, result.supplyCount)
    }

    @Test
    fun `onBanditAttack with no active combat is a full no-op`() {
        val state = initialState()

        val result = DungeonGame.onBanditAttack(state, guaranteedSuccess)

        assertEquals(state, result)
    }

    @Test
    fun `onRetreat clears combat without resolving the trap, losing position, or losing supplies`() {
        val state = initialState().copy(
            playerPosition = Vector2(0.5f, 2.4f),
            combat = DungeonCombatState(trapId = "trap_2_0", banditToughnessRemaining = 1),
            supplyCount = 2,
        )

        val result = DungeonGame.onRetreat(state)

        assertEquals(DungeonOutcome.RETREATED, result.lastOutcome)
        assertNull(result.combat)
        assertFalse("trap_2_0" in result.resolvedTrapIds)
        assertEquals(state.playerPosition, result.playerPosition)
        assertEquals(2, result.supplyCount)
    }

    @Test
    fun `onRetreat with no active combat is a full no-op`() {
        val state = initialState()

        val result = DungeonGame.onRetreat(state)

        assertEquals(state, result)
    }

    @Test
    fun `re-entering an unresolved trap after retreating starts a fresh full-toughness fight`() {
        var state = initialState().copy(playerPosition = Vector2(0.5f, 0.9f), supplyCount = 1)
        state = tickUntilEvent(state, Vector2(0f, 1f)) // trigger
        state = DungeonGame.onSupplyThrown(state, guaranteedSuccess) // one hit, not enough to resolve
        state = DungeonGame.onRetreat(state)

        // Leaving the radius and coming back re-arms the trigger.
        state = tickUntilEvent(state, Vector2(0f, -1f))
        state = tickUntilEvent(state, Vector2(0f, 1f))

        assertEquals(DungeonOutcome.TRAP_ENTERED, state.lastOutcome)
        assertEquals(DungeonGame.BANDIT_INITIAL_TOUGHNESS, state.combat?.banditToughnessRemaining)
    }

    @Test
    fun `retreating from a trap does not immediately re-trigger it on the very next tick`() {
        var state = initialState().copy(playerPosition = Vector2(0.5f, 0.9f), supplyCount = 0)
        state = tickUntilEvent(state, Vector2(0f, 1f)) // trigger
        state = DungeonGame.onRetreat(state)

        val result = DungeonGame.tick(state, Vector2(0f, 1f), deltaSeconds = 1f / 60f)

        assertNull("still inside the trigger radius from before — no re-fire until the player actually leaves and comes back", result.combat)
    }

    @Test
    fun `reaching the checkpoint without enough supplies leaves it unactivated`() {
        // Approach from the open cell to its right (col 2) — the only open
        // corridor into the checkpoint in this tiny map, since directly
        // above it (row 1, col 1) is a wall.
        var state = initialState().copy(playerPosition = Vector2(2.4f, 2.5f), supplyCount = 0)

        state = tickUntilEvent(state, Vector2(-1f, 0f))

        assertEquals(DungeonOutcome.CHECKPOINT_NEEDS_SUPPLIES, state.lastOutcome)
        assertFalse(state.checkpointActivated)
        assertEquals(0, state.supplyCount)
    }

    @Test
    fun `reaching the checkpoint with enough supplies activates it and spends the cost`() {
        var state = initialState().copy(playerPosition = Vector2(2.4f, 2.5f), supplyCount = 2)

        state = tickUntilEvent(state, Vector2(-1f, 0f))

        assertEquals(DungeonOutcome.CHECKPOINT_ACTIVATED, state.lastOutcome)
        assertTrue(state.checkpointActivated)
        assertEquals(2 - DungeonGame.CHECKPOINT_SUPPLY_COST, state.supplyCount)
    }

    @Test
    fun `isComplete requires both checkpoint activation and goal proximity`() {
        val atGoalUnactivated = initialState().copy(playerPosition = Vector2(2.5f, 3.5f))
        assertFalse(atGoalUnactivated.isComplete)

        val atGoalActivated = atGoalUnactivated.copy(checkpointActivated = true)
        assertTrue(atGoalActivated.isComplete)

        val activatedButFar = initialState().copy(checkpointActivated = true)
        assertFalse(activatedButFar.isComplete)
    }

    @Test
    fun `once complete, tick, onSupplyThrown, and onRetreat are all identity no-ops`() {
        val completeState = initialState().copy(playerPosition = Vector2(2.5f, 3.5f), checkpointActivated = true)

        assertEquals(completeState, DungeonGame.tick(completeState, Vector2(1f, 1f), deltaSeconds = 1f))
        assertEquals(completeState, DungeonGame.onSupplyThrown(completeState))
        assertEquals(completeState, DungeonGame.onRetreat(completeState))
    }

    @Test
    fun `replaying the production map's hand-verified waypoint route reaches isComplete`() {
        // A generous supply cushion, not the map's own natural pickup
        // economy — this test's job is verifying the route/map itself is
        // walkable and completable end to end (a regression guard against a
        // broken waypoint or layout edit), not validating combat pacing —
        // whether the map's *natural* supply trickle keeps up with
        // BANDIT_INITIAL_TOUGHNESS (raised to 3 once the Good Samaritan
        // joined as a second real attacker — see DungeonGame.onSamaritanAttack)
        // is an on-device-feel balance question, not something this test
        // should gate on. Starting well-stocked means every encounter
        // resolves by throwing alone, so a fight is never forced to retreat
        // mid-route — which matters here because retreating leaves the
        // player off the exact straight line a later leg's steerToward
        // call was hand-verified against, and resuming from that unverified
        // midpoint can wall-deadlock steerToward's own "aim straight at the
        // target" steering (a real failure mode hit while developing this
        // fix, not a hypothetical).
        var state = DungeonGame.fromLayout(GoodSamaritanContent.mapLayout, GoodSamaritanContent.banditPatrols)
            .copy(supplyCount = 100)

        GoodSamaritanContent.dungeonRouteWaypoints.forEach { waypoint ->
            state = steerToward(state, waypoint)
            while (state.combat != null) {
                state = DungeonGame.onSupplyThrown(state, guaranteedSuccess)
            }
        }

        assertTrue(state.isComplete)
        assertEquals(7, state.collectedSupplyIds.size)
    }

    @Test
    fun `a patrolling bandit walking into a stationary player still starts combat`() {
        // A bandit patrolling back and forth between its spawn (col 4) and
        // the player's own starting cell (col 0) — the player never touches
        // the joystick, directly regression-testing the dead-zone-early-
        // return bug: without the fix, a moving trap's approach was never
        // checked at all when the player's own input was below
        // MIN_JOYSTICK_MAGNITUDE, since `tick` returned before any trigger
        // logic ran. T/I are required by fromLayout but otherwise unused
        // here.
        val layout = listOf("S...X", "..T.I")
        val patrols = mapOf("trap_0_4" to listOf(Vector2(0.5f, 0.5f), Vector2(4.5f, 0.5f)))
        val state = tickUntilEvent(DungeonGame.fromLayout(layout, patrols), direction = Vector2(0f, 0f))

        assertEquals(DungeonOutcome.TRAP_ENTERED, state.lastOutcome)
        assertEquals(DungeonCombatState(trapId = "trap_0_4", banditToughnessRemaining = DungeonGame.BANDIT_INITIAL_TOUGHNESS), state.combat)
        assertEquals("the player never moved", Vector2(0.5f, 0.5f), state.playerPosition)
    }

    /**
     * Closed-loop steering toward [target], self-correcting every tick from
     * wherever collision actually left the player. Normalizes the
     * direction to a fixed magnitude (well above
     * [DungeonGame.MIN_JOYSTICK_MAGNITUDE]) rather than feeding the raw,
     * shrinking distance-to-target vector: that naive approach gets
     * permanently stuck just short of arrival, since the remaining
     * distance itself eventually drops below the dead zone and
     * [DungeonGame.tick] starts ignoring it — a real joystick never has
     * this problem (the knob's drag offset doesn't shrink just because the
     * *player* is getting close to something), so this is purely an
     * artifact of using distance-to-target as a stand-in for a joystick
     * reading, not a real engine bug.
     */
    private fun steerToward(state: DungeonGameState, target: Vector2, deltaSeconds: Float = 1f / 60f): DungeonGameState {
        var current = state
        var safety = 0
        while (current.combat == null && !current.isComplete && current.playerPosition.distanceTo(target) > 0.05f) {
            check(safety++ < 5_000) { "Steering toward $target from ${current.playerPosition} did not converge" }
            val dx = target.x - current.playerPosition.x
            val dy = target.y - current.playerPosition.y
            val magnitude = kotlin.math.hypot(dx, dy)
            val direction = Vector2(dx / magnitude, dy / magnitude)
            current = DungeonGame.tick(current, direction, deltaSeconds)
        }
        return current
    }
}
