package com.bibleadventures.data.repository

import com.bibleadventures.data.local.PlayerProfileLocalDataSource
import com.bibleadventures.domain.model.AdventureProgress
import com.bibleadventures.domain.model.AudioSettings
import com.bibleadventures.domain.model.ChapterId
import com.bibleadventures.domain.model.CharacterCustomization
import com.bibleadventures.domain.model.PlayerProfile
import com.bibleadventures.domain.repository.PlayerProfileRepository
import kotlinx.coroutines.flow.Flow

class PlayerProfileRepositoryImpl(
    private val localDataSource: PlayerProfileLocalDataSource,
) : PlayerProfileRepository {

    override val profile: Flow<PlayerProfile> = localDataSource.profile

    override suspend fun updateCharacter(customization: CharacterCustomization) {
        localDataSource.update { it.copy(character = customization) }
    }

    override suspend fun updateAudioSettings(audioSettings: AudioSettings) {
        localDataSource.update { it.copy(audioSettings = audioSettings) }
    }

    override suspend fun markSceneCompleted(chapterId: ChapterId, sceneId: String) {
        localDataSource.update { current ->
            val existing = current.progressByChapter[chapterId] ?: AdventureProgress(chapterId = chapterId)
            val updatedProgress = existing.copy(completedActivities = existing.completedActivities + sceneId)
            current.copy(progressByChapter = current.progressByChapter + (chapterId to updatedProgress))
        }
    }

    override suspend fun completeChapter(
        chapterId: ChapterId,
        stars: Int,
        badgeId: String,
        scriptureCardIds: List<String>,
    ) {
        localDataSource.update { current ->
            val existing = current.progressByChapter[chapterId] ?: AdventureProgress(chapterId = chapterId)
            val completedProgress = existing.copy(completed = true, stars = stars)
            current.copy(
                progressByChapter = current.progressByChapter + (chapterId to completedProgress),
                completedChapters = current.completedChapters + chapterId,
                stars = current.stars + stars,
                badges = current.badges + badgeId,
                scriptureCards = current.scriptureCards + scriptureCardIds,
            )
        }
    }

    override suspend fun resetProgress() {
        localDataSource.update {
            it.copy(
                unlockedChapters = PlayerProfile.DEFAULT.unlockedChapters,
                completedChapters = emptySet(),
                progressByChapter = emptyMap(),
                stars = 0,
                badges = emptySet(),
                scriptureCards = emptySet(),
            )
        }
    }

    override suspend fun addPlayTime(durationMillis: Long) {
        localDataSource.update { it.copy(totalPlayTimeMillis = it.totalPlayTimeMillis + durationMillis) }
    }
}
