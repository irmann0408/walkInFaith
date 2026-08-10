package com.bibleadventures.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bibleadventures.domain.model.PlayerProfile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlayerProfileLocalDataSourceInstrumentedTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var dataSource: DataStorePlayerProfileLocalDataSource

    @Before
    fun setUp() {
        runBlocking { context.playerProfileDataStore.edit { it.clear() } }
        dataSource = DataStorePlayerProfileLocalDataSource(context)
    }

    @After
    fun tearDown() {
        runBlocking { context.playerProfileDataStore.edit { it.clear() } }
    }

    @Test
    fun writtenProfileRoundTripsThroughRealDataStore() = runBlocking {
        val updated = PlayerProfile.DEFAULT.copy(stars = 7)

        dataSource.update { updated }

        assertEquals(updated, dataSource.profile.first())
    }

    @Test
    fun missingSavedDataFallsBackToDefaultProfile() = runBlocking {
        assertEquals(PlayerProfile.DEFAULT, dataSource.profile.first())
    }

    @Test
    fun corruptedSavedDataFallsBackToDefaultProfileInsteadOfCrashing() = runBlocking {
        context.playerProfileDataStore.edit { preferences ->
            preferences[PLAYER_PROFILE_JSON_KEY] = "this is not valid json"
        }

        assertEquals(PlayerProfile.DEFAULT, dataSource.profile.first())
    }
}
