package com.bibleadventures.ui.screens.feeding5000.catching

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bibleadventures.R
import com.bibleadventures.game.puzzles.rhythmlane.NoteJudgment
import com.bibleadventures.game.puzzles.rhythmlane.RhythmLaneChart
import com.bibleadventures.game.puzzles.rhythmlane.RhythmLaneGameState
import com.bibleadventures.game.stories.Feeding5000Content
import com.bibleadventures.ui.LocalReducedMotion
import com.bibleadventures.ui.components.PuzzleTopBar
import com.bibleadventures.ui.screens.feeding5000.Feeding5000ViewModel
import com.bibleadventures.ui.theme.BibleAdventuresTheme
import kotlinx.coroutines.isActive

private const val LANE_COUNT = 3
private val NOTE_SIZE = 40.dp
private val BASKET_SIZE = 56.dp
private const val TRAVEL_DURATION_MS = 1800L
private const val NOTE_GRACE_MS = 300L


/**
 * Phase B of the Grand Feast finale — still reuses `rhythmlane` exactly as
 * before (confirmed with the user, no new engine code), but the
 * interaction changed per direct feedback: instead of 3 independently-
 * tappable lanes, there's a single basket that slides between the 3 lanes,
 * moved one lane at a time via left/right buttons (same `FilledTonalIconButton`
 * D-pad idiom as Good Samaritan's/Daniel's grid-maze movement — the
 * natural extension of "grid-based movement uses buttons, never tap-on-
 * tile" to a 1D lane). A catch is judged automatically, every frame,
 * against whichever lane the basket currently sits in
 * ([Feeding5000ViewModel.onCatchingTimeAdvanced]) — the challenge is
 * steering the basket into place *before* an item lands, not reacting to
 * it. `requiredHits = 12`, John 6:13's twelve baskets, exactly.
 */
@Composable
fun Feeding5000CatchingScreen(
    viewModel: Feeding5000ViewModel,
    onContinue: () -> Unit,
    onBackToMainMenu: () -> Unit,
    previouslyCompleted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Feeding5000CatchingContent(
        catchingState = uiState.catchingState,
        basketLane = uiState.catchingBasketLane,
        onBasketMoved = viewModel::onCatchingBasketMoved,
        onTimeAdvanced = viewModel::onCatchingTimeAdvanced,
        onContinue = onContinue,
        onBackToMainMenu = onBackToMainMenu,
        previouslyCompleted = previouslyCompleted,
        modifier = modifier,
    )
}

