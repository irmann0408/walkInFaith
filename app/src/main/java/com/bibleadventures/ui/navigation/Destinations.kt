package com.bibleadventures.ui.navigation

/**
 * Single source of truth for navigation routes. Screens should never
 * construct route strings themselves — this keeps navigation logic out
 * of the UI layer as the game grows past the MVP.
 */
sealed class Destination(val route: String) {
    data object MainMenu : Destination("main_menu")
    data object Character : Destination("character")
    data object WorldMap : Destination("world_map")
    data object Badges : Destination("badges")
    data object ScriptureCards : Destination("scripture_cards")
    data object Settings : Destination("settings")

    /** Nested graph — every scene shares one graph-scoped `NoahsArkViewModel`. */
    sealed class NoahsArk(route: String) : Destination(route) {
        data object Intro : NoahsArk("noahs_ark/intro")
        data object FindAnimalsContext : NoahsArk("noahs_ark/find_animals_context")
        data object FindAnimals : NoahsArk("noahs_ark/find_animals")
        data object AnimalMatching : NoahsArk("noahs_ark/animal_matching")
        data object OrganizeArkContext : NoahsArk("noahs_ark/organize_ark_context")
        data object OrganizeArk : NoahsArk("noahs_ark/organize_ark")
        data object FindMissingItems : NoahsArk("noahs_ark/find_missing_items")
        data object Lesson : NoahsArk("noahs_ark/lesson")
        data object Reward : NoahsArk("noahs_ark/reward")

        companion object {
            const val GRAPH_ROUTE = "noahs_ark"
        }
    }

    /** Nested graph — every scene shares one graph-scoped `DavidGoliathViewModel`. */
    sealed class DavidGoliath(route: String) : Destination(route) {
        data object Intro : DavidGoliath("david_goliath/intro")
        data object SheepCountingContext : DavidGoliath("david_goliath/sheep_counting_context")
        data object SheepCounting : DavidGoliath("david_goliath/sheep_counting")
        data object ChooseStonesContext : DavidGoliath("david_goliath/choose_stones_context")
        data object ChooseStones : DavidGoliath("david_goliath/choose_stones")
        data object SlingPracticeContext : DavidGoliath("david_goliath/sling_practice_context")
        data object Choice : DavidGoliath("david_goliath/choice")
        data object DodgeContext : DavidGoliath("david_goliath/dodge_context")
        data object Dodge : DavidGoliath("david_goliath/dodge")
        data object SlingPractice : DavidGoliath("david_goliath/sling_practice")
        data object Lesson : DavidGoliath("david_goliath/lesson")
        data object Reward : DavidGoliath("david_goliath/reward")

        companion object {
            const val GRAPH_ROUTE = "david_goliath"
        }
    }

    /** Nested graph — every scene shares one graph-scoped `GoodSamaritanViewModel`. */
    sealed class GoodSamaritan(route: String) : Destination(route) {
        data object Intro : GoodSamaritan("good_samaritan/intro")
        data object ExploreContext : GoodSamaritan("good_samaritan/explore_context")
        data object Explore : GoodSamaritan("good_samaritan/explore")
        data object Lesson : GoodSamaritan("good_samaritan/lesson")
        data object Reward : GoodSamaritan("good_samaritan/reward")

        companion object {
            const val GRAPH_ROUTE = "good_samaritan"
        }
    }

    /** Nested graph — every scene shares one graph-scoped `DanielViewModel`. */
    sealed class Daniel(route: String) : Destination(route) {
        data object Intro : Daniel("daniel/intro")
        data object StealthContext : Daniel("daniel/stealth_context")
        data object Stealth : Daniel("daniel/stealth")
        data object Choice : Daniel("daniel/choice")
        data object LionsDenContext : Daniel("daniel/lions_den_context")
        data object LionsDen : Daniel("daniel/lions_den")
        data object DariusContext : Daniel("daniel/darius_context")
        data object DariusMaze : Daniel("daniel/darius_maze")
        data object Lesson : Daniel("daniel/lesson")
        data object Reward : Daniel("daniel/reward")

        companion object {
            const val GRAPH_ROUTE = "daniel"
        }
    }

    /** Nested graph — every scene shares one graph-scoped `EstherNewQueenViewModel`. */
    sealed class EstherNewQueen(route: String) : Destination(route) {
        data object Intro : EstherNewQueen("esther_new_queen/intro")
        data object SearchContext : EstherNewQueen("esther_new_queen/search_context")
        data object RoyalAttire : EstherNewQueen("esther_new_queen/royal_attire")
        data object CrownedContext : EstherNewQueen("esther_new_queen/crowned_context")
        data object Choice : EstherNewQueen("esther_new_queen/choice")
        data object Lesson : EstherNewQueen("esther_new_queen/lesson")
        data object Reward : EstherNewQueen("esther_new_queen/reward")

