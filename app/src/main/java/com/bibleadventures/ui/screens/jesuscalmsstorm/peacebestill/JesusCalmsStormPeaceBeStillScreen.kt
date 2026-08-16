package com.bibleadventures.ui.screens.jesuscalmsstorm.peacebestill

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bibleadventures.R
import com.bibleadventures.game.puzzles.rhythmlane.NoteJudgment
import com.bibleadventures.game.puzzles.rhythmlane.RhythmLaneChart
import com.bibleadventures.game.puzzles.rhythmlane.RhythmLaneGameState
import com.bibleadventures.game.stories.JesusCalmsStormContent
import com.bibleadventures.ui.components.AdventureMenuButton
import com.bibleadventures.ui.components.BackToMainMenuTopBar
import com.bibleadventures.ui.screens.jesuscalmsstorm.JesusCalmsStormViewModel
import com.bibleadventures.ui.theme.BibleAdventuresTheme
import kotlinx.coroutines.isActive

private const val LANE_COUNT = 3

/** How far ahead of a note's exact hit time its word starts glowing — long enough for a child to react, short enough that glowing isn't just "always on." */
private const val APPROACH_WINDOW_MS = 700L
private const val GRACE_WINDOW_MS = 300L

private val STORM_COLOR = Color(0xFF3B3160)
private val CALM_COLOR = Color(0xFFF5C94A)

/**
 * "He got up, rebuked the wind, and said to the waves, 'Quiet! Be still!'"
 * (Mark 4:39) — the climax. Unlike every other `rhythmlane` chart in this
 * app, the 3 lanes here are static, always-visible word buttons (PEACE/BE/
 * STILL), not a steered object — same tap-when-lit shape as Esther's
 * Corridor / Jericho's marches, reused via [RhythmLaneGame.onLaneTapped]
 * with `lane` meaning "which word," not a spatial position. Unlike those
 * chapters, word order matters (Jesus's actual words) — only the next
 * expected word (`chart.notes[hits].lane`) glows or registers a tap; the
 * other two are visibly idle even mid-window, enforced in
 * [JesusCalmsStormViewModel.onPeaceBeStillWordTapped]. The storm
 * background recedes in step with [RhythmLaneGameState.hits] (0/1/2/3),
 * driven by a manual `withFrameNanos` clock like every other real-time
 * mechanic in this app — never `rememberInfiniteTransition`.
 */
@Composable
fun JesusCalmsStormPeaceBeStillScreen(
    viewModel: JesusCalmsStormViewModel,
    onContinue: () -> Unit,
    onBackToMainMenu: () -> Unit,
    previouslyCompleted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    JesusCalmsStormPeaceBeStillContent(
        peaceBeStillState = uiState.peaceBeStillState,
        onWordTapped = viewModel::onPeaceBeStillWordTapped,
        onTimeAdvanced = viewModel::onPeaceBeStillTimeAdvanced,
        onContinue = onContinue,
        onBackToMainMenu = onBackToMainMenu,
        previouslyCompleted = previouslyCompleted,
        modifier = modifier,
    )
}

@Composable
private fun JesusCalmsStormPeaceBeStillContent(
    peaceBeStillState: RhythmLaneGameState,
    onWordTapped: (Int, Long) -> Unit,
    onTimeAdvanced: (Long) -> Unit,
    onContinue: () -> Unit,
    onBackToMainMenu: () -> Unit,
    previouslyCompleted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var elapsedMs by remember { mutableLongStateOf(0L) }
    val isComplete = peaceBeStillState.isComplete

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

    val feedback = when (peaceBeStillState.lastJudgment) {
        NoteJudgment.PERFECT, NoteJudgment.GREAT -> stringResource(R.string.feedback_great_job)
        NoteJudgment.MISSED -> stringResource(R.string.feedback_try_another_one)
        null -> ""
    }
    val backgroundColor = lerp(STORM_COLOR, CALM_COLOR, peaceBeStillState.progressFraction)
    val wordLabels = listOf(
        R.string.jesus_calms_storm_peace_be_still_word_peace,
        R.string.jesus_calms_storm_peace_be_still_word_be,
        R.string.jesus_calms_storm_peace_be_still_word_still,
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { if (previouslyCompleted) BackToMainMenuTopBar(onBackToMainMenu) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.jesus_calms_storm_peace_be_still_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = stringResource(R.string.jesus_calms_storm_peace_be_still_instructions),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = stringResource(R.string.jesus_calms_storm_peace_be_still_progress_label, peaceBeStillState.hits, peaceBeStillState.requiredHits),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
            )

            Box(modifier = Modifier.height(28.dp).padding(top = 4.dp)) {
                Text(text = feedback, style = MaterialTheme.typography.titleMedium)
            }

            if (!isComplete) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = true)
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    val expectedLane = peaceBeStillState.chart.notes.getOrNull(peaceBeStillState.hits)?.lane
                    repeat(LANE_COUNT) { lane ->
                        WordLane(
                            wordRes = wordLabels[lane],
                            isApproaching = lane == expectedLane &&
                                isApproaching(peaceBeStillState.chart, lane, peaceBeStillState.judgedNoteKeys, elapsedMs),
                            onTapped = { onWordTapped(lane, elapsedMs) },
                            modifier = Modifier.weight(1f).fillMaxSize(),
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

/** One word: a large, always-tappable button that glows once its note is about to land — a hint for rhythm, not a requirement (tapping early or late just doesn't count, no penalty). */
@Composable
private fun WordLane(wordRes: Int, isApproaching: Boolean, onTapped: () -> Unit, modifier: Modifier = Modifier) {
    val word = stringResource(wordRes)
    val backgroundColor = if (isApproaching) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isApproaching) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = modifier
            .aspectRatio(0.7f)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable(onClickLabel = word, onClick = onTapped)
            .semantics { contentDescription = word },
        contentAlignment = Alignment.Center,
    ) {
        Text(text = word, style = MaterialTheme.typography.headlineSmall, color = textColor, fontWeight = FontWeight.Bold)
    }
}

/** Whether [lane]'s next not-yet-judged note is close enough to its hit time to glow — same visible-window idea as `visibleNotes` elsewhere, but a single Boolean since there's no traveling icon here, just the button itself. */
private fun isApproaching(chart: RhythmLaneChart, lane: Int, judgedNoteKeys: Set<String>, elapsedMs: Long): Boolean {
    val currentLoopIndex = elapsedMs / chart.loopDurationMs
    return chart.notes
        .filter { it.lane == lane }
        .any { note ->
            (currentLoopIndex..currentLoopIndex + 1).any loopCheck@{ loopIndex ->
                if ("$loopIndex:${note.id}" in judgedNoteKeys) return@loopCheck false
                val msUntilHit = (loopIndex * chart.loopDurationMs + note.hitTimeMs) - elapsedMs
                msUntilHit in -GRACE_WINDOW_MS..APPROACH_WINDOW_MS
            }
        }
}

@Preview(showBackground = true)
@Composable
private fun JesusCalmsStormPeaceBeStillPreview() {
    BibleAdventuresTheme {
        JesusCalmsStormPeaceBeStillContent(
            peaceBeStillState = RhythmLaneGameState(
                chart = JesusCalmsStormContent.peaceBeStillChart,
                requiredHits = JesusCalmsStormContent.PEACE_BE_STILL_REQUIRED_HITS,
            ),
            onWordTapped = { _, _ -> },
            onTimeAdvanced = {},
            onContinue = {},
            onBackToMainMenu = {},
        )
    }
}
