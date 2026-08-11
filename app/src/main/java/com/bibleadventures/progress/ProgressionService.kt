package com.bibleadventures.progress

import com.bibleadventures.domain.model.Chapter
import com.bibleadventures.domain.model.ChapterId
import com.bibleadventures.domain.model.ChapterStatus
import com.bibleadventures.domain.repository.PlayerProfileRepository
import com.bibleadventures.game.stories.ChapterCatalog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * The single place that knows how chapter unlocking works — no screen
 * computes unlock/reward logic itself (spec section 11).
 */
class ProgressionService(
    private val repository: PlayerProfileRepository,
    private val catalog: List<Chapter> = ChapterCatalog.all,
) {
    val chapterStatuses: Flow<Map<ChapterId, ChapterStatus>> = repository.profile
        .map { profile -> ChapterUnlockRules.computeStatuses(catalog, profile.completedChapters) }

    suspend fun completeChapter(chapterId: ChapterId, stars: Int, badgeId: String, scriptureCardIds: List<String>) {
        repository.completeChapter(chapterId, stars, badgeId, scriptureCardIds)
    }
}
