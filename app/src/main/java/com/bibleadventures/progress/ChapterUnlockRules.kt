package com.bibleadventures.progress

import com.bibleadventures.domain.model.Chapter
import com.bibleadventures.domain.model.ChapterId
import com.bibleadventures.domain.model.ChapterStatus

/**
 * Pure unlock logic — the "progression service" spec section 11 asks for,
 * kept separate from any screen so unlock rules can change (e.g. a
 * non-linear unlock graph) without touching UI code.
 */
object ChapterUnlockRules {

    fun computeStatuses(catalog: List<Chapter>, completedChapters: Set<ChapterId>): Map<ChapterId, ChapterStatus> {
        val unlocked = computeUnlocked(catalog, completedChapters)
        return catalog.associate { chapter ->
            chapter.id to when {
                chapter.id in completedChapters -> ChapterStatus.COMPLETED
                chapter.id in unlocked -> ChapterStatus.UNLOCKED
                else -> ChapterStatus.LOCKED
            }
        }
    }

    private fun computeUnlocked(catalog: List<Chapter>, completedChapters: Set<ChapterId>): Set<ChapterId> =
        catalog
            .filter { chapter -> chapter.requiredChapter == null || chapter.requiredChapter in completedChapters }
            .map { it.id }
            .toSet()
}
