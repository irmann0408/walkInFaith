package com.bibleadventures.game.puzzles.dragsort

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private val lion = SortableItem("lion", iconRes = 1, contentDescriptionRes = 1, categoryKey = "animals")
private val bread = SortableItem("bread", iconRes = 2, contentDescriptionRes = 2, categoryKey = "food")
private val categories = listOf(
    SortCategory("animals", labelRes = 10),
    SortCategory("food", labelRes = 11),
)

private fun initialState() = DragSortGameState(items = listOf(lion, bread), categories = categories)

class DragSortGameTest {

    @Test
    fun `dropping an item on its correct category places it and reports CORRECT`() {
        val state = DragSortGame.onItemDroppedOnCategory(initialState(), "lion", "animals")

        assertEquals(SortOutcome.CORRECT, state.lastOutcome)
        assertEquals("animals", state.placedItems["lion"])
    }

    @Test
    fun `dropping an item on the wrong category reports TRY_AGAIN and leaves it unplaced`() {
        val state = DragSortGame.onItemDroppedOnCategory(initialState(), "lion", "food")

        assertEquals(SortOutcome.TRY_AGAIN, state.lastOutcome)
        assertTrue(state.placedItems.isEmpty())
    }

    @Test
    fun `a wrong drop does not block placing the item correctly afterward`() {
        var state = initialState()
        state = DragSortGame.onItemDroppedOnCategory(state, "lion", "food")
        state = DragSortGame.onItemDroppedOnCategory(state, "lion", "animals")

        assertEquals(SortOutcome.CORRECT, state.lastOutcome)
        assertEquals("animals", state.placedItems["lion"])
    }

    @Test
    fun `game is complete once every item is placed`() {
        var state = initialState()
        assertFalse(state.isComplete)

        state = DragSortGame.onItemDroppedOnCategory(state, "lion", "animals")
        state = DragSortGame.onItemDroppedOnCategory(state, "bread", "food")

        assertTrue(state.isComplete)
    }
}
