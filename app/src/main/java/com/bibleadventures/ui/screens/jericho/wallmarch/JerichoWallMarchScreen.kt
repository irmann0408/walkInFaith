package com.bibleadventures.ui.screens.jericho.wallmarch

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
import com.bibleadventures.game.stories.JerichoContent
import com.bibleadventures.game.stories.MarchOptionDef
import com.bibleadventures.ui.components.AdventureMenuButton
import com.bibleadventures.ui.screens.jericho.JerichoViewModel
import com.bibleadventures.ui.theme.BibleAdventuresTheme

/**
 * The March and the Shout (Joshua 6) — the chapter's centerpiece, per the
 * user's steer that this mechanic, not Rahab's helping beat, should carry
 * the "unconventional obedience... not brute force" theme. Reuses
 * [com.bibleadventures.game.puzzles.decisionpath] with entirely different
 * content/framing than Esther's banquet timing.
 */
@Composable
fun JerichoWallMarchScreen(
    viewModel: JerichoViewModel,
    onContinue: () -> Unit,
    previouslyCompleted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    JerichoWallMarchContent(
        decisionPathState = uiState.decisionPathState,
        onOptionTapped = viewModel::onMarchOptionTapped,
        onContinue = onContinue,
        previouslyCompleted = previouslyCompleted,
        modifier = modifier,
    )
}

@Composable
private fun JerichoWallMarchContent(
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
                text = stringResource(R.string.jericho_wall_march_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = stringResource(R.string.jericho_wall_march_instructions),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp),
            )

            val dayLabelRes = decisionPathState.currentStep?.id?.let { JerichoContent.marchStepDayLabels[it] }
            if (dayLabelRes != null) {
                Text(
                    text = stringResource(dayLabelRes),
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

            Image(
                painter = painterResource(if (decisionPathState.isComplete) R.drawable.ic_jericho_wall_fallen else R.drawable.ic_jericho_wall_intact),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(120.dp).padding(vertical = 16.dp),
            )

            if (!decisionPathState.isComplete) {
                val currentOptions = decisionPathState.currentStep?.optionIds
                    ?.mapNotNull { id -> JerichoContent.marchOptions.find { it.id == id } }
                    .orEmpty()
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth().widthIn(max = 480.dp),
                ) {
                    currentOptions.forEach { option ->
                        MarchOptionCard(
                            option = option,
                            onClick = onOptionTapped,
                            modifier = Modifier.weight(1f),
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
private fun MarchOptionCard(
    option: MarchOptionDef,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(option.labelRes)
    ElevatedCard(
        modifier = modifier
            .clickable(onClickLabel = label) { onClick(option.id) }
            .semantics { contentDescription = label },
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(painter = painterResource(option.iconRes), contentDescription = null, modifier = Modifier.size(56.dp))
            Text(text = label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun JerichoWallMarchPreview() {
    BibleAdventuresTheme {
        JerichoWallMarchContent(
            decisionPathState = DecisionPathGameState(steps = JerichoContent.marchSteps),
            onOptionTapped = {},
            onContinue = {},
        )
    }
}
