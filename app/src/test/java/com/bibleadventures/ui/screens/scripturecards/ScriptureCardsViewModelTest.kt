package com.bibleadventures.ui.screens.scripturecards

import com.bibleadventures.FakePlayerProfileRepository
import com.bibleadventures.MainDispatcherRule
import com.bibleadventures.domain.model.PlayerProfile
import com.bibleadventures.game.rewards.NoahsArkReward
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScriptureCardsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `a scripture card not yet in the profile is reported as not earned`() = runTest {
        val viewModel = ScriptureCardsViewModel(
            FakePlayerProfileRepository(),
            catalog = listOf(NoahsArkReward.scriptureCard),
        )
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value.cards.single()
        assertEquals(NoahsArkReward.scriptureCard, state.card)
        assertFalse(state.earned)

        job.cancel()
    }

    @Test
    fun `a scripture card id present in the profile is reported as earned`() = runTest {
        val profile = PlayerProfile.DEFAULT.copy(scriptureCards = setOf(NoahsArkReward.scriptureCard.id))
        val viewModel = ScriptureCardsViewModel(
            FakePlayerProfileRepository(initial = profile),
            catalog = listOf(NoahsArkReward.scriptureCard),
        )
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.cards.single().earned)

        job.cancel()
    }
}
