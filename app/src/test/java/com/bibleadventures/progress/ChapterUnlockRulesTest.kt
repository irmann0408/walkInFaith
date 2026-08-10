package com.bibleadventures.progress

import com.bibleadventures.game.stories.ChapterCatalog
import com.bibleadventures.domain.model.ChapterId
import com.bibleadventures.domain.model.ChapterStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class ChapterUnlockRulesTest {

    @Test
    fun `only Noah's Ark is unlocked when nothing is completed`() {
        val statuses = ChapterUnlockRules.computeStatuses(ChapterCatalog.all, completedChapters = emptySet())

        assertEquals(ChapterStatus.UNLOCKED, statuses.getValue(ChapterId.NOAHS_ARK))
        assertEquals(ChapterStatus.LOCKED, statuses.getValue(ChapterId.DAVID_GOLIATH))
        assertEquals(ChapterStatus.LOCKED, statuses.getValue(ChapterId.GOOD_SAMARITAN))
        assertEquals(ChapterStatus.LOCKED, statuses.getValue(ChapterId.FEEDING_5000))
        assertEquals(ChapterStatus.LOCKED, statuses.getValue(ChapterId.DANIEL))
        assertEquals(ChapterStatus.LOCKED, statuses.getValue(ChapterId.JESUS_CALMS_STORM))
    }

    @Test
    fun `completing a chapter unlocks the next one in the chain`() {
        val statuses = ChapterUnlockRules.computeStatuses(
            ChapterCatalog.all,
            completedChapters = setOf(ChapterId.NOAHS_ARK),
        )

        assertEquals(ChapterStatus.COMPLETED, statuses.getValue(ChapterId.NOAHS_ARK))
        assertEquals(ChapterStatus.UNLOCKED, statuses.getValue(ChapterId.DAVID_GOLIATH))
        assertEquals(ChapterStatus.LOCKED, statuses.getValue(ChapterId.GOOD_SAMARITAN))
    }

    @Test
    fun `a chapter never unlocks without its prerequisite completed`() {
        // A synthetic out-of-order completion (not reachable through the app's own
        // flow) to exercise the pure function's per-chapter logic in isolation:
        // only the chapter right after a completed one should unlock.
        val statuses = ChapterUnlockRules.computeStatuses(
            ChapterCatalog.all,
            completedChapters = setOf(ChapterId.GOOD_SAMARITAN),
        )

        assertEquals(ChapterStatus.LOCKED, statuses.getValue(ChapterId.DAVID_GOLIATH))
        assertEquals(ChapterStatus.COMPLETED, statuses.getValue(ChapterId.GOOD_SAMARITAN))
        assertEquals(ChapterStatus.UNLOCKED, statuses.getValue(ChapterId.FEEDING_5000))
        assertEquals(ChapterStatus.LOCKED, statuses.getValue(ChapterId.DANIEL))
    }

    @Test
    fun `completing the final chapter leaves everything unlocked or completed`() {
        val statuses = ChapterUnlockRules.computeStatuses(
            ChapterCatalog.all,
            completedChapters = ChapterId.entries.toSet(),
        )

        statuses.values.forEach { status ->
            assert(status == ChapterStatus.COMPLETED) { "Expected COMPLETED but was $status" }
        }
    }
}
