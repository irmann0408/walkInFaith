package com.bibleadventures.game.puzzles.groupfill

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GroupFillGameTest {

    private fun twoCircleState(): GroupFillGameState = GroupFillGameState(
        families = listOf(
            FamilyGroup("a", 10),
            FamilyGroup("b", 10),
            FamilyGroup("c", 20),
            FamilyGroup("d", 20),
        ),
        circleTargets = listOf(20, 20),
    )

    @Test
    fun `a family that fits is added to the circle`() {
        val next = GroupFillGame.onFamilyDropped(twoCircleState(), "a", circleIndex = 0)

        assertEquals(listOf("a"), next.circleContents[0])
        assertEquals(GroupFillOutcome.ADDED, next.lastOutcome)
    }

    @Test
    fun `a family that would overshoot is rejected without being placed`() {
        var state = twoCircleState()
        state = GroupFillGame.onFamilyDropped(state, "a", circleIndex = 0) // circle 0 sum = 10
        state = GroupFillGame.onFamilyDropped(state, "c", circleIndex = 0) // would be 30 > target 20, rejected

        assertTrue(state.circleContents[0].none { it == "c" })
        assertEquals(GroupFillOutcome.REJECTED_OVERSHOOT, state.lastOutcome)
        assertEquals(10, state.circleSum(0)) // prior progress kept
    }

    @Test
    fun `a family that exactly completes a circle sets CIRCLE_COMPLETE`() {
        var state = twoCircleState()
        state = GroupFillGame.onFamilyDropped(state, "a", circleIndex = 0)
        state = GroupFillGame.onFamilyDropped(state, "b", circleIndex = 0)

        assertTrue(state.isCircleComplete(0))
        assertEquals(GroupFillOutcome.CIRCLE_COMPLETE, state.lastOutcome)
        assertFalse(state.isComplete) // circle 1 still empty
    }

    @Test
    fun `completing every circle sets ALL_COMPLETE`() {
        var state = twoCircleState()
        state = GroupFillGame.onFamilyDropped(state, "a", circleIndex = 0)
        state = GroupFillGame.onFamilyDropped(state, "b", circleIndex = 0)
        state = GroupFillGame.onFamilyDropped(state, "c", circleIndex = 1)

        assertEquals(GroupFillOutcome.ALL_COMPLETE, state.lastOutcome)
        assertTrue(state.isComplete)
    }

    @Test
    fun `remainingFamilyIds excludes already-placed families`() {
        val next = GroupFillGame.onFamilyDropped(twoCircleState(), "a", circleIndex = 0)

        assertEquals(listOf("b", "c", "d"), next.remainingFamilyIds)
    }

    @Test
    fun `placing an already-placed family again is a no-op`() {
        var state = twoCircleState()
        state = GroupFillGame.onFamilyDropped(state, "a", circleIndex = 0)

        val unchanged = GroupFillGame.onFamilyDropped(state, "a", circleIndex = 1)

        assertEquals(state, unchanged)
    }

    @Test
    fun `once complete, further drops are a no-op`() {
        var state = twoCircleState()
        state = GroupFillGame.onFamilyDropped(state, "a", circleIndex = 0)
        state = GroupFillGame.onFamilyDropped(state, "b", circleIndex = 0)
        state = GroupFillGame.onFamilyDropped(state, "c", circleIndex = 1)
        state = GroupFillGame.onFamilyDropped(state, "d", circleIndex = 1)
        val completed = state

        val unchanged = GroupFillGame.onFamilyDropped(completed, "a", circleIndex = 0)

        assertEquals(completed, unchanged)
    }
}
