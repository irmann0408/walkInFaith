package com.bibleadventures.game.puzzles.slidingpuzzle

import kotlin.random.Random

object SlidingPuzzleGame {

    /** Swaps [index] with the empty slot if they're orthogonally adjacent; otherwise a pure no-op. */
    fun onTileTapped(state: SlidingPuzzleGameState, index: Int): SlidingPuzzleGameState {
        if (state.isComplete) return state
        if (!isAdjacentToEmpty(index, state.emptyIndex, state.size)) return state
        return slide(state, index)
    }

    /**
     * Solvable-by-construction: starts from the solved grid and applies
     * [moveCount] random *legal* slides, each a uniformly random neighbor
     * of the empty slot (occasionally undoing the previous move, same as
     * a real shuffle — that's fine, 80 steps still mixes the board well).
     * Every intermediate state is reachable from — and so can always slide
     * back to — the solved state, which sidesteps needing 15-puzzle-style
     * parity math to reject unsolvable permutations. Uses [slide] directly
     * rather than [onTileTapped], since the walk starts from the solved
     * state itself and [onTileTapped]'s "no moves once complete" guard —
     * correct for player input — would otherwise block the very first step.
     */
    fun newShuffled(size: Int, moveCount: Int = 80, random: Random = Random.Default): SlidingPuzzleGameState {
        var state = SlidingPuzzleGameState(tiles = (1 until size * size).toList() + 0, size = size)

        repeat(moveCount) {
            val moveIndex = neighborsOf(state.emptyIndex, size).random(random)
            state = slide(state, moveIndex)
        }
        return state
    }

    private fun slide(state: SlidingPuzzleGameState, index: Int): SlidingPuzzleGameState {
        val tiles = state.tiles.toMutableList()
        val emptyIndex = state.emptyIndex
        tiles[emptyIndex] = tiles[index]
        tiles[index] = 0
        return state.copy(tiles = tiles)
    }

    private fun isAdjacentToEmpty(index: Int, emptyIndex: Int, size: Int): Boolean {
        val row = index / size
        val col = index % size
        val emptyRow = emptyIndex / size
        val emptyCol = emptyIndex % size
        return (row == emptyRow && kotlin.math.abs(col - emptyCol) == 1) ||
            (col == emptyCol && kotlin.math.abs(row - emptyRow) == 1)
    }

    private fun neighborsOf(index: Int, size: Int): List<Int> {
        val row = index / size
        val col = index % size
        val neighbors = mutableListOf<Int>()
        if (row > 0) neighbors += index - size
        if (row < size - 1) neighbors += index + size
        if (col > 0) neighbors += index - 1
        if (col < size - 1) neighbors += index + 1
        return neighbors
    }
}
