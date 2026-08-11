package com.bibleadventures.ui.screens.estherbanquetsrescue.revealhaman

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bibleadventures.R
import com.bibleadventures.game.puzzles.decisionpath.DecisionOutcome
import com.bibleadventures.game.puzzles.decisionpath.DecisionPathGameState
import com.bibleadventures.game.stories.EstherBanquetsRescueContent
import com.bibleadventures.ui.components.AdventureMenuButton
import com.bibleadventures.ui.screens.estherbanquetsrescue.EstherBanquetsRescueViewModel
import com.bibleadventures.ui.theme.BibleAdventuresTheme

@Composable
fun EstherBanquetsRescueRevealHamanScreen(
    viewModel: EstherBanquetsRescueViewModel,
    onContinue: () -> Unit,
    previouslyCompleted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    EstherBanquetsRescueRevealHamanContent(
        decisionPathState = uiState.decisionPathState,
        onOptionTapped = viewModel::onRevealOptionTapped,
        onContinue = onContinue,
        previouslyCompleted = previouslyCompleted,
        modifier = modifier,
    )
}

@Composable
private fun EstherBanquetsRescueRevealHamanContent(
    decisionPathState: DecisionPathGameState,
    onOptionTapped: (String) -> Unit,
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
                text = stringResource(R.string.esther_banquets_rescue_reveal_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = stringResource(R.string.esther_banquets_rescue_reveal_instructions),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp),
            )

            val promptRes = decisionPathState.currentStep?.id?.let { EstherBanquetsRescueContent.revealStepPromptLabels[it] }
            if (promptRes != null) {
                Text(
                    text = stringResource(promptRes),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            val feedback = when (decisionPathState.lastOutcome) {
                DecisionOutcome.CORRECT, DecisionOutcome.COMPLETE -> stringResource(R.string.feedback_great_job)
                DecisionOutcome.INCORRECT -> stringResource(R.string.feedback_try_another_one)
                DecisionOutcome.NONE -> ""
            }
            Box(modifier = Modifier.height(32.dp)) {
                Text(text = feedback, style = MaterialTheme.typography.titleLarge)
            }

            if (!decisionPathState.isComplete) {
                val currentOptions = decisionPathState.currentStep?.optionIds
                    ?.mapNotNull { id -> EstherBanquetsRescueContent.revealOptions.find { it.id == id } }
                    .orEmpty()
                Column(
                    modifier = Modifier.fillMaxWidth().widthIn(max = 480.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    currentOptions.forEach { option ->
                        RevealOptionCard(
                            label = stringResource(option.labelRes),
                            onClick = { onOptionTapped(option.id) },
                        )
                    }
                }
                if (previouslyCompleted) {
                    Text(
                        text = stringResource(R.string.puzzle_already_completed_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    AdventureMenuButton(
                        text = stringResource(R.string.action_continue),
                        onClick = onContinue,
                        modifier = Modifier.widthIn(max = 320.dp).padding(top = 8.dp),
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
private fun RevealOptionCard(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClickLabel = label, onClick = onClick)
            .semantics { contentDescription = label },
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EstherBanquetsRescueRevealHamanPreview() {
    BibleAdventuresTheme {
        EstherBanquetsRescueRevealHamanContent(
            decisionPathState = DecisionPathGameState(steps = EstherBanquetsRescueContent.revealSteps),
            onOptionTapped = {},
            onContinue = {},
        )
    }
}
