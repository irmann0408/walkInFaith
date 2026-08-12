package com.bibleadventures.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.bibleadventures.game.stories.DanielContent
import com.bibleadventures.game.stories.DavidGoliathContent
import com.bibleadventures.game.stories.EstherContent
import com.bibleadventures.game.stories.Feeding5000Content
import com.bibleadventures.game.stories.GoodSamaritanContent
import com.bibleadventures.game.stories.JerichoContent
import com.bibleadventures.game.stories.JesusCalmsStormContent
import com.bibleadventures.game.stories.NoahsArkContent
import com.bibleadventures.ui.AppViewModelProvider
import com.bibleadventures.ui.components.StoryBeatScreen
import com.bibleadventures.ui.screens.badges.BadgesScreen
import com.bibleadventures.ui.screens.character.CharacterScreen
import com.bibleadventures.ui.screens.comingsoon.ComingSoonScreen
import com.bibleadventures.ui.screens.daniel.DanielViewModel
import com.bibleadventures.ui.screens.daniel.choice.DanielChoiceScreen
import com.bibleadventures.ui.screens.daniel.dariusmaze.DanielDariusMazeScreen
import com.bibleadventures.ui.screens.daniel.intro.DanielIntroScreen
import com.bibleadventures.ui.screens.daniel.lesson.DanielLessonScreen
import com.bibleadventures.ui.screens.daniel.lionsden.DanielLionsDenScreen
import com.bibleadventures.ui.screens.daniel.reward.DanielRewardScreen
import com.bibleadventures.ui.screens.daniel.stealth.DanielStealthScreen
import com.bibleadventures.ui.screens.davidgoliath.DavidGoliathViewModel
import com.bibleadventures.ui.screens.davidgoliath.choice.DavidGoliathChoiceScreen
import com.bibleadventures.ui.screens.davidgoliath.choosestones.DavidGoliathChooseStonesScreen
import com.bibleadventures.ui.screens.davidgoliath.dodge.DavidGoliathDodgeScreen
import com.bibleadventures.ui.screens.davidgoliath.intro.DavidGoliathIntroScreen
import com.bibleadventures.ui.screens.davidgoliath.lesson.DavidGoliathLessonScreen
import com.bibleadventures.ui.screens.davidgoliath.reward.DavidGoliathRewardScreen
import com.bibleadventures.ui.screens.davidgoliath.sheepcounting.DavidGoliathSheepCountingScreen
import com.bibleadventures.ui.screens.davidgoliath.slingpractice.DavidGoliathSlingPracticeScreen
import com.bibleadventures.ui.screens.esther.EstherViewModel
import com.bibleadventures.ui.screens.esther.corridor.EstherCorridorScreen
import com.bibleadventures.ui.screens.esther.courtyardstealth.EstherCourtyardStealthScreen
import com.bibleadventures.ui.screens.esther.decisionchoice.EstherDecisionChoiceScreen
import com.bibleadventures.ui.screens.esther.greetingchoice.EstherGreetingChoiceScreen
import com.bibleadventures.ui.screens.esther.intro.EstherIntroScreen
import com.bibleadventures.ui.screens.esther.lesson.EstherLessonScreen
import com.bibleadventures.ui.screens.esther.messengersudoku.EstherMessengerSudokuScreen
import com.bibleadventures.ui.screens.esther.reward.EstherRewardScreen
import com.bibleadventures.ui.screens.esther.royalattire.EstherRoyalAttireScreen
import com.bibleadventures.ui.screens.feeding5000.Feeding5000ViewModel
import com.bibleadventures.ui.screens.feeding5000.boysgift.Feeding5000BoysGiftScreen
import com.bibleadventures.ui.screens.feeding5000.catching.Feeding5000CatchingScreen
import com.bibleadventures.ui.screens.feeding5000.choice.Feeding5000ChoiceScreen
import com.bibleadventures.ui.screens.feeding5000.gatheringcrowd.Feeding5000GatheringCrowdScreen
import com.bibleadventures.ui.screens.feeding5000.intro.Feeding5000IntroScreen
import com.bibleadventures.ui.screens.feeding5000.lesson.Feeding5000LessonScreen
import com.bibleadventures.ui.screens.feeding5000.miraclemultiplication.Feeding5000MiracleMultiplicationScreen
import com.bibleadventures.ui.screens.feeding5000.reward.Feeding5000RewardScreen
import com.bibleadventures.ui.screens.feeding5000.searchingforfood.Feeding5000SearchingForFoodScreen
import com.bibleadventures.ui.screens.feeding5000.serving.Feeding5000ServingScreen
import com.bibleadventures.ui.screens.goodsamaritan.GoodSamaritanViewModel
import com.bibleadventures.ui.screens.goodsamaritan.explore.GoodSamaritanExploreScreen
import com.bibleadventures.ui.screens.goodsamaritan.intro.GoodSamaritanIntroScreen
import com.bibleadventures.ui.screens.goodsamaritan.lesson.GoodSamaritanLessonScreen
import com.bibleadventures.ui.screens.goodsamaritan.reward.GoodSamaritanRewardScreen
import com.bibleadventures.ui.screens.jericho.JerichoViewModel
import com.bibleadventures.ui.screens.jericho.blowshofar.JerichoBlowShofarScreen
import com.bibleadventures.ui.screens.jericho.choice.JerichoChoiceScreen
import com.bibleadventures.ui.screens.jericho.fastmarch.JerichoFastMarchScreen
import com.bibleadventures.ui.screens.jericho.intro.JerichoIntroScreen
import com.bibleadventures.ui.screens.jericho.lesson.JerichoLessonScreen
import com.bibleadventures.ui.screens.jericho.reward.JerichoRewardScreen
import com.bibleadventures.ui.screens.jericho.settingupcamp.JerichoSettingUpCampScreen
import com.bibleadventures.ui.screens.jericho.shout.JerichoShoutScreen
import com.bibleadventures.ui.screens.jericho.sixdaymarch.JerichoSixDayMarchScreen
import com.bibleadventures.ui.screens.jericho.spiesescape.JerichoSpiesEscapeScreen
import com.bibleadventures.ui.screens.jesuscalmsstorm.JesusCalmsStormChoiceScreen
import com.bibleadventures.ui.screens.jesuscalmsstorm.JesusCalmsStormIntroScreen
import com.bibleadventures.ui.screens.jesuscalmsstorm.JesusCalmsStormLessonScreen
import com.bibleadventures.ui.screens.jesuscalmsstorm.JesusCalmsStormRewardScreen
import com.bibleadventures.ui.screens.jesuscalmsstorm.JesusCalmsStormViewModel
import com.bibleadventures.ui.screens.jesuscalmsstorm.bailingtheboat.JesusCalmsStormBailingTheBoatScreen
import com.bibleadventures.ui.screens.jesuscalmsstorm.loadingtheboat.JesusCalmsStormLoadingTheBoatScreen
import com.bibleadventures.ui.screens.jesuscalmsstorm.peacebestill.JesusCalmsStormPeaceBeStillScreen
import com.bibleadventures.ui.screens.jesuscalmsstorm.reachingjesus.JesusCalmsStormReachingJesusScreen
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
import com.bibleadventures.ui.screens.settings.SettingsScreen
import com.bibleadventures.ui.screens.worldmap.WorldMapScreen

