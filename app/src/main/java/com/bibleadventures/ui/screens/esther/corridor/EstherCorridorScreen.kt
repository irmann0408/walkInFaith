package com.bibleadventures.ui.screens.esther.corridor

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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.bibleadventures.game.stories.EstherContent
import com.bibleadventures.ui.components.AdventureMenuButton
import com.bibleadventures.ui.components.BackToMainMenuTopBar
import com.bibleadventures.ui.screens.esther.EstherViewModel
import com.bibleadventures.ui.theme.BibleAdventuresTheme
import kotlinx.coroutines.isActive

private const val LANE_COUNT = 3
private val NOTE_SIZE = 40.dp
private const val TRAVEL_DURATION_MS = 1500L
private const val NOTE_GRACE_MS = 300L

/**
 * A 3-lane, downward-scrolling rhythm mini-game — Beatstar-inspired, tap
 * notes only. Mirrors every other real-time mechanic in this app (Sling
 * Practice, this scene's own previous single-marker version): the screen
 * owns the live scroll clock and reports each lane tap with a timestamp;
 * [com.bibleadventures.game.puzzles.rhythmlane.RhythmLaneGame] only judges
 * that tap against the chart. A missed note (nobody tapped its lane in
 * time) is never a setback — it just disappears, the chart loops forever,
 * and the courage meter only ever fills, never resets, so there's no way
 * to get stuck unable to finish.
 *
 * Deliberately driven by a manual [withFrameNanos] accumulator, not
 * `rememberInfiniteTransition` — Chapter 2's Sling Practice work found,
 * confirmed three separate ways, that `rememberInfiniteTransition`
 * animations don't progress at all under Compose's frozen test clock,
 * which is exactly the technique an instrumented test needs to drive this
 * deterministically (freeze the clock, advance to each note's exact time,
 * tap that lane).
 */
@Composable
fun EstherCorridorScreen(
    viewModel: EstherViewModel,
    onContinue: () -> Unit,
    onBackToMainMenu: () -> Unit,
    previouslyCompleted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    EstherCorridorContent(
        rhythmLaneState = uiState.rhythmLaneState,
        onLaneTapped = viewModel::onCorridorLaneTapped,
        onTimeAdvanced = viewModel::onCorridorTimeAdvanced,
        onContinue = onContinue,
        onBackToMainMenu = onBackToMainMenu,
        previouslyCompleted = previouslyCompleted,
        modifier = modifier,
    )
}

@Composable
private fun EstherCorridorContent(
    rhythmLaneState: RhythmLaneGameState,
    onLaneTapped: (Int, Long) -> Unit,
    onTimeAdvanced: (Long) -> Unit,
    onContinue: () -> Unit,
    onBackToMainMenu: () -> Unit,
    previouslyCompleted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var elapsedMs by remember { mutableLongStateOf(0L) }
    val isComplete = rhythmLaneState.isComplete

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

    val feedback = when (rhythmLaneState.lastJudgment) {
        NoteJudgment.PERFECT, NoteJudgment.GREAT -> stringResource(R.string.esther_brave_approach_corridor_great_rhythm)
        NoteJudgment.MISSED -> stringResource(R.string.esther_brave_approach_corridor_keep_going)
        null -> ""
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { if (previouslyCompleted) BackToMainMenuTopBar(onBackToMainMenu) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.esther_brave_approach_corridor_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = stringResource(R.string.esther_brave_approach_corridor_instructions),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = stringResource(R.string.esther_brave_approach_corridor_progress_label, rhythmLaneState.hits, rhythmLaneState.requiredHits),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
            )

            Box(modifier = Modifier.height(28.dp).padding(top = 4.dp)) {
                Text(text = feedback, style = MaterialTheme.typography.titleMedium)
            }

            // Courage meter — monotonically fills, never resets.
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
                        .fillMaxWidth(rhythmLaneState.progressFraction)
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primary),
                )
            }

            if (!isComplete) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = true)
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    repeat(LANE_COUNT) { lane ->
                        NoteLane(
                            lane = lane,
                            chart = rhythmLaneState.chart,
                            judgedNoteKeys = rhythmLaneState.judgedNoteKeys,
                            elapsedMs = elapsedMs,
                            onTapped = { onLaneTapped(lane, elapsedMs) },
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                    }
                }
            }

            if (previouslyCompleted && !isComplete) {
                Text(
                    text = stringResource(R.string.puzzle_already_completed_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            if (isComplete || previouslyCompleted) {
                AdventureMenuButton(
                    text = stringResource(R.string.action_continue),
                    onClick = onContinue,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
        }
    }
}

/** One lane: a vertical note-scroll track above a large, always-tappable hit zone at the bottom. */
@Composable
private fun NoteLane(
    lane: Int,
    chart: RhythmLaneChart,
    judgedNoteKeys: Set<String>,
    elapsedMs: Long,
    onTapped: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val laneDescription = stringResource(R.string.esther_brave_approach_corridor_lane_content_description, lane + 1)

    Column(modifier = modifier) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = true),
        ) {
            val trackHeight = maxHeight
            visibleNotes(chart, lane, judgedNoteKeys, elapsedMs).forEach { msUntilHit ->
                val fraction = (1f - msUntilHit.toFloat() / TRAVEL_DURATION_MS).coerceIn(0f, 1f)
                Image(
                    painter = painterResource(R.drawable.ic_courage_marker),
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (trackHeight - NOTE_SIZE) * fraction)
                        .size(NOTE_SIZE),
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(top = 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClickLabel = laneDescription, onClick = onTapped)
                .semantics { contentDescription = laneDescription },
        )
    }
}

/** Every not-yet-judged note in [lane] currently within its travel window, as milliseconds until it reaches the hit zone. */
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
private fun EstherCorridorPreview() {
    BibleAdventuresTheme {
        EstherCorridorContent(
            rhythmLaneState = RhythmLaneGameState(chart = EstherContent.corridorChart, requiredHits = 10),
            onLaneTapped = { _, _ -> },
            onTimeAdvanced = {},
            onContinue = {},
            onBackToMainMenu = {},
        )
    }
}
