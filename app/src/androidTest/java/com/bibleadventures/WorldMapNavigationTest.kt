package com.bibleadventures

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.bibleadventures.game.stories.ChapterCatalog
import org.junit.Rule
import org.junit.Test

class WorldMapNavigationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun worldMap_showsHomeVillageAndAllChapterNodesWithCorrectLockState() {
        val adventuresLabel = composeTestRule.activity.getString(R.string.menu_adventures)
        val worldMapTitle = composeTestRule.activity.getString(R.string.world_map_title)
        val homeVillage = composeTestRule.activity.getString(R.string.world_map_home_village)

        composeTestRule.onNodeWithText(adventuresLabel).performClick()
        composeTestRule.onNodeWithText(worldMapTitle).assertExists()
        composeTestRule.onNodeWithText(homeVillage).assertExists()

        val noahsArkTitle = composeTestRule.activity.getString(R.string.chapter_noahs_ark_title)
        composeTestRule.onNodeWithText(noahsArkTitle).assertIsEnabled()

        ChapterCatalog.all.drop(1).forEach { chapter ->
            val title = composeTestRule.activity.getString(chapter.titleRes)
            // The World Map is a LazyColumn — later chapters aren't composed until
            // scrolled into view. performScrollTo() only works on already-composed
            // off-screen nodes, so scroll the tagged list itself to bring a
            // not-yet-composed item into existence first.
            composeTestRule.onNodeWithTag("world_map_chapter_list").performScrollToNode(hasText(title))
            composeTestRule.onNodeWithText(title).assertIsNotEnabled()
        }
    }

    @Test
    fun tappingNoahsArkNode_navigatesToTheRealAdventure() {
        // As of Milestone 4, Noah's Ark has real gameplay — see NoahsArkFlowTest for
        // the full walkthrough. This just confirms the World Map routes there instead
        // of ComingSoonScreen.
        val adventuresLabel = composeTestRule.activity.getString(R.string.menu_adventures)
        val noahsArkTitle = composeTestRule.activity.getString(R.string.chapter_noahs_ark_title)
        val continueLabel = composeTestRule.activity.getString(R.string.action_continue)

        composeTestRule.onNodeWithText(adventuresLabel).performClick()
        composeTestRule.onNodeWithText(noahsArkTitle).performClick()

        composeTestRule.onNodeWithText(continueLabel).assertExists()
    }
}
