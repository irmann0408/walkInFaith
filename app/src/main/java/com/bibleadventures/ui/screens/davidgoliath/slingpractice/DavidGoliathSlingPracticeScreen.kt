package com.bibleadventures.ui.screens.davidgoliath.slingpractice

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bibleadventures.R
import com.bibleadventures.game.puzzles.slingshot.SlingshotGameState
import com.bibleadventures.game.puzzles.slingshot.SlingshotOutcome
import com.bibleadventures.ui.components.AspectRatioFitBox
import com.bibleadventures.ui.components.PuzzleTopBar
import com.bibleadventures.ui.screens.davidgoliath.DavidGoliathViewModel
import com.bibleadventures.ui.screens.davidgoliath.ShieldZone
import com.bibleadventures.ui.theme.BibleAdventuresTheme
import kotlinx.coroutines.isActive
import kotlin.math.roundToInt

private const val MARK_MIN_FRACTION = 0.15f
private const val MARK_MAX_FRACTION = 0.85f

/** One-way sweep duration — the mark takes this long to cross MIN to MAX, then reverses (linear ping-pong, period = 2x this). */
private const val MARK_SWEEP_MS = 2200L

// The shield IMAGE's own bounding-box span, expressed as fixed fractions of
// the same 0..1 track the mark and aim already use. Positioned (not just
// sized) explicitly via offset + width below — not via fillMaxWidth +
// align(TopCenter), which always centers at fraction 0.5 regardless of
// these constants, silently decoupling what's drawn from what's checked.
private const val SHIELD_IMAGE_WIDTH_FRACTION = 0.60f

// ic_goliath_shield.xml's silhouette is narrower than its own bounding box:
// its widest point (the top edge, where the mark's line sits) spans x=12..52
// of a 64-wide viewport. Both the hit-test's "shield perimeter" and the
// mark's rendered line width are derived from this actual top-edge span, not
// the full (partly transparent) bounding box — "within the shield" means
// within the shape a player can actually see, not its invisible padding.
private const val SHIELD_TOP_EDGE_LEFT_RATIO = 12f / 64f
private const val SHIELD_TOP_EDGE_RIGHT_RATIO = 52f / 64f

// The shield's true, visible perimeter width — what the hit-test checks and
// what the mark's (halved) line width is derived from. Same for every zone;
// only the zone changes *where* this window sits, never its width.
private val SHIELD_WIDTH_FRACTION = (SHIELD_TOP_EDGE_RIGHT_RATIO - SHIELD_TOP_EDGE_LEFT_RATIO) * SHIELD_IMAGE_WIDTH_FRACTION

/**
 * Where the shield IMAGE's left edge sits for each zone, spaced so all 3
 * zones' true (top-edge) perimeters land within the mark's
 * [MARK_MIN_FRACTION]..[MARK_MAX_FRACTION] sweep: LEFT keeps the original
 * single-shield position, MIDDLE is centered, RIGHT mirrors LEFT.
 */
private fun shieldImageMinFraction(zone: ShieldZone): Float = when (zone) {
    ShieldZone.LEFT -> 0.00f
    ShieldZone.MIDDLE -> 0.20f
    ShieldZone.RIGHT -> 0.40f
}

private fun shieldMinFraction(zone: ShieldZone): Float =
    shieldImageMinFraction(zone) + SHIELD_TOP_EDGE_LEFT_RATIO * SHIELD_IMAGE_WIDTH_FRACTION

private fun shieldMaxFraction(zone: ShieldZone): Float =
    shieldImageMinFraction(zone) + SHIELD_TOP_EDGE_RIGHT_RATIO * SHIELD_IMAGE_WIDTH_FRACTION

/** The linear ping-pong the mark used to get for free from `rememberInfiniteTransition(tween(..., RepeatMode.Reverse))`, now test-controllable via a manual clock. */
private fun markFractionAt(elapsedMs: Long): Float {
    val periodMs = MARK_SWEEP_MS * 2
    val t = elapsedMs % periodMs
    val progress = if (t <= MARK_SWEEP_MS) {
        t / MARK_SWEEP_MS.toFloat()
    } else {
        (periodMs - t) / MARK_SWEEP_MS.toFloat()
    }
    return MARK_MIN_FRACTION + (MARK_MAX_FRACTION - MARK_MIN_FRACTION) * progress
}