@Composable
private fun Feeding5000CatchingContent(
    catchingState: RhythmLaneGameState,
    basketLane: Int,
    onBasketMoved: (Int) -> Unit,
    onTimeAdvanced: (Long) -> Unit,
    onContinue: () -> Unit,
    onBackToMainMenu: () -> Unit,
    previouslyCompleted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var elapsedMs by remember { mutableLongStateOf(0L) }
    val isComplete = catchingState.isComplete

    LaunchedEffect(isComplete) {
        if (isComplete) return@LaunchedEffect
        var startFrameNanos = -1L
        while (isActive) {
            withFrameNanos { frameNanos ->
                if (startFrameNanos < 0) startFrameNanos = frameNanos
                elapsedMs = (frameNanos - startFrameNanos) / 1_000_000
            }
            onTimeAdvanced(elapsedMs)
        }
    }

    val feedback = when (catchingState.lastJudgment) {
        NoteJudgment.PERFECT, NoteJudgment.GREAT -> stringResource(R.string.feedback_great_job)
        NoteJudgment.MISSED -> stringResource(R.string.feedback_try_another_one)
        null -> ""
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            if (previouslyCompleted || isComplete) {
                PuzzleTopBar(
                    showBackButton = previouslyCompleted,
                    onBackToMainMenu = onBackToMainMenu,
                    showNextButton = isComplete || previouslyCompleted,
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
                text = stringResource(R.string.feeding_5000_catching_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = stringResource(R.string.feeding_5000_catching_instructions),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = stringResource(R.string.feeding_5000_catching_progress_label, catchingState.hits, catchingState.requiredHits),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
            )

            Box(modifier = Modifier.height(28.dp).padding(top = 4.dp)) {
                Text(text = feedback, style = MaterialTheme.typography.titleMedium)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .height(20.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(catchingState.progressFraction)
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primary),
                )
            }

            if (!isComplete) {
                // weight(1f, fill = true) hands this wrapping Column exactly the
                // space left over after title/instructions/progress-meter claim
                // theirs, then the falling-lane Row's own weight(1f, fill = true)
                // divides that against its own fixed-size siblings (the basket
                // track, the move controls) — nested the same way, so nothing here
                // ever needs to scroll.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = true)
                        .padding(top = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().weight(1f, fill = true),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        repeat(LANE_COUNT) { lane ->
                            FallingLaneTrack(
                                lane = lane,
                                chart = catchingState.chart,
                                judgedNoteKeys = catchingState.judgedNoteKeys,
                                elapsedMs = elapsedMs,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                            )
                        }
                    }

                    SingleBasketTrack(basketLane = basketLane, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))

                    BasketMoveControls(onBasketMoved = onBasketMoved, modifier = Modifier.padding(top = 12.dp))
                }
            }

            if (previouslyCompleted && !isComplete) {
                Text(
                    text = stringResource(R.string.puzzle_already_completed_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

/** Falling bread only — no basket, no click handling; the single basket in [SingleBasketTrack] is what catches. */
@Composable
private fun FallingLaneTrack(
    lane: Int,
    chart: RhythmLaneChart,
    judgedNoteKeys: Set<String>,
    elapsedMs: Long,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier) {
        val trackHeight = maxHeight
        visibleNotes(chart, lane, judgedNoteKeys, elapsedMs).forEach { msUntilHit ->
            val fraction = (1f - msUntilHit.toFloat() / TRAVEL_DURATION_MS).coerceIn(0f, 1f)
            Image(
                painter = painterResource(R.drawable.ic_supply_bread),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (trackHeight - NOTE_SIZE) * fraction)
                    .size(NOTE_SIZE),
            )
        }
    }
}

/** One basket, sliding to whichever of the 3 lanes [basketLane] names — same width as the [FallingLaneTrack] row above it, so its resting spot lines up under each lane. */
@Composable
private fun SingleBasketTrack(basketLane: Int, modifier: Modifier = Modifier) {
    val basketDescription = stringResource(R.string.feeding_5000_catching_basket_content_description, basketLane + 1)

    val reducedMotion = LocalReducedMotion.current
    BoxWithConstraints(modifier = modifier.height(64.dp)) {
        val laneWidth = maxWidth / LANE_COUNT
        val basketOffsetX by animateDpAsState(
            targetValue = laneWidth * basketLane + (laneWidth - BASKET_SIZE) / 2,
            animationSpec = if (reducedMotion) snap() else spring(),
            label = "catchingBasketOffsetX",
        )
        Box(
            modifier = Modifier
                .offset(x = basketOffsetX)
                .size(BASKET_SIZE)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .semantics { contentDescription = basketDescription },
            contentAlignment = Alignment.Center,
        ) {
            Image(painter = painterResource(R.drawable.ic_leftover_basket), contentDescription = null, modifier = Modifier.size(40.dp))
        }
    }
}

@Composable
private fun BasketMoveControls(onBasketMoved: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(56.dp)) {
        MoveButton(
            icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            contentDescription = stringResource(R.string.feeding_5000_catching_move_left_content_description),
            onClick = { onBasketMoved(-1) },
        )
        MoveButton(
            icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = stringResource(R.string.feeding_5000_catching_move_right_content_description),
            onClick = { onBasketMoved(1) },
        )
    }
}

@Composable
private fun MoveButton(icon: ImageVector, contentDescription: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .clickable(onClickLabel = contentDescription, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription, modifier = Modifier.size(32.dp))
    }
}

private fun visibleNotes(chart: RhythmLaneChart, lane: Int, judgedNoteKeys: Set<String>, elapsedMs: Long): List<Long> {
    val currentLoopIndex = elapsedMs / chart.loopDurationMs
    return chart.notes
        .filter { it.lane == lane }
        .flatMap { note ->
            (currentLoopIndex..currentLoopIndex + 1).mapNotNull { loopIndex ->
                if ("$loopIndex:${note.id}" in judgedNoteKeys) return@mapNotNull null
                val msUntilHit = (loopIndex * chart.loopDurationMs + note.hitTimeMs) - elapsedMs
                msUntilHit.takeIf { it in -NOTE_GRACE_MS..TRAVEL_DURATION_MS }
            }
        }
}

@Preview(showBackground = true)
@Composable
private fun Feeding5000CatchingPreview() {
    BibleAdventuresTheme {
        Feeding5000CatchingContent(
            catchingState = RhythmLaneGameState(chart = Feeding5000Content.catchingChart, requiredHits = Feeding5000Content.CATCHING_REQUIRED_HITS),
            basketLane = 1,
            onBasketMoved = {},
            onTimeAdvanced = {},
            onContinue = {},
            onBackToMainMenu = {},
        )
    }
}