        companion object {
            const val GRAPH_ROUTE = "esther_new_queen"
        }
    }

    /** Nested graph — every scene shares one graph-scoped `EstherSecretPlotViewModel`. */
    sealed class EstherSecretPlot(route: String) : Destination(route) {
        data object Intro : EstherSecretPlot("esther_secret_plot/intro")
        data object DangerContext : EstherSecretPlot("esther_secret_plot/danger_context")
        data object CourtyardStealth : EstherSecretPlot("esther_secret_plot/courtyard_stealth")
        data object WarnedContext : EstherSecretPlot("esther_secret_plot/warned_context")
        data object Lesson : EstherSecretPlot("esther_secret_plot/lesson")
        data object Reward : EstherSecretPlot("esther_secret_plot/reward")

        companion object {
            const val GRAPH_ROUTE = "esther_secret_plot"
        }
    }

    /** Nested graph — every scene shares one graph-scoped `EstherThreatViewModel`. */
    sealed class EstherThreat(route: String) : Destination(route) {
        data object Intro : EstherThreat("esther_threat/intro")
        data object DecreeContext : EstherThreat("esther_threat/decree_context")
        data object MessengerSudoku : EstherThreat("esther_threat/messenger_sudoku")
        data object MourningContext : EstherThreat("esther_threat/mourning_context")
        data object Lesson : EstherThreat("esther_threat/lesson")
        data object Reward : EstherThreat("esther_threat/reward")

        companion object {
            const val GRAPH_ROUTE = "esther_threat"
        }
    }

    /** Nested graph — every scene shares one graph-scoped `EstherBraveApproachViewModel`. */
    sealed class EstherBraveApproach(route: String) : Destination(route) {
        data object Intro : EstherBraveApproach("esther_brave_approach/intro")
        data object Choice : EstherBraveApproach("esther_brave_approach/choice")
        data object FastingContext : EstherBraveApproach("esther_brave_approach/fasting_context")
        data object Corridor : EstherBraveApproach("esther_brave_approach/corridor")
        data object ScepterContext : EstherBraveApproach("esther_brave_approach/scepter_context")
        data object Lesson : EstherBraveApproach("esther_brave_approach/lesson")
        data object Reward : EstherBraveApproach("esther_brave_approach/reward")

        companion object {
            const val GRAPH_ROUTE = "esther_brave_approach"
        }
    }

    /** Nested graph — every scene shares one graph-scoped `EstherBanquetsRescueViewModel`. */
    sealed class EstherBanquetsRescue(route: String) : Destination(route) {
        data object Intro : EstherBanquetsRescue("esther_banquets_rescue/intro")
        data object PlanningContext : EstherBanquetsRescue("esther_banquets_rescue/planning_context")
        data object BanquetJigsaw : EstherBanquetsRescue("esther_banquets_rescue/banquet_jigsaw")
        data object SecondBanquetContext : EstherBanquetsRescue("esther_banquets_rescue/second_banquet_context")
        data object RevealHaman : EstherBanquetsRescue("esther_banquets_rescue/reveal_haman")
        data object SavedContext : EstherBanquetsRescue("esther_banquets_rescue/saved_context")
        data object Lesson : EstherBanquetsRescue("esther_banquets_rescue/lesson")
        data object Reward : EstherBanquetsRescue("esther_banquets_rescue/reward")

        companion object {
            const val GRAPH_ROUTE = "esther_banquets_rescue"
        }
    }

    /** Nested graph — every scene shares one graph-scoped `JerichoViewModel`. */
    sealed class Jericho(route: String) : Destination(route) {
        data object Intro : Jericho("jericho/intro")
        data object RahabHouseContext : Jericho("jericho/rahab_house_context")
        data object RahabHelping : Jericho("jericho/rahab_helping")
        data object Choice : Jericho("jericho/choice")
        data object WallMarch : Jericho("jericho/wall_march")
        data object RahabSavedContext : Jericho("jericho/rahab_saved_context")
        data object Lesson : Jericho("jericho/lesson")
        data object Reward : Jericho("jericho/reward")

        companion object {
            const val GRAPH_ROUTE = "jericho"
        }
    }

    /** Temporary landing spot for menu items not yet implemented. */
    data class ComingSoon(val featureTitle: String) : Destination("coming_soon") {
        companion object {
            const val ARG_FEATURE_TITLE = "featureTitle"
            const val ROUTE_WITH_ARGS = "coming_soon/{$ARG_FEATURE_TITLE}"
        }

        fun routeWithArgs(): String = "coming_soon/$featureTitle"
    }
}
