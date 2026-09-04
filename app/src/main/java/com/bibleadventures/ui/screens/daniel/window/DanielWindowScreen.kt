package com.bibleadventures.ui.screens.daniel.window

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bibleadventures.R
import com.bibleadventures.game.puzzles.slideout.LatchBlock
import com.bibleadventures.game.puzzles.slideout.SlideDirection
import com.bibleadventures.game.puzzles.slideout.SlideOutGame
import com.bibleadventures.game.puzzles.slideout.SlideOutGameState
import com.bibleadventures.game.puzzles.slideout.SlideOutOutcome
import com.bibleadventures.game.stories.DanielContent
import com.bibleadventures.ui.components.AspectRatioFitBox
import com.bibleadventures.ui.components.PuzzleTopBar
import com.bibleadventures.ui.screens.daniel.DanielViewModel
import com.bibleadventures.ui.theme.BibleAdventuresTheme
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

private val MIN_CELL_SIZE = 48.dp
private const val LATCH_RELEASE_DURATION_MS = 280
private const val LATCH_SHAKE_DURATION_MS = 350

/** How far (in cells) a released latch travels off-board during its fly-off animation — well past the board edge regardless of where it started. */
private const val RELEASE_TRAVEL_CELLS = 3f
private val SHAKE_AMPLITUDE = 6.dp
private const val SHAKE_CYCLES = 3f

/**
 * "Arrow Block: Slide Out" — every latch has one fixed exit direction.
 * Tapping one either flies it off the board (its path to the edge is
 * clear) or leaves it stuck in place (something else is in the way). The
 * board is fully tiled with real directional variety — a latch's direction
 * isn't tied to which edge it's nearest (see the generator behind
 * [DanielContent.windowLatchDirection] for how a densely packed, still
 * always-solvable board is built); see [SlideOutGame] for the pure engine.
 */
@Composable
fun DanielWindowScreen(
    viewModel: DanielViewModel,
    onContinue: () -> Unit,
    onBackToMainMenu: () -> Unit,
    previouslyCompleted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DanielWindowContent(
        windowLatchState = uiState.windowLatchState,
        onLatchTapped = viewModel::onLatchTapped,
        onContinue = onContinue,
        onBackToMainMenu = onBackToMainMenu,
        previouslyCompleted = previouslyCompleted,
        modifier = modifier,
    )
}

