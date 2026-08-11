package com.bibleadventures.data.repository

import com.bibleadventures.data.local.PlayerProfileLocalDataSource
import com.bibleadventures.domain.model.Appearance
import com.bibleadventures.domain.model.ChapterId
import com.bibleadventures.domain.model.CharacterCustomization
import com.bibleadventures.domain.model.PlayerProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakePlayerProfileLocalDataSource(
    initial: PlayerProfile = PlayerProfile.DEFAULT,
) : PlayerProfileLocalDataSource {
    private val state = MutableStateFlow(initial)
    override val profile: Flow<PlayerProfile> = state

    override suspend fun update(transform: (PlayerProfile) -> PlayerProfile) {
        state.value = transform(state.value)
    }

    fun current(): PlayerProfile = state.value
}

class PlayerProfileRepositoryImplTest {

    @Test
    fun `updateCharacter replaces the character field only`() = runTest {
        val localDataSource = FakePlayerProfileLocalDataSource()
        val repository = PlayerProfileRepositoryImpl(localDataSource)
        val newCharacter = CharacterCustomization(appearance = Appearance.GIRL)

        repository.updateCharacter(newCharacter)

        assertEquals(newCharacter, localDataSource.current().character)
    }

    @Test
    fun `markSceneCompleted creates progress when none exists yet`() = runTest {
        val localDataSource = FakePlayerProfileLocalDataSource()
        val repository = PlayerProfileRepositoryImpl(localDataSource)

        repository.markSceneCompleted(ChapterId.NOAHS_ARK, "intro")

        val progress = localDataSource.current().progressByChapter.getValue(ChapterId.NOAHS_ARK)
        assertTrue("intro" in progress.completedActivities)
        assertEquals(false, progress.completed)
    }

    @Test
    fun `markSceneCompleted accumulates activities across calls`() = runTest {
        val localDataSource = FakePlayerProfileLocalDataSource()
        val repository = PlayerProfileRepositoryImpl(localDataSource)

        repository.markSceneCompleted(ChapterId.NOAHS_ARK, "intro")
        repository.markSceneCompleted(ChapterId.NOAHS_ARK, "find_animals")

        val progress = localDataSource.current().progressByChapter.getValue(ChapterId.NOAHS_ARK)
        assertEquals(setOf("intro", "find_animals"), progress.completedActivities)
    }

    @Test
    fun `completeChapter merges stars badge scripture card and completed chapters`() = runTest {
        val localDataSource = FakePlayerProfileLocalDataSource()
        val repository = PlayerProfileRepositoryImpl(localDataSource)

        repository.completeChapter(
            chapterId = ChapterId.NOAHS_ARK,
            stars = 3,
            badgeId = "ARK_BUILDER",
            scriptureCardIds = listOf("GENESIS_6_22"),
        )

        val profile = localDataSource.current()
        assertTrue(ChapterId.NOAHS_ARK in profile.completedChapters)
        assertEquals(3, profile.stars)
        assertTrue("ARK_BUILDER" in profile.badges)
        assertTrue("GENESIS_6_22" in profile.scriptureCards)
        assertTrue(profile.progressByChapter.getValue(ChapterId.NOAHS_ARK).completed)
    }

    @Test
    fun `completeChapter accumulates stars across multiple chapters`() = runTest {
        val localDataSource = FakePlayerProfileLocalDataSource()
        val repository = PlayerProfileRepositoryImpl(localDataSource)

        repository.completeChapter(ChapterId.NOAHS_ARK, stars = 3, badgeId = "ARK_BUILDER", scriptureCardIds = listOf("GENESIS_6_22"))
        repository.completeChapter(ChapterId.DAVID_GOLIATH, stars = 2, badgeId = "BRAVE_HEART", scriptureCardIds = listOf("SAMUEL_17_45"))

        assertEquals(5, localDataSource.current().stars)
    }
}
