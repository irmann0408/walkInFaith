package com.bibleadventures.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.bibleadventures.R
import com.bibleadventures.domain.model.ChapterId
import com.bibleadventures.game.stories.NoahsArkContent
import com.bibleadventures.ui.AppViewModelProvider
import com.bibleadventures.ui.components.StoryBeatScreen
import com.bibleadventures.ui.screens.badges.BadgesScreen
import com.bibleadventures.ui.screens.character.CharacterScreen
import com.bibleadventures.ui.screens.comingsoon.ComingSoonScreen
import com.bibleadventures.ui.screens.mainmenu.MainMenuScreen
import com.bibleadventures.ui.screens.noahsark.NoahsArkViewModel
import com.bibleadventures.ui.screens.noahsark.findanimals.NoahsArkFindAnimalsScreen
import com.bibleadventures.ui.screens.noahsark.gathersupplies.NoahsArkGatherSuppliesScreen
import com.bibleadventures.ui.screens.noahsark.intro.NoahsArkIntroScreen
import com.bibleadventures.ui.screens.noahsark.lesson.NoahsArkLessonScreen
import com.bibleadventures.ui.screens.noahsark.matching.NoahsArkMatchingScreen
import com.bibleadventures.ui.screens.noahsark.missingitems.NoahsArkMissingItemsScreen
import com.bibleadventures.ui.screens.noahsark.organizeark.NoahsArkOrganizeArkScreen
import com.bibleadventures.ui.screens.noahsark.reward.NoahsArkRewardScreen
import com.bibleadventures.ui.screens.scripturecards.ScriptureCardsScreen
import com.bibleadventures.ui.screens.worldmap.WorldMapScreen

@Composable
fun BibleAdventuresNavHost(navController: NavHostController = rememberNavController()) {
    val comingSoonTitles = mapOf(
        MenuItemId.SETTINGS to stringResource(R.string.menu_settings),
        MenuItemId.PARENT_AREA to stringResource(R.string.menu_parent_area),
    )

    NavHost(navController = navController, startDestination = Destination.MainMenu.route) {
        composable(Destination.MainMenu.route) {
            MainMenuScreen(
                onMenuItemClick = { itemId ->
                    when (itemId) {
                        MenuItemId.CHARACTER -> navController.navigate(Destination.Character.route)
                        MenuItemId.ADVENTURES -> navController.navigate(Destination.WorldMap.route)
                        // Continuing simply takes the player back to the World Map, where
                        // the in-progress chapter's node already shows its state.
                        MenuItemId.CONTINUE_ADVENTURE -> navController.navigate(Destination.WorldMap.route)
                        MenuItemId.BADGES -> navController.navigate(Destination.Badges.route)
                        MenuItemId.SCRIPTURE_CARDS -> navController.navigate(Destination.ScriptureCards.route)
                        else -> {
                            // No real destination exists yet for these; every other menu
                            // item routes to the placeholder until its owning milestone lands.
                            val title = comingSoonTitles.getValue(itemId)
                            navController.navigate(Destination.ComingSoon(title).routeWithArgs())
                        }
                    }
                },
            )
        }
        composable(Destination.Character.route) {
            CharacterScreen(onBack = { navController.popBackStack() })
        }
        composable(Destination.WorldMap.route) {
            WorldMapScreen(
                onBack = { navController.popBackStack() },
                onChapterSelected = { chapterId ->
                    // Only Noah's Ark is ever unlocked/clickable right now, and it's the
                    // only chapter with real gameplay built so far.
                    if (chapterId == ChapterId.NOAHS_ARK) {
                        navController.navigate(Destination.NoahsArk.Intro.route)
                    }
                },
            )
        }
        composable(Destination.Badges.route) {
            BadgesScreen(onBack = { navController.popBackStack() })
        }
        composable(Destination.ScriptureCards.route) {
            ScriptureCardsScreen(onBack = { navController.popBackStack() })
        }
        noahsArkGraph(navController)
        composable(Destination.ComingSoon.ROUTE_WITH_ARGS) { backStackEntry ->
            val featureTitle =
                backStackEntry.arguments?.getString(Destination.ComingSoon.ARG_FEATURE_TITLE).orEmpty()
            ComingSoonScreen(
                featureTitle = featureTitle,
                onBack = { navController.popBackStack() },
            )
        }
    }
}

