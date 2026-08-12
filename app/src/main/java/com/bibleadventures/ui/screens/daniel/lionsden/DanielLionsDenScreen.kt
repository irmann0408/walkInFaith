package com.bibleadventures.ui.screens.daniel.lionsden

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
import com.bibleadventures.game.stories.DanielContent
import com.bibleadventures.game.stories.MathOperator
import com.bibleadventures.game.stories.MathProblem
import com.bibleadventures.ui.components.AdventureMenuButton
import com.bibleadventures.ui.screens.daniel.DanielViewModel
import com.bibleadventures.ui.theme.BibleAdventuresTheme

/**
 * The Angel's Shield: solve a math problem (addition or subtraction, random
 * every playthrough — [com.bibleadventures.ui.screens.daniel.DanielViewModel.newLionsDenProblems])
 * and pick the correct answer from 3 choices to light the next light. Wrong
 * guesses just re-prompt the same problem, never a setback — same
 * never-FAILED shape as [com.bibleadventures.game.puzzles.decisionpath.DecisionPathGame]
 * everywhere else. Once all 5 lights are lit (a gold polyline connecting
 * them, mirroring Sling Practice's dashed trajectory line, solid here since
 * it marks real progress rather than a live aim), the lions calm — a shape
 * change (ic_lion_pacing -> ic_lion_calm), never color-only.
 */
@Composable
fun DanielLionsDenScreen(
    viewModel: DanielViewModel,
    onContinue: () -> Unit,
    previouslyCompleted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DanielLionsDenContent(
        lionsDenState = uiState.lionsDenState,
        problems = uiState.lionsDenProblems,
        onAnswerTapped = viewModel::onLionsDenAnswerTapped,
        onContinue = onContinue,
        previouslyCompleted = previouslyCompleted,
        modifier = modifier,
    )
}

@Composable
private fun DanielLionsDenContent(
    lionsDenState: DecisionPathGameState,
    problems: List<MathProblem>,
    onAnswerTapped: (Int) -> Unit,
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
                text = stringResource(R.string.daniel_lions_den_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = stringResource(R.string.daniel_lions_den_instructions),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = stringResource(R.string.daniel_lions_den_progress_label, lionsDenState.currentStepIndex, DanielContent.LIONS_DEN_PROBLEM_COUNT),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
            )

            val feedback = when (lionsDenState.lastOutcome) {
                DecisionOutcome.CORRECT, DecisionOutcome.COMPLETE -> stringResource(R.string.feedback_great_job)
                DecisionOutcome.INCORRECT -> stringResource(R.string.feedback_try_another_one)
                DecisionOutcome.NONE -> ""
            }
            Box(modifier = Modifier.height(32.dp)) {
                Text(text = feedback, style = MaterialTheme.typography.titleLarge)
            }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
            ) {
                Image(
                    painter = painterResource(R.drawable.bg_daniel_den),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )

                val lionIcon = if (lionsDenState.isComplete) R.drawable.ic_lion_calm else R.drawable.ic_lion_pacing
                val lionsDescription = stringResource(R.string.daniel_lions_den_lions_content_description)
                Row(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    Image(painter = painterResource(lionIcon), contentDescription = lionsDescription, modifier = Modifier.size(72.dp))
                    Image(painter = painterResource(lionIcon), contentDescription = null, modifier = Modifier.size(72.dp))
                }

                val litPositions = DanielContent.lionsDenLightPositions.take(lionsDenState.currentStepIndex)
                if (litPositions.size > 1) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        for (i in 0 until litPositions.size - 1) {
                            drawLine(
                                color = Color(0xFFFFD54A),
                                start = Offset(size.width * litPositions[i].x, size.height * litPositions[i].y),
                                end = Offset(size.width * litPositions[i + 1].x, size.height * litPositions[i + 1].y),
                                strokeWidth = 6f,
                            )
                        }
                    }
                }

                DanielContent.lionsDenLightPositions.forEachIndexed { index, position ->
                    LightPoint(
                        isLit = index < lionsDenState.currentStepIndex,
                        modifier = Modifier.offset(x = maxWidth * position.x - 24.dp, y = maxHeight * position.y - 24.dp),
                    )
                }
            }

            val currentProblem = lionsDenState.currentStep?.let { step -> problems.first { it.id == step.id } }
            if (currentProblem != null) {
                val problemText = if (currentProblem.operator == MathOperator.ADD) {
                    stringResource(R.string.daniel_lions_den_addition_problem, currentProblem.operandA, currentProblem.operandB)
                } else {
                    stringResource(R.string.daniel_lions_den_subtraction_problem, currentProblem.operandA, currentProblem.operandB)
                }
                Text(
                    text = problemText,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(top = 12.dp).testTag("lions_den_problem"),
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                ) {
                    currentProblem.choiceValues.forEachIndexed { index, value ->
                        AnswerChoice(
                            value = value,
                            testTag = "lions_den_choice_$index",
                            onClick = { onAnswerTapped(value) },
                        )
                    }
                }
            }

            if (previouslyCompleted && !lionsDenState.isComplete) {
                Text(
                    text = stringResource(R.string.puzzle_already_completed_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            if (lionsDenState.isComplete || previouslyCompleted) {
                AdventureMenuButton(
                    text = stringResource(R.string.action_continue),
                    onClick = onContinue,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun LightPoint(isLit: Boolean, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(if (isLit) R.drawable.ic_light_point_lit else R.drawable.ic_light_point),
        contentDescription = null,
        modifier = modifier.size(48.dp),
    )
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
private fun DanielLionsDenPreview() {
    val previewProblems = listOf(
        MathProblem(id = "problem_1", operandA = 542, operandB = 216, operator = MathOperator.ADD, choiceValues = listOf(758, 748, 768)),
    )
    BibleAdventuresTheme {
        DanielLionsDenContent(
            lionsDenState = DecisionPathGameState(
                steps = previewProblems.map { p ->
                    DecisionStep(
                        id = p.id,
                        correctOptionId = p.correctValue.toString(),
                        optionIds = p.choiceValues.map { it.toString() },
                    )
                },
            ),
            problems = previewProblems,
            onAnswerTapped = {},
            onContinue = {},
        )
    }
}
