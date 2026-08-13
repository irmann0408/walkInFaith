package com.bibleadventures.domain.repository

import com.bibleadventures.domain.model.AudioSettings
import com.bibleadventures.domain.model.ChapterId
import com.bibleadventures.domain.model.CharacterCustomization
import com.bibleadventures.domain.model.PlayerProfile
import kotlinx.coroutines.flow.Flow

interface PlayerProfileRepository {
    val profile: Flow<PlayerProfile>

    suspend fun updateCharacter(customization: CharacterCustomization)

    suspend fun updateAudioSettings(audioSettings: AudioSettings)

    suspend fun updateReducedMotion(enabled: Boolean)

    suspend fun markSceneCompleted(chapterId: ChapterId, sceneId: String)

    suspend fun completeChapter(
        chapterId: ChapterId,
        stars: Int,
        badgeId: String,
        scriptureCardIds: List<String>,
    )

    /** Progress-only reset (Parent Area) — leaves `character`/`audioSettings`/`totalPlayTimeMillis` untouched. */
    suspend fun resetProgress()

    suspend fun addPlayTime(durationMillis: Long)
}