@Composable
fun BibleAdventuresNavHost(navController: NavHostController = rememberNavController()) {
    val comingSoonTitles = mapOf(
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
                        MenuItemId.SETTINGS -> navController.navigate(Destination.Settings.route)
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
        composable(Destination.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() })
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
                    } else if (chapterId == ChapterId.DANIEL) {
                        navController.navigate(Destination.Daniel.Intro.route)
                    } else if (chapterId == ChapterId.ESTHER) {
                        navController.navigate(Destination.Esther.Intro.route)
                    } else if (chapterId == ChapterId.JERICHO) {
                        navController.navigate(Destination.Jericho.Intro.route)
                    } else if (chapterId == ChapterId.FEEDING_5000) {
                        navController.navigate(Destination.Feeding5000.Intro.route)
                    } else if (chapterId == ChapterId.JESUS_CALMS_STORM) {
                        navController.navigate(Destination.JesusCalmsStorm.Intro.route)
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
        danielGraph(navController)
        estherGraph(navController)
        jerichoGraph(navController)
        feeding5000Graph(navController)
        jesusCalmsStormGraph(navController)
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
            val previouslyCompletedSceneIds by viewModel.previouslyCompletedSceneIds.collectAsStateWithLifecycle()
            NoahsArkFindAnimalsScreen(
                viewModel = viewModel,
                previouslyCompleted = "find_animals" in previouslyCompletedSceneIds,
                onContinue = {
                    viewModel.onSceneCompleted("find_animals")
                    navController.navigate(Destination.NoahsArk.AnimalMatching.route)
                },
            )
        }
        composable(Destination.NoahsArk.AnimalMatching.route) { entry ->
            val viewModel = navController.noahsArkViewModel(entry)
            val previouslyCompletedSceneIds by viewModel.previouslyCompletedSceneIds.collectAsStateWithLifecycle()
            NoahsArkMatchingScreen(
                viewModel = viewModel,
                previouslyCompleted = "animal_matching" in previouslyCompletedSceneIds,
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
            val previouslyCompletedSceneIds by viewModel.previouslyCompletedSceneIds.collectAsStateWithLifecycle()
            NoahsArkOrganizeArkScreen(
                viewModel = viewModel,
                previouslyCompleted = "organize_ark" in previouslyCompletedSceneIds,
                onContinue = {
                    viewModel.onSceneCompleted("organize_ark")
                    navController.navigate(Destination.NoahsArk.FindMissingItems.route)
                },
            )
        }
        composable(Destination.NoahsArk.FindMissingItems.route) { entry ->
            val viewModel = navController.noahsArkViewModel(entry)
            val previouslyCompletedSceneIds by viewModel.previouslyCompletedSceneIds.collectAsStateWithLifecycle()
            NoahsArkMissingItemsScreen(
                viewModel = viewModel,
                previouslyCompleted = "find_missing_items" in previouslyCompletedSceneIds,
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
            val previouslyCompletedSceneIds by viewModel.previouslyCompletedSceneIds.collectAsStateWithLifecycle()
            DavidGoliathSheepCountingScreen(
                viewModel = viewModel,
                previouslyCompleted = "sheep_counting" in previouslyCompletedSceneIds,
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
            val previouslyCompletedSceneIds by viewModel.previouslyCompletedSceneIds.collectAsStateWithLifecycle()
            DavidGoliathChooseStonesScreen(
                viewModel = viewModel,
                previouslyCompleted = "choose_stones" in previouslyCompletedSceneIds,
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
            val previouslyCompletedSceneIds by viewModel.previouslyCompletedSceneIds.collectAsStateWithLifecycle()
            DavidGoliathDodgeScreen(
                viewModel = viewModel,
                previouslyCompleted = "dodge" in previouslyCompletedSceneIds,
                onContinue = {
                    viewModel.onSceneCompleted("dodge")
                    navController.navigate(Destination.DavidGoliath.SlingPractice.route)
                },
            )
        }
        composable(Destination.DavidGoliath.SlingPractice.route) { entry ->
            val viewModel = navController.davidGoliathViewModel(entry)
            val previouslyCompletedSceneIds by viewModel.previouslyCompletedSceneIds.collectAsStateWithLifecycle()
            DavidGoliathSlingPracticeScreen(
                viewModel = viewModel,
                previouslyCompleted = "sling_practice" in previouslyCompletedSceneIds,
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
            val previouslyCompletedSceneIds by viewModel.previouslyCompletedSceneIds.collectAsStateWithLifecycle()
            GoodSamaritanExploreScreen(
                viewModel = viewModel,
                previouslyCompleted = "explore" in previouslyCompletedSceneIds,
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

private fun NavGraphBuilder.danielGraph(navController: NavHostController) {
    navigation(
        startDestination = Destination.Daniel.Intro.route,
        route = Destination.Daniel.GRAPH_ROUTE,
    ) {
        composable(Destination.Daniel.Intro.route) { entry ->
            val viewModel = navController.danielViewModel(entry)
            DanielIntroScreen(
                viewModel = viewModel,
                onContinue = {
                    viewModel.onSceneCompleted("intro")
                    navController.navigate(Destination.Daniel.StealthContext.route)
                },
            )
        }
        composable(Destination.Daniel.StealthContext.route) {
            StoryBeatScreen(
                titleRes = R.string.daniel_stealth_context_title,
                lineRes = DanielContent.stealthContextLines,
                onContinue = { navController.navigate(Destination.Daniel.Stealth.route) },
            )
        }
        composable(Destination.Daniel.Stealth.route) { entry ->
            val viewModel = navController.danielViewModel(entry)
            val previouslyCompletedSceneIds by viewModel.previouslyCompletedSceneIds.collectAsStateWithLifecycle()
            DanielStealthScreen(
                viewModel = viewModel,
                previouslyCompleted = "stealth" in previouslyCompletedSceneIds,
                onContinue = {
                    viewModel.onSceneCompleted("stealth")
                    navController.navigate(Destination.Daniel.Choice.route)
                },
            )
        }
        composable(Destination.Daniel.Choice.route) { entry ->
            val viewModel = navController.danielViewModel(entry)
            DanielChoiceScreen(
                viewModel = viewModel,
                onContinue = {
                    viewModel.onSceneCompleted("choice")
                    navController.navigate(Destination.Daniel.LionsDenContext.route)
                },
            )
        }
        composable(Destination.Daniel.LionsDenContext.route) {
            StoryBeatScreen(
                titleRes = R.string.daniel_lions_den_context_title,
                lineRes = DanielContent.lionsDenContextLines,
                onContinue = { navController.navigate(Destination.Daniel.LionsDen.route) },
            )
        }
        composable(Destination.Daniel.LionsDen.route) { entry ->
            val viewModel = navController.danielViewModel(entry)
            val previouslyCompletedSceneIds by viewModel.previouslyCompletedSceneIds.collectAsStateWithLifecycle()
            DanielLionsDenScreen(
                viewModel = viewModel,
                previouslyCompleted = "lions_den" in previouslyCompletedSceneIds,
                onContinue = {
                    viewModel.onSceneCompleted("lions_den")
                    navController.navigate(Destination.Daniel.DariusContext.route)
                },
            )
        }
        composable(Destination.Daniel.DariusContext.route) {
            StoryBeatScreen(
                titleRes = R.string.daniel_darius_context_title,
                lineRes = DanielContent.dariusContextLines,
                onContinue = { navController.navigate(Destination.Daniel.DariusMaze.route) },
            )
        }
        composable(Destination.Daniel.DariusMaze.route) { entry ->
            val viewModel = navController.danielViewModel(entry)
            val previouslyCompletedSceneIds by viewModel.previouslyCompletedSceneIds.collectAsStateWithLifecycle()
            DanielDariusMazeScreen(
                viewModel = viewModel,
                previouslyCompleted = "darius_maze" in previouslyCompletedSceneIds,
                onContinue = {
                    viewModel.onSceneCompleted("darius_maze")
                    navController.navigate(Destination.Daniel.Lesson.route)
                },
            )
        }
        composable(Destination.Daniel.Lesson.route) { entry ->
            val viewModel = navController.danielViewModel(entry)
            DanielLessonScreen(
                onContinue = {
                    viewModel.onSceneCompleted("lesson")
                    navController.navigate(Destination.Daniel.Reward.route)
                },
            )
        }
        composable(Destination.Daniel.Reward.route) { entry ->
            DanielRewardScreen(
                viewModel = navController.danielViewModel(entry),
                onReturnToMap = {
                    // Clears the whole Daniel back stack so Back from the map can't
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
private fun NavHostController.danielViewModel(entry: NavBackStackEntry): DanielViewModel {
    val parentEntry = remember(entry) { getBackStackEntry(Destination.Daniel.GRAPH_ROUTE) }
    return viewModel(viewModelStoreOwner = parentEntry, factory = AppViewModelProvider.Factory)
}

private fun NavGraphBuilder.estherGraph(navController: NavHostController) {
    navigation(
        startDestination = Destination.Esther.Intro.route,
        route = Destination.Esther.GRAPH_ROUTE,
    ) {
        composable(Destination.Esther.Intro.route) { entry ->
            val viewModel = navController.estherViewModel(entry)
            EstherIntroScreen(
                viewModel = viewModel,
                onContinue = {
                    viewModel.onSceneCompleted("intro")
                    navController.navigate(Destination.Esther.SearchContext.route)
                },
            )
        }
        composable(Destination.Esther.SearchContext.route) {
            StoryBeatScreen(
                titleRes = R.string.esther_new_queen_search_context_title,
                lineRes = EstherContent.searchContextLines,
                onContinue = { navController.navigate(Destination.Esther.RoyalAttire.route) },
            )
        }
        composable(Destination.Esther.RoyalAttire.route) { entry ->
            val viewModel = navController.estherViewModel(entry)
            val previouslyCompletedSceneIds by viewModel.previouslyCompletedSceneIds.collectAsStateWithLifecycle()
            EstherRoyalAttireScreen(
                viewModel = viewModel,
                previouslyCompleted = "royal_attire" in previouslyCompletedSceneIds,
                onContinue = {
                    viewModel.onSceneCompleted("royal_attire")
                    navController.navigate(Destination.Esther.CrownedContext.route)
                },
            )
        }
        composable(Destination.Esther.CrownedContext.route) {
            StoryBeatScreen(
                titleRes = R.string.esther_new_queen_crowned_context_title,
                lineRes = EstherContent.crownedContextLines,
                onContinue = { navController.navigate(Destination.Esther.GreetingChoice.route) },
            )
        }
        composable(Destination.Esther.GreetingChoice.route) { entry ->
            val viewModel = navController.estherViewModel(entry)
            EstherGreetingChoiceScreen(
                viewModel = viewModel,
                onContinue = {
                    viewModel.onSceneCompleted("greeting_choice")
                    navController.navigate(Destination.Esther.DangerContext.route)
                },
            )
        }
        composable(Destination.Esther.DangerContext.route) {
            StoryBeatScreen(
                titleRes = R.string.esther_secret_plot_danger_context_title,
                lineRes = EstherContent.dangerContextLines,
                onContinue = { navController.navigate(Destination.Esther.CourtyardStealth.route) },
            )
        }
        composable(Destination.Esther.CourtyardStealth.route) { entry ->
            val viewModel = navController.estherViewModel(entry)
            val previouslyCompletedSceneIds by viewModel.previouslyCompletedSceneIds.collectAsStateWithLifecycle()
            EstherCourtyardStealthScreen(
                viewModel = viewModel,
                previouslyCompleted = "courtyard_stealth" in previouslyCompletedSceneIds,
                onContinue = {
                    viewModel.onSceneCompleted("courtyard_stealth")
                    navController.navigate(Destination.Esther.WarnedContext.route)
                },
            )
        }
        composable(Destination.Esther.WarnedContext.route) {
            StoryBeatScreen(
                titleRes = R.string.esther_secret_plot_warned_context_title,
                lineRes = EstherContent.warnedContextLines,
                onContinue = { navController.navigate(Destination.Esther.DecreeContext.route) },
            )
        }
        composable(Destination.Esther.DecreeContext.route) {
            StoryBeatScreen(
                titleRes = R.string.esther_threat_decree_context_title,
                lineRes = EstherContent.decreeContextLines,
                onContinue = { navController.navigate(Destination.Esther.MessengerSudoku.route) },
            )
        }
        composable(Destination.Esther.MessengerSudoku.route) { entry ->
            val viewModel = navController.estherViewModel(entry)
            val previouslyCompletedSceneIds by viewModel.previouslyCompletedSceneIds.collectAsStateWithLifecycle()
            EstherMessengerSudokuScreen(
                viewModel = viewModel,
                previouslyCompleted = "messenger_sudoku" in previouslyCompletedSceneIds,
                onContinue = {
                    viewModel.onSceneCompleted("messenger_sudoku")
                    navController.navigate(Destination.Esther.MourningContext.route)
                },
            )
        }
        composable(Destination.Esther.MourningContext.route) {
            StoryBeatScreen(
                titleRes = R.string.esther_threat_mourning_context_title,
                lineRes = EstherContent.mourningContextLines,
                onContinue = { navController.navigate(Destination.Esther.DecisionChoice.route) },
            )
        }
        composable(Destination.Esther.DecisionChoice.route) { entry ->
            val viewModel = navController.estherViewModel(entry)
            EstherDecisionChoiceScreen(
                viewModel = viewModel,
                onContinue = {
                    viewModel.onSceneCompleted("decision_choice")
                    navController.navigate(Destination.Esther.FastingContext.route)
                },
            )
        }
        composable(Destination.Esther.FastingContext.route) {
            StoryBeatScreen(
                titleRes = R.string.esther_brave_approach_fasting_context_title,
                lineRes = EstherContent.fastingContextLines,
                onContinue = { navController.navigate(Destination.Esther.Corridor.route) },
            )
        }
        composable(Destination.Esther.Corridor.route) { entry ->
            val viewModel = navController.estherViewModel(entry)
            val previouslyCompletedSceneIds by viewModel.previouslyCompletedSceneIds.collectAsStateWithLifecycle()
            EstherCorridorScreen(
                viewModel = viewModel,
                previouslyCompleted = "corridor" in previouslyCompletedSceneIds,
                onContinue = {
                    viewModel.onSceneCompleted("corridor")
                    navController.navigate(Destination.Esther.Lesson.route)
                },
            )
        }
        composable(Destination.Esther.Lesson.route) { entry ->
            val viewModel = navController.estherViewModel(entry)
            EstherLessonScreen(
                onContinue = {
                    viewModel.onSceneCompleted("lesson")
                    navController.navigate(Destination.Esther.Reward.route)
                },
            )
        }
        composable(Destination.Esther.Reward.route) { entry ->
            EstherRewardScreen(
                viewModel = navController.estherViewModel(entry),
                onReturnToMap = {
                    // Clears the whole chapter back stack so Back from the map can't
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
private fun NavHostController.estherViewModel(entry: NavBackStackEntry): EstherViewModel {
    val parentEntry = remember(entry) { getBackStackEntry(Destination.Esther.GRAPH_ROUTE) }
    return viewModel(viewModelStoreOwner = parentEntry, factory = AppViewModelProvider.Factory)
}

private fun NavGraphBuilder.jerichoGraph(navController: NavHostController) {
    navigation(
        startDestination = Destination.Jericho.Intro.route,
        route = Destination.Jericho.GRAPH_ROUTE,
    ) {
        composable(Destination.Jericho.Intro.route) { entry ->
            val viewModel = navController.jerichoViewModel(entry)
            JerichoIntroScreen(
                viewModel = viewModel,
                onContinue = {
                    viewModel.onSceneCompleted("intro")
                    navController.navigate(Destination.Jericho.RahabHouseContext.route)
                },
            )
        }
        composable(Destination.Jericho.RahabHouseContext.route) {
            StoryBeatScreen(
                titleRes = R.string.jericho_rahab_house_context_title,
                lineRes = JerichoContent.rahabHouseContextLines,
                onContinue = { navController.navigate(Destination.Jericho.RahabHelping.route) },
            )
        }
        composable(Destination.Jericho.RahabHelping.route) { entry ->
            val viewModel = navController.jerichoViewModel(entry)
            StoryBeatScreen(
                titleRes = R.string.jericho_rahab_helping_title,
                lineRes = JerichoContent.rahabHelpingLines,
                onContinue = {
                    viewModel.onSceneCompleted("rahab_helping")
                    navController.navigate(Destination.Jericho.SpiesEscape.route)
                },
            )
        }
        composable(Destination.Jericho.SpiesEscape.route) { entry ->
            val viewModel = navController.jerichoViewModel(entry)
            val previouslyCompletedSceneIds by viewModel.previouslyCompletedSceneIds.collectAsStateWithLifecycle()
            JerichoSpiesEscapeScreen(
                viewModel = viewModel,
                previouslyCompleted = "spies_escape" in previouslyCompletedSceneIds,
                onContinue = {
                    viewModel.onSceneCompleted("spies_escape")
                    navController.navigate(Destination.Jericho.SpiesEscapedContext.route)
                },
            )
        }
        composable(Destination.Jericho.SpiesEscapedContext.route) {
            StoryBeatScreen(
                titleRes = R.string.jericho_spies_escaped_context_title,
                lineRes = JerichoContent.spiesEscapedContextLines,
                onContinue = { navController.navigate(Destination.Jericho.Choice.route) },
            )
        }
        composable(Destination.Jericho.Choice.route) { entry ->
            val viewModel = navController.jerichoViewModel(entry)
            JerichoChoiceScreen(
                viewModel = viewModel,
                onContinue = {
                    viewModel.onSceneCompleted("choice")
                    navController.navigate(Destination.Jericho.CampContext.route)
                },
            )
        }
        composable(Destination.Jericho.CampContext.route) {
            StoryBeatScreen(
                titleRes = R.string.jericho_camp_context_title,
                lineRes = JerichoContent.campContextLines,
                onContinue = { navController.navigate(Destination.Jericho.SettingUpCamp.route) },
            )
        }
        composable(Destination.Jericho.SettingUpCamp.route) { entry ->
            val viewModel = navController.jerichoViewModel(entry)
            val previouslyCompletedSceneIds by viewModel.previouslyCompletedSceneIds.collectAsStateWithLifecycle()
            JerichoSettingUpCampScreen(
                viewModel = viewModel,
                previouslyCompleted = "setting_up_camp" in previouslyCompletedSceneIds,
                onContinue = {
                    viewModel.onSceneCompleted("setting_up_camp")
                    navController.navigate(Destination.Jericho.TentsContext.route)
                },
            )
        }
        composable(Destination.Jericho.TentsContext.route) {
            StoryBeatScreen(
                titleRes = R.string.jericho_tents_context_title,
                lineRes = JerichoContent.tentsContextLines,
                onContinue = { navController.navigate(Destination.Jericho.WallsContext.route) },
            )
        }
        composable(Destination.Jericho.WallsContext.route) {
            StoryBeatScreen(
                titleRes = R.string.jericho_walls_context_title,
                lineRes = JerichoContent.wallsContextLines,
                onContinue = { navController.navigate(Destination.Jericho.SixDayMarch.route) },
            )
        }
        composable(Destination.Jericho.SixDayMarch.route) { entry ->
            val viewModel = navController.jerichoViewModel(entry)
            val previouslyCompletedSceneIds by viewModel.previouslyCompletedSceneIds.collectAsStateWithLifecycle()
            JerichoSixDayMarchScreen(
                viewModel = viewModel,
                previouslyCompleted = "six_day_march" in previouslyCompletedSceneIds,
                onContinue = {
                    viewModel.onSceneCompleted("six_day_march")
                    navController.navigate(Destination.Jericho.SeventhDayContext.route)
                },
            )
        }
        composable(Destination.Jericho.SeventhDayContext.route) {
            StoryBeatScreen(
                titleRes = R.string.jericho_seventh_day_context_title,
                lineRes = JerichoContent.seventhDayContextLines,
                onContinue = { navController.navigate(Destination.Jericho.FastMarch.route) },
            )
        }
        composable(Destination.Jericho.FastMarch.route) { entry ->
            val viewModel = navController.jerichoViewModel(entry)
            val previouslyCompletedSceneIds by viewModel.previouslyCompletedSceneIds.collectAsStateWithLifecycle()
            JerichoFastMarchScreen(
                viewModel = viewModel,
                previouslyCompleted = "fast_march" in previouslyCompletedSceneIds,
                onContinue = {
                    viewModel.onSceneCompleted("fast_march")
                    navController.navigate(Destination.Jericho.BlowShofar.route)
                },
            )
        }
        composable(Destination.Jericho.BlowShofar.route) { entry ->
            val viewModel = navController.jerichoViewModel(entry)
            val previouslyCompletedSceneIds by viewModel.previouslyCompletedSceneIds.collectAsStateWithLifecycle()
            JerichoBlowShofarScreen(
                viewModel = viewModel,
                previouslyCompleted = "blow_shofar" in previouslyCompletedSceneIds,
                onContinue = {
                    viewModel.onSceneCompleted("blow_shofar")
                    navController.navigate(Destination.Jericho.Shout.route)
                },
            )
        }
        composable(Destination.Jericho.Shout.route) { entry ->
            val viewModel = navController.jerichoViewModel(entry)
            val previouslyCompletedSceneIds by viewModel.previouslyCompletedSceneIds.collectAsStateWithLifecycle()
            JerichoShoutScreen(
                viewModel = viewModel,
                previouslyCompleted = "shout" in previouslyCompletedSceneIds,
                onContinue = {
                    viewModel.onSceneCompleted("shout")
                    navController.navigate(Destination.Jericho.RahabSavedContext.route)
                },
            )
        }
        composable(Destination.Jericho.RahabSavedContext.route) {
            StoryBeatScreen(
                titleRes = R.string.jericho_rahab_saved_context_title,
                lineRes = JerichoContent.rahabSavedContextLines,
                onContinue = { navController.navigate(Destination.Jericho.Lesson.route) },
            )
        }
        composable(Destination.Jericho.Lesson.route) { entry ->
            val viewModel = navController.jerichoViewModel(entry)
            JerichoLessonScreen(
                onContinue = {
                    viewModel.onSceneCompleted("lesson")
                    navController.navigate(Destination.Jericho.Reward.route)
                },
            )
        }
        composable(Destination.Jericho.Reward.route) { entry ->
            JerichoRewardScreen(
                viewModel = navController.jerichoViewModel(entry),
                onReturnToMap = {
                    // Clears the whole Jericho back stack so Back from the map can't
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
private fun NavHostController.jerichoViewModel(entry: NavBackStackEntry): JerichoViewModel {
    val parentEntry = remember(entry) { getBackStackEntry(Destination.Jericho.GRAPH_ROUTE) }
    return viewModel(viewModelStoreOwner = parentEntry, factory = AppViewModelProvider.Factory)
}

private fun NavGraphBuilder.feeding5000Graph(navController: NavHostController) {
    navigation(
        startDestination = Destination.Feeding5000.Intro.route,
        route = Destination.Feeding5000.GRAPH_ROUTE,
    ) {
        composable(Destination.Feeding5000.Intro.route) { entry ->
            val viewModel = navController.feeding5000ViewModel(entry)
            Feeding5000IntroScreen(
                viewModel = viewModel,
                onContinue = {
                    viewModel.onSceneCompleted("intro")
                    navController.navigate(Destination.Feeding5000.CrowdContext.route)
                },
            )
        }
        composable(Destination.Feeding5000.CrowdContext.route) {
            StoryBeatScreen(
                titleRes = R.string.feeding_5000_crowd_context_title,
                lineRes = Feeding5000Content.crowdContextLines,
                onContinue = { navController.navigate(Destination.Feeding5000.GatheringCrowd.route) },
            )
        }
        composable(Destination.Feeding5000.GatheringCrowd.route) { entry ->
            val viewModel = navController.feeding5000ViewModel(entry)
            val previouslyCompletedSceneIds by viewModel.previouslyCompletedSceneIds.collectAsStateWithLifecycle()
            Feeding5000GatheringCrowdScreen(
                viewModel = viewModel,
                previouslyCompleted = "gathering_crowd" in previouslyCompletedSceneIds,
                onContinue = {
                    viewModel.onSceneCompleted("gathering_crowd")
                    navController.navigate(Destination.Feeding5000.SearchingContext.route)
                },
            )
        }
        composable(Destination.Feeding5000.SearchingContext.route) {
            StoryBeatScreen(
                titleRes = R.string.feeding_5000_searching_context_title,
                lineRes = Feeding5000Content.searchingContextLines,
                onContinue = { navController.navigate(Destination.Feeding5000.SearchingForFood.route) },
            )
        }
        composable(Destination.Feeding5000.SearchingForFood.route) { entry ->
            val viewModel = navController.feeding5000ViewModel(entry)
            val previouslyCompletedSceneIds by viewModel.previouslyCompletedSceneIds.collectAsStateWithLifecycle()
            Feeding5000SearchingForFoodScreen(
                viewModel = viewModel,
                previouslyCompleted = "searching_for_food" in previouslyCompletedSceneIds,
                onContinue = {
                    viewModel.onSceneCompleted("searching_for_food")
                    navController.navigate(Destination.Feeding5000.BoysGiftContext.route)
                },
            )
        }
        composable(Destination.Feeding5000.BoysGiftContext.route) {
            StoryBeatScreen(
                titleRes = R.string.feeding_5000_boys_gift_context_title,
                lineRes = Feeding5000Content.boysGiftContextLines,
                onContinue = { navController.navigate(Destination.Feeding5000.BoysGift.route) },
            )
        }
        composable(Destination.Feeding5000.BoysGift.route) { entry ->
            val viewModel = navController.feeding5000ViewModel(entry)
            val previouslyCompletedSceneIds by viewModel.previouslyCompletedSceneIds.collectAsStateWithLifecycle()
            Feeding5000BoysGiftScreen(
                viewModel = viewModel,
                previouslyCompleted = "boys_gift" in previouslyCompletedSceneIds,
                onContinue = {
                    viewModel.onSceneCompleted("boys_gift")
                    navController.navigate(Destination.Feeding5000.Choice.route)
                },
            )
        }
        composable(Destination.Feeding5000.Choice.route) { entry ->
            val viewModel = navController.feeding5000ViewModel(entry)
            Feeding5000ChoiceScreen(
                viewModel = viewModel,
                onContinue = {
                    viewModel.onSceneCompleted("choice")
                    navController.navigate(Destination.Feeding5000.MiracleContext.route)
                },
            )
        }
        composable(Destination.Feeding5000.MiracleContext.route) {
            StoryBeatScreen(
                titleRes = R.string.feeding_5000_miracle_context_title,
                lineRes = Feeding5000Content.miracleContextLines,
                onContinue = { navController.navigate(Destination.Feeding5000.MiracleMultiplication.route) },
            )
        }
        composable(Destination.Feeding5000.MiracleMultiplication.route) { entry ->
            val viewModel = navController.feeding5000ViewModel(entry)
            val previouslyCompletedSceneIds by viewModel.previouslyCompletedSceneIds.collectAsStateWithLifecycle()
            Feeding5000MiracleMultiplicationScreen(
                viewModel = viewModel,
                previouslyCompleted = "miracle_multiplication" in previouslyCompletedSceneIds,
                onContinue = {
                    viewModel.onSceneCompleted("miracle_multiplication")
                    navController.navigate(Destination.Feeding5000.FeastContext.route)
                },
            )
        }
        composable(Destination.Feeding5000.FeastContext.route) {
            StoryBeatScreen(
                titleRes = R.string.feeding_5000_feast_context_title,
                lineRes = Feeding5000Content.feastContextLines,
                onContinue = { navController.navigate(Destination.Feeding5000.Serving.route) },
            )
        }
        composable(Destination.Feeding5000.Serving.route) { entry ->
            val viewModel = navController.feeding5000ViewModel(entry)
            val previouslyCompletedSceneIds by viewModel.previouslyCompletedSceneIds.collectAsStateWithLifecycle()
            Feeding5000ServingScreen(
                viewModel = viewModel,
                previouslyCompleted = "serving" in previouslyCompletedSceneIds,
                onContinue = {
                    viewModel.onSceneCompleted("serving")
                    navController.navigate(Destination.Feeding5000.Catching.route)
                },
            )
        }
        composable(Destination.Feeding5000.Catching.route) { entry ->
            val viewModel = navController.feeding5000ViewModel(entry)
            val previouslyCompletedSceneIds by viewModel.previouslyCompletedSceneIds.collectAsStateWithLifecycle()
            Feeding5000CatchingScreen(
                viewModel = viewModel,
                previouslyCompleted = "catching" in previouslyCompletedSceneIds,
                onContinue = {
                    viewModel.onSceneCompleted("catching")
                    navController.navigate(Destination.Feeding5000.Lesson.route)
                },
            )
        }
        composable(Destination.Feeding5000.Lesson.route) { entry ->
            val viewModel = navController.feeding5000ViewModel(entry)
            Feeding5000LessonScreen(
                onContinue = {
                    viewModel.onSceneCompleted("lesson")
                    navController.navigate(Destination.Feeding5000.Reward.route)
                },
            )
        }
        composable(Destination.Feeding5000.Reward.route) { entry ->
            Feeding5000RewardScreen(
                viewModel = navController.feeding5000ViewModel(entry),
                onReturnToMap = {
                    // Clears the whole Feeding5000 back stack so Back from the map can't
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

private fun NavGraphBuilder.jesusCalmsStormGraph(navController: NavHostController) {
    navigation(
        startDestination = Destination.JesusCalmsStorm.Intro.route,
        route = Destination.JesusCalmsStorm.GRAPH_ROUTE,
    ) {
        composable(Destination.JesusCalmsStorm.Intro.route) { entry ->
            val viewModel = navController.jesusCalmsStormViewModel(entry)
            JesusCalmsStormIntroScreen(
                viewModel = viewModel,
                onContinue = {
                    viewModel.onSceneCompleted("intro")
                    navController.navigate(Destination.JesusCalmsStorm.LoadingContext.route)
                },
            )
        }
        composable(Destination.JesusCalmsStorm.LoadingContext.route) {
            StoryBeatScreen(
                titleRes = R.string.jesus_calms_storm_loading_context_title,
                lineRes = JesusCalmsStormContent.loadingContextLines,
                onContinue = { navController.navigate(Destination.JesusCalmsStorm.LoadingTheBoat.route) },
            )
        }
        composable(Destination.JesusCalmsStorm.LoadingTheBoat.route) { entry ->
            val viewModel = navController.jesusCalmsStormViewModel(entry)
            val previouslyCompletedSceneIds by viewModel.previouslyCompletedSceneIds.collectAsStateWithLifecycle()
            JesusCalmsStormLoadingTheBoatScreen(
                viewModel = viewModel,
                previouslyCompleted = "loading_the_boat" in previouslyCompletedSceneIds,
                onContinue = {
                    viewModel.onSceneCompleted("loading_the_boat")
                    navController.navigate(Destination.JesusCalmsStorm.StormContext.route)
                },
            )
        }
        composable(Destination.JesusCalmsStorm.StormContext.route) {
            StoryBeatScreen(
                titleRes = R.string.jesus_calms_storm_storm_context_title,
                lineRes = JesusCalmsStormContent.stormContextLines,
                onContinue = { navController.navigate(Destination.JesusCalmsStorm.BailingTheBoat.route) },
            )
        }
        composable(Destination.JesusCalmsStorm.BailingTheBoat.route) { entry ->
            val viewModel = navController.jesusCalmsStormViewModel(entry)
            val previouslyCompletedSceneIds by viewModel.previouslyCompletedSceneIds.collectAsStateWithLifecycle()
            JesusCalmsStormBailingTheBoatScreen(
                viewModel = viewModel,
                previouslyCompleted = "bailing_the_boat" in previouslyCompletedSceneIds,
                onContinue = {
                    viewModel.onSceneCompleted("bailing_the_boat")
                    navController.navigate(Destination.JesusCalmsStorm.Choice.route)
                },
            )
        }
        composable(Destination.JesusCalmsStorm.Choice.route) { entry ->
            val viewModel = navController.jesusCalmsStormViewModel(entry)
            JesusCalmsStormChoiceScreen(
                viewModel = viewModel,
                onContinue = {
                    viewModel.onSceneCompleted("choice")
                    navController.navigate(Destination.JesusCalmsStorm.FindJesusContext.route)
                },
            )
        }
        composable(Destination.JesusCalmsStorm.FindJesusContext.route) {
            StoryBeatScreen(
                titleRes = R.string.jesus_calms_storm_findjesus_context_title,
                lineRes = JesusCalmsStormContent.findJesusContextLines,
                onContinue = { navController.navigate(Destination.JesusCalmsStorm.ReachingJesus.route) },
            )
        }
        composable(Destination.JesusCalmsStorm.ReachingJesus.route) { entry ->
            val viewModel = navController.jesusCalmsStormViewModel(entry)
            val previouslyCompletedSceneIds by viewModel.previouslyCompletedSceneIds.collectAsStateWithLifecycle()
            JesusCalmsStormReachingJesusScreen(
                viewModel = viewModel,
                previouslyCompleted = "reaching_jesus" in previouslyCompletedSceneIds,
                onContinue = {
                    viewModel.onSceneCompleted("reaching_jesus")
                    navController.navigate(Destination.JesusCalmsStorm.CalmContext.route)
                },
            )
        }
        composable(Destination.JesusCalmsStorm.CalmContext.route) {
            StoryBeatScreen(
                titleRes = R.string.jesus_calms_storm_calm_context_title,
                lineRes = JesusCalmsStormContent.calmContextLines,
                onContinue = { navController.navigate(Destination.JesusCalmsStorm.PeaceBeStill.route) },
            )
        }
        composable(Destination.JesusCalmsStorm.PeaceBeStill.route) { entry ->
            val viewModel = navController.jesusCalmsStormViewModel(entry)
            val previouslyCompletedSceneIds by viewModel.previouslyCompletedSceneIds.collectAsStateWithLifecycle()
            JesusCalmsStormPeaceBeStillScreen(
                viewModel = viewModel,
                previouslyCompleted = "peace_be_still" in previouslyCompletedSceneIds,
                onContinue = {
                    viewModel.onSceneCompleted("peace_be_still")
                    navController.navigate(Destination.JesusCalmsStorm.Lesson.route)
                },
            )
        }
        composable(Destination.JesusCalmsStorm.Lesson.route) { entry ->
            val viewModel = navController.jesusCalmsStormViewModel(entry)
            JesusCalmsStormLessonScreen(
                onContinue = {
                    viewModel.onSceneCompleted("lesson")
                    navController.navigate(Destination.JesusCalmsStorm.Reward.route)
                },
            )
        }
        composable(Destination.JesusCalmsStorm.Reward.route) { entry ->
            JesusCalmsStormRewardScreen(
                viewModel = navController.jesusCalmsStormViewModel(entry),
                onReturnToMap = {
                    // Clears the whole Jesus Calms the Storm back stack so Back from the map
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
private fun NavHostController.jesusCalmsStormViewModel(entry: NavBackStackEntry): JesusCalmsStormViewModel {
    val parentEntry = remember(entry) { getBackStackEntry(Destination.JesusCalmsStorm.GRAPH_ROUTE) }
    return viewModel(viewModelStoreOwner = parentEntry, factory = AppViewModelProvider.Factory)
}

@Composable
private fun NavHostController.feeding5000ViewModel(entry: NavBackStackEntry): Feeding5000ViewModel {
    val parentEntry = remember(entry) { getBackStackEntry(Destination.Feeding5000.GRAPH_ROUTE) }
    return viewModel(viewModelStoreOwner = parentEntry, factory = AppViewModelProvider.Factory)
}
