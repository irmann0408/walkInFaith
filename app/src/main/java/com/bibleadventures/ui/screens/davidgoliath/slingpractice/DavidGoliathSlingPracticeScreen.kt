package com.bibleadventures.ui.screens.davidgoliath.slingpractice

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bibleadventures.R
import com.bibleadventures.domain.model.CharacterCustomization
import com.bibleadventures.game.puzzles.slingshot.SlingshotGame
import com.bibleadventures.game.puzzles.slingshot.SlingshotGameState
import com.bibleadventures.game.puzzles.slingshot.SlingshotOutcome
import com.bibleadventures.game.puzzles.slingshot.Vector2
import com.bibleadventures.ui.components.AspectRatioFitBox
import com.bibleadventures.ui.components.CharacterCallout
import com.bibleadventures.ui.components.Posture
import com.bibleadventures.ui.components.PuzzleTopBar
import com.bibleadventures.ui.screens.davidgoliath.DavidGoliathViewModel
import com.bibleadventures.ui.theme.BibleAdventuresTheme
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlinx.coroutines.isActive

private const val RAT_MIN_X_FRACTION = 0.15f
private const val RAT_MAX_X_FRACTION = 0.85f

/** Time to cross one row horizontally (MIN to MAX or back) before stepping down and reversing — a classic "descending invader" pattern, not a diagonal drift. */
private const val RAT_ROW_TRAVEL_MS = 2200L

/** How many horizontal passes a rat gets before it reaches the escape line unhit. */
private const val RAT_TOTAL_ROWS = 3

private const val RAT_TOP_Y_FRACTION = 0.04f
private const val RAT_ESCAPE_Y_FRACTION = 0.55f

private const val RAT_ASPECT_RATIO = 1024f / 559f
private val RAT_WIDTH = 64.dp
private val RAT_HEIGHT = RAT_WIDTH / RAT_ASPECT_RATIO

/** How often the rat's walk-cycle swaps between its "relaxed" and "humped" frame — purely a cosmetic scurry animation, unrelated to [RAT_ROW_TRAVEL_MS]. */
private const val RAT_WALK_FRAME_MS = 250L

/** The sling's fixed pivot — "the middle" of the play area, near the bottom where the player's hand would be. Internal, not private: an instrumented test needs these exact fractions (alongside [ratElapsedMs]) to convert the rat's live pixel bounds into the same 0..1 track space `onDragEnd` below computes in. */
internal const val ANCHOR_X_FRACTION = 0.5f
internal const val ANCHOR_Y_FRACTION = 0.85f

/**
 * Both sling art states are flat illustrations of one continuous cord, not
 * a rig that can be stretched — so rather than trying to make the drawn
 * pouch/knot literally track the dragged stone, the whole image is
 * rotated as a rigid piece around the loop it's drawn hanging from, which
 * stays pinned at [ANCHOR_X_FRACTION]/[ANCHOR_Y_FRACTION] regardless of
 * rotation (that's the "pivot is the loop" the art was made for). The
 * fraction/angle constants below were measured directly off each PNG's
 * own pixel geometry — the loop's own hole, and the pouch/knot at the
 * cord's far end — not eyeballed, so the rotation lines up with what's
 * actually drawn.
 */
private const val SLING_PULLED_PIVOT_X = 0.259f
private const val SLING_PULLED_PIVOT_Y = 0.132f
private const val SLING_PULLED_DEFAULT_ANGLE_DEG = 62.2f
private const val SLING_PULLED_ASPECT_RATIO = 1024f / 717f
private val SLING_PULLED_WIDTH = 84.dp
private val SLING_PULLED_HEIGHT = SLING_PULLED_WIDTH / SLING_PULLED_ASPECT_RATIO

private const val SLING_RELEASED_PIVOT_X = 0.156f
private const val SLING_RELEASED_PIVOT_Y = 0.944f
private const val SLING_RELEASED_DEFAULT_ANGLE_DEG = -65.4f
private const val SLING_RELEASED_ASPECT_RATIO = 706f / 1081f
private val SLING_RELEASED_WIDTH = 30.dp
private val SLING_RELEASED_HEIGHT = SLING_RELEASED_WIDTH / SLING_RELEASED_ASPECT_RATIO

private val RADIANS_TO_DEGREES = 180f / PI.toFloat()

/**
 * Exposes the rat's current elapsed-time clock value via semantics, purely
 * for instrumented-test introspection — never read aloud, not a real
 * accessibility property. Since the hit-test now leads a moving target
 * (aims at the rat's *projected* position when the stone is due to arrive,
 * not where it's standing at release — see `onDragEnd` below), a test
 * needs to know the live `elapsedMs` value to compute that same
 * projection itself, the same way it already reads the rat's rendered
 * position instead of assuming one.
 */
