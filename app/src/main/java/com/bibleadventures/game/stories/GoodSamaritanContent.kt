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

    // 56x30 map, row-major — the real road from Jerusalem to Jericho
    // (Luke 10:30), replacing the old placeholder 10x10 grid now that real
    // map art exists (`bg_good_samaritan_road_map.jpg`). '.' the walkable
    // road, '#' everything else (rock/terrain — rendered as part of the
    // single background image, not tiled wall sprites), 'X' a bandit
    // ambush's spawn point (patrols — see [banditPatrols] — rather than
    // sitting still), 'M' a medical-supply pickup, 'T' the injured
    // traveler (checkpoint), 'I' the Inn (goal), 'S' the start.
    //
    // Traced from "The Road to Jericho Map - walkable blue area.jpe" (a
    // solid blue overlay tracing the drawn road, provided by the artist —
    // re-supplied once already, to fix a few areas the artist noticed were
    // wrongly excluded, hence 427 walkable cells rather than the original
    // pass's 414): built a blue-channel pixel mask, rasterized it into this
    // 56x30 grid (each cell walkable if ≥35% of its pixel area is blue),
    // and verified by BFS/flood-fill that every walkable cell forms one
    // connected body. Start placed at the walkable cell nearest the drawn
    // Jerusalem castle icon; the checkpoint (traveler) and goal (Inn)
    // positions come from a second artist-marked reference image ("The
    // Road to Jericho Map - Traveller and Inn.jpe", a blue ink marker for
    // the traveler and a green one for the Inn) rather than a visual
    // estimate — an earlier estimate had the Inn wrong, corrected once the
    // artist marked it precisely.
    //
    // The 5 bandit ambushes patrol closed loops circling the rocky/grassy
    // "islands" the road's own switchbacks curl around, per a third
    // artist-marked reference ("The Road to Jericho Map - Bandits.jpe",
    // 5 red-ink rings) — see [banditPatrols] for the loop waypoints
    // themselves, extracted from that art by isolating each ring's red
    // pixels, intersecting with the walkable road, and tracing an angular
    // order around each loop's own enclosed "island". 4 of the 5 loops are
    // the *only* road through that stretch of map (removing the loop's
    // cells disconnects the route), so passing through one is unavoidable
    // — but since the bandit is only ever near one point of its own loop
    // at a time, timing a crossing for when it's on the far side avoids a
    // fight; getting caught starts the turn-based fight screen. 7 supply
    // pickups sit in genuine dead-end pockets (real detours, not just
    // decoration) — one deliberately right next to the Inn, mirroring
    // where the artist placed it on the source art, and two sharing the
    // same dead-end corridor near (10,44)/(11,49) (one at the mouth, one
    // deeper in), rewarding the length of that particular detour with two
    // pickups instead of one. Treat exact counts/positions as a first
    // pass, same as every other number in this file — verify feel
    // on-device before calling it final.
    val mapLayout: List<String> = listOf(
        "########################################################",
        "########################################################",
        "########################################################",
        "########################################################",
        "########################################################",
        "########################################################",
        "#################........#######.......####......#######",
        "################..#####...#####....##...##...##...######",
        "#########S.####..###M####..###X.##..###.#..######..#####",
        "#########..####..#.....##..###..#.......#..######..#####",
        "########...####.##.##......###....##..###..#M.###..#####",
        "######.....###..#..##.....#####...##.......##....M######",
        "#####..##......##..##..#..#####...M#.....#.#############",
        "#####..#########..##..#....####...##X.#.##.....#########",
        "#####..#####......#..##..#........##..#.####......######",
        "#####...###...#..##..##X####...#####....#########T.#####",
        "######....#..#...##..##..###..#######...#########..#####",
        "########....##.#####..#......#####....###..######..#####",
        "#########..###.##M##..##....###......##...........######",
        "#######..X.###..#.....###..###...#..###.#####...########",
        "######....#####.##...####.###..####.....################",
        "#####..#..#####.########..###X.####....#################",
        "#####..#........#######..###M..####.####################",
        "#####..##....#########..#####..###..######....##########",
        "#####..#############...######.........##......IM########",
        "######...............##########..............###########",
        "########################################################",
        "########################################################",
        "########################################################",
        "########################################################",
    )

    // Each `X` in [mapLayout] is a spawn point; keyed by that cell's
    // deterministic id ("trap_${row}_$col", from [DungeonGame.fromLayout])
    // to the closed loop of waypoints it cycles through at
    // [DungeonGame.BANDIT_PATROL_SPEED_CELLS_PER_SECOND] — see the
    // extraction method described in [mapLayout]'s own comment. Ordered so
    // the bandit's very first move heads to the *second* point of its loop
    // (its spawn cell is already the last entry), avoiding a trivial
    // zero-distance "arrival" on the very first frame.
    val banditPatrols: Map<String, List<Vector2>> = mapOf(
        "trap_8_30" to listOf(
            Vector2(33.5f, 7.5f), Vector2(34.5f, 6.5f), Vector2(37.5f, 7.5f), Vector2(39.5f, 8.5f),
            Vector2(37.5f, 10.5f), Vector2(33.5f, 10.5f), Vector2(33.5f, 9.5f), Vector2(30.5f, 8.5f),
        ),
        "trap_13_36" to listOf(
            Vector2(36.5f, 11.5f), Vector2(38.5f, 11.5f), Vector2(39.5f, 12.5f),
            Vector2(39.5f, 15.5f), Vector2(37.5f, 14.5f), Vector2(36.5f, 13.5f),
        ),
        "trap_15_23" to listOf(
            Vector2(24.5f, 13.5f), Vector2(26.5f, 13.5f), Vector2(29.5f, 14.5f), Vector2(28.5f, 17.5f),
            Vector2(26.5f, 18.5f), Vector2(24.5f, 17.5f), Vector2(23.5f, 15.5f),
        ),
        "trap_19_9" to listOf(
            Vector2(11.5f, 17.5f), Vector2(12.5f, 14.5f), Vector2(14.5f, 16.5f), Vector2(14.5f, 17.5f),
            Vector2(14.5f, 19.5f), Vector2(14.5f, 22.5f), Vector2(11.5f, 22.5f), Vector2(8.5f, 22.5f), Vector2(9.5f, 19.5f),
        ),
        "trap_21_29" to listOf(
            Vector2(31.5f, 19.5f), Vector2(33.5f, 18.5f), Vector2(35.5f, 20.5f),
            Vector2(35.5f, 24.5f), Vector2(32.5f, 25.5f), Vector2(29.5f, 24.5f), Vector2(29.5f, 21.5f),
        ),
    )

    // A hand-verified route through the road map, as turning points only
    // (not every cell) — each consecutive pair is a straight, wall-free
    // corridor run, so a real steering test only needs to aim at the next
    // point and hold until it arrives; anything sitting exactly on a
    // straight run between two waypoints (a supply pickup, say) is picked
    // up automatically as the player passes through it, no separate stop
    // needed. Verified (not just hand-traced) by porting DungeonGame.tick's
    // exact physics — including bandit patrol movement and
    // DungeonGame.BANDIT_DETECTION_RADIUS's wider "spotted, not just
    // touched" trigger — to a matching script and replaying this route
    // frame-by-frame: reaches every supply, activates the checkpoint, and
    // completes at the Inn, with all 4 of the unavoidable bandit loops
    // actually spotting the player at least once in that replay (fought to
    // full resolution each time, per the loop below).
    //
    // A route replay's own steering only aims at the *next* waypoint in
    // this list — if a bandit spots the player mid-leg, the resulting fight
    // freezes movement wherever that happened, and once it's won, steering
    // resumes toward whatever waypoint comes *next* in this list, not back
    // toward the one that got interrupted (see DungeonGameTest's
    // `steerToward` for the exact mechanics this list is authored against).
    // That's why a couple of stretches here look like an odd detour rather
    // than the shortest path: they're the exact recovery route from a
    // verified fight-interruption point back onto the intended corridor,
    // baked in by the same verification script rather than guessed by hand.
    //
    // Order: the medical supply next to the Inn (`supply_24_47`) is
    // deliberately visited *before* the checkpoint, not after: it's a true
    // dead end whose only neighbor is the Inn's own cell, so visiting it
    // after the checkpoint is activated would trip [DungeonGameState.isComplete]
    // the moment the player gets close enough to reach it — reaching the
    // Inn's proximity while the checkpoint is active always ends the run,
    // there's no way to "pass through" it first. Every other supply and
    // bandit is visited in roughly the road's own west-to-east order.
    // Used by both DungeonGameTest's end-to-end replay and the
    // instrumented flow test's real-gesture steering.
    val dungeonRouteWaypoints: List<Vector2> = listOf(
        Vector2(9.5f, 8.5f), // start
        Vector2(9.5f, 12.5f),
        Vector2(14.5f, 12.5f),
        Vector2(14.5f, 11.5f),
        Vector2(15.5f, 11.5f),
        Vector2(15.5f, 8.5f),
        Vector2(16.5f, 8.5f),
        Vector2(16.5f, 7.5f),
        Vector2(17.5f, 7.5f),
        Vector2(17.5f, 6.5f),
        Vector2(23.5f, 6.5f),
        Vector2(23.5f, 7.5f),
        Vector2(25.5f, 7.5f),
        Vector2(25.5f, 11.5f),
        Vector2(22.5f, 11.5f),
        Vector2(22.5f, 12.5f),
        Vector2(21.5f, 12.5f),
        Vector2(21.5f, 13.5f),
        Vector2(20.5f, 13.5f),
        Vector2(20.5f, 19.5f),
        Vector2(17.5f, 19.5f),
        Vector2(17.5f, 18.5f), // supply (18,17)
        Vector2(17.5f, 19.5f),
        Vector2(20.5f, 19.5f),
        Vector2(20.5f, 13.5f),
        Vector2(21.5f, 13.5f),
        Vector2(21.5f, 9.5f),
        Vector2(20.5f, 9.5f),
        Vector2(20.5f, 8.5f), // supply (8,20)
        Vector2(20.5f, 9.5f),
        Vector2(21.5f, 9.5f),
        Vector2(21.5f, 11.5f),
        Vector2(24.5f, 11.5f),
        Vector2(24.5f, 14.5f), // bandit loop spawn (15,23)
        Vector2(23.5f, 14.5f),
        Vector2(23.5f, 15.5f),
        Vector2(23.5f, 13.5f),
        Vector2(26.5f, 13.5f),
        Vector2(26.5f, 14.5f),
        Vector2(31.5f, 14.5f),
        Vector2(31.5f, 8.5f),
        Vector2(30.5f, 8.5f), // bandit loop spawn (8,30)
        Vector2(30.5f, 10.5f),
        Vector2(33.5f, 10.5f),
        Vector2(33.5f, 9.5f),
        Vector2(36.5f, 9.5f),
        Vector2(36.5f, 11.5f),
        Vector2(41.5f, 11.5f),
        Vector2(41.5f, 8.5f),
        // Fight-interruption recovery detour: the bandit loop spawned at
        // (8,30) spots the player partway along this leg, well short of
        // (41.5, 8.5) — this bridges from that verified interruption point
        // back to it rather than cutting through a wall.
        Vector2(39.5f, 8.5f),
        Vector2(39.5f, 9.5f),
        Vector2(37.5f, 9.5f),
        Vector2(37.5f, 11.5f),
        Vector2(41.5f, 11.5f),
        Vector2(41.5f, 8.5f),
        Vector2(42.5f, 8.5f),
        Vector2(42.5f, 7.5f),
        Vector2(43.5f, 7.5f),
        Vector2(43.5f, 6.5f),
        Vector2(47.5f, 6.5f),
        Vector2(47.5f, 7.5f),
        Vector2(49.5f, 7.5f),
        Vector2(49.5f, 11.5f), // supply (11,49) — the mouth of the same dead-end pocket as (10,44) below; sitting exactly on this turn, so no separate stop needed
        Vector2(45.5f, 11.5f),
        Vector2(45.5f, 10.5f),
        Vector2(44.5f, 10.5f), // supply (10,44) — deeper in the same pocket
        Vector2(45.5f, 10.5f),
        Vector2(45.5f, 11.5f),
        Vector2(49.5f, 11.5f),
        Vector2(49.5f, 7.5f),
        Vector2(48.5f, 7.5f),
        Vector2(48.5f, 6.5f),
        Vector2(44.5f, 6.5f),
        Vector2(44.5f, 7.5f),
        Vector2(42.5f, 7.5f),
        Vector2(42.5f, 11.5f),
        Vector2(37.5f, 11.5f),
        Vector2(37.5f, 9.5f),
        Vector2(33.5f, 9.5f),
        Vector2(33.5f, 12.5f),
        Vector2(34.5f, 12.5f), // supply (12,34)
        Vector2(33.5f, 12.5f),
        Vector2(33.5f, 9.5f),
        Vector2(36.5f, 9.5f),
        Vector2(36.5f, 13.5f), // bandit loop spawn (13,36)
        Vector2(36.5f, 15.5f),
        Vector2(37.5f, 15.5f),
        Vector2(37.5f, 17.5f),
        Vector2(36.5f, 17.5f),
        Vector2(36.5f, 18.5f),
        Vector2(32.5f, 18.5f),
        Vector2(32.5f, 19.5f),
        Vector2(30.5f, 19.5f),
        Vector2(30.5f, 21.5f),
        Vector2(29.5f, 21.5f), // bandit loop spawn (21,29)
        Vector2(29.5f, 22.5f),
        Vector2(28.5f, 22.5f), // supply (22,28)
        Vector2(29.5f, 22.5f),
        Vector2(29.5f, 24.5f),
        Vector2(31.5f, 24.5f),
        Vector2(31.5f, 25.5f),
        Vector2(40.5f, 25.5f),
        Vector2(40.5f, 24.5f),
        Vector2(47.5f, 24.5f), // supply (24,47) — dead end off the Inn's own cell, visited before the checkpoint (see note above)
        Vector2(44.5f, 24.5f),
        Vector2(44.5f, 25.5f),
        Vector2(37.5f, 25.5f),
        Vector2(37.5f, 24.5f),
        Vector2(35.5f, 24.5f),
        Vector2(35.5f, 20.5f),
        Vector2(39.5f, 20.5f),
        Vector2(39.5f, 18.5f),
        Vector2(49.5f, 18.5f),
        Vector2(49.5f, 15.5f), // checkpoint — the injured traveler (15,49)
        Vector2(49.5f, 18.5f),
        Vector2(39.5f, 18.5f),
        Vector2(39.5f, 20.5f),
        Vector2(38.5f, 20.5f),
        Vector2(38.5f, 21.5f),
        Vector2(35.5f, 21.5f),
        Vector2(35.5f, 25.5f),
        Vector2(40.5f, 25.5f),
        Vector2(40.5f, 24.5f),
        Vector2(46.5f, 24.5f), // the Inn — goal (24,46)
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
