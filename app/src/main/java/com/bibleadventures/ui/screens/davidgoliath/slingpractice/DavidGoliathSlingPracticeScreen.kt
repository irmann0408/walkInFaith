package com.bibleadventures.ui.screens.davidgoliath.slingpractice

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.bibleadventures.ui.components.AdventureMenuButton
import com.bibleadventures.ui.screens.davidgoliath.DavidGoliathViewModel
import com.bibleadventures.ui.theme.BibleAdventuresTheme
import kotlin.math.roundToInt

private const val MARK_MIN_FRACTION = 0.15f
private const val MARK_MAX_FRACTION = 0.85f

// The shield IMAGE's own bounding-box span, expressed as fixed fractions of
// the same 0..1 track the mark and aim already use. Positioned (not just
// sized) explicitly via offset + width below — not via fillMaxWidth +
// align(TopCenter), which always centers at fraction 0.5 regardless of
// these constants, silently decoupling what's drawn from what's checked.
private const val SHIELD_IMAGE_MIN_FRACTION = 0.00f
private const val SHIELD_IMAGE_MAX_FRACTION = 0.60f

// ic_goliath_shield.xml's silhouette is narrower than its own bounding box:
// its widest point (the top edge, where the mark's line sits) spans x=12..52
// of a 64-wide viewport. Both the hit-test's "shield perimeter" and the
// mark's rendered line width are derived from this actual top-edge span, not
// the full (partly transparent) bounding box — "within the shield" means
// within the shape a player can actually see, not its invisible padding.
private const val SHIELD_TOP_EDGE_LEFT_RATIO = 12f / 64f
private const val SHIELD_TOP_EDGE_RIGHT_RATIO = 52f / 64f

private val SHIELD_IMAGE_WIDTH_FRACTION = SHIELD_IMAGE_MAX_FRACTION - SHIELD_IMAGE_MIN_FRACTION

// The shield's true, visible perimeter — what the hit-test checks and what
// the mark's line width matches. The mark sweeps a wider range
// (MARK_MIN..MARK_MAX) than this window, so it spends real time outside the
// shield entirely; timing the release to when it's actually over the shield
// is the point. Also comfortably includes MARK_MIN_FRACTION with margin, so
// DavidGoliathFlowTest.kt's simple freeze-and-drag technique keeps working
// (rememberInfiniteTransition animations don't progress once the test clock
// is frozen — a known Compose testing limitation — so the test always drags
// to the animation's initialValue).
private val SHIELD_MIN_FRACTION = SHIELD_IMAGE_MIN_FRACTION + SHIELD_TOP_EDGE_LEFT_RATIO * SHIELD_IMAGE_WIDTH_FRACTION
private val SHIELD_MAX_FRACTION = SHIELD_IMAGE_MIN_FRACTION + SHIELD_TOP_EDGE_RIGHT_RATIO * SHIELD_IMAGE_WIDTH_FRACTION

@Composable
fun DavidGoliathSlingPracticeScreen(
    viewModel: DavidGoliathViewModel,
    onContinue: () -> Unit,
    previouslyCompleted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DavidGoliathSlingPracticeContent(
        slingshotState = uiState.slingshotState,
        onStoneReleased = viewModel::onStoneReleased,
        onContinue = onContinue,
        previouslyCompleted = previouslyCompleted,
        modifier = modifier,
    )
}

@Composable
private fun DavidGoliathSlingPracticeContent(
    slingshotState: SlingshotGameState,
    onStoneReleased: (aimedPosition: Float, markPosition: Float, shieldMinFraction: Float, shieldMaxFraction: Float) -> Unit,
    onContinue: () -> Unit,
    previouslyCompleted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
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

            val feedback = when (slingshotState.lastOutcome) {
                SlingshotOutcome.HIT -> stringResource(R.string.feedback_great_job)
                SlingshotOutcome.MISS -> stringResource(R.string.feedback_try_another_one)
                SlingshotOutcome.NONE -> ""
            }
            Box(modifier = Modifier.height(32.dp)) {
                Text(text = feedback, style = MaterialTheme.typography.titleLarge)
            }

            // The mark's motion lives entirely here in the screen — never written into
            // the ViewModel — so the pure engine only has to judge "was this release
            // close enough," never worry about animation/time (see SlingshotGame's doc).
            val markFraction by rememberInfiniteTransition(label = "targetMark").animateFloat(
                initialValue = MARK_MIN_FRACTION,
                targetValue = MARK_MAX_FRACTION,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 2200, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "targetMarkFraction",
            )

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .aspectRatio(1f),
            ) {
                val density = LocalDensity.current
                val trackWidthPx = with(density) { maxWidth.toPx() }
                var dragOffsetX by remember { mutableStateOf(0f) }
                val aimFraction = (0.5f + dragOffsetX / trackWidthPx).coerceIn(0f, 1f)

                val shieldContentDescription = stringResource(R.string.david_goliath_sling_shield_content_description)
                Image(
                    painter = painterResource(
                        if (slingshotState.isHit) R.drawable.ic_goliath_shield_surprised else R.drawable.ic_goliath_shield,
                    ),
                    contentDescription = shieldContentDescription,
                    modifier = Modifier
                        .offset(x = maxWidth * SHIELD_IMAGE_MIN_FRACTION, y = 8.dp)
                        .width(maxWidth * SHIELD_IMAGE_WIDTH_FRACTION),
                )

                // The mark is a horizontal line as wide as the shield itself, not a dot —
                // it's meant to be lined up with, not just tapped. It only counts as a hit
                // while its center falls within the shield's own span above; the mark
                // sweeps a much wider range than that, so most of its swing it isn't a
                // scoring opportunity at all.
                val markContentDescription = stringResource(R.string.david_goliath_sling_target_mark_content_description)
                val markWidth = maxWidth * (SHIELD_MAX_FRACTION - SHIELD_MIN_FRACTION)
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
                                    // Recomputed from the live drag/animation state, not the
                                    // `aimFraction`/`markFraction` captured when this gesture
                                    // handler was first created (pointerInput(Unit) never
                                    // restarts, so those outer vals would otherwise be stale).
                                    val releasedAim = (0.5f + dragOffsetX / trackWidthPx).coerceIn(0f, 1f)
                                    onStoneReleased(releasedAim, markFraction, SHIELD_MIN_FRACTION, SHIELD_MAX_FRACTION)
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

            if (previouslyCompleted && !slingshotState.isComplete) {
                Text(
                    text = stringResource(R.string.puzzle_already_completed_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            if (slingshotState.isComplete || previouslyCompleted) {
                AdventureMenuButton(
                    text = stringResource(R.string.action_continue),
                    onClick = onContinue,
                    modifier = Modifier.padding(top = 16.dp),
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
            onStoneReleased = { _, _, _, _ -> },
            onContinue = {},
        )
    }
}
