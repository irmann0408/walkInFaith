package com.bibleadventures.game.puzzles.rhythmlane

import kotlin.math.abs

/**
 * Mirrors [com.bibleadventures.game.puzzles.slingshot.SlingshotGame]'s
 * split: the screen owns the live, real-time note-scroll animation and
 * reports each lane tap with a timestamp; this engine only judges a tap
 * against the chart and turns it into progress. A tap with no nearby note
 * in that lane is a pure no-op — never a setback, same "every input either
 * helps or does nothing" guarantee as every other engine in this codebase.
 */
object RhythmLaneGame {

    private const val HIT_WINDOW_MS = 300L
    private const val PERFECT_WINDOW_MS = 100L

    /** [nowMs] is elapsed time since the chart started (screen owns the real ticking clock). */
    fun onLaneTapped(state: RhythmLaneGameState, lane: Int, nowMs: Long): RhythmLaneGameState {
        if (state.isComplete) return state
        val loopIndex = nowMs / state.chart.loopDurationMs
        val loopElapsedMs = nowMs % state.chart.loopDurationMs

        val candidate = state.chart.notes
            .asSequence()
            .filter { it.lane == lane }
            .filter { "$loopIndex:${it.id}" !in state.judgedNoteKeys }
            .minByOrNull { abs(it.hitTimeMs - loopElapsedMs) }
            ?.takeIf { abs(it.hitTimeMs - loopElapsedMs) <= HIT_WINDOW_MS }
            ?: return state

        val judgment = if (abs(candidate.hitTimeMs - loopElapsedMs) <= PERFECT_WINDOW_MS) {
            NoteJudgment.PERFECT
        } else {
            NoteJudgment.GREAT
        }
        return state.copy(
            hits = (state.hits + 1).coerceAtMost(state.requiredHits),
            judgedNoteKeys = state.judgedNoteKeys + "$loopIndex:${candidate.id}",
            lastJudgment = judgment,
        )
    }

    /**
     * The inverse of [onLaneTapped]'s "catch" semantics — for chapters where
     * the falling/rolling object is a hazard to dodge, not something to
     * collect (David & Goliath's Crossing the Valley, Daniel's Hurrying to
     * Pray). Succeeds when [currentLane] does NOT match a note landing
     * within the hit window; staying in the hazard's own lane is never a
     * failure, it's just not yet avoided — the player can still move before
     * the window closes, and [onTimeAdvanced] ages an un-avoided note out to
     * MISSED same as any other unhit note, with no penalty.
     */
    fun onLaneAvoided(state: RhythmLaneGameState, currentLane: Int, nowMs: Long): RhythmLaneGameState {
        if (state.isComplete) return state
        val loopIndex = nowMs / state.chart.loopDurationMs
        val loopElapsedMs = nowMs % state.chart.loopDurationMs

        val candidate = state.chart.notes
            .asSequence()
            .filter { "$loopIndex:${it.id}" !in state.judgedNoteKeys }
            .minByOrNull { abs(it.hitTimeMs - loopElapsedMs) }
            ?.takeIf { abs(it.hitTimeMs - loopElapsedMs) <= HIT_WINDOW_MS }
            ?: return state

        if (candidate.lane == currentLane) return state

        val judgment = if (abs(candidate.hitTimeMs - loopElapsedMs) <= PERFECT_WINDOW_MS) {
            NoteJudgment.PERFECT
        } else {
            NoteJudgment.GREAT
        }
        return state.copy(
            hits = (state.hits + 1).coerceAtMost(state.requiredHits),
            judgedNoteKeys = state.judgedNoteKeys + "$loopIndex:${candidate.id}",
            lastJudgment = judgment,
        )
    }

    /** Marks notes whose hit window has fully passed as MISSED — bookkeeping/feedback only, never reduces [RhythmLaneGameState.hits]. */
    fun onTimeAdvanced(state: RhythmLaneGameState, nowMs: Long): RhythmLaneGameState {
        if (state.isComplete) return state
        val loopIndex = nowMs / state.chart.loopDurationMs
        val loopElapsedMs = nowMs % state.chart.loopDurationMs

        val newlyMissed = state.chart.notes.filter { note ->
            "$loopIndex:${note.id}" !in state.judgedNoteKeys && loopElapsedMs - note.hitTimeMs > HIT_WINDOW_MS
        }
        if (newlyMissed.isEmpty()) return state

        return state.copy(
            judgedNoteKeys = state.judgedNoteKeys + newlyMissed.map { "$loopIndex:${it.id}" },
            lastJudgment = NoteJudgment.MISSED,
        )
    }
}
