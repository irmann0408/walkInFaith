package com.bibleadventures.ui.screens.esther.corridor

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.getValue
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
import com.bibleadventures.game.puzzles.meter.MeterGameState
import com.bibleadventures.game.puzzles.meter.TapPrecision
import com.bibleadventures.ui.components.AdventureMenuButton
import com.bibleadventures.ui.screens.esther.EstherViewModel
import com.bibleadventures.ui.theme.BibleAdventuresTheme

/**
 * A self-paced "tap along" mechanic, not a real rhythm-game fail state:
 * every tap adds positive progress to the meter regardless of timing —
 * timing only changes the feedback text, never whether progress is made.
 * Mirrors Sling Practice's split exactly: this screen owns the live,
 * looping animation and classifies each tap's timing; [MeterGame] only
 * turns that classification into progress.
 */
@Composable
fun EstherCorridorScreen(
    viewModel: EstherViewModel,
    onContinue: () -> Unit,
    previouslyCompleted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    EstherCorridorContent(
        meterState = uiState.meterState,
        onTapped = viewModel::onCorridorTapped,
        onContinue = onContinue,
        previouslyCompleted = previouslyCompleted,
        modifier = modifier,
    )
}

@Composable
private fun EstherCorridorContent(
    meterState: MeterGameState,
    onTapped: (TapPrecision) -> Unit,
    onContinue: () -> Unit,
    previouslyCompleted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val pulseFraction by rememberInfiniteTransition(label = "corridorPulse").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "corridorPulseFraction",
    )

    val feedback = when (meterState.lastPrecision) {
        TapPrecision.PERFECT, TapPrecision.GOOD -> stringResource(R.string.esther_brave_approach_corridor_great_rhythm)
        TapPrecision.EARLY_OR_LATE -> stringResource(R.string.esther_brave_approach_corridor_keep_going)
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
                text = stringResource(R.string.esther_brave_approach_corridor_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = stringResource(R.string.esther_brave_approach_corridor_instructions),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp),
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
                        .fillMaxWidth(meterState.progressFraction)
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
                val tapDescription = stringResource(R.string.esther_brave_approach_corridor_tap_content_description)
                if (!meterState.isComplete) {
                    Image(
                        painter = painterResource(R.drawable.ic_courage_marker),
                        contentDescription = tapDescription,
                        modifier = Modifier
                            .size(96.dp)
                            .scale(0.7f + 0.3f * pulseFraction)
                            .clickable(onClickLabel = tapDescription) {
                                val precision = when {
                                    pulseFraction >= 0.85f -> TapPrecision.PERFECT
                                    pulseFraction >= 0.6f -> TapPrecision.GOOD
                                    else -> TapPrecision.EARLY_OR_LATE
                                }
                                onTapped(precision)
                            }
                            .semantics { contentDescription = tapDescription },
                    )
                }
            }

            if (previouslyCompleted && !meterState.isComplete) {
                Text(
                    text = stringResource(R.string.puzzle_already_completed_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            if (meterState.isComplete || previouslyCompleted) {
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
private fun EstherCorridorPreview() {
    BibleAdventuresTheme {
        EstherCorridorContent(
            meterState = MeterGameState(requiredProgress = 10),
            onTapped = {},
            onContinue = {},
        )
    }
}
