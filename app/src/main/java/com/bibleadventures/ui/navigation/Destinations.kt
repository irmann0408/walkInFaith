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

    /** Nested graph — every scene shares one graph-scoped `EstherViewModel`. */
    sealed class Esther(route: String) : Destination(route) {
        data object Intro : Esther("esther/intro")
        data object KingsTroubleContext : Esther("esther/kings_trouble_context")
        data object HamansAngerContext : Esther("esther/hamans_anger_context")
        data object Choice : Esther("esther/choice")
        data object ScepterContext : Esther("esther/scepter_context")
        data object Banquet : Esther("esther/banquet")
        data object TruthRevealedContext : Esther("esther/truth_revealed_context")
        data object Lesson : Esther("esther/lesson")
        data object Reward : Esther("esther/reward")

        companion object {
            const val GRAPH_ROUTE = "esther"
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
