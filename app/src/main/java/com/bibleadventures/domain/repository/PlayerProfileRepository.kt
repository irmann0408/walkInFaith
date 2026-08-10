package com.bibleadventures.domain.repository

import com.bibleadventures.domain.model.ChapterId
import com.bibleadventures.domain.model.CharacterCustomization
import com.bibleadventures.domain.model.PlayerProfile
import kotlinx.coroutines.flow.Flow

interface PlayerProfileRepository {
    val profile: Flow<PlayerProfile>

    suspend fun updateCharacter(customization: CharacterCustomization)

    suspend fun markSceneCompleted(chapterId: ChapterId, sceneId: String)

    suspend fun completeChapter(
        chapterId: ChapterId,
        stars: Int,
        badgeId: String,
        scriptureCardId: String,
    )
}
