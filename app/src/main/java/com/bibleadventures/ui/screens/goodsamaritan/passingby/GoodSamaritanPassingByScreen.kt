package com.bibleadventures.ui.screens.goodsamaritan.passingby

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bibleadventures.R
import com.bibleadventures.audio.CharacterVoiceLine
import com.bibleadventures.domain.model.CharacterCustomization
import com.bibleadventures.game.puzzles.roadblock.Block
import com.bibleadventures.game.puzzles.roadblock.Direction
import com.bibleadventures.game.puzzles.roadblock.Orientation
import com.bibleadventures.game.puzzles.roadblock.RoadblockGame
import com.bibleadventures.game.puzzles.roadblock.RoadblockGameState
import com.bibleadventures.game.stories.GoodSamaritanContent
import com.bibleadventures.ui.components.AspectRatioFitBox
import com.bibleadventures.ui.components.CharacterCallout
import com.bibleadventures.ui.components.Posture
import com.bibleadventures.ui.components.PuzzleTopBar
import com.bibleadventures.ui.LocalAudioController
import com.bibleadventures.ui.screens.goodsamaritan.GoodSamaritanViewModel
import com.bibleadventures.ui.theme.BibleAdventuresTheme
import kotlin.math.hypot
import kotlin.math.roundToInt

/** Below this many px of raw drag, no direction is locked yet — avoids jitter re-locking the axis on a wobbly touch-down. */
private const val DRAG_LOCK_SLOP_DP = 8

/**
 * Every [PassingByLevel][com.bibleadventures.game.stories.GoodSamaritanContent.PassingByLevel]
 * carries exactly one of these 4 canonical excuse ids among its blocks —
 * its own spotlighted one (see that class's own doc comment). On-device
 * feedback asked for every movable non-target tile in a level to share
 * that one name, rather than a mix of the spotlighted excuse plus
 * numbered "Obstacle 1/2/3" filler — simpler for a 7-year-old to read,
 * even though the individual `"obstacle_1"`/`"obstacle_2"`/... ids
 * underneath are still what the engine and RoadblockMove solutions
 * actually key off.
 */
private val excuseLabelResByCanonicalId = mapOf(
    "ritual_purity" to R.string.good_samaritan_passing_by_excuse_ritual_purity,
    "fear_of_ambush" to R.string.good_samaritan_passing_by_excuse_fear_of_ambush,
    "strict_schedule" to R.string.good_samaritan_passing_by_excuse_strict_schedule,
    "not_my_problem" to R.string.good_samaritan_passing_by_excuse_not_my_problem,
)

private val MIN_CELL_SIZE = 48.dp

/**
 * The character's own callout box is a fixed 96dp, and its speech bubble
 * (when shown) grows upward from there by up to roughly its
 * `bubbleAboveClearance` (76dp here) before wrapping text starts eating
 * into that headroom instead. Reserving a plain Column row this tall for
 * the character — not weighted, so the puzzle above shrinks to leave room
 * for it — keeps the bubble's entire growth inside this row instead of
 * spilling up into the board above it.
 */
private val CHARACTER_ROW_HEIGHT = 180.dp

/**
 * A Rush-Hour/Unblock-Me-style sliding block puzzle. Deliberately kept as a
 * "the religious leader successfully passes by" win condition — that's
 * what actually happened in the parable — with the moral delivered
 * separately by the player's own character once solved, in
 * [Posture.STANDING], never [Posture.THUMBS_UP]: this scene is never a
 * celebration.
 */
@Composable
fun GoodSamaritanPassingByScreen(
    viewModel: GoodSamaritanViewModel,
    onContinue: () -> Unit,
    onBackToMainMenu: () -> Unit,
    previouslyCompleted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val characterCustomization by viewModel.characterCustomization.collectAsStateWithLifecycle()

    GoodSamaritanPassingByContent(
        roadblockState = uiState.roadblockState,
        levelIndex = uiState.passingByLevelIndex,
        levelCount = GoodSamaritanContent.passingByLevels.size,
        characterCustomization = characterCustomization,
        onSlideAttempted = viewModel::onSlideAttempted,
        onNextLevel = viewModel::onPassingByNextLevel,
        onContinue = onContinue,
        onBackToMainMenu = onBackToMainMenu,
        previouslyCompleted = previouslyCompleted,
        modifier = modifier,
    )
}

