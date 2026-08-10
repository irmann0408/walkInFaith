package com.bibleadventures.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.bibleadventures.BibleAdventuresApplication
import com.bibleadventures.ui.screens.character.CharacterViewModel
import com.bibleadventures.ui.screens.noahsark.NoahsArkViewModel
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
    }
}
