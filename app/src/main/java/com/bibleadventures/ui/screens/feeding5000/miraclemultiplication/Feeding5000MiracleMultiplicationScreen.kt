package com.bibleadventures.ui.screens.feeding5000.miraclemultiplication

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.testTag
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
import com.bibleadventures.game.puzzles.decisionpath.DecisionStep
import com.bibleadventures.game.stories.MathOperator
import com.bibleadventures.game.stories.MathProblem
import com.bibleadventures.ui.LocalReducedMotion
import com.bibleadventures.ui.components.PuzzleTopBar
import com.bibleadventures.ui.screens.feeding5000.Feeding5000ViewModel
import com.bibleadventures.ui.theme.BibleAdventuresTheme

/**
 * Replaces an external blueprint's "tap the basket to watch numbers
 * multiply" gimmick — the exact "just tapping the screen" shape rejected
 * four times already this session — with real multiplication problems
 * tied to the miracle's own numbers, reusing `decisionpath` exactly as
 * Daniel's Angel's Shield and Jericho's Blow the Shofar do. Each correct
 * answer triggers a small decorative burst (a scale pulse on the loaf/fish
 * icons) — the arithmetic is the mechanic, the burst is the reward for
 * solving it, not a substitute for a real puzzle.
 */
@Composable
fun Feeding5000MiracleMultiplicationScreen(
    viewModel: Feeding5000ViewModel,
    onContinue: () -> Unit,
    onBackToMainMenu: () -> Unit,
    previouslyCompleted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Feeding5000MiracleMultiplicationContent(
        miracleState = uiState.miracleState,
        problems = uiState.miracleProblems,
        onAnswerTapped = viewModel::onMiracleAnswerTapped,
        onContinue = onContinue,
        onBackToMainMenu = onBackToMainMenu,
        previouslyCompleted = previouslyCompleted,
        modifier = modifier,
    )
}

@Composable
private fun Feeding5000MiracleMultiplicationContent(
    miracleState: DecisionPathGameState,
    problems: List<MathProblem>,
    onAnswerTapped: (Int) -> Unit,
    onContinue: () -> Unit,
    onBackToMainMenu: () -> Unit,
    previouslyCompleted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val burstScale = remember { Animatable(1f) }
    val reducedMotion = LocalReducedMotion.current
    LaunchedEffect(miracleState.lastOutcome) {
        if (miracleState.lastOutcome == DecisionOutcome.CORRECT || miracleState.lastOutcome == DecisionOutcome.COMPLETE) {
            val burstSpec: AnimationSpec<Float> = if (reducedMotion) snap() else spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
            burstScale.snapTo(1f)
            burstScale.animateTo(1.3f, animationSpec = burstSpec)
            burstScale.animateTo(1f, animationSpec = burstSpec)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            if (previouslyCompleted || miracleState.isComplete) {
                PuzzleTopBar(
                    showBackButton = previouslyCompleted,
                    onBackToMainMenu = onBackToMainMenu,
                    showNextButton = miracleState.isComplete || previouslyCompleted,
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
                text = stringResource(R.string.feeding_5000_miracle_multiplication_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = stringResource(R.string.feeding_5000_miracle_multiplication_instructions),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = stringResource(R.string.feeding_5000_miracle_multiplication_progress_label, miracleState.currentStepIndex, problems.size),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
            )

            val feedback = when (miracleState.lastOutcome) {
                DecisionOutcome.CORRECT, DecisionOutcome.COMPLETE -> stringResource(R.string.feedback_great_job)
                DecisionOutcome.INCORRECT -> stringResource(R.string.feedback_try_another_one)
                DecisionOutcome.NONE -> ""
            }
            Box(modifier = Modifier.height(32.dp)) {
                Text(text = feedback, style = MaterialTheme.typography.titleLarge)
            }

            Row(
                modifier = Modifier.scale(burstScale.value).padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Image(painter = painterResource(R.drawable.ic_supply_bread), contentDescription = null, modifier = Modifier.size(48.dp))
                Image(painter = painterResource(R.drawable.ic_supply_fish), contentDescription = null, modifier = Modifier.size(48.dp))
                Image(painter = painterResource(R.drawable.ic_supply_bread), contentDescription = null, modifier = Modifier.size(48.dp))
            }

            val currentProblem = miracleState.currentStep?.let { step -> problems.first { it.id == step.id } }
            if (currentProblem != null) {
                Text(
                    text = stringResource(R.string.feeding_5000_miracle_multiplication_problem, currentProblem.operandA, currentProblem.operandB),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(top = 4.dp).testTag("miracle_problem"),
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                ) {
                    currentProblem.choiceValues.forEachIndexed { index, value ->
                        AnswerChoice(
                            value = value,
                            testTag = "miracle_choice_$index",
                            onClick = { onAnswerTapped(value) },
                        )
                    }
                }
            }

            if (previouslyCompleted && !miracleState.isComplete) {
                Text(
                    text = stringResource(R.string.puzzle_already_completed_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun AnswerChoice(value: Int, testTag: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .testTag(testTag)
            .size(width = 88.dp, height = 56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .clickable(onClick = onClick)
            .semantics { contentDescription = value.toString() },
        contentAlignment = Alignment.Center,
    ) {
        Text(text = value.toString(), style = MaterialTheme.typography.titleLarge)
    }
}

@Preview(showBackground = true)
@Composable
private fun Feeding5000MiracleMultiplicationPreview() {
    val previewProblems = listOf(
        MathProblem(id = "problem_1", operandA = 5, operandB = 4, operator = MathOperator.MULTIPLY, choiceValues = listOf(20, 24, 16)),
    )
    BibleAdventuresTheme {
        Feeding5000MiracleMultiplicationContent(
            miracleState = DecisionPathGameState(
                steps = previewProblems.map { p ->
                    DecisionStep(id = p.id, correctOptionId = p.correctValue.toString(), optionIds = p.choiceValues.map { it.toString() })
                },
            ),
            problems = previewProblems,
            onAnswerTapped = {},
            onContinue = {},
            onBackToMainMenu = {},
        )
    }
}
