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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
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
import com.bibleadventures.ui.screens.goodsamaritan.GoodSamaritanViewModel
import com.bibleadventures.ui.theme.BibleAdventuresTheme
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt

/** Below this many px of raw drag, no direction is locked yet — avoids jitter re-locking the axis on a wobbly touch-down. */
private const val DRAG_LOCK_SLOP_DP = 8

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
        characterCustomization = characterCustomization,
        onSlideAttempted = viewModel::onSlideAttempted,
        onContinue = onContinue,
        onBackToMainMenu = onBackToMainMenu,
        previouslyCompleted = previouslyCompleted,
        modifier = modifier,
    )
}

@Composable
private fun GoodSamaritanPassingByContent(
    roadblockState: RoadblockGameState,
    characterCustomization: CharacterCustomization,
    onSlideAttempted: (blockId: String, direction: Direction, cellsAttempted: Int) -> Unit,
    onContinue: () -> Unit,
    onBackToMainMenu: () -> Unit,
    previouslyCompleted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            if (previouslyCompleted || roadblockState.isComplete) {
                PuzzleTopBar(
                    showBackButton = previouslyCompleted,
                    onBackToMainMenu = onBackToMainMenu,
                    showNextButton = roadblockState.isComplete || previouslyCompleted,
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
            Box(modifier = Modifier.fillMaxWidth().height(CHARACTER_ROW_HEIGHT)) {
                CharacterCallout(
                    characterCustomization = characterCustomization,
                    message = if (roadblockState.isComplete) {
                        stringResource(R.string.good_samaritan_passing_by_moral_message)
                    } else {
                        stringResource(R.string.good_samaritan_passing_by_instructions)
                    },
                    posture = Posture.STANDING,
                    modifier = Modifier.align(Alignment.BottomStart),
                    bubbleAboveClearance = 76.dp,
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
 * gesture handler with no branching between them: axis-locking falls out
 * naturally from [maxSlideDistance] itself always returning 0 for a
 * block's off-axis directions, so this composable never needs to know
 * which kind of block it's holding, or that the Priest/Levite tile is any
 * different from an excuse block.
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
    maxSlideDistance: (Direction) -> Int,
    onSlideAttempted: (Direction, Int) -> Unit,
) {
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var lockedDirection by remember { mutableStateOf<Direction?>(null) }
    val density = LocalDensity.current
    val slopPx = with(density) { DRAG_LOCK_SLOP_DP.dp.toPx() }

    val widthCells = if (block.orientation == Orientation.HORIZONTAL) block.length else 1
    val heightCells = if (block.orientation == Orientation.HORIZONTAL) 1 else block.length

    var modifier = Modifier
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
                        val cells = (signedPx / cellSizePx).roundToInt()
                        if (cells > 0) onSlideAttempted(direction, cells)
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
                        val newDirection = if (abs(raw.x) > abs(raw.y)) {
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
                        val legalPx = maxSlideDistance(direction) * cellSizePx
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

    RoadblockPieceContent(block = block, modifier = modifier)
}

/**
 * Every tile — the Priest/Levite target, the fixed injured man, and every
 * excuse block alike — renders as the same plain rectangle with a border
 * (needed since some fill colors otherwise read as almost the same as the
 * board background) and its own visible text label, matching real Unblock
 * Me's uniform tile look (a distinct color for the target, not a distinct
 * *shape* or character sprite) while still naming what each piece is.
 */
@Composable
private fun RoadblockPieceContent(block: Block, modifier: Modifier = Modifier) {
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
                "rock" -> R.string.good_samaritan_passing_by_rock_content_description
                else -> null
            }
            Triple(
                descriptionRes?.let { stringResource(it) }.orEmpty(),
                MaterialTheme.colorScheme.outline,
                MaterialTheme.colorScheme.surface,
            )
        }
        else -> {
            val labelRes = when (block.id) {
                "ritual_purity" -> R.string.good_samaritan_passing_by_excuse_ritual_purity
                "fear_of_ambush" -> R.string.good_samaritan_passing_by_excuse_fear_of_ambush
                "strict_schedule" -> R.string.good_samaritan_passing_by_excuse_strict_schedule
                "not_my_problem" -> R.string.good_samaritan_passing_by_excuse_not_my_problem
                else -> null
            }
            Triple(
                labelRes?.let { stringResource(it) }.orEmpty(),
                MaterialTheme.colorScheme.secondaryContainer,
                MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
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
        val roadblockState = RoadblockGame.fromLayout(
            layout = GoodSamaritanContent.passingByLayout,
            blockSpecs = GoodSamaritanContent.passingByBlockSpecs,
            protagonistId = GoodSamaritanContent.passingByProtagonistId,
            exitColumns = GoodSamaritanContent.passingByExitColumns,
        )
        GoodSamaritanPassingByContent(
            roadblockState = roadblockState,
            characterCustomization = CharacterCustomization(),
            onSlideAttempted = { _, _, _ -> },
            onContinue = {},
            onBackToMainMenu = {},
        )
    }
}
