package com.bibleadventures.game.puzzles.stackbuild

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StackBuildGameTest {

    private fun threeItemState(): StackBuildGameState = StackBuildGameState(itemIds = listOf("a", "b", "c"))

    @Test
    fun `placing the correct next item appends it to placedOrder`() {
        val next = StackBuildGame.onItemPlaced(threeItemState(), "a")

        assertEquals(listOf("a"), next.placedOrder)
        assertEquals(StackBuildOutcome.PLACED, next.lastOutcome)
    }

    @Test
    fun `placing an item out of order does not advance, but keeps prior progress`() {
        var state = threeItemState()
        state = StackBuildGame.onItemPlaced(state, "a") // correct
        state = StackBuildGame.onItemPlaced(state, "c") // wrong — "b" is expected next

        assertEquals(listOf("a"), state.placedOrder)
        assertEquals(StackBuildOutcome.WRONG_ORDER, state.lastOutcome)
    }

    @Test
    fun `placing items in the required order completes the stack`() {
        var state = threeItemState()
        state = StackBuildGame.onItemPlaced(state, "a")
        state = StackBuildGame.onItemPlaced(state, "b")
        state = StackBuildGame.onItemPlaced(state, "c")

        assertEquals(listOf("a", "b", "c"), state.placedOrder)
        assertEquals(StackBuildOutcome.COMPLETE, state.lastOutcome)
        assertTrue(state.isComplete)
    }

    @Test
    fun `nextExpectedId tracks the next required item, null once complete`() {
        var state = threeItemState()
        assertEquals("a", state.nextExpectedId)

        state = StackBuildGame.onItemPlaced(state, "a")
        assertEquals("b", state.nextExpectedId)

        state = StackBuildGame.onItemPlaced(state, "b")
        state = StackBuildGame.onItemPlaced(state, "c")
        assertNull(state.nextExpectedId)
    }

    @Test
    fun `remainingIds excludes already-placed items`() {
        val next = StackBuildGame.onItemPlaced(threeItemState(), "a")

        assertEquals(listOf("b", "c"), next.remainingIds)
    }

    @Test
    fun `isComplete is true only once every item is placed in order`() {
        var state = threeItemState()
        assertFalse(state.isComplete)

        state = StackBuildGame.onItemPlaced(state, "a")
        state = StackBuildGame.onItemPlaced(state, "b")
        assertFalse(state.isComplete)

        state = StackBuildGame.onItemPlaced(state, "c")
        assertTrue(state.isComplete)
    }

    @Test
    fun `once complete, further placements are a no-op`() {
        var state = threeItemState()
        listOf("a", "b", "c").forEach { state = StackBuildGame.onItemPlaced(state, it) }

        val unchanged = StackBuildGame.onItemPlaced(state, "a")

        assertEquals(state, unchanged)
    }
}
