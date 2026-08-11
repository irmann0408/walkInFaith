package com.bibleadventures.game.puzzles.stackbuild

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StackBuildGameTest {

    private fun threeItemState(): StackBuildGameState = StackBuildGameState(itemIds = listOf("a", "b", "c"))

    @Test
    fun `placing an item appends it to placedOrder`() {
        val next = StackBuildGame.onItemPlaced(threeItemState(), "b")

        assertEquals(listOf("b"), next.placedOrder)
    }

    @Test
    fun `items can be placed in any order`() {
        var state = threeItemState()
        state = StackBuildGame.onItemPlaced(state, "c")
        state = StackBuildGame.onItemPlaced(state, "a")
        state = StackBuildGame.onItemPlaced(state, "b")

        assertEquals(listOf("c", "a", "b"), state.placedOrder)
        assertTrue(state.isComplete)
    }

    @Test
    fun `placing the same item twice is a no-op`() {
        val once = StackBuildGame.onItemPlaced(threeItemState(), "a")
        val twice = StackBuildGame.onItemPlaced(once, "a")

        assertEquals(once, twice)
    }

    @Test
    fun `placing an unknown item id is a no-op`() {
        val state = threeItemState()

        val next = StackBuildGame.onItemPlaced(state, "not-a-real-item")

        assertEquals(state, next)
    }

    @Test
    fun `remainingIds excludes already-placed items`() {
        val next = StackBuildGame.onItemPlaced(threeItemState(), "b")

        assertEquals(listOf("a", "c"), next.remainingIds)
    }

    @Test
    fun `isComplete is true only once every item is placed`() {
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
