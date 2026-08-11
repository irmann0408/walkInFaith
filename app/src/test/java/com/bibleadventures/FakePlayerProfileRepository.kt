package com.bibleadventures

import com.bibleadventures.domain.model.AdventureProgress
import com.bibleadventures.domain.model.AudioSettings
import com.bibleadventures.domain.model.ChapterId
import com.bibleadventures.domain.model.CharacterCustomization
import com.bibleadventures.domain.model.PlayerProfile
import com.bibleadventures.domain.repository.PlayerProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * In-memory [PlayerProfileRepository] shared by ViewModel unit tests that need
 * real merge behavior (progress/badges/scripture cards) without DataStore.
 */
class FakePlayerProfileRepository(
    initial: PlayerProfile = PlayerProfile.DEFAULT,
) : PlayerProfileRepository {
    private val state = MutableStateFlow(initial)
    override val profile: Flow<PlayerProfile> = state

    override suspend fun updateCharacter(customization: CharacterCustomization) {
        state.value = state.value.copy(character = customization)
    }

    override suspend fun updateAudioSettings(audioSettings: AudioSettings) {
        state.value = state.value.copy(audioSettings = audioSettings)
    }

    override suspend fun markSceneCompleted(chapterId: ChapterId, sceneId: String) {
        state.value = state.value.let { current ->
            val progress = (current.progressByChapter[chapterId] ?: AdventureProgress(chapterId = chapterId))
                .copy(completedActivities = (current.progressByChapter[chapterId]?.completedActivities ?: emptySet()) + sceneId)
            current.copy(progressByChapter = current.progressByChapter + (chapterId to progress))
        }
    }

    override suspend fun completeChapter(
        chapterId: ChapterId,
        stars: Int,
        badgeId: String,
        scriptureCardId: String,
    ) {
        state.value = state.value.let { current ->
            val progress = (current.progressByChapter[chapterId] ?: AdventureProgress(chapterId = chapterId))
                .copy(completed = true, stars = stars)
            current.copy(
                progressByChapter = current.progressByChapter + (chapterId to progress),
                completedChapters = current.completedChapters + chapterId,
                stars = current.stars + stars,
                badges = current.badges + badgeId,
                scriptureCards = current.scriptureCards + scriptureCardId,
            )
        }
    }

    fun current(): PlayerProfile = state.value
}
