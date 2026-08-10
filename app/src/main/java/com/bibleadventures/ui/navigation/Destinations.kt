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

    /** Nested graph — every scene shares one graph-scoped `NoahsArkViewModel`. */
    sealed class NoahsArk(route: String) : Destination(route) {
        data object Intro : NoahsArk("noahs_ark/intro")
        data object FindAnimals : NoahsArk("noahs_ark/find_animals")
        data object AnimalMatching : NoahsArk("noahs_ark/animal_matching")
        data object GatherSupplies : NoahsArk("noahs_ark/gather_supplies")
        data object OrganizeArk : NoahsArk("noahs_ark/organize_ark")
        data object FindMissingItems : NoahsArk("noahs_ark/find_missing_items")
        data object Lesson : NoahsArk("noahs_ark/lesson")
        data object Reward : NoahsArk("noahs_ark/reward")

        companion object {
            const val GRAPH_ROUTE = "noahs_ark"
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