private fun NavGraphBuilder.noahsArkGraph(navController: NavHostController) {
    navigation(
        startDestination = Destination.NoahsArk.Intro.route,
        route = Destination.NoahsArk.GRAPH_ROUTE,
    ) {
        composable(Destination.NoahsArk.Intro.route) { entry ->
            val viewModel = navController.noahsArkViewModel(entry)
            NoahsArkIntroScreen(
                viewModel = viewModel,
                onContinue = {
                    viewModel.onSceneCompleted("intro")
                    navController.navigate(Destination.NoahsArk.FindAnimalsContext.route)
                },
            )
        }
        composable(Destination.NoahsArk.FindAnimalsContext.route) {
            StoryBeatScreen(
                titleRes = R.string.noahs_ark_find_animals_context_title,
                lineRes = NoahsArkContent.findAnimalsContextLines,
                onContinue = { navController.navigate(Destination.NoahsArk.FindAnimals.route) },
            )
        }
        composable(Destination.NoahsArk.FindAnimals.route) { entry ->
            val viewModel = navController.noahsArkViewModel(entry)
            NoahsArkFindAnimalsScreen(
                viewModel = viewModel,
                onContinue = {
                    viewModel.onSceneCompleted("find_animals")
                    navController.navigate(Destination.NoahsArk.AnimalMatching.route)
                },
            )
        }
        composable(Destination.NoahsArk.AnimalMatching.route) { entry ->
            val viewModel = navController.noahsArkViewModel(entry)
            NoahsArkMatchingScreen(
                viewModel = viewModel,
                onContinue = {
                    viewModel.onSceneCompleted("animal_matching")
                    navController.navigate(Destination.NoahsArk.GatherSuppliesContext.route)
                },
            )
        }
        composable(Destination.NoahsArk.GatherSuppliesContext.route) {
            StoryBeatScreen(
                titleRes = R.string.noahs_ark_gather_supplies_context_title,
                lineRes = NoahsArkContent.gatherSuppliesContextLines,
                onContinue = { navController.navigate(Destination.NoahsArk.GatherSupplies.route) },
            )
        }
        composable(Destination.NoahsArk.GatherSupplies.route) { entry ->
            val viewModel = navController.noahsArkViewModel(entry)
            NoahsArkGatherSuppliesScreen(
                viewModel = viewModel,
                onContinue = {
                    viewModel.onSceneCompleted("gather_supplies")
                    navController.navigate(Destination.NoahsArk.OrganizeArkContext.route)
                },
            )
        }
        composable(Destination.NoahsArk.OrganizeArkContext.route) {
            StoryBeatScreen(
                titleRes = R.string.noahs_ark_organize_context_title,
                lineRes = NoahsArkContent.organizeArkContextLines,
                onContinue = { navController.navigate(Destination.NoahsArk.OrganizeArk.route) },
            )
        }
        composable(Destination.NoahsArk.OrganizeArk.route) { entry ->
            val viewModel = navController.noahsArkViewModel(entry)
            NoahsArkOrganizeArkScreen(
                viewModel = viewModel,
                onContinue = {
                    viewModel.onSceneCompleted("organize_ark")
                    navController.navigate(Destination.NoahsArk.FindMissingItems.route)
                },
            )
        }
        composable(Destination.NoahsArk.FindMissingItems.route) { entry ->
            val viewModel = navController.noahsArkViewModel(entry)
            NoahsArkMissingItemsScreen(
                viewModel = viewModel,
                onContinue = {
                    viewModel.onSceneCompleted("find_missing_items")
                    navController.navigate(Destination.NoahsArk.Lesson.route)
                },
            )
        }
        composable(Destination.NoahsArk.Lesson.route) { entry ->
            val viewModel = navController.noahsArkViewModel(entry)
            NoahsArkLessonScreen(
                onContinue = {
                    viewModel.onSceneCompleted("lesson")
                    navController.navigate(Destination.NoahsArk.Reward.route)
                },
            )
        }
        composable(Destination.NoahsArk.Reward.route) { entry ->
            NoahsArkRewardScreen(
                viewModel = navController.noahsArkViewModel(entry),
                onReturnToMap = {
                    // Clears the whole Noah's Ark back stack so Back from the map can't
                    // re-enter a finished run or re-trigger onChapterFinished().
                    navController.navigate(Destination.WorldMap.route) {
                        popUpTo(Destination.WorldMap.route)
                        launchSingleTop = true
                    }
                },
            )
        }
    }
}

@Composable
private fun NavHostController.noahsArkViewModel(entry: NavBackStackEntry): NoahsArkViewModel {
    val parentEntry = remember(entry) { getBackStackEntry(Destination.NoahsArk.GRAPH_ROUTE) }
    return viewModel(viewModelStoreOwner = parentEntry, factory = AppViewModelProvider.Factory)
}
