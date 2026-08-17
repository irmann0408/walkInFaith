package com.bibleadventures.ui.screens.jericho.blowshofar

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
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
import com.bibleadventures.game.stories.JerichoContent
import com.bibleadventures.game.stories.MathOperator
import com.bibleadventures.game.stories.MathProblem
import com.bibleadventures.ui.LocalReducedMotion
import com.bibleadventures.ui.components.AspectRatioFitBox
import com.bibleadventures.ui.components.PuzzleTopBar
import com.bibleadventures.ui.screens.jericho.JerichoViewModel
import com.bibleadventures.ui.theme.BibleAdventuresTheme

/**
 * Blow the Shofar: solve a math problem (multiplication or division, random
 * every playthrough — [com.bibleadventures.ui.screens.jericho.JerichoViewModel.newShofarProblems])
 * and pick the correct answer from 3 choices to light the next colored
 * note. Wrong guesses just re-prompt the same problem, never a setback —
 * reuses `game/puzzles/decisionpath`, the same engine and never-FAILED
 * shape as Daniel's Angel's Shield.
 */
@Composable
fun JerichoBlowShofarScreen(
    viewModel: JerichoViewModel,
    onContinue: () -> Unit,
    onBackToMainMenu: () -> Unit,
    previouslyCompleted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    JerichoBlowShofarContent(
        shofarState = uiState.shofarState,
        problems = uiState.shofarProblems,
        onAnswerTapped = viewModel::onShofarAnswerTapped,
        onContinue = onContinue,
        onBackToMainMenu = onBackToMainMenu,
        previouslyCompleted = previouslyCompleted,
        modifier = modifier,
    )
}

private fun colorFor(noteId: String): Color = when (noteId) {
    "red" -> Color(0xFFE53935)
    "orange" -> Color(0xFFFB8C00)
    "yellow" -> Color(0xFFFDD835)
    "green" -> Color(0xFF43A047)
    "blue" -> Color(0xFF1E88E5)
    else -> Color.Gray
}

@Composable
private fun JerichoBlowShofarContent(
    shofarState: DecisionPathGameState,
    problems: List<MathProblem>,
    onAnswerTapped: (Int) -> Unit,
    onContinue: () -> Unit,
    onBackToMainMenu: () -> Unit,
    previouslyCompleted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    // Celebration pulse on a correct answer — brings this screen in line with
    // Feeding 5,000's Miracle Multiplication (same decisionpath math-quiz
    // mechanic), which already had this and made Feeding 5,000 the only one
    // of the 3 math-quiz screens with any reward polish beyond the static
    // light/note fill-in.
    val burstScale = remember { Animatable(1f) }
    val reducedMotion = LocalReducedMotion.current
    LaunchedEffect(shofarState.lastOutcome) {
        if (shofarState.lastOutcome == DecisionOutcome.CORRECT || shofarState.lastOutcome == DecisionOutcome.COMPLETE) {
            val burstSpec: AnimationSpec<Float> = if (reducedMotion) snap() else spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
            burstScale.snapTo(1f)
            burstScale.animateTo(1.1f, animationSpec = burstSpec)
            burstScale.animateTo(1f, animationSpec = burstSpec)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            if (previouslyCompleted || shofarState.isComplete) {
                PuzzleTopBar(
                    showBackButton = previouslyCompleted,
                    onBackToMainMenu = onBackToMainMenu,
                    showNextButton = shofarState.isComplete || previouslyCompleted,
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
                text = stringResource(R.string.jericho_blow_shofar_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = stringResource(R.string.jericho_blow_shofar_instructions),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = stringResource(R.string.jericho_blow_shofar_progress_label, shofarState.currentStepIndex, JerichoContent.shofarNoteIds.size),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
            )

            val feedback = when (shofarState.lastOutcome) {
                DecisionOutcome.CORRECT, DecisionOutcome.COMPLETE -> stringResource(R.string.feedback_great_job)
                DecisionOutcome.INCORRECT -> stringResource(R.string.feedback_try_another_one)
                DecisionOutcome.NONE -> ""
            }
            Box(modifier = Modifier.height(32.dp)) {
                Text(text = feedback, style = MaterialTheme.typography.titleLarge)
            }

            // weight(1f, fill = true) hands this element exactly the space left
            // over after every other (naturally-sized) sibling in this Column —
            // including the answer-choice row below — and AspectRatioFitBox
            // letterbox-fits within that bounded box, so nothing here ever needs
            // to scroll. The nested BoxWithConstraints re-reads the fitted box's
            // own size so note positions below can still be placed as fractions
            // of it.
            AspectRatioFitBox(ratio = 1f, modifier = Modifier.weight(1f, fill = true).fillMaxSize()) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize().scale(burstScale.value)) {
                    JerichoContent.shofarNoteIds.forEachIndexed { index, noteId ->
                        val position = JerichoContent.shofarNotePositions[index]
                        NoteLight(
                            noteId = noteId,
                            isLit = index < shofarState.currentStepIndex,
                            modifier = Modifier.offset(x = maxWidth * position.x - 24.dp, y = maxHeight * position.y - 24.dp),
                        )
                    }
                }
            }

            val currentProblem = shofarState.currentStep?.let { step -> problems.first { it.id == step.id } }
            if (currentProblem != null) {
                val problemText = when (currentProblem.operator) {
                    MathOperator.MULTIPLY -> stringResource(R.string.jericho_blow_shofar_multiplication_problem, currentProblem.operandA, currentProblem.operandB)
                    else -> stringResource(R.string.jericho_blow_shofar_division_problem, currentProblem.operandA, currentProblem.operandB)
                }
                Text(
                    text = problemText,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(top = 12.dp).testTag("shofar_problem"),
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                ) {
                    currentProblem.choiceValues.forEachIndexed { index, value ->
                        AnswerChoice(
                            value = value,
                            testTag = "shofar_choice_$index",
                            onClick = { onAnswerTapped(value) },
                        )
                    }
                }
            }

            if (previouslyCompleted && !shofarState.isComplete) {
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
private fun NoteLight(noteId: String, isLit: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(if (isLit) colorFor(noteId) else MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (isLit) {
            Icon(imageVector = Icons.Filled.Check, contentDescription = null, tint = Color.White)
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
private fun JerichoBlowShofarPreview() {
    val previewProblems = listOf(
        MathProblem(id = "problem_1", operandA = 12, operandB = 7, operator = MathOperator.MULTIPLY, choiceValues = listOf(84, 74, 94)),
    )
    BibleAdventuresTheme {
        JerichoBlowShofarContent(
            shofarState = DecisionPathGameState(
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
            onBackToMainMenu = {},
        )
    }
}
