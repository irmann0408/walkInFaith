package com.bibleadventures.ui.screens.mainmenu

import com.bibleadventures.FakePlayerProfileRepository
import com.bibleadventures.MainDispatcherRule
import com.bibleadventures.domain.model.AdventureProgress
import com.bibleadventures.domain.model.ChapterId
import com.bibleadventures.domain.model.PlayerProfile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainMenuViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `continue adventure is disabled when no progress exists yet`() = runTest {
        val viewModel = MainMenuViewModel(FakePlayerProfileRepository())
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.hasAdventureInProgress)

        job.cancel()
    }

    @Test
    fun `continue adventure is enabled once a chapter has activities but isn't completed`() = runTest {
        val profile = PlayerProfile.DEFAULT.copy(
            progressByChapter = mapOf(
                ChapterId.NOAHS_ARK to AdventureProgress(
                    chapterId = ChapterId.NOAHS_ARK,
                    completedActivities = setOf("intro"),
                ),
            ),
        )
        val viewModel = MainMenuViewModel(FakePlayerProfileRepository(initial = profile))
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.hasAdventureInProgress)

        job.cancel()
    }

    @Test
    fun `continue adventure is disabled once the in-progress chapter is completed`() = runTest {
        val profile = PlayerProfile.DEFAULT.copy(
            progressByChapter = mapOf(
                ChapterId.NOAHS_ARK to AdventureProgress(
                    chapterId = ChapterId.NOAHS_ARK,
                    completed = true,
                    completedActivities = setOf("intro", "reward"),
                ),
            ),
        )
        val viewModel = MainMenuViewModel(FakePlayerProfileRepository(initial = profile))
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.hasAdventureInProgress)

        job.cancel()
    }
}
