package com.bibleadventures.game.stories

import com.bibleadventures.R
import com.bibleadventures.game.puzzles.dungeon.Vector2
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

    // 10x10 map, row-major, now walked in real time with an analog joystick
    // (see game/puzzles/dungeon) instead of the old D-pad. '.' path, '#'
    // rocky-terrain wall, 'X' a bandit ambush — a proximity-triggered
    // turn-based fight now, not a wall variant like it used to be under the
    // old discrete gridmaze engine — 'M' a medical-supply pickup, 'T' the
    // injured traveler (checkpoint), 'I' the Inn (goal), 'S' the start.
    //
    // This is the same wall footprint as the original discrete-maze layout,
    // byte-for-byte: the two 'X' cells that used to be wall cells (row 1
    // col 7, row 7 col 0) became plain '#' walls (a trap must sit on a
    // walkable cell, not inside a wall), and every new 'M'/'X' marker was
    // placed only on a cell that was already open ('.') in the original —
    // so the original's hand-verified connectivity carries over unchanged;
    // no fresh BFS was needed to confirm every cell is still reachable.
    //
    // Full connectivity WAS re-verified by hand for the new bandit
    // placements specifically, since a proximity trigger (unlike a wall)
    // can turn out to be a mandatory chokepoint rather than an optional
    // detour depending on exactly where it sits: the bandit at (2,6) turns
    // out to be entirely optional — (4,4)-(4,9)'s cluster (and the Inn) has
    // an independent route in via (2,8)-(3,8)-(4,8) that never comes near
    // it, so a player can route around it for free. The bandits at (6,4)
    // and (8,6) are genuine chokepoints, though: they're the *only* way to
    // reach the two supply pickups at (8,0) and (9,3) (row 7 has no other
    // opening into that pocket). So the real "mandatory" cost is 2 bandits
    // x 2 toughness (4 supplies) + the checkpoint's cost (1) = 5, not 3
    // bandits' worth — comfortably covered by the 10 pickups on the map
    // even after a failed attempt needs re-supplying. Treat the exact
    // counts/positions as a first pass, same as every other number in this
    // file — verify feel on-device before calling it final.
    val mapLayout: List<String> = listOf(
        "S.M..M....",
        ".##.####..",
        ".M....X#.T",
        "##.###.#.#",
        "M..#M...M.",
        ".#.######.",
        "M#..X...#M",
        "######.##.",
        "M....#X...",
        "...M...##I",
    )

    // A hand-verified route through the revised map, as turning points only
    // (not every cell) — each consecutive pair is a straight, wall-free
    // corridor run, so a real steering test only needs to aim at the next
    // point and hold until it arrives; anything sitting exactly on a
    // straight run between two waypoints (a supply pickup, say) is picked
    // up automatically as the player passes through it, no separate stop
    // needed. Order: all 8 supply pickups reachable without a fight, then
    // the (6,4) bandit, then the (8,6) bandit (fought to full resolution
    // each time before continuing — see DungeonGame.BANDIT_INITIAL_TOUGHNESS),
    // which opens the way to the last 2 pickups behind them, then back to
    // the checkpoint (with supplies to spare), then the Inn. Deliberately
    // routes around the optional (2,6) bandit entirely (see the map comment
    // above) rather than fighting it for no required benefit. Used by both
    // DungeonGameTest's end-to-end replay and the instrumented flow test's
    // real-gesture steering.
    val dungeonRouteWaypoints: List<Vector2> = listOf(
        Vector2(0.5f, 0.5f), // start
        Vector2(5.5f, 0.5f), // supply (0,5)
        Vector2(3.5f, 0.5f),
        Vector2(3.5f, 2.5f),
        Vector2(1.5f, 2.5f), // supply (2,1)
        Vector2(2.5f, 2.5f),
        Vector2(2.5f, 4.5f),
        Vector2(0.5f, 4.5f), // supply (4,0)
        Vector2(0.5f, 6.5f), // supply (6,0)
        Vector2(0.5f, 4.5f),
        Vector2(2.5f, 4.5f),
        Vector2(2.5f, 2.5f),
        Vector2(3.5f, 2.5f),
        Vector2(3.5f, 0.5f),
        Vector2(9.5f, 0.5f),
        Vector2(9.5f, 1.5f),
        Vector2(8.5f, 1.5f),
        Vector2(8.5f, 4.5f), // supply (4,8)
        Vector2(4.5f, 4.5f), // supply (4,4) — a dead end, direction reverses here
        Vector2(9.5f, 4.5f),
        Vector2(9.5f, 6.5f), // supply (6,9)
        Vector2(9.5f, 4.5f),
        Vector2(8.5f, 4.5f),
        Vector2(8.5f, 0.5f),
        Vector2(3.5f, 0.5f),
        Vector2(3.5f, 2.5f),
        Vector2(2.5f, 2.5f),
        Vector2(2.5f, 6.5f),
        Vector2(4.5f, 6.5f), // bandit (6,4)
        Vector2(6.5f, 6.5f),
        Vector2(6.5f, 8.5f), // bandit (8,6)
        Vector2(6.5f, 9.5f),
        Vector2(0.5f, 9.5f),
        Vector2(0.5f, 8.5f), // supply (8,0)
        Vector2(0.5f, 9.5f),
        Vector2(6.5f, 9.5f),
        Vector2(6.5f, 8.5f), // bandit (8,6), already resolved — passes through safely
        Vector2(6.5f, 6.5f),
        Vector2(2.5f, 6.5f), // continuing west, past the already-resolved (6,4) bandit along the way
        Vector2(2.5f, 2.5f),
        Vector2(3.5f, 2.5f),
        Vector2(3.5f, 0.5f),
        Vector2(9.5f, 0.5f),
        Vector2(9.5f, 2.5f), // checkpoint — the injured traveler
        Vector2(8.5f, 2.5f),
        Vector2(8.5f, 4.5f),
        Vector2(9.5f, 4.5f),
        Vector2(9.5f, 9.5f), // the Inn — goal
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
