package com.bibleadventures.game.puzzles.sudoku

/**
 * A conflicting placement (icon already used elsewhere in the same row or
 * column) is rejected and never committed — the same non-committing pattern
 * used throughout this app's puzzle engines for an invalid drop.
 * Re-filling an already-filled cell with a different icon is treated as a
 * correction, not a failure — the old value simply isn't counted against
 * itself when checking for a conflict.
 */
object SudokuGame {

    fun onCellFilled(state: SudokuGameState, row: Int, col: Int, icon: String): SudokuGameState {
        if (state.givens.containsKey(row to col)) return state
        if (state.filled[row to col] == icon) return state

        val rowHasIcon = (0 until state.size).any { c -> c != col && state.valueAt(row, c) == icon }
        val colHasIcon = (0 until state.size).any { r -> r != row && state.valueAt(r, col) == icon }
        if (rowHasIcon || colHasIcon) {
            return state.copy(lastOutcome = SudokuOutcome.CONFLICT)
        }

        val rowNowComplete = (0 until state.size).all { c ->
            if (c == col) true else state.valueAt(row, c) != null
        }
        val newCompletedRows = if (rowNowComplete) state.completedRows + row else state.completedRows

        val next = state.copy(
            filled = state.filled + ((row to col) to icon),
            completedRows = newCompletedRows,
        )

        val outcome = when {
            next.isComplete -> SudokuOutcome.COMPLETE
            rowNowComplete && row !in state.completedRows -> SudokuOutcome.ROW_COMPLETE
            else -> SudokuOutcome.PLACED
        }
        return next.copy(lastOutcome = outcome)
    }

    fun onCellCleared(state: SudokuGameState, row: Int, col: Int): SudokuGameState {
        if (state.givens.containsKey(row to col)) return state
        if (!state.filled.containsKey(row to col)) return state
        return state.copy(
            filled = state.filled - (row to col),
            completedRows = state.completedRows - row,
            lastOutcome = SudokuOutcome.NONE,
        )
    }
}
