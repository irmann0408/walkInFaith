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
import com.bibleadventures.game.stories.DavidGoliathContent
import com.bibleadventures.game.stories.GoodSamaritanContent
import com.bibleadventures.game.stories.NoahsArkContent
import com.bibleadventures.ui.AppViewModelProvider
import com.bibleadventures.ui.components.StoryBeatScreen
import com.bibleadventures.ui.screens.badges.BadgesScreen
import com.bibleadventures.ui.screens.character.CharacterScreen
import com.bibleadventures.ui.screens.comingsoon.ComingSoonScreen
import com.bibleadventures.ui.screens.davidgoliath.DavidGoliathViewModel
import com.bibleadventures.ui.screens.davidgoliath.choice.DavidGoliathChoiceScreen
import com.bibleadventures.ui.screens.davidgoliath.choosestones.DavidGoliathChooseStonesScreen
import com.bibleadventures.ui.screens.davidgoliath.dodge.DavidGoliathDodgeScreen
import com.bibleadventures.ui.screens.davidgoliath.intro.DavidGoliathIntroScreen
import com.bibleadventures.ui.screens.davidgoliath.lesson.DavidGoliathLessonScreen
import com.bibleadventures.ui.screens.davidgoliath.reward.DavidGoliathRewardScreen
import com.bibleadventures.ui.screens.davidgoliath.sheepcounting.DavidGoliathSheepCountingScreen
import com.bibleadventures.ui.screens.davidgoliath.slingpractice.DavidGoliathSlingPracticeScreen
import com.bibleadventures.ui.screens.goodsamaritan.GoodSamaritanViewModel
import com.bibleadventures.ui.screens.goodsamaritan.explore.GoodSamaritanExploreScreen
import com.bibleadventures.ui.screens.goodsamaritan.intro.GoodSamaritanIntroScreen
import com.bibleadventures.ui.screens.goodsamaritan.lesson.GoodSamaritanLessonScreen
import com.bibleadventures.ui.screens.goodsamaritan.reward.GoodSamaritanRewardScreen
import com.bibleadventures.ui.screens.mainmenu.MainMenuScreen
import com.bibleadventures.ui.screens.noahsark.NoahsArkViewModel
import com.bibleadventures.ui.screens.noahsark.findanimals.NoahsArkFindAnimalsScreen
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
                    // Only chapters with real gameplay built so far get a real destination.
                    if (chapterId == ChapterId.NOAHS_ARK) {
                        navController.navigate(Destination.NoahsArk.Intro.route)
                    } else if (chapterId == ChapterId.DAVID_GOLIATH) {
                        navController.navigate(Destination.DavidGoliath.Intro.route)
                    } else if (chapterId == ChapterId.GOOD_SAMARITAN) {
                        navController.navigate(Destination.GoodSamaritan.Intro.route)
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
        davidGoliathGraph(navController)
        goodSamaritanGraph(navController)
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

private fun NavGraphBuilder.davidGoliathGraph(navController: NavHostController) {
    navigation(
        startDestination = Destination.DavidGoliath.Intro.route,
        route = Destination.DavidGoliath.GRAPH_ROUTE,
    ) {
        composable(Destination.DavidGoliath.Intro.route) { entry ->
            val viewModel = navController.davidGoliathViewModel(entry)
            DavidGoliathIntroScreen(
                viewModel = viewModel,
                onContinue = {
                    viewModel.onSceneCompleted("intro")
                    navController.navigate(Destination.DavidGoliath.SheepCountingContext.route)
                },
            )
        }
        composable(Destination.DavidGoliath.SheepCountingContext.route) {
            StoryBeatScreen(
                titleRes = R.string.david_goliath_sheep_counting_context_title,
                lineRes = DavidGoliathContent.sheepCountingContextLines,
                onContinue = { navController.navigate(Destination.DavidGoliath.SheepCounting.route) },
            )
        }
        composable(Destination.DavidGoliath.SheepCounting.route) { entry ->
            val viewModel = navController.davidGoliathViewModel(entry)
            DavidGoliathSheepCountingScreen(
                viewModel = viewModel,
                onContinue = {
                    viewModel.onSceneCompleted("sheep_counting")
                    navController.navigate(Destination.DavidGoliath.ChooseStonesContext.route)
                },
            )
        }
        composable(Destination.DavidGoliath.ChooseStonesContext.route) {
            StoryBeatScreen(
                titleRes = R.string.david_goliath_choose_stones_context_title,
                lineRes = DavidGoliathContent.chooseStonesContextLines,
                onContinue = { navController.navigate(Destination.DavidGoliath.ChooseStones.route) },
            )
        }
        composable(Destination.DavidGoliath.ChooseStones.route) { entry ->
            val viewModel = navController.davidGoliathViewModel(entry)
            DavidGoliathChooseStonesScreen(
                viewModel = viewModel,
                onContinue = {
                    viewModel.onSceneCompleted("choose_stones")
                    navController.navigate(Destination.DavidGoliath.SlingPracticeContext.route)
                },
            )
        }
        composable(Destination.DavidGoliath.SlingPracticeContext.route) {
            StoryBeatScreen(
                titleRes = R.string.david_goliath_sling_practice_context_title,
                lineRes = DavidGoliathContent.slingPracticeContextLines,
                onContinue = { navController.navigate(Destination.DavidGoliath.Choice.route) },
            )
        }
        composable(Destination.DavidGoliath.Choice.route) { entry ->
            val viewModel = navController.davidGoliathViewModel(entry)
            DavidGoliathChoiceScreen(
                viewModel = viewModel,
                onContinue = {
                    viewModel.onSceneCompleted("choice")
                    navController.navigate(Destination.DavidGoliath.DodgeContext.route)
                },
            )
        }
        composable(Destination.DavidGoliath.DodgeContext.route) {
            StoryBeatScreen(
                titleRes = R.string.david_goliath_dodge_context_title,
                lineRes = DavidGoliathContent.dodgeContextLines,
                onContinue = { navController.navigate(Destination.DavidGoliath.Dodge.route) },
            )
        }
        composable(Destination.DavidGoliath.Dodge.route) { entry ->
            val viewModel = navController.davidGoliathViewModel(entry)
            DavidGoliathDodgeScreen(
                viewModel = viewModel,
                onContinue = {
                    viewModel.onSceneCompleted("dodge")
                    navController.navigate(Destination.DavidGoliath.SlingPractice.route)
                },
            )
        }
        composable(Destination.DavidGoliath.SlingPractice.route) { entry ->
            val viewModel = navController.davidGoliathViewModel(entry)
            DavidGoliathSlingPracticeScreen(
                viewModel = viewModel,
                onContinue = {
                    viewModel.onSceneCompleted("sling_practice")
                    navController.navigate(Destination.DavidGoliath.Lesson.route)
                },
            )
        }
        composable(Destination.DavidGoliath.Lesson.route) { entry ->
            val viewModel = navController.davidGoliathViewModel(entry)
            DavidGoliathLessonScreen(
                onContinue = {
                    viewModel.onSceneCompleted("lesson")
                    navController.navigate(Destination.DavidGoliath.Reward.route)
                },
            )
        }
        composable(Destination.DavidGoliath.Reward.route) { entry ->
            DavidGoliathRewardScreen(
                viewModel = navController.davidGoliathViewModel(entry),
                onReturnToMap = {
                    // Clears the whole David & Goliath back stack so Back from the map
                    // can't re-enter a finished run or re-trigger onChapterFinished().
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
private fun NavHostController.davidGoliathViewModel(entry: NavBackStackEntry): DavidGoliathViewModel {
    val parentEntry = remember(entry) { getBackStackEntry(Destination.DavidGoliath.GRAPH_ROUTE) }
    return viewModel(viewModelStoreOwner = parentEntry, factory = AppViewModelProvider.Factory)
}

private fun NavGraphBuilder.goodSamaritanGraph(navController: NavHostController) {
    navigation(
        startDestination = Destination.GoodSamaritan.Intro.route,
        route = Destination.GoodSamaritan.GRAPH_ROUTE,
    ) {
        composable(Destination.GoodSamaritan.Intro.route) { entry ->
            val viewModel = navController.goodSamaritanViewModel(entry)
            GoodSamaritanIntroScreen(
                viewModel = viewModel,
                onContinue = {
                    viewModel.onSceneCompleted("intro")
                    navController.navigate(Destination.GoodSamaritan.ExploreContext.route)
                },
            )
        }
        composable(Destination.GoodSamaritan.ExploreContext.route) {
            StoryBeatScreen(
                titleRes = R.string.good_samaritan_explore_context_title,
                lineRes = GoodSamaritanContent.exploreContextLines,
                onContinue = { navController.navigate(Destination.GoodSamaritan.Explore.route) },
            )
        }
        composable(Destination.GoodSamaritan.Explore.route) { entry ->
            val viewModel = navController.goodSamaritanViewModel(entry)
            GoodSamaritanExploreScreen(
                viewModel = viewModel,
                onContinue = {
                    viewModel.onSceneCompleted("explore")
                    navController.navigate(Destination.GoodSamaritan.Lesson.route)
                },
            )
        }
        composable(Destination.GoodSamaritan.Lesson.route) { entry ->
            val viewModel = navController.goodSamaritanViewModel(entry)
            GoodSamaritanLessonScreen(
                onContinue = {
                    viewModel.onSceneCompleted("lesson")
                    navController.navigate(Destination.GoodSamaritan.Reward.route)
                },
            )
        }
        composable(Destination.GoodSamaritan.Reward.route) { entry ->
            GoodSamaritanRewardScreen(
                viewModel = navController.goodSamaritanViewModel(entry),
                onReturnToMap = {
                    // Clears the whole Good Samaritan back stack so Back from the map
                    // can't re-enter a finished run or re-trigger onChapterFinished().
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
private fun NavHostController.goodSamaritanViewModel(entry: NavBackStackEntry): GoodSamaritanViewModel {
    val parentEntry = remember(entry) { getBackStackEntry(Destination.GoodSamaritan.GRAPH_ROUTE) }
    return viewModel(viewModelStoreOwner = parentEntry, factory = AppViewModelProvider.Factory)
}
