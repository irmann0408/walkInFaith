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

    /** Nested graph — every scene shares one graph-scoped `NoahsArkViewModel`. */
    sealed class NoahsArk(route: String) : Destination(route) {
        data object Intro : NoahsArk("noahs_ark/intro")
        data object FindAnimalsContext : NoahsArk("noahs_ark/find_animals_context")
        data object FindAnimals : NoahsArk("noahs_ark/find_animals")
        data object AnimalMatching : NoahsArk("noahs_ark/animal_matching")
        data object GatherSuppliesContext : NoahsArk("noahs_ark/gather_supplies_context")
        data object GatherSupplies : NoahsArk("noahs_ark/gather_supplies")
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

    /** Temporary landing spot for menu items not yet implemented. */
    data class ComingSoon(val featureTitle: String) : Destination("coming_soon") {
        companion object {
            const val ARG_FEATURE_TITLE = "featureTitle"
            const val ROUTE_WITH_ARGS = "coming_soon/{$ARG_FEATURE_TITLE}"
        }

        fun routeWithArgs(): String = "coming_soon/$featureTitle"
    }
}
