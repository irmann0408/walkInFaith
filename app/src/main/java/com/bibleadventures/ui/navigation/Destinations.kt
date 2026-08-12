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
    data object ParentArea : Destination("parent_area")

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

    /**
     * Nested graph — every scene shares one graph-scoped `EstherViewModel`.
     * One chapter built from 4 sequential mini-puzzles; the banquet jigsaw
     * mini-game was dropped per playtesting feedback (repeat mechanic, too
     * easy). Reveal Haman's Plot and its surrounding context cards were
     * dropped too, per the user's explicit request to tighten the tail end
     * of the chapter — Corridor goes straight into the Lesson now.
     */
    sealed class Esther(route: String) : Destination(route) {
        data object Intro : Esther("esther/intro")
        data object SearchContext : Esther("esther/search_context")
        data object RoyalAttire : Esther("esther/royal_attire")
        data object CrownedContext : Esther("esther/crowned_context")
        data object GreetingChoice : Esther("esther/greeting_choice")
        data object DangerContext : Esther("esther/danger_context")
        data object CourtyardStealth : Esther("esther/courtyard_stealth")
        data object WarnedContext : Esther("esther/warned_context")
        data object DecreeContext : Esther("esther/decree_context")
        data object MessengerSudoku : Esther("esther/messenger_sudoku")
        data object MourningContext : Esther("esther/mourning_context")
        data object DecisionChoice : Esther("esther/decision_choice")
        data object FastingContext : Esther("esther/fasting_context")
        data object Corridor : Esther("esther/corridor")
        data object Lesson : Esther("esther/lesson")
        data object Reward : Esther("esther/reward")

        companion object {
            const val GRAPH_ROUTE = "esther"
        }
    }

    /**
     * Nested graph — every scene shares one graph-scoped `JerichoViewModel`.
     * Rebuilt with 4 real mini-puzzles (the old 4-flashcard "March and the
     * Shout" was too easy): the spies' rope escape, setting up camp, the
     * six-day silent march, and the seventh-day fast march/shofar/shout
     * finale.
     */
    sealed class Jericho(route: String) : Destination(route) {
        data object Intro : Jericho("jericho/intro")
        data object RahabHouseContext : Jericho("jericho/rahab_house_context")
        data object RahabHelping : Jericho("jericho/rahab_helping")
        data object SpiesEscape : Jericho("jericho/spies_escape")
        data object SpiesEscapedContext : Jericho("jericho/spies_escaped_context")
        data object Choice : Jericho("jericho/choice")
        data object CampContext : Jericho("jericho/camp_context")
        data object SettingUpCamp : Jericho("jericho/setting_up_camp")
        data object TentsContext : Jericho("jericho/tents_context")
        data object WallsContext : Jericho("jericho/walls_context")
        data object SixDayMarch : Jericho("jericho/six_day_march")
        data object SeventhDayContext : Jericho("jericho/seventh_day_context")
        data object FastMarch : Jericho("jericho/fast_march")
        data object BlowShofar : Jericho("jericho/blow_shofar")
        data object Shout : Jericho("jericho/shout")
        data object RahabSavedContext : Jericho("jericho/rahab_saved_context")
        data object Lesson : Jericho("jericho/lesson")
        data object Reward : Jericho("jericho/reward")

        companion object {
            const val GRAPH_ROUTE = "jericho"
        }
    }

    /**
     * Nested graph — every scene shares one graph-scoped `Feeding5000ViewModel`.
     * Five real mini-puzzles mapped onto John 6:1-14 / Mark 6:35-44: gathering
     * the crowd into seating groups (`groupfill`), searching for the boy with
     * the loaves and fish (`hiddenobject`), finding exactly what's in his
     * basket among decoys (`hiddenobject`), the miracle of multiplication as
     * real arithmetic (`decisionpath`), and a two-phase serve-then-gather
     * finale (`rhythmlane` reused twice more).
     */
    sealed class Feeding5000(route: String) : Destination(route) {
        data object Intro : Feeding5000("feeding_5000/intro")
        data object CrowdContext : Feeding5000("feeding_5000/crowd_context")
        data object GatheringCrowd : Feeding5000("feeding_5000/gathering_crowd")
        data object SearchingContext : Feeding5000("feeding_5000/searching_context")
        data object SearchingForFood : Feeding5000("feeding_5000/searching_for_food")
        data object BoysGiftContext : Feeding5000("feeding_5000/boys_gift_context")
        data object BoysGift : Feeding5000("feeding_5000/boys_gift")
        data object Choice : Feeding5000("feeding_5000/choice")
        data object MiracleContext : Feeding5000("feeding_5000/miracle_context")
        data object MiracleMultiplication : Feeding5000("feeding_5000/miracle_multiplication")
        data object FeastContext : Feeding5000("feeding_5000/feast_context")
        data object Serving : Feeding5000("feeding_5000/serving")
        data object Catching : Feeding5000("feeding_5000/catching")
        data object Lesson : Feeding5000("feeding_5000/lesson")
        data object Reward : Feeding5000("feeding_5000/reward")

        companion object {
            const val GRAPH_ROUTE = "feeding_5000"
        }
    }

    /** Nested graph — every scene shares one graph-scoped `JesusCalmsStormViewModel`. */
    sealed class JesusCalmsStorm(route: String) : Destination(route) {
        data object Intro : JesusCalmsStorm("jesus_calms_storm/intro")
        data object LoadingContext : JesusCalmsStorm("jesus_calms_storm/loading_context")
        data object LoadingTheBoat : JesusCalmsStorm("jesus_calms_storm/loading_the_boat")
        data object StormContext : JesusCalmsStorm("jesus_calms_storm/storm_context")
        data object BailingTheBoat : JesusCalmsStorm("jesus_calms_storm/bailing_the_boat")
        data object Choice : JesusCalmsStorm("jesus_calms_storm/choice")
        data object FindJesusContext : JesusCalmsStorm("jesus_calms_storm/find_jesus_context")
        data object ReachingJesus : JesusCalmsStorm("jesus_calms_storm/reaching_jesus")
        data object CalmContext : JesusCalmsStorm("jesus_calms_storm/calm_context")
        data object PeaceBeStill : JesusCalmsStorm("jesus_calms_storm/peace_be_still")
        data object Lesson : JesusCalmsStorm("jesus_calms_storm/lesson")
        data object Reward : JesusCalmsStorm("jesus_calms_storm/reward")

        companion object {
            const val GRAPH_ROUTE = "jesus_calms_storm"
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
