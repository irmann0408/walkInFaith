package com.bibleadventures

import android.content.Context
import com.bibleadventures.audio.AudioController
import com.bibleadventures.audio.NoOpAudioController
import com.bibleadventures.data.local.DataStorePlayerProfileLocalDataSource
import com.bibleadventures.data.repository.PlayerProfileRepositoryImpl
import com.bibleadventures.domain.repository.PlayerProfileRepository
import com.bibleadventures.progress.ProgressionService

/**
 * Manual dependency container. Grows one property per milestone as new
 * repositories/services are introduced — deliberately not a DI framework,
 * since the dependency graph here is small (see spec section 5: no
 * unnecessary dependencies).
 */
class AppContainer(private val appContext: Context) {
    val playerProfileRepository: PlayerProfileRepository by lazy {
        PlayerProfileRepositoryImpl(DataStorePlayerProfileLocalDataSource(appContext))
    }

    val progressionService: ProgressionService by lazy {
        ProgressionService(playerProfileRepository)
    }

    val audioController: AudioController by lazy { NoOpAudioController() }
}
