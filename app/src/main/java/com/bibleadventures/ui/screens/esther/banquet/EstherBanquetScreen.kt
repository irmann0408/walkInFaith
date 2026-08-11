package com.bibleadventures.ui.screens.esther.banquet

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bibleadventures.R
import com.bibleadventures.game.puzzles.decisionpath.DecisionOutcome
import com.bibleadventures.game.puzzles.decisionpath.DecisionPathGameState
import com.bibleadventures.game.stories.EstherContent
import com.bibleadventures.ui.components.AdventureMenuButton
import com.bibleadventures.ui.screens.esther.EstherViewModel
import com.bibleadventures.ui.theme.BibleAdventuresTheme

/**
 * The Two Banquets — Esther deliberately waits through the first banquet and
 * the second invitation before finally speaking at the second one (Esther
 * 5:3-8, 7:1-6). Reuses [com.bibleadventures.game.puzzles.decisionpath]
 * with entirely different content/framing than Jericho's march — same
 * engine, unrelated-feeling scene, mirroring how the `matching` engine
 * already powers both Sheep Counting and Animal Matching in this codebase.
 */
@Composable
fun EstherBanquetScreen(
    viewModel: EstherViewModel,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    EstherBanquetContent(
        decisionPathState = uiState.decisionPathState,
        onOptionTapped = viewModel::onBanquetOptionTapped,
        onContinue = onContinue,
        modifier = modifier,
    )
}

@Composable
private fun EstherBanquetContent(
    decisionPathState: DecisionPathGameState,
    onOptionTapped: (String) -> Unit,
    onContinue: () -> Unit,
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
                text = stringResource(R.string.esther_banquet_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = stringResource(R.string.esther_banquet_instructions),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp),
            )

            val feedback = when (decisionPathState.lastOutcome) {
                DecisionOutcome.CORRECT, DecisionOutcome.COMPLETE -> stringResource(R.string.feedback_great_job)
                DecisionOutcome.INCORRECT -> stringResource(R.string.feedback_try_another_one)
                DecisionOutcome.NONE -> ""
            }
            Box(modifier = Modifier.height(32.dp)) {
                Text(text = feedback, style = MaterialTheme.typography.titleLarge)
            }

            Image(
                painter = painterResource(if (decisionPathState.isComplete) R.drawable.ic_scroll_open else R.drawable.ic_scroll_sealed),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(120.dp).padding(vertical = 16.dp),
            )

            if (!decisionPathState.isComplete) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth().widthIn(max = 480.dp),
                ) {
                    BanquetOption(
                        optionId = "wait",
                        iconRes = R.drawable.ic_wait,
                        labelRes = R.string.esther_banquet_option_wait,
                        onClick = onOptionTapped,
                        modifier = Modifier.weight(1f),
                    )
                    BanquetOption(
                        optionId = "speak_now",
                        iconRes = R.drawable.ic_speak_now,
                        labelRes = R.string.esther_banquet_option_speak_now,
                        onClick = onOptionTapped,
                        modifier = Modifier.weight(1f),
                    )
                }
            } else {
                AdventureMenuButton(
                    text = stringResource(R.string.action_continue),
                    onClick = onContinue,
                    modifier = Modifier.widthIn(max = 320.dp).padding(top = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun BanquetOption(
    optionId: String,
    iconRes: Int,
    labelRes: Int,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(labelRes)
    ElevatedCard(
        modifier = modifier
            .clickable(onClickLabel = label) { onClick(optionId) }
            .semantics { contentDescription = label },
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(painter = painterResource(iconRes), contentDescription = null, modifier = Modifier.size(56.dp))
            Text(text = label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EstherBanquetPreview() {
    BibleAdventuresTheme {
        EstherBanquetContent(
            decisionPathState = DecisionPathGameState(steps = EstherContent.banquetSteps),
            onOptionTapped = {},
            onContinue = {},
        )
    }
}
