package com.bibleadventures.game.puzzles.hiddenobject

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private val bread = HiddenItem("bread", Offset(0.2f, 0.3f), iconRes = 1, contentDescriptionRes = 1)
private val water = HiddenItem("water", Offset(0.7f, 0.5f), iconRes = 2, contentDescriptionRes = 2)

private fun initialState() = HiddenObjectGameState(items = listOf(bread, water))

class HiddenObjectGameTest {

    @Test
    fun `tapping a hidden item marks it found`() {
        val state = HiddenObjectGame.onItemTapped(initialState(), "bread")

        assertTrue("bread" in state.foundIds)
        assertFalse(state.isComplete)
    }

    @Test
    fun `tapping an already-found item is a no-op`() {
        var state = initialState()
        state = HiddenObjectGame.onItemTapped(state, "bread")
        val afterFirstTap = state

        state = HiddenObjectGame.onItemTapped(state, "bread")

        assertEquals(afterFirstTap.foundIds, state.foundIds)
    }

    @Test
    fun `game is complete once every item is found`() {
        var state = initialState()
        state = HiddenObjectGame.onItemTapped(state, "bread")
        state = HiddenObjectGame.onItemTapped(state, "water")

        assertTrue(state.isComplete)
    }
}
