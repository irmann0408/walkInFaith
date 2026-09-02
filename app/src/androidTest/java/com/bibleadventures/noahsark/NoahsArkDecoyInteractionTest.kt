package com.bibleadventures.noahsark

import android.content.Context
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.bibleadventures.MainActivity
import com.bibleadventures.R
import com.bibleadventures.data.local.playerProfileDataStore
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Covers "Find the Tools"'s decoy behavior: unlike a typical hidden-object
 * scene with a separate, always-wrong decoy item, its 10 tools are baked
 * into one background image with no other tappable object at all — tapping
 * anywhere that isn't a not-yet-found tool hotspot is itself the "wrong"
 * interaction, judged by [com.bibleadventures.ui.screens.noahsark.NoahsArkViewModel.onFindToolsBackgroundTapped].
 * Confirms it shows feedback, never completes the scene, and stays
 * interactive after repeated attempts. Kept separate from [NoahsArkFlowTest]
 * so that "happy path" walk doesn't have to carry decoy-specific
 * assertions.
 *
 * "Load the Ark" (replacing the old "Organize the Ark") has no equivalent
 * fixed-wrong-item to test here: its basket headcounts and deck targets are
 * randomly generated every run (see
 * [com.bibleadventures.game.puzzles.groupfill.GroupFillGame.randomSolvablePartition]),
 * so there's no basket that's guaranteed to overshoot every deck on every
 * playthrough the way the old drag-to-category decoy was guaranteed wrong.
 */
class NoahsArkDecoyInteractionTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    // A prior test run (or this suite's own other tests) may have already
    // solved find_tools for real, which would otherwise make Continue
    // appear early via the "skip an already-completed puzzle" shortcut —
    // clearing the profile first keeps this test deterministic regardless
    // of what ran before it, same idiom as
    // PlayerProfileLocalDataSourceInstrumentedTest.
    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        runBlocking { context.playerProfileDataStore.edit { it.clear() } }
    }

    @Test
    fun tappingOutsideEveryToolHotspot_showsFeedback_neverCompletesTheScene_andStaysInteractiveAfterRepeatedTaps() {
        val activity = composeTestRule.activity
        val nextPageLabel = activity.getString(R.string.action_next_page)

        composeTestRule.onNodeWithText(activity.getString(R.string.menu_adventures)).performClick()
        composeTestRule.onNodeWithText(activity.getString(R.string.chapter_noahs_ark_title)).performClick()
        composeTestRule.onNodeWithText(nextPageLabel).performClick() // Intro video -> Find the Tools

        // The background image's own center point falls outside all 10 fixed
        // tool hotspots (each pixel-matched to its own small area — none sit
        // exactly at the scene's center), so tapping the tagged background at
        // its default center never counts as finding a tool.
        val notATool = activity.getString(R.string.feedback_not_a_tool)
        repeat(2) {
            composeTestRule.onNodeWithTag("find_tools_background").performClick()
            composeTestRule.onNodeWithText(notATool).assertExists()
        }
        composeTestRule.onNodeWithText(nextPageLabel).assertDoesNotExist()
    }
}
