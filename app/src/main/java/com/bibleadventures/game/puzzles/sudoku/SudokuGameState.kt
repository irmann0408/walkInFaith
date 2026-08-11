package com.bibleadventures.game.puzzles.sudoku

// Never FAILED — a conflicting placement is rejected without ever committing.
enum class SudokuOutcome { NONE, PLACED, CONFLICT, ROW_COMPLETE, COMPLETE }

/**
 * A small icon-based logic grid: real row-and-column uniqueness (no box
 * region — [size] doesn't subdivide cleanly at 5, and this app's audience
 * doesn't need the extra constraint). [givens] are immutable pre-filled
 * cells; [filled] holds only the player's own placements.
 */
data class SudokuGameState(
    val size: Int,
    val givens: Map<Pair<Int, Int>, String>,
    val filled: Map<Pair<Int, Int>, String> = emptyMap(),
    val completedRows: Set<Int> = emptySet(),
    val lastOutcome: SudokuOutcome = SudokuOutcome.NONE,
) {
    fun valueAt(row: Int, col: Int): String? = givens[row to col] ?: filled[row to col]

    val isComplete: Boolean
        get() = (0 until size).all { row -> (0 until size).all { col -> valueAt(row, col) != null } }
}
