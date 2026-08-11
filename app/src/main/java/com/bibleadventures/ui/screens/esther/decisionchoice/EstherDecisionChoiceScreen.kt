package com.bibleadventures.ui.screens.esther.decisionchoice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bibleadventures.R
import com.bibleadventures.game.stories.EstherContent
import com.bibleadventures.ui.components.AdventureMenuButton
import com.bibleadventures.ui.screens.esther.EstherViewModel
import com.bibleadventures.ui.theme.BibleAdventuresTheme

@Composable
fun EstherDecisionChoiceScreen(
    viewModel: EstherViewModel,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    EstherDecisionChoiceContent(
        selectedChoiceId = uiState.selectedDecisionChoiceId,
        onChoiceSelected = viewModel::onDecisionChoiceSelected,
        onContinue = onContinue,
        modifier = modifier,
    )
}

/** A one-shot, no-wrong-answer pick from flavor-text responses, mirroring every other chapter's Choice scene. */
@Composable
private fun EstherDecisionChoiceContent(
    selectedChoiceId: String?,
    onChoiceSelected: (String) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selected = EstherContent.decisionChoiceOptions.find { it.id == selectedChoiceId }

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.esther_brave_approach_choice_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = stringResource(R.string.esther_brave_approach_choice_instructions),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
            )

            if (selected == null) {
                Column(
                    modifier = Modifier.widthIn(max = 480.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    EstherContent.decisionChoiceOptions.forEach { option ->
                        AdventureMenuButton(
                            text = stringResource(option.textRes),
                            onClick = { onChoiceSelected(option.id) },
                        )
                    }
                }
            } else {
                Text(
                    text = stringResource(selected.reactionTextRes),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.widthIn(max = 480.dp),
                )
                AdventureMenuButton(
                    text = stringResource(R.string.action_continue),
                    onClick = onContinue,
                    modifier = Modifier
                        .widthIn(max = 320.dp)
                        .padding(top = 24.dp),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EstherDecisionChoicePreview() {
    BibleAdventuresTheme {
        EstherDecisionChoiceContent(selectedChoiceId = null, onChoiceSelected = {}, onContinue = {})
    }
}
