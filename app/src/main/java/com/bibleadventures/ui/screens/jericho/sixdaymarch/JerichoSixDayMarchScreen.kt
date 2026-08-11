package com.bibleadventures.ui.screens.jericho.sixdaymarch

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bibleadventures.R
import com.bibleadventures.game.puzzles.rhythmlane.NoteJudgment
import com.bibleadventures.game.puzzles.rhythmlane.RhythmLaneGameState
import com.bibleadventures.game.stories.JerichoContent
import com.bibleadventures.ui.components.AdventureMenuButton
import com.bibleadventures.ui.screens.jericho.JerichoViewModel
import com.bibleadventures.ui.theme.BibleAdventuresTheme
import kotlinx.coroutines.isActive

/**
 * One lap around the wall per tap, in silence, once a day for six days
 * (Joshua 6:3) — reuses `rhythmlane` at its simplest parameterization (a
 * single lane, always `0`), so "Day X of 6" reads directly off
 * [RhythmLaneGameState.hits]. A missed beat is never a setback — no
 * danger meter, just a "keep in step" nudge — see [JerichoContent.sixDayMarchChart]'s
 * own doc for why. Drives its clock with a manual [withFrameNanos]
 * accumulator, not `rememberInfiniteTransition`, for the same
 * test-determinism reason as Esther's corridor.
 */
@Composable
fun JerichoSixDayMarchScreen(
    viewModel: JerichoViewModel,
    onContinue: () -> Unit,
    previouslyCompleted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    JerichoSixDayMarchContent(
        marchState = uiState.sixDayMarchState,
        onTapped = viewModel::onSixDayMarchTapped,
        onTimeAdvanced = viewModel::onSixDayMarchTimeAdvanced,
        onContinue = onContinue,
        previouslyCompleted = previouslyCompleted,
        modifier = modifier,
    )
}

@Composable
private fun JerichoSixDayMarchContent(
    marchState: RhythmLaneGameState,
    onTapped: (Long) -> Unit,
    onTimeAdvanced: (Long) -> Unit,
    onContinue: () -> Unit,
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

    val hitTimeMs = marchState.chart.notes.first().hitTimeMs
    val loopElapsedMs = elapsedMs % marchState.chart.loopDurationMs
    val distanceFromBeat = kotlin.math.abs(loopElapsedMs - hitTimeMs)
    val pulseFraction = (1f - distanceFromBeat.toFloat() / (marchState.chart.loopDurationMs / 2f)).coerceIn(0f, 1f)

    val dayNumber = (marchState.hits + 1).coerceAtMost(marchState.requiredHits)
    val feedback = when (marchState.lastJudgment) {
        NoteJudgment.PERFECT, NoteJudgment.GREAT -> stringResource(R.string.jericho_march_on_beat)
        NoteJudgment.MISSED -> stringResource(R.string.jericho_march_off_beat)
        null -> ""
    }

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
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

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f, fill = true),
                contentAlignment = Alignment.Center,
            ) {
                val tapDescription = stringResource(R.string.jericho_march_tap_content_description)
                if (!isComplete) {
                    Image(
                        painter = painterResource(R.drawable.ic_march_footprint),
                        contentDescription = tapDescription,
                        modifier = Modifier
                            .size(96.dp)
                            .scale(0.7f + 0.3f * pulseFraction)
                            .clickable(onClickLabel = tapDescription) { onTapped(elapsedMs) }
                            .semantics { contentDescription = tapDescription },
                    )
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

@Preview(showBackground = true)
@Composable
private fun JerichoSixDayMarchPreview() {
    BibleAdventuresTheme {
        JerichoSixDayMarchContent(
            marchState = RhythmLaneGameState(chart = JerichoContent.sixDayMarchChart, requiredHits = JerichoContent.SIX_DAY_MARCH_REQUIRED_HITS),
            onTapped = {},
            onTimeAdvanced = {},
            onContinue = {},
        )
    }
}