@Composable
private fun DanielWindowContent(
    windowLatchState: SlideOutGameState,
    onLatchTapped: (String) -> Unit,
    onContinue: () -> Unit,
    onBackToMainMenu: () -> Unit,
    previouslyCompleted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val totalLatches = DanielContent.WINDOW_LATCH_ROWS * DanielContent.WINDOW_LATCH_COLS
    val openedLatches = totalLatches - windowLatchState.blocks.size

    val feedback = when (windowLatchState.lastOutcome) {
        SlideOutOutcome.RELEASED, SlideOutOutcome.COMPLETE -> stringResource(R.string.daniel_window_feedback_released)
        SlideOutOutcome.BLOCKED -> stringResource(R.string.daniel_window_feedback_stuck)
        SlideOutOutcome.NONE -> ""
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            if (previouslyCompleted || windowLatchState.isComplete) {
                PuzzleTopBar(
                    showBackButton = previouslyCompleted,
                    onBackToMainMenu = onBackToMainMenu,
                    showNextButton = windowLatchState.isComplete || previouslyCompleted,
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
                text = stringResource(R.string.daniel_window_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = stringResource(R.string.daniel_window_instructions),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = stringResource(R.string.daniel_window_progress_label, openedLatches, totalLatches),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
            )

            Box(modifier = Modifier.height(28.dp).padding(top = 4.dp)) {
                Text(text = feedback, style = MaterialTheme.typography.titleMedium)
            }

            AspectRatioFitBox(
                ratio = windowLatchState.cols.toFloat() / windowLatchState.rows.toFloat(),
                modifier = Modifier.weight(1f, fill = true).fillMaxSize().padding(top = 12.dp),
            ) {
                LatchBoard(
                    windowLatchState = windowLatchState,
                    onLatchTapped = onLatchTapped,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                )
            }

            if (previouslyCompleted && !windowLatchState.isComplete) {
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
 * Renders every remaining latch plus, while it's mid-flight, the one
 * latch that was just released — the engine already removed it from
 * [SlideOutGameState.blocks] the instant the tap resolved, so
 * [releasingBlock] keeps it visible here until its own fly-off animation
 * genuinely finishes (same "commit immediately, animate cosmetically"
 * decoupling as `GoodSamaritanExploreScreen`'s `BanditCombatOverlay`).
 * Only one latch resolves at a time — [pendingBlock] blocks further taps
 * until the current one's animation completes.
 */
@Composable
private fun LatchBoard(
    windowLatchState: SlideOutGameState,
    onLatchTapped: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var pendingBlock by remember { mutableStateOf<LatchBlock?>(null) }
    var tapTrigger by remember { mutableIntStateOf(0) }
    var releasingBlock by remember { mutableStateOf<LatchBlock?>(null) }
    var shakingBlockId by remember { mutableStateOf<String?>(null) }
    val releaseProgress = remember { Animatable(0f) }
    val shakeProgress = remember { Animatable(0f) }

    LaunchedEffect(tapTrigger) {
        val tapped = pendingBlock ?: return@LaunchedEffect
        if (windowLatchState.blocks.none { it.id == tapped.id }) {
            releasingBlock = tapped
            releaseProgress.snapTo(0f)
            releaseProgress.animateTo(1f, animationSpec = tween(LATCH_RELEASE_DURATION_MS))
            releasingBlock = null
        } else if (windowLatchState.lastBlockedId == tapped.id) {
            shakingBlockId = tapped.id
            shakeProgress.snapTo(0f)
            shakeProgress.animateTo(1f, animationSpec = tween(LATCH_SHAKE_DURATION_MS))
            shakingBlockId = null
        }
        pendingBlock = null
    }

    fun handleTap(block: LatchBlock) {
        if (pendingBlock != null) return
        pendingBlock = block
        tapTrigger++
        onLatchTapped(block.id)
    }

    BoxWithConstraints(modifier = modifier) {
        val density = LocalDensity.current
        val cellSize = minOf(maxWidth / windowLatchState.cols, maxHeight / windowLatchState.rows).coerceAtLeast(MIN_CELL_SIZE)
        val cellSizePx = with(density) { cellSize.toPx() }

        windowLatchState.blocks.forEach { block ->
            key(block.id) {
                LatchTile(
                    block = block,
                    cellSize = cellSize,
                    cellSizePx = cellSizePx,
                    contentDescription = latchContentDescription(block),
                    shakeProgress = if (block.id == shakingBlockId) shakeProgress.value else 0f,
                    onTap = { handleTap(block) },
                )
            }
        }
        releasingBlock?.let { block ->
            key("releasing_${block.id}") {
                LatchTile(
                    block = block,
                    cellSize = cellSize,
                    cellSizePx = cellSizePx,
                    contentDescription = "",
                    releaseProgress = releaseProgress.value,
                    onTap = {},
                )
            }
        }
    }
}

@Composable
private fun LatchTile(
    block: LatchBlock,
    cellSize: Dp,
    cellSizePx: Float,
    contentDescription: String,
    releaseProgress: Float = 0f,
    shakeProgress: Float = 0f,
    onTap: () -> Unit,
) {
    val density = LocalDensity.current
    val baseX = block.position.col * cellSizePx
    val baseY = block.position.row * cellSizePx

    val releaseOffsetPx = releaseProgress * RELEASE_TRAVEL_CELLS * cellSizePx
    val (releaseDx, releaseDy) = when (block.direction) {
        SlideDirection.UP -> 0f to -releaseOffsetPx
        SlideDirection.DOWN -> 0f to releaseOffsetPx
        SlideDirection.LEFT -> -releaseOffsetPx to 0f
        SlideDirection.RIGHT -> releaseOffsetPx to 0f
    }
    val shakeAmplitudePx = with(density) { SHAKE_AMPLITUDE.toPx() }
    val shakeOffsetPx = if (shakeProgress > 0f) sin(shakeProgress * SHAKE_CYCLES * 2 * PI).toFloat() * shakeAmplitudePx else 0f
    val isShaking = shakeProgress > 0f

    val backgroundColor = if (isShaking) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer
    val borderColor = if (isShaking) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
    val iconTint = if (isShaking) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer

    Box(
        modifier = Modifier
            .offset { IntOffset((baseX + releaseDx + shakeOffsetPx).roundToInt(), (baseY + releaseDy).roundToInt()) }
            .size(cellSize)
            .padding(4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(width = if (isShaking) 3.dp else 2.dp, color = borderColor, shape = RoundedCornerShape(8.dp))
            .clickable(onClickLabel = contentDescription, onClick = onTap)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = block.direction.toIcon(),
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(cellSize * 0.6f),
        )
    }
}

private fun SlideDirection.toIcon(): ImageVector = when (this) {
    SlideDirection.UP -> Icons.Filled.KeyboardArrowUp
    SlideDirection.DOWN -> Icons.Filled.KeyboardArrowDown
    SlideDirection.LEFT -> Icons.AutoMirrored.Filled.KeyboardArrowLeft
    SlideDirection.RIGHT -> Icons.AutoMirrored.Filled.KeyboardArrowRight
}

/** Generated from the latch's own row/column/direction rather than a per-id lookup table — doesn't scale to hand-author individually once the board is a dense 36-tile grid instead of 8 named pieces. */
@Composable
private fun latchContentDescription(block: LatchBlock): String {
    val row = block.position.row + 1
    val col = block.position.col + 1
    val stringRes = when (block.direction) {
        SlideDirection.UP -> R.string.daniel_window_latch_up_content_description
        SlideDirection.DOWN -> R.string.daniel_window_latch_down_content_description
        SlideDirection.LEFT -> R.string.daniel_window_latch_left_content_description
        SlideDirection.RIGHT -> R.string.daniel_window_latch_right_content_description
    }
    return stringResource(stringRes, row, col)
}

@Preview(showBackground = true)
@Composable
private fun DanielWindowPreview() {
    BibleAdventuresTheme {
        DanielWindowContent(
            windowLatchState = SlideOutGame.fromGrid(
                DanielContent.WINDOW_LATCH_ROWS,
                DanielContent.WINDOW_LATCH_COLS,
                DanielContent::windowLatchDirection,
            ),
            onLatchTapped = {},
            onContinue = {},
            onBackToMainMenu = {},
        )
    }
}
