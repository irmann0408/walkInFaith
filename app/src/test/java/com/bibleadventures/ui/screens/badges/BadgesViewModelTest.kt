package com.bibleadventures.ui.screens.badges

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
class BadgesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `a badge not yet in the profile is reported as not earned`() = runTest {
        val viewModel = BadgesViewModel(FakePlayerProfileRepository(), catalog = listOf(NoahsArkReward.badge))
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value.badges.single()
        assertEquals(NoahsArkReward.badge, state.badge)
        assertFalse(state.earned)

        job.cancel()
    }

    @Test
    fun `a badge id present in the profile is reported as earned`() = runTest {
        val profile = PlayerProfile.DEFAULT.copy(badges = setOf(NoahsArkReward.badge.id))
        val viewModel = BadgesViewModel(
            FakePlayerProfileRepository(initial = profile),
            catalog = listOf(NoahsArkReward.badge),
        )
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.badges.single().earned)

        job.cancel()
    }
}