internal val RatElapsedMsKey = SemanticsPropertyKey<Long>("RatElapsedMs")
private var SemanticsPropertyReceiver.ratElapsedMs by RatElapsedMsKey

/** How far (as a fraction of the track) the stone visibly travels when let fly — purely cosmetic, the hit-test itself doesn't care how far the ray is drawn. */
private const val FLIGHT_TRAVEL_FRACTION = 0.9f

/** Internal, not private: an instrumented test needs this exact value to compute the same "projected impact position" `onDragEnd` below does, rather than duplicating it. */
internal const val FLIGHT_DURATION_MS = 300

/** Which row the rat is currently on — driven by a manual clock (not `rememberInfiniteTransition`, which never advances once a test freezes the clock) so an instrumented test can compute an exact elapsedMs that places the rat wherever needed. */
private fun ratRowAt(elapsedMs: Long): Int = (elapsedMs / RAT_ROW_TRAVEL_MS).toInt()

/**
 * Horizontal position within the current row — alternates direction every
 * row (even rows sweep MIN->MAX, odd rows MAX->MIN), so the rat visibly
 * reverses each time it steps down. Internal, not private: an instrumented
 * test calls this directly (the same function `onDragEnd` below uses) to
 * predict where the rat will be when a shot arrives, since the hit-test
 * now leads a moving target rather than aiming at its current position.
 */
internal fun ratXFractionAt(elapsedMs: Long): Float {
    val row = ratRowAt(elapsedMs)
    val withinRowMs = elapsedMs % RAT_ROW_TRAVEL_MS
    val progress = withinRowMs / RAT_ROW_TRAVEL_MS.toFloat()
    return if (row % 2 == 0) {
        RAT_MIN_X_FRACTION + (RAT_MAX_X_FRACTION - RAT_MIN_X_FRACTION) * progress
    } else {
        RAT_MAX_X_FRACTION - (RAT_MAX_X_FRACTION - RAT_MIN_X_FRACTION) * progress
    }
}

/** Steps down once per row, holding still between steps — the rat only ever moves horizontally except at a row change. Internal for the same reason as [ratXFractionAt]. */
internal fun ratYFractionAt(elapsedMs: Long): Float {
    val row = ratRowAt(elapsedMs).coerceAtMost(RAT_TOTAL_ROWS - 1)
    val rowHeightFraction = (RAT_ESCAPE_Y_FRACTION - RAT_TOP_Y_FRACTION) / RAT_TOTAL_ROWS
    return RAT_TOP_Y_FRACTION + rowHeightFraction * row
}

private fun ratHasEscapedAt(elapsedMs: Long): Boolean = ratRowAt(elapsedMs) >= RAT_TOTAL_ROWS

/** Picks the walk-cycle frame (relaxed/humped) and left/right-facing sprite to match the row's current sweep direction — purely cosmetic, the hit-test only ever reasons in terms of [ratXFractionAt]/[ratYFractionAt]. */
private fun ratDrawableRes(elapsedMs: Long): Int {
    val movingRight = ratRowAt(elapsedMs) % 2 == 0
    val humped = (elapsedMs / RAT_WALK_FRAME_MS) % 2 == 1L
    return when {
        movingRight -> if (humped) R.drawable.ic_rat_humped_right else R.drawable.ic_rat_relaxed_right
        else -> if (humped) R.drawable.ic_rat_humped_left else R.drawable.ic_rat_relaxed_left
    }
}

/**
 * The flying stone's start/end points (0..1 track fractions) plus the
 * exact (anchor, pull, ratPosition) used to resolve the shot. [ratPosition]
 * is the rat's *projected* position at the moment the stone is due to
 * arrive (`elapsedMs + FLIGHT_DURATION_MS`, computed in `onDragEnd` below),
 * not where it was standing at release — the rat keeps moving the whole
 * time, so the shot has to lead it, exactly like judging a real moving
 * target. The actual outcome isn't committed to game state until the
 * flight animation finishes — [endX]/[endY] are chosen from a same-math
 * prediction ([SlingshotGame.wouldHit]) so a hit visibly lands the stone
 * right on the rat, not just off in the general direction.
 */
private data class StoneFlight(
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val anchor: Vector2,
    val pull: Vector2,
    val ratPosition: Vector2,
)