@Composable
private fun GoodSamaritanPassingByContent(
    roadblockState: RoadblockGameState,
    levelIndex: Int,
    levelCount: Int,
    characterCustomization: CharacterCustomization,
    onSlideAttempted: (blockId: String, direction: Direction, cellsAttempted: Int) -> Unit,
    onNextLevel: () -> Unit,
    onContinue: () -> Unit,
    onBackToMainMenu: () -> Unit,
    previouslyCompleted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val audioController = LocalAudioController.current
    LaunchedEffect(Unit) { audioController.playCharacterLine(CharacterVoiceLine.GOOD_SAMARITAN_PASSING_BY_INSTRUCTIONS) }

    // The last of GoodSamaritanContent.passingByLevels actually leaves the
    // scene (and shows the parable's moral); every earlier level's own
    // completion just advances in place to the next one — see
    // GoodSamaritanViewModel.onPassingByNextLevel.
    val isLastLevel = levelIndex == levelCount - 1

    // "Next Page" is reserved for actually leaving this scene (to the next
    // video) — per on-device feedback, it used to also double as "advance
    // to the next of the 4 puzzles," which read as leaving the scene early
    // every time a level finished. An in-progress level's own completion
    // is announced by the character instead (see the tappable
    // CharacterCallout below), not this top bar.
    val readyToLeaveScene = (roadblockState.isComplete && isLastLevel) || previouslyCompleted

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            if (readyToLeaveScene) {
                PuzzleTopBar(
                    showBackButton = previouslyCompleted,
                    onBackToMainMenu = onBackToMainMenu,
                    showNextButton = readyToLeaveScene,
                    onNext = onContinue,
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.good_samaritan_passing_by_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = stringResource(R.string.good_samaritan_passing_by_level_label, levelIndex + 1, levelCount),
                style = MaterialTheme.typography.bodyMedium,
            )

            // TopCenter, not the default Center: letterboxing this board in the
            // middle of its weighted space wasted the gap between the title and
            // the board as empty margin, and just as much again below it — with
            // a character+bubble row still to fit in afterward, that wasted top
            // margin is space the puzzle can actually use, and the bottom margin
            // is exactly where the character needs to go instead.
            AspectRatioFitBox(
                ratio = roadblockState.cols.toFloat() / roadblockState.rows.toFloat(),
                alignment = Alignment.TopCenter,
                modifier = Modifier.weight(1f, fill = true).fillMaxSize().padding(top = 12.dp),
            ) {
                // clipToBounds so the religious leader's own progressive exit (it can
                // hang halfway off the bottom edge as a legal intermediate state, see
                // RoadblockGame.maxSlideDistance) is cleanly cropped at the board's
                // own edge instead of spilling into whatever sits below it.
                BoxWithConstraints(
                    modifier = Modifier.fillMaxSize().clipToBounds().background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    val density = LocalDensity.current
                    val cellSize = minOf(maxWidth / roadblockState.cols, maxHeight / roadblockState.rows).coerceAtLeast(MIN_CELL_SIZE)
                    val cellSizePx = with(density) { cellSize.toPx() }
                    val gateContentDescription = stringResource(R.string.good_samaritan_passing_by_exit_gate_content_description)
                    // Always present — see excuseLabelResByCanonicalId's own doc comment.
                    val excuseLabelRes = roadblockState.blocks.firstNotNullOf { excuseLabelResByCanonicalId[it.id] }

                    // Drawn first (behind every block) so it reads as part of the
                    // board rather than a piece — the only visible cue for where
                    // the religious leader actually needs to reach.
                    roadblockState.exitColumns.forEach { exitColumn ->
                        key("gate_$exitColumn") {
                            Image(
                                painter = painterResource(R.drawable.ic_exit_gate),
                                contentDescription = gateContentDescription,
                                modifier = Modifier
                                    .offset {
                                        IntOffset(
                                            (exitColumn * cellSizePx).roundToInt(),
                                            ((roadblockState.rows - 1) * cellSizePx).roundToInt(),
                                        )
                                    }
                                    .size(cellSize),
                            )
                        }
                    }

                    roadblockState.blocks.forEach { block ->
                        key(block.id) {
                            RoadblockPiece(
                                block = block,
                                cellSize = cellSize,
                                cellSizePx = cellSizePx,
                                excuseLabelRes = excuseLabelRes,
                                maxSlideDistance = { direction -> RoadblockGame.maxSlideDistance(roadblockState, block.id, direction) },
                                onSlideAttempted = { direction, cells -> onSlideAttempted(block.id, direction, cells) },
                            )
                        }
                    }

                }
            }

            // A dedicated row, not weighted — reserves its own fixed height so
            // the puzzle above (weight(1f, fill = true)) automatically shrinks
            // to leave it room, guaranteeing the character and its upward-
            // growing speech bubble never overlap the board no matter how the
            // board's own letterboxing works out. Sized for the character's own
            // 96dp box plus the bubble's clearance above it, so the bubble's
            // full growth stays inside this row instead of spilling above it.
            // An in-progress level's own completion (roadblockState.isComplete
            // but not the last one) is announced by the character itself,
            // tappable to advance — see onNextLevel — rather than the top
            // bar's "Next Page" (reserved for actually leaving this scene).
            val readyForNextLevel = roadblockState.isComplete && !isLastLevel
            Box(modifier = Modifier.fillMaxWidth().height(CHARACTER_ROW_HEIGHT)) {
                CharacterCallout(
                    characterCustomization = characterCustomization,
                    message = when {
                        roadblockState.isComplete && isLastLevel -> stringResource(R.string.good_samaritan_passing_by_moral_message)
                        roadblockState.isComplete -> stringResource(R.string.good_samaritan_passing_by_level_complete)
                        else -> stringResource(R.string.good_samaritan_passing_by_instructions)
                    },
                    posture = Posture.STANDING,
                    modifier = Modifier.align(Alignment.BottomStart),
                    bubbleAboveClearance = 76.dp,
                    onClick = if (readyForNextLevel) onNextLevel else null,
                    onClickContentDescription = if (readyForNextLevel) {
                        stringResource(R.string.good_samaritan_passing_by_next_level_content_description)
                    } else {
                        null
                    },
                )
            }

            if (previouslyCompleted && !roadblockState.isComplete) {
                Text(
                    text = stringResource(R.string.puzzle_already_completed_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

/**
 * One draggable (or, for the fixed injured man, static) piece. Every
 * block — including the Priest/Levite tile — shares this exact same
 * gesture handler with no branching between them: which of the two
 * directions a drag can possibly commit to comes directly from the
 * block's own [Block.orientation] (see the `onDrag` axis pick below), not
 * from guessing which way the gesture *looks* like it's going — a real
 * finger swipe is rarely perfectly straight, and comparing raw x/y deltas
 * to guess the axis could occasionally out-vote the intended direction
 * with stray sideways motion, locking onto the one axis a given block can
 * *never* legally move along (see [maxSlideDistance]'s own off-axis check)
 * and making it look permanently stuck no matter how it's dragged — a real
 * bug reported on-device (a vertical tile that could never be moved up),
 * fixed by reading the axis off the block instead of the gesture.
 *
 * This composable still never needs to know which kind of block it's
 * holding, or that the Priest/Levite tile is any different from an excuse
 * block — [Block.orientation] alone is enough.
 *
 * The live drag is clamped to whole legal cells along whichever direction
 * the gesture first commits to (past [DRAG_LOCK_SLOP_DP] of raw movement) —
 * the same "screen decides where to visually clamp a live drag, engine
 * only judges" idiom as `NoahsArkOrganizeArkScreen`'s nearest-drop-zone
 * snap and `DavidGoliathSlingPracticeScreen`'s pull vector. Because the
 * clamped offset already reflects the exact number of cells that will be
 * committed, [block]'s new position (once the ViewModel state updates)
 * lands exactly where the drag left off — no separate snap animation
 * needed, the same instant-move precedent `GridMazeGame`'s D-pad already
 * established for this chapter.
 */
@Composable
private fun RoadblockPiece(
    block: Block,
    cellSize: Dp,
    cellSizePx: Float,
    excuseLabelRes: Int,
    maxSlideDistance: (Direction) -> Int,
    onSlideAttempted: (Direction, Int) -> Unit,
) {
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var lockedDirection by remember { mutableStateOf<Direction?>(null) }
    val density = LocalDensity.current
    val slopPx = with(density) { DRAG_LOCK_SLOP_DP.dp.toPx() }

    val widthCells = if (block.orientation == Orientation.HORIZONTAL) block.length else 1
    val heightCells = if (block.orientation == Orientation.HORIZONTAL) 1 else block.length

    // Every Passing By level reuses the same handful of ids ("obstacle_1",
    // "obstacle_2", ...) for a *different* board each time — the same
    // id's own orientation, cell size, and legality function can all
    // change from one level to the next. Since `key(block.id)` in the
    // parent `forEach` keeps this composable's identity (and thus this
    // `pointerInput(block.id)` coroutine) alive across that level
    // transition, `block`/`cellSizePx`/`maxSlideDistance`/`onSlideAttempted`
    // captured directly inside `onDrag` below would stay frozen at
    // whatever they were the *first* time this id ever appeared — level
    // 1's data, forever, even once a later level re-labels the same id
    // onto a differently-shaped tile. That's a real bug found on-device:
    // level 1's "obstacle_2" (Strict Schedule) is horizontal, so once
    // level 2 reused that id for a *vertical* tile, its drag handler kept
    // reading level 1's stale horizontal orientation — the tile rendered
    // correctly (that part reads live `block` from this function's own
    // recomposition, not from inside the frozen coroutine) but every drag
    // still got routed through the horizontal LEFT/RIGHT branch no matter
    // which way it was actually dragged, matching the exact symptom
    // reported: "trying to slide it up but it slightly moved sideways."
    // `rememberUpdatedState` is the standard fix — it hands the coroutine
    // a reference that always reads whatever was most recently passed in,
    // regardless of whether the coroutine itself ever restarts.
    val latestBlock by rememberUpdatedState(block)
    val latestCellSizePx by rememberUpdatedState(cellSizePx)
    val latestMaxSlideDistance by rememberUpdatedState(maxSlideDistance)
    val latestOnSlideAttempted by rememberUpdatedState(onSlideAttempted)

    var modifier = Modifier
        // Every non-target excuse tile in a level now shares one visible
        // label/content description (see excuseLabelResByCanonicalId), so
        // block.id is no longer unique enough for an instrumented test to
        // find a *specific* tile by content description — testTag exposes
        // the real underlying id for exactly that purpose, invisibly to
        // the player and to any real screen reader.
        .testTag(block.id)
        .offset {
            IntOffset(
                (block.origin.col * cellSizePx + dragOffset.x).roundToInt(),
                (block.origin.row * cellSizePx + dragOffset.y).roundToInt(),
            )
        }
        .size(width = cellSize * widthCells, height = cellSize * heightCells)

    if (!block.isFixed) {
        modifier = modifier.pointerInput(block.id) {
            detectDragGestures(
                onDragStart = {
                    dragOffset = Offset.Zero
                    lockedDirection = null
                },
                onDragEnd = {
                    val direction = lockedDirection
                    if (direction != null) {
                        val signedPx = when (direction) {
                            Direction.RIGHT -> dragOffset.x
                            Direction.LEFT -> -dragOffset.x
                            Direction.DOWN -> dragOffset.y
                            Direction.UP -> -dragOffset.y
                        }
                        val cells = (signedPx / latestCellSizePx).roundToInt()
                        if (cells > 0) latestOnSlideAttempted(direction, cells)
                    }
                    dragOffset = Offset.Zero
                    lockedDirection = null
                },
                onDragCancel = {
                    dragOffset = Offset.Zero
                    lockedDirection = null
                },
                onDrag = { change, dragAmount ->
                    change.consume()
                    val raw = dragOffset + dragAmount
                    val direction = lockedDirection ?: run {
                        if (hypot(raw.x, raw.y) < slopPx) return@run null
                        // Pick the axis from the block's own fixed orientation,
                        // never by comparing which raw delta happens to be
                        // bigger: a real finger swipe is rarely perfectly
                        // straight, and a block confined to one axis (see
                        // RoadblockGame.maxSlideDistance) can NEVER legally
                        // move off it regardless of how the drag started — so
                        // guessing the axis from the gesture, rather than
                        // just reading it off the block, could silently lock
                        // onto the one axis this block can never use at all,
                        // making it look permanently stuck no matter how it's
                        // dragged.
                        val newDirection = if (latestBlock.orientation == Orientation.HORIZONTAL) {
                            if (raw.x > 0) Direction.RIGHT else Direction.LEFT
                        } else {
                            if (raw.y > 0) Direction.DOWN else Direction.UP
                        }
                        lockedDirection = newDirection
                        newDirection
                    }
                    dragOffset = if (direction == null) {
                        raw
                    } else {
                        val legalPx = latestMaxSlideDistance(direction) * latestCellSizePx
                        when (direction) {
                            Direction.RIGHT -> Offset(raw.x.coerceIn(0f, legalPx), 0f)
                            Direction.LEFT -> Offset(raw.x.coerceIn(-legalPx, 0f), 0f)
                            Direction.DOWN -> Offset(0f, raw.y.coerceIn(0f, legalPx))
                            Direction.UP -> Offset(0f, raw.y.coerceIn(-legalPx, 0f))
                        }
                    }
                },
            )
        }
    }

    RoadblockPieceContent(block = block, excuseLabelRes = excuseLabelRes, modifier = modifier)
}

/**
 * Every tile — the Priest/Levite target, the fixed injured man, and every
 * excuse block alike — renders as the same plain rectangle with a border
 * (needed since some fill colors otherwise read as almost the same as the
 * board background) and its own visible text label, matching real Unblock
 * Me's uniform tile look (a distinct color for the target, not a distinct
 * *shape* or character sprite) while still naming what each piece is —
 * every non-fixed, non-target tile in a level uses the SAME [excuseLabelRes]
 * (that level's own spotlighted excuse — see
 * [com.bibleadventures.game.stories.GoodSamaritanContent.PassingByLevel]),
 * on-device feedback having found individually-numbered "Obstacle 1/2/3"
 * labels more confusing than helpful for a 7-year-old player.
 */
@Composable
private fun RoadblockPieceContent(block: Block, excuseLabelRes: Int, modifier: Modifier = Modifier) {
    val isTarget = block.id == "religious_leader"
    val (label, backgroundColor, contentColor) = when {
        isTarget -> Triple(
            stringResource(R.string.good_samaritan_passing_by_protagonist_content_description),
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.onPrimary,
        )
        // Every fixed, never-movable tile (the injured man, a rock, any future
        // one) shares this same muted styling regardless of its id — a visual
        // "this one doesn't slide" cue.
        block.isFixed -> {
            val descriptionRes = when (block.id) {
                "injured_man" -> R.string.good_samaritan_traveler_content_description
                // Level 2 has a second, distinctly-id'd fixed rock ("rock2" —
                // see GoodSamaritanContent.passingByLevels' own doc comment
                // for why it can't just reuse "rock"'s letter) that reads
                // identically to the player as just another rock.
                "rock", "rock2" -> R.string.good_samaritan_passing_by_rock_content_description
                else -> null
            }
            Triple(
                descriptionRes?.let { stringResource(it) }.orEmpty(),
                MaterialTheme.colorScheme.outline,
                MaterialTheme.colorScheme.surface,
            )
        }
        else -> Triple(
            stringResource(excuseLabelRes),
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
    Box(
        modifier = modifier
            .padding(2.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundColor)
            .border(width = 2.dp, color = MaterialTheme.colorScheme.outline, shape = RoundedCornerShape(6.dp))
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            color = contentColor,
            modifier = Modifier.padding(2.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun GoodSamaritanPassingByPreview() {
    BibleAdventuresTheme {
        val level = GoodSamaritanContent.passingByLevels.first()
        val roadblockState = RoadblockGame.fromLayout(
            layout = level.layout,
            blockSpecs = level.blockSpecs,
            protagonistId = GoodSamaritanContent.passingByProtagonistId,
            exitColumns = level.exitColumns,
        )
        GoodSamaritanPassingByContent(
            roadblockState = roadblockState,
            levelIndex = 0,
            levelCount = GoodSamaritanContent.passingByLevels.size,
            characterCustomization = CharacterCustomization(),
            onSlideAttempted = { _, _, _ -> },
            onNextLevel = {},
            onContinue = {},
            onBackToMainMenu = {},
        )
    }
}
