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
    // decoration) — one right next to the injured traveler at (15,49)
    // (originally placed one cell past the Inn instead, mirroring the
    // artist's own source art, but that spot sat behind the goal cell
    // itself: reaching goalPosition while the checkpoint is already active
    // immediately sets DungeonGameState.isComplete, which stops
    // GoodSamaritanExploreScreen's per-frame tick loop entirely — a real
    // on-device report confirmed it was never actually reachable; moved
    // next to the Inn instead as a first fix, then moved again, per
    // further on-device feedback, to sit beside the traveler himself
    // instead — reaching the checkpoint doesn't freeze anything, so
    // there's no equivalent reachability risk here), and two sharing the
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
        "######....#..#...##..##..###..#######...#########M.#####",
        "########....##.#####..#......#####....###..######..#####",
        "#########..###.##M##..##....###......##...........######",
        "#######..X.###..#.....###..###...#..###.#####...########",
        "######....#####.##...####.###..####.....################",
        "#####..#..#####.########..###X.####....#################",
        "#####..#........#######..###M..####.####################",
        "#####..##....#########..#####..###..######....##########",
        "#####..#############...######.........##......I.########",
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
        Vector2(44.5f, 24.5f), // turning point toward the Inn — no longer a supply stop (see mapLayout's own note: that pickup now sits by the traveler instead)
        Vector2(44.5f, 25.5f),
        Vector2(37.5f, 25.5f),
        Vector2(37.5f, 24.5f),
        Vector2(35.5f, 24.5f),
        Vector2(35.5f, 20.5f),
        Vector2(39.5f, 20.5f),
        Vector2(39.5f, 18.5f),
        Vector2(49.5f, 18.5f),
        Vector2(49.5f, 15.5f), // checkpoint — the injured traveler (15,49); this leg's own straight line passes right through supply (16,49) on the way
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
    // Deliberately kept as a "successful passing by" win condition (it's
    // what actually happened in the parable); the moral is delivered
    // separately by the player's own character once solved (see
    // GoodSamaritanPassingByScreen), never as a celebration.
    //
    // 4 levels now (originally 1) — on-device feedback from a real 7-year-
    // old player went through several rounds: first "good but too easy,"
    // wanting real difficulty variety (one level per excuse the priest/
    // Levite actually gives in the parable); then, once levels 2-4 shipped
    // as permutations of one single top-to-bottom relay chain, "puzzle 3
    // and 4 are exactly the same puzzle" (true — permuting which excuse
    // sits where never changes the actual moves needed) and "the extra
    // tile doesn't have to be moved... what's the point of adding it"
    // (also true — that relay already filled every row the board had, so
    // the added 5th piece had nowhere to go but a side corridor where it
    // could never matter). Levels 3 and 4 were rebuilt from scratch as
    // genuinely different puzzles, each independently checked with a
    // general BFS solver (not just one hand-traced path) confirming every
    // single piece is load-bearing. Played sequentially in one visit
    // (GoodSamaritanViewModel tracks `passingByLevelIndex`), the same
    // "solve step N, advance to N+1 in place" shape as Daniel's Lions Den
    // math sequence — no new screens/routes, no separate per-level
    // progress tracking; only finishing all 4 marks the "passing_by" scene
    // complete. Level 1 is the original, untouched board (already tuned
    // and confirmed on-device):
    //   Level 1 (spotlight: ritual_purity) — depth-1 chain: fear_of_ambush
    //     must move before ritual_purity can slide into the cell it
    //     needs, then strict_schedule clears independently (no
    //     dependency). not_my_problem never even needs to move.
    //   Level 2 (spotlight: fear_of_ambush) — depth-2 chain:
    //     strict_schedule must move before ritual_purity before
    //     fear_of_ambush, which is now the piece sitting in column 2
    //     itself; not_my_problem clears independently, elsewhere, plus a
    //     5th piece ("obstacle_4") that's also independent — added after
    //     on-device feedback that level 2 alone didn't feel meaningfully
    //     harder than level 1.
    //   Level 3 (spotlight: not_my_problem) — a structurally different
    //     puzzle from levels 1/2: TWO separate column-2 blockers (rows 3
    //     and 6) sharing one piece as a gate between them. Clearing the
    //     first blocker requires a piece to move, then the leader makes a
    //     genuine *partial* descent (blocked further down by the second
    //     blocker), then that same piece has to slide back the way it
    //     came before the shared gate can finish opening the second one —
    //     a real "revisit this piece later" dependency, not just a longer
    //     one-way chain.
    //   Level 4 (spotlight: strict_schedule) — level 3's mechanic mirrored
    //     left-to-right (the shared gate runs down the opposite side of
    //     the board, the spotlighted excuse swapped from the bottom
    //     blocker to the top one), *plus* a genuine 6th movable piece
    //     ("obstacle_6") added after feedback that a pure mirror still
    //     felt like the same puzzle — it sits directly beneath the
    //     leader's own starting cell, blocking it from taking even a
    //     single step until its own simple gate ("gate_6") clears first.
    // Every level's spotlighted excuse — and, in levels 3/4, the other
    // column-2 blocker too — has its "free" escape direction permanently
    // walled off by a fixed rock — exactly like level 1's original design
    // — so the chain is load-bearing, not one option
    // among several; verified for all 4 levels (including that every
    // gated move is actually illegal, not just solvable some other way)
    // by porting RoadblockGame's exact move-legality rules to a matching
    // script, the same discipline this file's other hand-authored puzzle
    // content uses.
    data class PassingByLevel(
        val layout: List<String>,
        val blockSpecs: List<RoadblockBlockSpec>,
        val exitColumns: Set<Int>,
        /** Hand-verified solution, replayed by both DungeonGameTest-style unit tests and the instrumented flow test. */
        val solution: List<RoadblockMove>,
    )

    const val passingByProtagonistId = "religious_leader"

    val passingByLevels: List<PassingByLevel> = listOf(
        // Level 1 — original board, untouched. Legend: '.' open path, 'P'
        // the religious leader (2 cells, locked to column 2 via
        // forcedOrientation, the only block ever allowed through the exit
        // gate), 'M' the fixed injured man, 'R' a fixed rock, any other
        // letter a movable excuse whose axis is inferred from its own
        // shape.
        PassingByLevel(
            layout = listOf(
                "..P...",
                "..P...",
                "....MM",
                "RRAAB.",
                "....B.",
                ".CC...",
                ".....D",
                ".....D",
            ),
            blockSpecs = listOf(
                RoadblockBlockSpec(id = "religious_leader", letter = 'P', forcedOrientation = Orientation.VERTICAL),
                RoadblockBlockSpec(id = "injured_man", letter = 'M', isFixed = true),
                RoadblockBlockSpec(id = "rock", letter = 'R', isFixed = true),
                RoadblockBlockSpec(id = "ritual_purity", letter = 'A'),
                RoadblockBlockSpec(id = "obstacle_1", letter = 'B'),
                RoadblockBlockSpec(id = "obstacle_2", letter = 'C'),
                RoadblockBlockSpec(id = "obstacle_3", letter = 'D'),
            ),
            exitColumns = setOf(2),
            // 1. "obstacle_1" slides down out of (row 3, col 4) — the one
            //    cell "ritual_purity" needs to slide into.
            // 2. "ritual_purity" slides right into the now-empty (row 3,
            //    col 4), clearing column 2 — its only other escape (left,
            //    into cols 0-1) is permanently walled off by "rock", so
            //    this dependency is the actual solution, not an optional
            //    shortcut.
            // 3. "obstacle_2" slides left, clearing column 2 at row 5
            //    independently (no dependency needed there).
            // 4. The religious leader slides straight down through the
            //    gate in one motion.
            solution = listOf(
                RoadblockMove("obstacle_1", RoadblockDirection.DOWN, 1),
                RoadblockMove("ritual_purity", RoadblockDirection.RIGHT, 1),
                RoadblockMove("obstacle_2", RoadblockDirection.LEFT, 1),
                RoadblockMove("religious_leader", RoadblockDirection.DOWN, 8),
            ),
        ),
        // Level 2 — spotlight fear_of_ambush, now the piece sitting in
        // column 2 itself. Two fixed rocks this time ("rock" and "rock2",
        // distinct ids/letters since RoadblockGame.fromLayout merges every
        // cell sharing one letter into a single block — reusing 'R' for
        // both would silently weld them into one bogus non-contiguous
        // block): "rock" walls off fear_of_ambush's own free escape (same
        // role as level 1's rock), "rock2" walls off ritual_purity's
        // downward escape so it's forced to depend on strict_schedule
        // rather than dodging the chain entirely.
        //
        // "obstacle_3" (the independent, no-dependency piece) sits next to
        // the injured man and under the religious leader instead of next
        // to "obstacle_2" — on-device testing found it originally sitting
        // directly beside "obstacle_2" (same row, adjacent columns) with
        // no gap between them at all, and a real touch could land on the
        // wrong one of the two, making a perfectly legal move on the
        // *other* tile look like it silently refused to work. A first fix
        // moved "obstacle_3" onto its own fully isolated row instead,
        // which solved the mis-touch but grew the board to 10 rows — on a
        // real phone that pushed the puzzle's minimum per-cell touch size
        // past the available screen height, clipping the bottom rows
        // clean off (the bug report: "not all tiles can be seen," and a
        // fixed rock tile that had visibly gone missing). Touching only
        // fixed, never-dragged tiles (the injured man, the leader itself
        // — which can't move until everything else has cleared anyway, so
        // an accidental tap on it is just as harmlessly inert) costs
        // nothing, so this is the one adjacency change kept, on the
        // *original* 8-row board — solved by fixing the actual bug
        // (movable tiles that were too easy to swap by touch), not by
        // spending more screen space on it.
        PassingByLevel(
            layout = listOf(
                "..P...",
                "..P...",
                "MMDD..",
                "....C.",
                "....C.",
                "RRBBA.",
                "....A.",
                "..EEK.",
            ),
            blockSpecs = listOf(
                RoadblockBlockSpec(id = "religious_leader", letter = 'P', forcedOrientation = Orientation.VERTICAL),
                RoadblockBlockSpec(id = "injured_man", letter = 'M', isFixed = true),
                RoadblockBlockSpec(id = "rock", letter = 'R', isFixed = true),
                RoadblockBlockSpec(id = "rock2", letter = 'K', isFixed = true),
                RoadblockBlockSpec(id = "obstacle_1", letter = 'A'),
                RoadblockBlockSpec(id = "fear_of_ambush", letter = 'B'),
                RoadblockBlockSpec(id = "obstacle_2", letter = 'C'),
                RoadblockBlockSpec(id = "obstacle_3", letter = 'D'),
                // A 5th piece, added on top of the original 4 — on-device
                // feedback found the road from level 1 to level 2 too
                // small a step up ("not harder or more difficult"). Sits
                // below "obstacle_1", walled off from the rest of the
                // chain by "rock2" (its only neighbor besides the board
                // edge), so it's a genuinely separate, independent piece
                // to clear rather than a 5th link bolted onto the same
                // chain.
                RoadblockBlockSpec(id = "obstacle_4", letter = 'E'),
            ),
            exitColumns = setOf(2),
            // 1. "obstacle_2" slides up — nothing blocks it (the root of
            //    the chain) — done BEFORE "obstacle_3" specifically so its
            //    own upward path (through row 2, col 4) is still clear;
            //    reversing this order would have "obstacle_3" land right
            //    in the cell "obstacle_2" itself needs to pass through.
            // 2. "obstacle_3" clears independently (right, no real
            //    dependency — its own left escape is walled off by the
            //    injured man instead, purely to keep it off that side).
            // 3. "obstacle_4" clears independently too (left) — its own
            //    right escape is walled off by "rock2".
            // 4. "obstacle_1" can now slide up into the cell "obstacle_2"
            //    just vacated ("rock2" also walls off its other escape,
            //    down).
            // 5. "fear_of_ambush" can now slide right into the cell
            //    "obstacle_1" just vacated ("rock" walls off its only
            //    other escape, left) — clearing column 2.
            // 6. The religious leader slides straight down through the gate.
            solution = listOf(
                RoadblockMove("obstacle_2", RoadblockDirection.UP, 3),
                RoadblockMove("obstacle_3", RoadblockDirection.RIGHT, 2),
                RoadblockMove("obstacle_4", RoadblockDirection.LEFT, 2),
                RoadblockMove("obstacle_1", RoadblockDirection.UP, 2),
                RoadblockMove("fear_of_ambush", RoadblockDirection.RIGHT, 2),
                RoadblockMove("religious_leader", RoadblockDirection.DOWN, 8),
            ),
        ),
        // Level 3 — spotlight not_my_problem. On-device feedback (twice
        // over) found the earlier "single top-to-bottom relay" shape for
        // levels 3/4 both too easy to tell apart (permuting which excuse
        // sits where didn't change the actual moves needed) *and* prone to
        // shipping a piece that never actually needed to move — decorative
        // filler dressed up as an obstacle. This is a structurally
        // different puzzle: not_my_problem AND obstacle_1 are two
        // *separate* column-2 blockers (rows 3 and 6), not one — and
        // clearing one of them requires an ordinary drag that DEEPENS the
        // other's problem, then a second drag on the same piece to walk it
        // back once other things have shifted. Independently verified with
        // a general BFS solver over the real move-legality rules (not just
        // one hand-traced path) — confirmed every one of the 5 pieces is
        // load-bearing (removing any one leaves the puzzle unsolvable),
        // and the reported solution is genuinely the shortest available,
        // not an arbitrary one.
        PassingByLevel(
            layout = listOf(
                "..P...",
                "..P.C.",
                "MM..C.",
                "RRAAB.",
                "....B.",
                "....E.",
                "KKDDE.",
                "......",
            ),
            blockSpecs = listOf(
                RoadblockBlockSpec(id = "religious_leader", letter = 'P', forcedOrientation = Orientation.VERTICAL),
                RoadblockBlockSpec(id = "injured_man", letter = 'M', isFixed = true),
                RoadblockBlockSpec(id = "rock", letter = 'R', isFixed = true),
                RoadblockBlockSpec(id = "rock2", letter = 'K', isFixed = true),
                RoadblockBlockSpec(id = "obstacle_1", letter = 'A'),
                RoadblockBlockSpec(id = "obstacle_2", letter = 'B'),
                RoadblockBlockSpec(id = "obstacle_3", letter = 'C'),
                RoadblockBlockSpec(id = "obstacle_4", letter = 'E'),
                RoadblockBlockSpec(id = "not_my_problem", letter = 'D'),
            ),
            exitColumns = setOf(2),
            // 1. "obstacle_3" moves up — nothing blocks it.
            // 2. "obstacle_4" moves down one cell — not to clear anything
            //    of its own yet, just to open the cell "obstacle_2" needs
            //    for its own first move.
            // 3. "obstacle_2" can now slide down, clearing the way for
            //    "obstacle_1".
            // 4. "obstacle_1" slides right, clearing column 2 at row 3 —
            //    "rock" walls off its only other escape (left).
            // 5. The religious leader can now descend as far as row 3
            //    (not_my_problem, at row 6, is still in the way further
            //    down) — a genuine partial exit, not the final one.
            // 6. "obstacle_1" has to slide back left again: its own
            //    current position (from step 4) is exactly where
            //    "obstacle_2" needs to pass back through on its way up.
            // 7. "obstacle_2" slides back up, past where "obstacle_1" used
            //    to be blocking it — clearing the way for "obstacle_4".
            // 8. "obstacle_4" slides up, clearing column 2 at row 6.
            // 9. not_my_problem slides right — "rock2" walls off its only
            //    other escape (left).
            // 10. The religious leader finishes its descent and exits.
            solution = listOf(
                RoadblockMove("obstacle_3", RoadblockDirection.UP, 1),
                RoadblockMove("obstacle_4", RoadblockDirection.DOWN, 1),
                RoadblockMove("obstacle_2", RoadblockDirection.DOWN, 1),
                RoadblockMove("obstacle_1", RoadblockDirection.RIGHT, 1),
                RoadblockMove("religious_leader", RoadblockDirection.DOWN, 4),
                RoadblockMove("obstacle_1", RoadblockDirection.LEFT, 1),
                RoadblockMove("obstacle_2", RoadblockDirection.UP, 2),
                RoadblockMove("obstacle_4", RoadblockDirection.UP, 2),
                RoadblockMove("not_my_problem", RoadblockDirection.RIGHT, 1),
                RoadblockMove("religious_leader", RoadblockDirection.DOWN, 4),
            ),
        ),
        // Level 4 — spotlight strict_schedule, the hardest board: level 3's
        // "two column-2 blockers sharing one gate" idea, mirrored
        // left-to-right (the shared gate runs down column 1 instead of
        // column 4, both blockers escape the opposite direction, the
        // spotlighted one is on top instead of on the bottom) — plus a
        // genuine 3rd blocker ("obstacle_6") sitting right below the
        // leader's own start, with its own simple gate ("gate_6"). Added
        // after feedback that a pure mirror of level 3 still felt like
        // "the same puzzle" — this one has 6 movable pieces instead of 5,
        // every one load-bearing (confirmed with the same BFS solver used
        // for level 3): "obstacle_6" actually blocks the religious leader
        // from taking so much as its first step, which none of the other
        // 3 levels' pieces do.
        PassingByLevel(
            layout = listOf(
                "..P...",
                ".CP.G.",
                ".CFFG.",
                ".BAARR",
                ".B....",
                ".E....",
                ".ESSKK",
                "....MM",
            ),
            blockSpecs = listOf(
                RoadblockBlockSpec(id = "religious_leader", letter = 'P', forcedOrientation = Orientation.VERTICAL),
                RoadblockBlockSpec(id = "injured_man", letter = 'M', isFixed = true),
                RoadblockBlockSpec(id = "rock", letter = 'R', isFixed = true),
                RoadblockBlockSpec(id = "rock2", letter = 'K', isFixed = true),
                RoadblockBlockSpec(id = "obstacle_1", letter = 'A'),
                RoadblockBlockSpec(id = "obstacle_2", letter = 'B'),
                RoadblockBlockSpec(id = "obstacle_3", letter = 'C'),
                RoadblockBlockSpec(id = "obstacle_4", letter = 'E'),
                RoadblockBlockSpec(id = "strict_schedule", letter = 'S'),
                RoadblockBlockSpec(id = "obstacle_6", letter = 'F'),
                RoadblockBlockSpec(id = "gate_6", letter = 'G'),
            ),
            exitColumns = setOf(2),
            // 1. "gate_6" slides up — nothing blocks it — clearing the way
            //    for "obstacle_6" to slide right later.
            // 2-4. Same shape as level 3's solution, mirrored: "obstacle_3"
            //    up, "obstacle_4" down (opens a cell for "obstacle_2"),
            //    "obstacle_2" down.
            // 5. "obstacle_1" slides LEFT this time (not right — "rock" is
            //    on the right side of this board), clearing row 3.
            // 6. "obstacle_6" can now slide right — its own row is clear.
            // 7. The religious leader makes its first partial descent,
            //    past rows 1-2 and row 3, stopped by strict_schedule
            //    further down.
            // 8-9. "obstacle_1" has to slide back right so "obstacle_2" can
            //    get past it again on the way back up.
            // 10. "obstacle_4" clears the way for strict_schedule.
            // 11. strict_schedule slides LEFT this time ("rock2" walls off
            //    its right escape).
            // 12. The religious leader finishes its descent and exits.
            solution = listOf(
                RoadblockMove("gate_6", RoadblockDirection.UP, 1),
                RoadblockMove("obstacle_3", RoadblockDirection.UP, 1),
                RoadblockMove("obstacle_4", RoadblockDirection.DOWN, 1),
                RoadblockMove("obstacle_2", RoadblockDirection.DOWN, 1),
                RoadblockMove("obstacle_1", RoadblockDirection.LEFT, 2),
                RoadblockMove("obstacle_6", RoadblockDirection.RIGHT, 1),
                RoadblockMove("religious_leader", RoadblockDirection.DOWN, 4),
                RoadblockMove("obstacle_1", RoadblockDirection.RIGHT, 2),
                RoadblockMove("obstacle_2", RoadblockDirection.UP, 2),
                RoadblockMove("obstacle_4", RoadblockDirection.UP, 2),
                RoadblockMove("strict_schedule", RoadblockDirection.LEFT, 2),
                RoadblockMove("religious_leader", RoadblockDirection.DOWN, 4),
            ),
        ),
    )
}
