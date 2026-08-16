package com.bibleadventures.ui.screens.jericho.sixdaymarch

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
import com.bibleadventures.game.stories.JerichoContent
import com.bibleadventures.ui.components.AdventureMenuButton
import com.bibleadventures.ui.components.BackToMainMenuTopBar
import com.bibleadventures.ui.screens.jericho.JerichoViewModel
import com.bibleadventures.ui.theme.BibleAdventuresTheme
import kotlinx.coroutines.isActive

private const val LANE_COUNT = 3
private val NOTE_SIZE = 40.dp
private const val TRAVEL_DURATION_MS = 2200L
private const val NOTE_GRACE_MS = 400L

/**
 * The same 3-lane scrolling layout as Esther's "The Long Corridor"
 * ([com.bibleadventures.ui.screens.esther.corridor.EstherCorridorScreen]),
 * paced slower and with a footprint marker instead of Corridor's star —
 * one full, unhurried lap around the wall per successful hit, once a day
 * for six days (Joshua 6:3), so "Day X of 6" reads directly off
 * [RhythmLaneGameState.hits]. A missed beat is never a setback — no
 * danger meter, just a "keep in step" nudge — see
 * [JerichoContent.sixDayMarchChart]'s own doc for why. Drives its clock
 * with a manual [withFrameNanos] accumulator, not `rememberInfiniteTransition`,
 * for the same test-determinism reason as Esther's corridor.
 */
@Composable
fun JerichoSixDayMarchScreen(
    viewModel: JerichoViewModel,
    onContinue: () -> Unit,
    onBackToMainMenu: () -> Unit,
    previouslyCompleted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    JerichoSixDayMarchContent(
        marchState = uiState.sixDayMarchState,
        onLaneTapped = viewModel::onSixDayMarchTapped,
        onTimeAdvanced = viewModel::onSixDayMarchTimeAdvanced,
        onContinue = onContinue,
        onBackToMainMenu = onBackToMainMenu,
        previouslyCompleted = previouslyCompleted,
        modifier = modifier,
    )
}

@Composable
private fun JerichoSixDayMarchContent(
    marchState: RhythmLaneGameState,
    onLaneTapped: (Int, Long) -> Unit,
    onTimeAdvanced: (Long) -> Unit,
    onContinue: () -> Unit,
    onBackToMainMenu: () -> Unit,
    previouslyCompleted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var elapsedMs by remember { mutableLongStateOf(0L) }
    val isComplete = marchState.isComplete

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

    val dayNumber = (marchState.hits + 1).coerceAtMost(marchState.requiredHits)
    val feedback = when (marchState.lastJudgment) {
        NoteJudgment.PERFECT, NoteJudgment.GREAT -> stringResource(R.string.jericho_march_on_beat)
        NoteJudgment.MISSED -> stringResource(R.string.jericho_march_off_beat)
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
                text = stringResource(R.string.jericho_six_day_march_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = stringResource(R.string.jericho_six_day_march_instructions),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = stringResource(R.string.jericho_march_day_label, dayNumber, marchState.requiredHits),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp),
            )

            Box(modifier = Modifier.height(28.dp).padding(top = 4.dp)) {
                Text(text = feedback, style = MaterialTheme.typography.titleMedium)
            }

            // Progress meter — monotonically fills, never resets.
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
                        .fillMaxWidth(marchState.progressFraction)
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
                        MarchLane(
                            lane = lane,
                            chart = marchState.chart,
                            judgedNoteKeys = marchState.judgedNoteKeys,
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

/** One lane: a vertical footprint-scroll track above a large, always-tappable hit zone at the bottom. */
@Composable
private fun MarchLane(
    lane: Int,
    chart: RhythmLaneChart,
    judgedNoteKeys: Set<String>,
    elapsedMs: Long,
    onTapped: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val laneDescription = stringResource(R.string.jericho_six_day_march_lane_content_description, lane + 1)

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
                    painter = painterResource(R.drawable.ic_march_footprint),
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
private fun JerichoSixDayMarchPreview() {
    BibleAdventuresTheme {
        JerichoSixDayMarchContent(
            marchState = RhythmLaneGameState(chart = JerichoContent.sixDayMarchChart, requiredHits = JerichoContent.SIX_DAY_MARCH_REQUIRED_HITS),
            onLaneTapped = { _, _ -> },
            onTimeAdvanced = {},
            onContinue = {},
            onBackToMainMenu = {},
        )
    }
}
