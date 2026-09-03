package com.bibleadventures.game.stories

import com.bibleadventures.R
import com.bibleadventures.game.puzzles.gridmaze.Direction
import com.bibleadventures.game.puzzles.roadblock.Direction as RoadblockDirection
import com.bibleadventures.game.puzzles.roadblock.Orientation
import com.bibleadventures.game.puzzles.roadblock.RoadblockBlockSpec
import com.bibleadventures.game.puzzles.roadblock.RoadblockMove

/**
 * Static content for the Good Samaritan chapter. Kept separate from
 * `game/puzzles/gridmaze` so that pure engine stays reusable by any future
 * maze-shaped chapter — this file is the only thing that's Good-Samaritan-
 * specific.
 */
object GoodSamaritanContent {

    // Shown once the traveler is treated, paralleling Luke 10:34's specific,
    // non-branching sequence of care — nothing here is a player choice.
    val helpingBeatLines: List<Int> = listOf(
        R.string.good_samaritan_helping_beat_line_1,
        R.string.good_samaritan_helping_beat_line_2,
        R.string.good_samaritan_helping_beat_line_3,
    )

    // 10x10 map, row-major. '.' path, '#' rocky-terrain wall, 'X' bandit wall
    // (mechanically identical to '#' — see GridMazeGame; the two only differ
    // in which icon the screen draws for that cell), 'M' medicine, 'T' the
    // injured traveler, 'I' the Inn, 'S' the start (a walkable path tile).
    // Verified solvable by hand (BFS from start): start -> the medicine at
    // (0,2) -> a connected route to the traveler at (2,9) -> a separate
    // connected route to the Inn at (9,9), all in one connected component.
    // Not shuffled per playthrough (unlike other chapters' item order) —
    // randomizing tile layout risks an unsolvable maze with no in-app
    // solver/validator in scope for this pass.
    val mapLayout: List<String> = listOf(
        "S.M.......",
        ".##.###X..",
        ".......#.T",
        "##.###.#.#",
        "...#M.....",
        ".#.######.",
        ".#......#.",
        "X#####.##.",
        ".....#....",
        ".......##I",
    )

    // A hand-verified 20-move solution (9 right, 2 down, 1 left, 2 down,
    // 1 right, 5 down) — collects the medicine at (0,2) while crossing row
    // 0, treats the traveler at (2,9), then reaches the Inn at (9,9).
    // Used by the instrumented flow test to replay a known-solvable path
    // deterministically, since the map itself is intentionally not shuffled.
    val solutionPath: List<Direction> = listOf(
        Direction.RIGHT, Direction.RIGHT, Direction.RIGHT, Direction.RIGHT, Direction.RIGHT,
        Direction.RIGHT, Direction.RIGHT, Direction.RIGHT, Direction.RIGHT,
        Direction.DOWN, Direction.DOWN,
        Direction.LEFT,
        Direction.DOWN, Direction.DOWN,
        Direction.RIGHT,
        Direction.DOWN, Direction.DOWN, Direction.DOWN, Direction.DOWN, Direction.DOWN,
    )

    // "Passing By" — a genuine Rush-Hour/Unblock-Me-style sliding block
    // puzzle: the religious leader (labeled "Priest/Levite" on screen —
    // standing in for both, one puzzle, not two) is now a real 2-cell
    // rectangle like every other tile on the board (not a lone single
    // cell), confined to column 2 for the whole board exactly like every
    // other piece is confined to its own single axis — it can never step
    // sideways to dodge around something, and it slides off the bottom
    // progressively (see RoadblockGame.maxSlideDistance's partial-exit
    // handling), fully exiting only once both of its cells have cleared.
    // A larger, roomier 6x8 board this time — more (smaller) tiles, not a
    // cramped one — built specifically around one genuine dependency:
    // "fear_of_ambush" (vertical) sits exactly where "ritual_purity"
    // (horizontal) needs to slide into, so the player has to clear the
    // vertical tile out of the way *before* the horizontal one blocking
    // column 2 can move at all — sliding vertically to enable sliding
    // horizontally, not just two independent obstructions. "rock" (a
    // fixed, unlabeled obstacle — this chapter's Explore scene already
    // uses rocks as generic wall dressing) closes off "ritual_purity"'s
    // only other escape route, so the dependency is load-bearing, not
    // just one option among several. Deliberately kept as a "successful
    // passing by" win condition (it's what actually happened in the
    // parable); the moral is delivered separately by the player's own
    // character once solved (see GoodSamaritanPassingByScreen), never as
    // a celebration.
    //
    // 6 cols x 8 rows. Legend: '.' open path, 'P' the religious leader (2
    // cells, locked to column 2 — see passingByBlockSpecs'
    // forcedOrientation — and the only block ever allowed through the exit
    // gate at passingByExitColumns), 'M' the fixed, never-movable injured
    // man (placed off column 2 entirely — a fixed piece inside the
    // protagonist's own lane would make the puzzle unsolvable, since it
    // could never be cleared), 'R' a fixed rock (generic obstacle, no
    // label), any other letter a movable "excuse" block whose axis is
    // inferred by RoadblockGame.fromLayout from the shape its own letter's
    // cells form. Verified solvable by hand and by passingBySolution's own
    // unit-test replay (GoodSamaritanViewModelTest).
    val passingByLayout: List<String> = listOf(
        "..P...",
        "..P...",
        "....MM",
        "RRAAB.",
        "....B.",
        ".CC...",
        ".....D",
        ".....D",
    )

    val passingByBlockSpecs: List<RoadblockBlockSpec> = listOf(
        RoadblockBlockSpec(id = "religious_leader", letter = 'P', forcedOrientation = Orientation.VERTICAL),
        RoadblockBlockSpec(id = "injured_man", letter = 'M', isFixed = true),
        RoadblockBlockSpec(id = "rock", letter = 'R', isFixed = true),
        RoadblockBlockSpec(id = "ritual_purity", letter = 'A'),
        RoadblockBlockSpec(id = "fear_of_ambush", letter = 'B'),
        RoadblockBlockSpec(id = "strict_schedule", letter = 'C'),
        RoadblockBlockSpec(id = "not_my_problem", letter = 'D'),
    )
    const val passingByProtagonistId = "religious_leader"
    val passingByExitColumns: Set<Int> = setOf(2)

    // Hand-verified 4-move solution:
    // 1. "fear_of_ambush" slides down out of (row 3, col 4) — the one cell
    //    "ritual_purity" needs to slide into.
    // 2. "ritual_purity" slides right into the now-empty (row 3, col 4),
    //    clearing column 2 — its only other escape (left, into cols 0-1)
    //    is permanently walled off by "rock", so this dependency is the
    //    actual solution, not an optional shortcut.
    // 3. "strict_schedule" slides left, clearing column 2 at row 5
    //    independently (no dependency needed there).
    // 4. The religious leader slides straight down through the gate in one
    //    motion — both of its cells clear every remaining row in column 2
    //    (row 2 is clear, the injured man sits at cols 4-5; rows 6-7 are
    //    clear, "not_my_problem" sits at col 5) before fully exiting.
    // Used by both a unit-level replay test (fast regression) and the
    // instrumented flow test (real gesture replay).
    val passingBySolution: List<RoadblockMove> = listOf(
        RoadblockMove("fear_of_ambush", RoadblockDirection.DOWN, 1),
        RoadblockMove("ritual_purity", RoadblockDirection.RIGHT, 1),
        RoadblockMove("strict_schedule", RoadblockDirection.LEFT, 1),
        RoadblockMove("religious_leader", RoadblockDirection.DOWN, 8),
    )
}
