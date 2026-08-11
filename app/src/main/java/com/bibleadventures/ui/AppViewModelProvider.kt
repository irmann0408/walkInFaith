package com.bibleadventures.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.bibleadventures.BibleAdventuresApplication
import com.bibleadventures.ui.screens.badges.BadgesViewModel
import com.bibleadventures.ui.screens.character.CharacterViewModel
import com.bibleadventures.ui.screens.daniel.DanielViewModel
import com.bibleadventures.ui.screens.davidgoliath.DavidGoliathViewModel
import com.bibleadventures.ui.screens.esthernewqueen.EstherNewQueenViewModel
import com.bibleadventures.ui.screens.esthersecretplot.EstherSecretPlotViewModel
import com.bibleadventures.ui.screens.estherbanquetsrescue.EstherBanquetsRescueViewModel
import com.bibleadventures.ui.screens.estherbraveapproach.EstherBraveApproachViewModel
import com.bibleadventures.ui.screens.estherthreat.EstherThreatViewModel
import com.bibleadventures.ui.screens.goodsamaritan.GoodSamaritanViewModel
import com.bibleadventures.ui.screens.jericho.JerichoViewModel
import com.bibleadventures.ui.screens.mainmenu.MainMenuViewModel
import com.bibleadventures.ui.screens.noahsark.NoahsArkViewModel
import com.bibleadventures.ui.screens.scripturecards.ScriptureCardsViewModel
import com.bibleadventures.ui.screens.settings.SettingsViewModel
import com.bibleadventures.ui.screens.worldmap.WorldMapViewModel

/**
 * Resolves the [BibleAdventuresApplication.container] for a [CreationExtras],
 * so `ViewModelProvider.Factory` initializers can reach repositories/services
 * without a DI framework.
 */
fun CreationExtras.bibleAdventuresApplication(): BibleAdventuresApplication =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as BibleAdventuresApplication)

/**
 * Home for ViewModel factories that need constructor dependencies. Grows one
 * `initializer { }` block per ViewModel as milestones introduce them.
 */
object AppViewModelProvider {
    val Factory: ViewModelProvider.Factory = viewModelFactory {
        initializer {
            MainMenuViewModel(bibleAdventuresApplication().container.playerProfileRepository)
        }
        initializer {
            CharacterViewModel(bibleAdventuresApplication().container.playerProfileRepository)
        }
        initializer {
            val container = bibleAdventuresApplication().container
            WorldMapViewModel(container.progressionService, container.playerProfileRepository)
        }
        initializer {
            val container = bibleAdventuresApplication().container
            NoahsArkViewModel(container.progressionService, container.playerProfileRepository, container.audioController)
        }
        initializer {
            val container = bibleAdventuresApplication().container
            DavidGoliathViewModel(container.progressionService, container.playerProfileRepository, container.audioController)
        }
        initializer {
            val container = bibleAdventuresApplication().container
            GoodSamaritanViewModel(container.progressionService, container.playerProfileRepository, container.audioController)
        }
        initializer {
            val container = bibleAdventuresApplication().container
            DanielViewModel(container.progressionService, container.playerProfileRepository, container.audioController)
        }
        initializer {
            val container = bibleAdventuresApplication().container
            EstherNewQueenViewModel(container.progressionService, container.playerProfileRepository, container.audioController)
        }
        initializer {
            val container = bibleAdventuresApplication().container
            EstherSecretPlotViewModel(container.progressionService, container.playerProfileRepository, container.audioController)
        }
        initializer {
            val container = bibleAdventuresApplication().container
            EstherThreatViewModel(container.progressionService, container.playerProfileRepository, container.audioController)
        }
        initializer {
            val container = bibleAdventuresApplication().container
            EstherBraveApproachViewModel(container.progressionService, container.playerProfileRepository, container.audioController)
        }
        initializer {
            val container = bibleAdventuresApplication().container
            EstherBanquetsRescueViewModel(container.progressionService, container.playerProfileRepository, container.audioController)
        }
        initializer {
            val container = bibleAdventuresApplication().container
            JerichoViewModel(container.progressionService, container.playerProfileRepository, container.audioController)
        }
        initializer {
            BadgesViewModel(bibleAdventuresApplication().container.playerProfileRepository)
        }
        initializer {
            ScriptureCardsViewModel(bibleAdventuresApplication().container.playerProfileRepository)
        }
        initializer {
            SettingsViewModel(bibleAdventuresApplication().container.playerProfileRepository)
        }
    }
}
