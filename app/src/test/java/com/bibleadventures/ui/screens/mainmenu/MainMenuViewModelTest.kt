package com.bibleadventures.ui.screens.mainmenu

import org.junit.Assert.assertFalse
import org.junit.Test

class MainMenuViewModelTest {

    @Test
    fun `continue adventure is disabled when no progress exists yet`() {
        val viewModel = MainMenuViewModel()

        assertFalse(viewModel.uiState.value.hasAdventureInProgress)
    }
}
