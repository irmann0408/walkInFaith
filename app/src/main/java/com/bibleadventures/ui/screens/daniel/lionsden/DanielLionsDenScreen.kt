package com.bibleadventures.ui.screens.daniel.lionsden

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.bibleadventures.domain.model.CharacterCustomization
import com.bibleadventures.game.puzzles.decisionpath.DecisionOutcome
import com.bibleadventures.game.puzzles.decisionpath.DecisionPathGameState
import com.bibleadventures.game.puzzles.decisionpath.DecisionStep
import com.bibleadventures.game.stories.DanielContent
import com.bibleadventures.game.stories.MathOperator
import com.bibleadventures.game.stories.MathProblem
import com.bibleadventures.ui.LocalReducedMotion
import com.bibleadventures.ui.components.AspectRatioFitBox
import com.bibleadventures.ui.components.CharacterPreview
import com.bibleadventures.ui.components.Posture
import com.bibleadventures.ui.components.PuzzleTopBar
import com.bibleadventures.ui.screens.daniel.DanielViewModel
import com.bibleadventures.ui.screens.daniel.LION_PROXIMITY_MAX
import com.bibleadventures.ui.theme.BibleAdventuresTheme
import kotlinx.coroutines.delay

/** Fraction of the square play area's own min dimension. */
private const val CHARACTER_SIZE_FRACTION = 0.30f
private const val ANGEL_SIZE_FRACTION = 0.56f

/** The angel's own art sits lower within its transparent canvas than the character's within theirs — without this, centering both on the same point lines up their feet instead of the angel visibly rising up behind the character's back. Shifts the angel up by this fraction of the play area's min dimension. */
private const val ANGEL_Y_OFFSET_FRACTION = 0.14f
private const val RING_MIN_RADIUS_FRACTION = 0.20f
private const val RING_MAX_RADIUS_FRACTION = 0.46f
private const val LION_OUTER_X_FRACTION = 0.06f
private const val LION_INNER_X_FRACTION = 0.34f
private const val LION_COMPLETE_X_FRACTION = 0.14f
private const val LION_Y_FRACTION = 0.62f
private const val LION_BASE_SIZE_FRACTION = 0.20f
private const val LION_MAX_SIZE_FRACTION = 0.32f
private const val LION_APPROACH_FRAME_HOLD_MS = 120L
private const val LION_APPROACH_STEP_DURATION_MS = 350

/**
 * The Angel's Shield: solve a math problem (addition or subtraction, random
 * every playthrough — [com.bibleadventures.ui.screens.daniel.DanielViewModel.newLionsDenProblems])
 * and pick the correct answer from 3 choices to draw the next ring of the
 * shield around the player's own character, standing at the center. The 5th
 * ring also reveals the angel behind them. A wrong answer steps the two
 * lions (starting at the outer edges) one increment closer —
 * [com.bibleadventures.ui.screens.daniel.LION_PROXIMITY_MAX] wrong answers
 * (cumulative across the whole attempt, not per-problem) closes the
 * distance entirely, ending the attempt. This is an explicit, user-requested
 * exception to this app's "no failure states" rule (the third this app has
 * made — see the architectural decisions log), kept gentle the same way the
 * other two are: no chapter progress lost, unlimited retries via a manual
 * "Try Again" (never an automatic reset — the exact lesson already learned
 * from Connect Four's own auto-reset-felt-too-fast feedback).
 */
@Composable
fun DanielLionsDenScreen(
    viewModel: DanielViewModel,
    onContinue: () -> Unit,
    onBackToMainMenu: () -> Unit,
    previouslyCompleted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val characterCustomization by viewModel.characterCustomization.collectAsStateWithLifecycle()

    DanielLionsDenContent(
        lionsDenState = uiState.lionsDenState,
        problems = uiState.lionsDenProblems,
        lionProximity = uiState.lionsDenLionProximity,
        characterCustomization = characterCustomization,
        onAnswerTapped = viewModel::onLionsDenAnswerTapped,
        onRetry = viewModel::onLionsDenRetry,
        onContinue = onContinue,
        onBackToMainMenu = onBackToMainMenu,
        previouslyCompleted = previouslyCompleted,
        modifier = modifier,
    )
}

