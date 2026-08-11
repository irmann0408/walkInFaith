package com.bibleadventures.game.puzzles.rhythmlane

/** One scrolling note: which of the (typically 3) lanes it's in, and the elapsed-in-loop moment it reaches the hit zone. */
data class RhythmNote(val id: String, val lane: Int, val hitTimeMs: Long)

/**
 * A hand-authored, deterministic note sequence that loops forever — never a
 * one-shot "song" that can run out. Looping is what keeps this
 * failure-state-free: a child who misses several notes just keeps playing
 * a little longer, never gets stuck unable to finish, the same guarantee
 * [com.bibleadventures.game.puzzles.meter.MeterGame]'s never-ending pulsing
 * target used to give.
 */
data class RhythmLaneChart(val notes: List<RhythmNote>, val loopDurationMs: Long)

/** Feedback-text classification only — [MISSED] never subtracts anything, it's a no-op, not a penalty. */
enum class NoteJudgment { PERFECT, GREAT, MISSED }

data class RhythmLaneGameState(
    val chart: RhythmLaneChart,
    val requiredHits: Int,
    val hits: Int = 0,
    /** "loopIndex:noteId" of every note already judged (hit or missed) — keyed per loop iteration so each new loop re-arms every note. */
    val judgedNoteKeys: Set<String> = emptySet(),
    val lastJudgment: NoteJudgment? = null,
) {
    val isComplete: Boolean
        get() = hits >= requiredHits

    val progressFraction: Float
        get() = (hits.toFloat() / requiredHits).coerceIn(0f, 1f)
}
