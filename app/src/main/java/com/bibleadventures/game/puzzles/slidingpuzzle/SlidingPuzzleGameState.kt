package com.bibleadventures.game.puzzles.slidingpuzzle

/**
 * A square sliding-tile puzzle. [tiles] is a flat, row-major list of
 * length `size * size` — values `1..size*size - 1` are numbered tiles,
 * `0` is the empty slot. There's no "wrong move" concept here: every
 * legal slide is fully reversible, so unlike every other engine in this
 * app, no failure-state adaptation was needed for this one — it simply
 * has none to begin with.
 */
data class SlidingPuzzleGameState(val tiles: List<Int>, val size: Int) {
    val emptyIndex: Int
        get() = tiles.indexOf(0)

    val isComplete: Boolean
        get() = tiles == (1 until size * size).toList() + 0
}
