package com.bibleadventures.game.puzzles.slidingpuzzle

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SlidingPuzzleGameTest {

    // 1 2 3      1 2 3
    // 4 5 6  ->  4 5 6   (tapping index 8, adjacent to the empty slot at index 7)
    // 7 . 8      7 8 .
    private fun oneMoveFromSolved(): SlidingPuzzleGameState =
        SlidingPuzzleGameState(tiles = listOf(1, 2, 3, 4, 5, 6, 7, 0, 8), size = 3)

    @Test
    fun `tapping a tile adjacent to the empty slot slides it in`() {
        val next = SlidingPuzzleGame.onTileTapped(oneMoveFromSolved(), index = 8)

        assertEquals(listOf(1, 2, 3, 4, 5, 6, 7, 8, 0), next.tiles)
    }

    @Test
    fun `tapping a tile not adjacent to the empty slot is a pure no-op`() {
        val state = oneMoveFromSolved()
        val next = SlidingPuzzleGame.onTileTapped(state, index = 0) // far corner, not adjacent to index 7

        assertEquals(state, next)
    }

    @Test
    fun `isComplete is true only for the exact solved order with empty last`() {
        val solved = SlidingPuzzleGameState(tiles = listOf(1, 2, 3, 4, 5, 6, 7, 8, 0), size = 3)
        val notSolved = SlidingPuzzleGameState(tiles = listOf(1, 2, 3, 4, 5, 6, 7, 0, 8), size = 3)

        assertTrue(solved.isComplete)
        assertTrue(!notSolved.isComplete)
    }

    @Test
    fun `solving the puzzle from oneMoveFromSolved completes it`() {
        val next = SlidingPuzzleGame.onTileTapped(oneMoveFromSolved(), index = 8)

        assertTrue(next.isComplete)
    }

    @Test
    fun `once complete, further taps are a no-op`() {
        val solved = SlidingPuzzleGameState(tiles = listOf(1, 2, 3, 4, 5, 6, 7, 8, 0), size = 3)

        val unchanged = SlidingPuzzleGame.onTileTapped(solved, index = 7)

        assertEquals(solved, unchanged)
    }

    @Test
    fun `newShuffled with zero moves is already solved`() {
        val state = SlidingPuzzleGame.newShuffled(size = 3, moveCount = 0)

        assertTrue(state.isComplete)
    }

    @Test
    fun `newShuffled produces a valid permutation of every tile exactly once`() {
        val state = SlidingPuzzleGame.newShuffled(size = 3, moveCount = 80, random = Random(42))

        assertEquals((0..8).toSet(), state.tiles.toSet())
        assertEquals(9, state.tiles.size)
    }

    @Test
    fun `newShuffled is not always already solved`() {
        // An 80-step random walk occasionally returns to its start, so checking one
        // seed would be flaky — only "every one of many seeds is solved" would signal
        // a real bug (e.g. moves silently not applying).
        val allSolved = (0 until 20).all { seed ->
            SlidingPuzzleGame.newShuffled(size = 3, moveCount = 80, random = Random(seed)).isComplete
        }

        assertTrue(!allSolved)
    }

    @Test
    fun `newShuffled always produces a solvable arrangement`() {
        repeat(30) { seed ->
            val state = SlidingPuzzleGame.newShuffled(size = 3, moveCount = 80, random = Random(seed))

            assertTrue("seed=$seed tiles=${state.tiles} should be solvable", isSolvable(state.tiles))
        }
    }

    /**
     * The standard sliding-puzzle solvability check for an odd-width grid
     * (this app only ever uses size=3): a permutation is solvable iff its
     * tile sequence (blank excluded) has an even number of inversions.
     * Used here only to verify [SlidingPuzzleGame.newShuffled]'s "solvable
     * by construction" claim independently of how it was built.
     */
    private fun isSolvable(tiles: List<Int>): Boolean {
        val sequence = tiles.filter { it != 0 }
        var inversions = 0
        for (i in sequence.indices) {
            for (j in i + 1 until sequence.size) {
                if (sequence[i] > sequence[j]) inversions++
            }
        }
        return inversions % 2 == 0
    }
}
