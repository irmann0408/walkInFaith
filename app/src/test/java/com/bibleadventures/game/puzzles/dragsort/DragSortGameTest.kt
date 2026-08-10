package com.bibleadventures.game.puzzles.dragsort

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private val lion = SortableItem("lion", iconRes = 1, contentDescriptionRes = 1, categoryKey = "animals")
private val bread = SortableItem("bread", iconRes = 2, contentDescriptionRes = 2, categoryKey = "food")
private val hammer = SortableItem("hammer", iconRes = 3, contentDescriptionRes = 3, categoryKey = null)
private val categories = listOf(
    SortCategory("animals", labelRes = 10),
    SortCategory("food", labelRes = 11),
)

private fun initialState() = DragSortGameState(items = listOf(lion, bread), categories = categories)

private fun initialStateWithDecoy() = DragSortGameState(items = listOf(lion, bread, hammer), categories = categories)

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

    @Test
    fun `dropping an unsortable item on any category reports NOT_SORTABLE and never places it`() {
        var state = DragSortGame.onItemDroppedOnCategory(initialStateWithDecoy(), "hammer", "animals")
        assertEquals(SortOutcome.NOT_SORTABLE, state.lastOutcome)
        assertTrue("hammer" !in state.placedItems)

        state = DragSortGame.onItemDroppedOnCategory(state, "hammer", "food")
        assertEquals(SortOutcome.NOT_SORTABLE, state.lastOutcome)
        assertTrue("hammer" !in state.placedItems)
    }

    @Test
    fun `game is complete once every real item is placed, even with an unsortable item still unplaced`() {
        var state = initialStateWithDecoy()
        assertFalse(state.isComplete)

        state = DragSortGame.onItemDroppedOnCategory(state, "lion", "animals")
        state = DragSortGame.onItemDroppedOnCategory(state, "bread", "food")

        assertTrue(state.isComplete)
        assertTrue("hammer" !in state.placedItems)
    }

    @Test
    fun `dropping the same unsortable item repeatedly always reports NOT_SORTABLE and never blocks retrying`() {
        var state = initialStateWithDecoy()

        repeat(3) {
            state = DragSortGame.onItemDroppedOnCategory(state, "hammer", "food")
            assertEquals(SortOutcome.NOT_SORTABLE, state.lastOutcome)
        }
        assertTrue("hammer" !in state.placedItems)
    }
}