@Composable
fun DavidGoliathSlingPracticeScreen(
    viewModel: DavidGoliathViewModel,
    onContinue: () -> Unit,
    onBackToMainMenu: () -> Unit,
    previouslyCompleted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val characterCustomization by viewModel.characterCustomization.collectAsStateWithLifecycle()

    DavidGoliathSlingPracticeContent(
        slingshotState = uiState.slingshotState,
        characterCustomization = characterCustomization,
        onStoneReleased = viewModel::onStoneReleased,
        onRatEscaped = viewModel::onRatEscaped,
        onContinue = onContinue,
        onBackToMainMenu = onBackToMainMenu,
        previouslyCompleted = previouslyCompleted,
        modifier = modifier,
    )
}

@Composable
private fun DavidGoliathSlingPracticeContent(
    slingshotState: SlingshotGameState,
    characterCustomization: CharacterCustomization,
    onStoneReleased: (anchor: Vector2, pull: Vector2, ratPosition: Vector2) -> Unit,
    onRatEscaped: () -> Unit,
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
                text = stringResource(R.string.david_goliath_sling_practice_progress_label, slingshotState.hits, slingshotState.requiredHits),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 4.dp),
            )

            val feedback = when (slingshotState.lastOutcome) {
                SlingshotOutcome.HIT -> stringResource(R.string.feedback_great_job)
                SlingshotOutcome.MISS -> stringResource(R.string.feedback_try_another_one)
                SlingshotOutcome.ESCAPED -> stringResource(R.string.david_goliath_sling_feedback_escaped)
                SlingshotOutcome.NONE -> null
            }

            // A fresh local clock per rat: keyed on ratsSpawned so it resets to 0
            // the moment a rat is hit or escapes, letting the next one always start
            // its fall from the top. Driven by a manual clock (not
            // rememberInfiniteTransition, which never advances once a test freezes
            // the clock) so an instrumented test can step it deterministically.
            var elapsedMs by remember { mutableLongStateOf(0L) }
            val isComplete = slingshotState.isComplete

            // Gates the very first rat: it sits still at its starting spot
            // (elapsedMs stays 0 until this flips) until the player's first
            // touch on the stone (see onDragStart below), so there's time to
            // read the character's introduction before anything is moving —
            // no separate Start button needed, since pulling the stone back
            // is already the one gesture the whole game is built around.
            // Re-composed fresh (remember, no saved-state restore) each time
            // this screen is entered, so revisiting it for a retry gets the
            // same reading pause again.
            var hasStarted by remember { mutableStateOf(false) }

            // Hoisted above the rat-clock effect (not declared inside
            // BoxWithConstraints below) so that effect can read `flight` to
            // defer committing a natural escape while a shot is still
            // resolving (see below) — the rat itself keeps moving normally,
            // never frozen.
            var dragOffset by remember { mutableStateOf(Offset.Zero) }
            var flight by remember { mutableStateOf<StoneFlight?>(null) }
            val flightProgress = remember { Animatable(0f) }

            LaunchedEffect(slingshotState.ratsSpawned, isComplete, hasStarted) {
                if (isComplete || !hasStarted) return@LaunchedEffect
                elapsedMs = 0L
                var startFrameNanos = -1L
                while (isActive) {
                    withFrameNanos { frameNanos ->
                        if (startFrameNanos < 0) startFrameNanos = frameNanos
                        elapsedMs = (frameNanos - startFrameNanos) / 1_000_000
                    }
                    // Never committed while a shot is still resolving — a
                    // stone already in flight was aimed at where the rat was
                    // *projected* to be on arrival (see onDragEnd below), so
                    // that prediction must get to play out before an escape
                    // can independently end the rat's turn out from under it.
                    // Retried every frame rather than dropped, so the escape
                    // still lands the moment the flight clears if it's still
                    // overdue by then.
                    if (ratHasEscapedAt(elapsedMs) && flight == null) {
                        onRatEscaped()
                        break
                    }
                }
            }

            // The outcome is only committed to game state once the stone
            // visibly arrives — never at the moment of release — so a hit
            // always shows the stone actually reaching the rat before it
            // disappears and the count updates.
            LaunchedEffect(flight) {
                val currentFlight = flight
                if (currentFlight != null) {
                    flightProgress.snapTo(0f)
                    flightProgress.animateTo(1f, animationSpec = tween(FLIGHT_DURATION_MS, easing = LinearEasing))
                    onStoneReleased(currentFlight.anchor, currentFlight.pull, currentFlight.ratPosition)
                    flight = null
                }
            }

            val ratXFraction = ratXFractionAt(elapsedMs)
            val ratYFraction = ratYFractionAt(elapsedMs)

            // The outer Box fills the whole leftover Column space
            // (weight(1f, fill = true)); the character and its Start-gate
            // button are anchored to *that* full region (see below), not to
            // the fitted square inside it — otherwise, since the square is
            // top-aligned to sit closer to the title/status text above, the
            // character would have moved up right along with it instead of
            // staying put at the screen's actual bottom-left corner.
            Box(modifier = Modifier.weight(1f, fill = true).fillMaxSize()) {
            AspectRatioFitBox(ratio = 1f, modifier = Modifier.fillMaxSize(), alignment = Alignment.TopCenter) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val density = LocalDensity.current
                val trackWidthPx = with(density) { maxWidth.toPx() }
                val trackHeightPx = with(density) { maxHeight.toPx() }

                val ratContentDescription = stringResource(R.string.david_goliath_sling_rat_content_description)
                if (!isComplete) {
                    Image(
                        painter = painterResource(ratDrawableRes(elapsedMs)),
                        contentDescription = ratContentDescription,
                        modifier = Modifier
                            .offset(x = maxWidth * ratXFraction - RAT_WIDTH / 2, y = maxHeight * ratYFraction)
                            .size(width = RAT_WIDTH, height = RAT_HEIGHT)
                            .semantics {
                                contentDescription = ratContentDescription
                                ratElapsedMs = elapsedMs
                            },
                    )
                }

                val currentFlight = flight

                if (dragOffset != Offset.Zero) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawLine(
                            color = Color.Gray,
                            start = Offset(size.width * ANCHOR_X_FRACTION, size.height * ANCHOR_Y_FRACTION),
                            end = Offset(size.width * ANCHOR_X_FRACTION + dragOffset.x, size.height * ANCHOR_Y_FRACTION + dragOffset.y),
                            strokeWidth = 4f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f)),
                        )
                    }
                }

                // The cord snaps straight in the throw direction for the brief
                // moment a stone is in flight, then falls back to the loaded
                // "pulled" pose the instant it resolves — same rigid-rotation
                // treatment as the pulled pose below, just around its own
                // pivot/default-angle measurements.
                if (currentFlight != null) {
                    val launchDirX = -currentFlight.pull.x
                    val launchDirY = -currentFlight.pull.y
                    val rotationDeg = atan2(launchDirY, launchDirX) * RADIANS_TO_DEGREES - SLING_RELEASED_DEFAULT_ANGLE_DEG
                    Image(
                        painter = painterResource(R.drawable.ic_sling_released),
                        contentDescription = null,
                        modifier = Modifier
                            .offset(
                                x = maxWidth * ANCHOR_X_FRACTION - SLING_RELEASED_WIDTH * SLING_RELEASED_PIVOT_X,
                                y = maxHeight * ANCHOR_Y_FRACTION - SLING_RELEASED_HEIGHT * SLING_RELEASED_PIVOT_Y,
                            )
                            .size(width = SLING_RELEASED_WIDTH, height = SLING_RELEASED_HEIGHT)
                            .graphicsLayer(
                                rotationZ = rotationDeg,
                                transformOrigin = TransformOrigin(SLING_RELEASED_PIVOT_X, SLING_RELEASED_PIVOT_Y),
                            ),
                    )
                } else {
                    // Rotated to face the drag so the cord visibly stretches
                    // toward wherever the stone is being pulled; hangs in its
                    // own drawn resting pose (no rotation) once released back
                    // to Offset.Zero.
                    val rotationDeg = if (dragOffset == Offset.Zero) {
                        0f
                    } else {
                        atan2(dragOffset.y, dragOffset.x) * RADIANS_TO_DEGREES - SLING_PULLED_DEFAULT_ANGLE_DEG
                    }
                    Image(
                        painter = painterResource(R.drawable.ic_sling_pulled),
                        contentDescription = null,
                        modifier = Modifier
                            .offset(
                                x = maxWidth * ANCHOR_X_FRACTION - SLING_PULLED_WIDTH * SLING_PULLED_PIVOT_X,
                                y = maxHeight * ANCHOR_Y_FRACTION - SLING_PULLED_HEIGHT * SLING_PULLED_PIVOT_Y,
                            )
                            .size(width = SLING_PULLED_WIDTH, height = SLING_PULLED_HEIGHT)
                            .graphicsLayer(
                                rotationZ = rotationDeg,
                                transformOrigin = TransformOrigin(SLING_PULLED_PIVOT_X, SLING_PULLED_PIVOT_Y),
                            ),
                    )
                }

                if (currentFlight != null) {
                    val flightX = currentFlight.startX + (currentFlight.endX - currentFlight.startX) * flightProgress.value
                    val flightY = currentFlight.startY + (currentFlight.endY - currentFlight.startY) * flightProgress.value
                    Image(
                        painter = painterResource(R.drawable.ic_stone_smooth),
                        contentDescription = null,
                        modifier = Modifier
                            .offset(x = maxWidth * flightX - 20.dp, y = maxHeight * flightY - 20.dp)
                            .size(40.dp),
                    )
                }

                // Always present (even mid-flight of a previous throw) — a new
                // stone is ready in the sling immediately, no cooldown, and an
                // instrumented test can always find it by content description
                // without waiting out the flight animation.
                val stoneContentDescription = stringResource(R.string.david_goliath_sling_stone_content_description)
                Box(
                    modifier = Modifier
                        .offset(x = maxWidth * ANCHOR_X_FRACTION - 24.dp, y = maxHeight * ANCHOR_Y_FRACTION - 24.dp)
                        .offset { IntOffset(dragOffset.x.roundToInt(), dragOffset.y.roundToInt()) }
                        .size(48.dp)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                // The first touch on the stone is what starts the rat
                                // moving — see the rat-clock LaunchedEffect above — so
                                // there's no separate "Start" affordance to teach: the
                                // player's very first pull is the same gesture that
                                // plays the whole game.
                                onDragStart = { hasStarted = true },
                                onDragEnd = {
                                    // Ignored while a previous shot is still flying — only
                                    // one stone is ever in the air at a time, so the
                                    // deferred onStoneReleased call below always resolves
                                    // against the rat that's actually still on screen.
                                    if (flight == null) {
                                        // Recomputed from the live drag state, not values
                                        // captured when this gesture handler was first
                                        // created (pointerInput(Unit) never restarts).
                                        val anchor = Vector2(ANCHOR_X_FRACTION, ANCHOR_Y_FRACTION)
                                        val pull = Vector2(dragOffset.x / trackWidthPx, dragOffset.y / trackHeightPx)
                                        // The rat keeps moving while the stone is in the
                                        // air, so the shot must lead it: aimed at where
                                        // it's *projected* to be once the stone actually
                                        // arrives (elapsedMs + the flight's own travel
                                        // time), not where it was standing at release.
                                        val impactElapsedMs = elapsedMs + FLIGHT_DURATION_MS
                                        val ratPosition = Vector2(ratXFractionAt(impactElapsedMs), ratYFractionAt(impactElapsedMs))
                                        val pullDistance = hypot(pull.x, pull.y)

                                        if (pullDistance >= SlingshotGame.MIN_PULL_DISTANCE) {
                                            // The stone flies opposite the pull — a real
                                            // sling's launch direction, not the drag
                                            // direction itself. Predicting the hit up front
                                            // (same math `onStoneReleased` will commit) lets
                                            // a true hit land exactly on the rat instead of
                                            // just flying off in the right general direction.
                                            val willHit = SlingshotGame.wouldHit(anchor, pull, ratPosition)
                                            val dirX = -pull.x / pullDistance
                                            val dirY = -pull.y / pullDistance
                                            flight = StoneFlight(
                                                startX = ANCHOR_X_FRACTION,
                                                startY = ANCHOR_Y_FRACTION,
                                                endX = if (willHit) ratPosition.x else (ANCHOR_X_FRACTION + dirX * FLIGHT_TRAVEL_FRACTION).coerceIn(0f, 1f),
                                                endY = if (willHit) ratPosition.y else (ANCHOR_Y_FRACTION + dirY * FLIGHT_TRAVEL_FRACTION).coerceIn(0f, 1f),
                                                anchor = anchor,
                                                pull = pull,
                                                ratPosition = ratPosition,
                                            )
                                        }
                                    }
                                    dragOffset = Offset.Zero
                                },
                                onDragCancel = { dragOffset = Offset.Zero },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    if (flight == null) dragOffset += dragAmount
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

            // Anchored to the outer weighted Box (declared above,
            // surrounding AspectRatioFitBox) rather than the fitted square
            // inside it, so this stays put at the actual bottom-left corner
            // of the available space — its bubble points *up* toward the
            // character (default bubbleBelow=false), so it needs real
            // screen space above it to grow into regardless of where the
            // top-aligned square ends up.
            CharacterCallout(
                characterCustomization = characterCustomization,
                message = feedback ?: stringResource(R.string.david_goliath_sling_practice_instructions),
                posture = if (slingshotState.lastOutcome == SlingshotOutcome.HIT) Posture.THUMBS_UP else Posture.STANDING,
                modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
                bubbleAboveClearance = 76.dp,
            )
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
            characterCustomization = CharacterCustomization(),
            onStoneReleased = { _, _, _ -> },
            onRatEscaped = {},
            onContinue = {},
            onBackToMainMenu = {},
        )
    }
}