@Composable
fun DavidGoliathSlingPracticeScreen(
    viewModel: DavidGoliathViewModel,
    onContinue: () -> Unit,
    onBackToMainMenu: () -> Unit,
    previouslyCompleted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DavidGoliathSlingPracticeContent(
        slingshotState = uiState.slingshotState,
        shieldZone = uiState.shieldZone,
        onStoneReleased = viewModel::onStoneReleased,
        onContinue = onContinue,
        onBackToMainMenu = onBackToMainMenu,
        previouslyCompleted = previouslyCompleted,
        modifier = modifier,
    )
}

@Composable
private fun DavidGoliathSlingPracticeContent(
    slingshotState: SlingshotGameState,
    shieldZone: ShieldZone,
    onStoneReleased: (aimedPosition: Float, markPosition: Float, shieldMinFraction: Float, shieldMaxFraction: Float) -> Unit,
    onContinue: () -> Unit,
    onBackToMainMenu: () -> Unit,
    previouslyCompleted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            if (previouslyCompleted || slingshotState.isComplete) {
                PuzzleTopBar(
                    showBackButton = previouslyCompleted,
                    onBackToMainMenu = onBackToMainMenu,
                    showNextButton = slingshotState.isComplete || previouslyCompleted,
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
                text = stringResource(R.string.david_goliath_sling_practice_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = stringResource(R.string.david_goliath_sling_practice_instructions),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = stringResource(R.string.david_goliath_sling_practice_progress_label, slingshotState.hits, slingshotState.requiredHits),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 4.dp),
            )

            val feedback = when (slingshotState.lastOutcome) {
                SlingshotOutcome.HIT -> stringResource(R.string.feedback_great_job)
                SlingshotOutcome.MISS -> stringResource(R.string.feedback_try_another_one)
                SlingshotOutcome.NONE -> ""
            }
            Box(modifier = Modifier.height(32.dp)) {
                Text(text = feedback, style = MaterialTheme.typography.titleLarge)
            }

            // The mark's motion lives entirely here in the screen, driven by a manual
            // clock (not rememberInfiniteTransition, which never advances once a test
            // freezes the clock) — so an instrumented test can compute an exact elapsedMs
            // that places the mark inside whichever shield zone is currently active.
            var elapsedMs by remember { mutableLongStateOf(0L) }
            val isComplete = slingshotState.isComplete
            LaunchedEffect(isComplete) {
                // Keyed on isComplete (matches every other rhythmlane/real-time
                // screen in this app, e.g. DavidGoliathDodgeScreen) so the loop
                // actually stops once the practice is done — a `while(isActive)`
                // loop with no terminating condition never lets Compose's test
                // tooling reach idle at all, since it always has another frame
                // callback pending.
                if (isComplete) return@LaunchedEffect
                var startFrameNanos = -1L
                while (isActive) {
                    withFrameNanos { frameNanos ->
                        if (startFrameNanos < 0) startFrameNanos = frameNanos
                        elapsedMs = (frameNanos - startFrameNanos) / 1_000_000
                    }
                }
            }
            val markFraction = markFractionAt(elapsedMs)

            // The stone's pointerInput(Unit) gesture handler below is set up
            // once and never restarts, so a plain `val` read of shieldZone
            // captured into its onDragEnd closure would go stale the moment
            // the shield relocates after a hit — the hit-test would keep
            // checking the *old* zone's bounds forever, causing exactly the
            // reported bug (a visually-aligned release missing, or a
            // misaligned one still hitting). rememberUpdatedState keeps a
            // reference the closure can read fresh at release time, the same
            // way elapsedMs (a MutableState) already reads live there.
            val currentShieldZone by rememberUpdatedState(shieldZone)

            // weight(1f, fill = true) hands this element exactly the space left
            // over after every other (naturally-sized) sibling in this Column,
            // and AspectRatioFitBox letterbox-fits within that bounded box, so
            // nothing here ever needs to scroll. The nested BoxWithConstraints
            // re-reads the fitted box's own size so maxWidth/maxHeight below stay
            // correct.
            AspectRatioFitBox(ratio = 1f, modifier = Modifier.weight(1f, fill = true).fillMaxSize()) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val density = LocalDensity.current
                val trackWidthPx = with(density) { maxWidth.toPx() }
                var dragOffsetX by remember { mutableStateOf(0f) }
                val aimFraction = (0.5f + dragOffsetX / trackWidthPx).coerceIn(0f, 1f)

                val zoneLabel = when (shieldZone) {
                    ShieldZone.LEFT -> stringResource(R.string.david_goliath_sling_shield_zone_left)
                    ShieldZone.MIDDLE -> stringResource(R.string.david_goliath_sling_shield_zone_middle)
                    ShieldZone.RIGHT -> stringResource(R.string.david_goliath_sling_shield_zone_right)
                }
                val shieldContentDescription = stringResource(R.string.david_goliath_sling_shield_content_description, zoneLabel)
                Image(
                    painter = painterResource(
                        if (slingshotState.lastOutcome == SlingshotOutcome.HIT) R.drawable.ic_goliath_shield_surprised else R.drawable.ic_goliath_shield,
                    ),
                    contentDescription = shieldContentDescription,
                    modifier = Modifier
                        .offset(x = maxWidth * shieldImageMinFraction(shieldZone), y = 8.dp)
                        .width(maxWidth * SHIELD_IMAGE_WIDTH_FRACTION),
                )

                // The mark is a horizontal line, not a dot — meant to be lined up with,
                // not just tapped. Half as wide as the shield's own perimeter (rendering
                // only; HIT_TOLERANCE, the actual hit-test tolerance, is unchanged), so
                // lining it up precisely matters more than it used to. It only counts as
                // a hit while its center falls within the current zone's own span; the
                // mark sweeps a much wider range than any single zone, so most of its
                // swing isn't a scoring opportunity at all.
                val markContentDescription = stringResource(R.string.david_goliath_sling_target_mark_content_description)
                val markWidth = maxWidth * SHIELD_WIDTH_FRACTION / 2f
                Box(
                    modifier = Modifier
                        .offset(x = maxWidth * markFraction - markWidth / 2, y = maxHeight * 0.2f)
                        .width(markWidth)
                        .height(4.dp)
                        .background(MaterialTheme.colorScheme.primary)
                        .semantics { contentDescription = markContentDescription },
                )

                if (dragOffsetX != 0f) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawLine(
                            color = Color.Gray,
                            start = Offset(size.width * 0.5f, size.height * 0.85f),
                            end = Offset(size.width * aimFraction, size.height * 0.3f),
                            strokeWidth = 4f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f)),
                        )
                    }
                }

                Image(
                    painter = painterResource(R.drawable.ic_sling),
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp)
                        .size(64.dp),
                )

                val stoneContentDescription = stringResource(R.string.david_goliath_sling_stone_content_description)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset { IntOffset(dragOffsetX.roundToInt(), 0) }
                        .padding(bottom = 24.dp)
                        .size(48.dp)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragEnd = {
                                    // Recomputed from the live drag/animation/zone state, not
                                    // values captured when this gesture handler was first
                                    // created (pointerInput(Unit) never restarts, so plain vals
                                    // would otherwise be stale — see currentShieldZone's doc).
                                    val releasedAim = (0.5f + dragOffsetX / trackWidthPx).coerceIn(0f, 1f)
                                    val liveShieldMin = shieldMinFraction(currentShieldZone)
                                    val liveShieldMax = shieldMaxFraction(currentShieldZone)
                                    onStoneReleased(releasedAim, markFractionAt(elapsedMs), liveShieldMin, liveShieldMax)
                                    dragOffsetX = 0f
                                },
                                onDragCancel = { dragOffsetX = 0f },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffsetX += dragAmount.x
                                },
                            )
                        }
                        .semantics { contentDescription = stoneContentDescription },
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_stone_smooth),
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                    )
                }
            }
            }

            if (previouslyCompleted && !slingshotState.isComplete) {
                Text(
                    text = stringResource(R.string.puzzle_already_completed_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DavidGoliathSlingPracticePreview() {
    BibleAdventuresTheme {
        DavidGoliathSlingPracticeContent(
            slingshotState = SlingshotGameState(),
            shieldZone = ShieldZone.LEFT,
            onStoneReleased = { _, _, _, _ -> },
            onContinue = {},
            onBackToMainMenu = {},
        )
    }
}