@Composable
private fun DanielLionsDenContent(
    lionsDenState: DecisionPathGameState,
    problems: List<MathProblem>,
    lionProximity: Int,
    characterCustomization: CharacterCustomization,
    onAnswerTapped: (Int) -> Unit,
    onRetry: () -> Unit,
    onContinue: () -> Unit,
    onBackToMainMenu: () -> Unit,
    previouslyCompleted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val reducedMotion = LocalReducedMotion.current
    val tooClose = lionProximity >= LION_PROXIMITY_MAX

    // Celebration pulse on a correct answer — unchanged from before this
    // rework, brings this screen in line with Feeding 5,000's Miracle
    // Multiplication (same decisionpath math-quiz mechanic). The angel's
    // reveal on the 5th ring rides this exact same pulse for free.
    val burstScale = remember { Animatable(1f) }
    LaunchedEffect(lionsDenState.lastOutcome) {
        if (lionsDenState.lastOutcome == DecisionOutcome.CORRECT || lionsDenState.lastOutcome == DecisionOutcome.COMPLETE) {
            val burstSpec: AnimationSpec<Float> = if (reducedMotion) snap() else spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
            burstScale.snapTo(1f)
            burstScale.animateTo(1.1f, animationSpec = burstSpec)
            burstScale.animateTo(1f, animationSpec = burstSpec)
        }
    }

    // Drives the lions' approach: a brief stand/roar frame alternation (skipped
    // under reduced motion, and on the very first composition where proximity
    // is already 0) while their position animates one increment closer.
    var lionRoaring by remember { mutableStateOf(false) }
    val lionApproachProgress = remember { Animatable(0f) }
    LaunchedEffect(lionProximity) {
        val target = lionProximity.coerceIn(0, LION_PROXIMITY_MAX) / LION_PROXIMITY_MAX.toFloat()
        if (lionProximity == 0 || reducedMotion) {
            lionApproachProgress.snapTo(target)
            return@LaunchedEffect
        }
        repeat(2) {
            lionRoaring = true
            delay(LION_APPROACH_FRAME_HOLD_MS)
            lionRoaring = false
            delay(LION_APPROACH_FRAME_HOLD_MS)
        }
        lionApproachProgress.animateTo(target, animationSpec = tween(LION_APPROACH_STEP_DURATION_MS))
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            if (previouslyCompleted || lionsDenState.isComplete) {
                PuzzleTopBar(
                    showBackButton = previouslyCompleted,
                    onBackToMainMenu = onBackToMainMenu,
                    showNextButton = lionsDenState.isComplete || previouslyCompleted,
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

            val feedback = when {
                tooClose -> ""
                lionsDenState.lastOutcome == DecisionOutcome.CORRECT || lionsDenState.lastOutcome == DecisionOutcome.COMPLETE -> stringResource(R.string.feedback_great_job)
                lionsDenState.lastOutcome == DecisionOutcome.INCORRECT -> stringResource(R.string.feedback_try_another_one)
                else -> ""
            }
            Box(modifier = Modifier.height(32.dp)) {
                Text(text = feedback, style = MaterialTheme.typography.titleLarge)
            }

            // weight(1f, fill = true) hands this element exactly the space left
            // over after every other (naturally-sized) sibling in this Column —
            // including the answer-choice row below, which stays a plain,
            // natural-sized sibling — and AspectRatioFitBox letterbox-fits within
            // that bounded box, so nothing here ever needs to scroll.
            AspectRatioFitBox(ratio = 1f, modifier = Modifier.weight(1f, fill = true).fillMaxSize()) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize().scale(burstScale.value)) {
                    Image(
                        painter = painterResource(R.drawable.bg_daniel_den),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )

                    val ringCount = lionsDenState.currentStepIndex.coerceIn(0, DanielContent.LIONS_DEN_PROBLEM_COUNT)
                    if (ringCount > 0) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val center = Offset(size.width / 2f, size.height / 2f)
                            for (ring in 0 until ringCount) {
                                val t = if (DanielContent.LIONS_DEN_PROBLEM_COUNT <= 1) {
                                    1f
                                } else {
                                    ring / (DanielContent.LIONS_DEN_PROBLEM_COUNT - 1).toFloat()
                                }
                                val radius = size.minDimension * (RING_MIN_RADIUS_FRACTION + (RING_MAX_RADIUS_FRACTION - RING_MIN_RADIUS_FRACTION) * t)
                                // Once the shield is fully complete, every ring blazes
                                // fully bright white — a single unmistakable "it's
                                // finished" moment, not just the outermost ring being
                                // marginally brighter than the rest.
                                val ringColor = if (lionsDenState.isComplete) Color.White else Color(0xFFFFD54A).copy(alpha = 0.35f + 0.65f * t)
                                drawCircle(
                                    color = ringColor,
                                    radius = radius,
                                    center = center,
                                    style = Stroke(width = 6f + 6f * t),
                                )
                            }
                        }
                    }

                    if (lionsDenState.isComplete) {
                        val playAreaMinForAngel = maxWidth.coerceAtMost(maxHeight)
                        Image(
                            painter = painterResource(R.drawable.ic_angel),
                            contentDescription = null,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .offset(y = -(playAreaMinForAngel * ANGEL_Y_OFFSET_FRACTION))
                                .size(playAreaMinForAngel * ANGEL_SIZE_FRACTION),
                        )
                    }

                    CharacterPreview(
                        customization = characterCustomization,
                        posture = if (lionsDenState.isComplete) Posture.THUMBS_UP else Posture.STANDING,
                        modifier = Modifier.align(Alignment.Center).size(maxWidth.coerceAtMost(maxHeight) * CHARACTER_SIZE_FRACTION),
                    )

                    val lionsDescription = when {
                        lionsDenState.isComplete -> stringResource(R.string.daniel_lions_den_lions_calm_content_description)
                        lionProximity >= 4 -> stringResource(R.string.daniel_lions_den_lions_very_close_content_description)
                        lionProximity >= 2 -> stringResource(R.string.daniel_lions_den_lions_close_content_description)
                        else -> stringResource(R.string.daniel_lions_den_lions_far_content_description)
                    }
                    val leftXFraction = if (lionsDenState.isComplete) {
                        LION_COMPLETE_X_FRACTION
                    } else {
                        LION_OUTER_X_FRACTION + (LION_INNER_X_FRACTION - LION_OUTER_X_FRACTION) * lionApproachProgress.value
                    }
                    val lionSizeFraction = if (lionsDenState.isComplete) {
                        LION_BASE_SIZE_FRACTION
                    } else {
                        LION_BASE_SIZE_FRACTION + (LION_MAX_SIZE_FRACTION - LION_BASE_SIZE_FRACTION) * lionApproachProgress.value
                    }
                    val playAreaMin = maxWidth.coerceAtMost(maxHeight)
                    val lionSize = playAreaMin * lionSizeFraction
                    val leftLionRes = when {
                        lionsDenState.isComplete -> R.drawable.ic_lion_left_relaxed
                        lionRoaring -> R.drawable.ic_lion_left_roar
                        else -> R.drawable.ic_lion_left_stand
                    }
                    val rightLionRes = when {
                        lionsDenState.isComplete -> R.drawable.ic_lion_right_relaxed
                        lionRoaring -> R.drawable.ic_lion_right_roar
                        else -> R.drawable.ic_lion_right_stand
                    }
                    Image(
                        painter = painterResource(leftLionRes),
                        contentDescription = lionsDescription,
                        modifier = Modifier
                            .offset(x = maxWidth * leftXFraction - lionSize / 2, y = maxHeight * LION_Y_FRACTION - lionSize / 2)
                            .size(lionSize),
                    )
                    Image(
                        painter = painterResource(rightLionRes),
                        contentDescription = null,
                        modifier = Modifier
                            .offset(x = maxWidth * (1f - leftXFraction) - lionSize / 2, y = maxHeight * LION_Y_FRACTION - lionSize / 2)
                            .size(lionSize),
                    )
                }
            }

            if (tooClose) {
                Text(
                    text = stringResource(R.string.daniel_lions_den_too_close_message),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 12.dp),
                )
                Button(onClick = onRetry, modifier = Modifier.padding(top = 12.dp)) {
                    Text(text = stringResource(R.string.daniel_lions_den_try_again))
                }
            } else {
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
            }

            if (previouslyCompleted && !lionsDenState.isComplete) {
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
private fun DanielLionsDenPreview() {
    val previewProblems = listOf(
        MathProblem(id = "problem_1", operandA = 54, operandB = 21, operator = MathOperator.ADD, choiceValues = listOf(75, 74, 76)),
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
            lionProximity = 0,
            characterCustomization = CharacterCustomization(),
            onAnswerTapped = {},
            onRetry = {},
            onContinue = {},
            onBackToMainMenu = {},
        )
    }
}
