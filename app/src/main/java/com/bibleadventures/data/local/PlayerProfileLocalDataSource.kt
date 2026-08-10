package com.bibleadventures.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.bibleadventures.domain.model.PlayerProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.io.IOException

// internal (not private) so androidTest can write raw/corrupted values directly
// to exercise the "corrupted saved data" fallback (spec section 20) against a
// real DataStore file, without expanding the public API surface.
internal val Context.playerProfileDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "player_profile",
)

internal val PLAYER_PROFILE_JSON_KEY = stringPreferencesKey("player_profile_json")

private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/**
 * Interface (not a concrete class) so unit tests can substitute an
 * in-memory fake instead of exercising real DataStore file IO.
 */
interface PlayerProfileLocalDataSource {
    val profile: Flow<PlayerProfile>
    suspend fun update(transform: (PlayerProfile) -> PlayerProfile)
}

class DataStorePlayerProfileLocalDataSource(
    private val context: Context,
) : PlayerProfileLocalDataSource {

    override val profile: Flow<PlayerProfile> = context.playerProfileDataStore.data
        // A corrupted/missing DataStore file must never crash the app (spec section 20).
        .catch { error ->
            if (error is IOException) emit(androidx.datastore.preferences.core.emptyPreferences()) else throw error
        }
        .map { preferences -> preferences.decodeProfileOrDefault() }

    override suspend fun update(transform: (PlayerProfile) -> PlayerProfile) {
        context.playerProfileDataStore.edit { preferences ->
            val current = preferences.decodeProfileOrDefault()
            val updated = transform(current)
            preferences[PLAYER_PROFILE_JSON_KEY] = json.encodeToString(PlayerProfile.serializer(), updated)
        }
    }

    private fun Preferences.decodeProfileOrDefault(): PlayerProfile {
        val raw = this[PLAYER_PROFILE_JSON_KEY] ?: return PlayerProfile.DEFAULT
        return runCatching { json.decodeFromString(PlayerProfile.serializer(), raw) }
            .getOrDefault(PlayerProfile.DEFAULT)
    }
}
