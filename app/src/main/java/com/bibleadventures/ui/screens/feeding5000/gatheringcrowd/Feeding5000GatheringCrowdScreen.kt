package com.bibleadventures.ui.screens.feeding5000.gatheringcrowd

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bibleadventures.R
import com.bibleadventures.game.puzzles.groupfill.FamilyGroup
import com.bibleadventures.game.puzzles.groupfill.GroupFillGameState
import com.bibleadventures.game.puzzles.groupfill.GroupFillOutcome
import com.bibleadventures.ui.components.AdventureMenuButton
import com.bibleadventures.ui.screens.feeding5000.Feeding5000ViewModel
import com.bibleadventures.ui.theme.BibleAdventuresTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val FAMILY_TILE_SIZE = 56.dp
private val CIRCLE_SIZE = 96.dp
private val SNAP_RADIUS = 60.dp

/**
 * Mark 6:39-40's "ranks of hundreds and fifties" as a real sum-matching
 * puzzle — drag families into a seating circle only if they fit its exact
 * remaining capacity; families are pooled across all circles and shuffled,
 * so there's a genuine decision each drop, not blind category-matching.
 * Reuses Setting Up Camp's exact `detectDragGestures` + `Animatable`-driven
 * snap idiom (including its `pointerInput` keying fix — this screen keys on
 * every value read from `onGloballyPositioned`/live state, not just the
 * dragged item's own id) and its faster `Spring.StiffnessMedium` settle.
 */
@Composable
fun Feeding5000GatheringCrowdScreen(
    viewModel: Feeding5000ViewModel,
    onContinue: () -> Unit,
    previouslyCompleted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Feeding5000GatheringCrowdContent(
        groupFillState = uiState.groupFillState,
        onFamilyDropped = viewModel::onFamilyDropped,
        onContinue = onContinue,
        previouslyCompleted = previouslyCompleted,
        modifier = modifier,
    )
}

@Composable
private fun Feeding5000GatheringCrowdContent(
    groupFillState: GroupFillGameState,
    onFamilyDropped: (familyId: String, circleIndex: Int) -> Unit,
    onContinue: () -> Unit,
    previouslyCompleted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var circleCenters by remember { mutableStateOf(List(groupFillState.circleTargets.size) { Offset.Zero }) }
    val snapRadiusPx = with(LocalDensity.current) { SNAP_RADIUS.toPx() }
    val circleSums = groupFillState.circleTargets.indices.map { groupFillState.circleSum(it) }

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.feeding_5000_gathering_crowd_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = stringResource(R.string.feeding_5000_gathering_crowd_instructions),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp),
            )

            val feedback = when (groupFillState.lastOutcome) {
                GroupFillOutcome.ADDED, GroupFillOutcome.CIRCLE_COMPLETE, GroupFillOutcome.ALL_COMPLETE -> stringResource(R.string.feedback_great_job)
                GroupFillOutcome.REJECTED_OVERSHOOT -> stringResource(R.string.feedback_try_another_one)
                GroupFillOutcome.NONE -> ""
            }
            Box(modifier = Modifier.height(28.dp).padding(top = 4.dp)) {
                Text(text = feedback, style = MaterialTheme.typography.titleMedium)
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            ) {
                groupFillState.circleTargets.forEachIndexed { index, target ->
                    key(index) {
                        CircleDropZone(
                            index = index,
                            sum = circleSums[index],
                            target = target,
                            isComplete = groupFillState.isCircleComplete(index),
                            onCenterChanged = { center ->
                                circleCenters = circleCenters.toMutableList().also { it[index] = center }
                            },
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                groupFillState.remainingFamilyIds.chunked(5).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    ) {
                        row.forEach { familyId ->
                            key(familyId) {
                                val headcount = groupFillState.families.first { it.id == familyId }.headcount
                                DraggableFamily(
                                    familyId = familyId,
                                    headcount = headcount,
                                    circleCenters = circleCenters,
                                    circleSums = circleSums,
                                    circleTargets = groupFillState.circleTargets,
                                    snapRadiusPx = snapRadiusPx,
                                    onDropped = onFamilyDropped,
                                )
                            }
                        }
                    }
                }
            }

            if (previouslyCompleted && !groupFillState.isComplete) {
                Text(
                    text = stringResource(R.string.puzzle_already_completed_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            if (groupFillState.isComplete || previouslyCompleted) {
                AdventureMenuButton(
                    text = stringResource(R.string.action_continue),
                    onClick = onContinue,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
        }
    }
}

/**
 * The circle's own drag-target bounds ([onCenterChanged], read via
 * `onGloballyPositioned`) must track only the circular shape itself, not
 * the "Circle N" caption below it — so that caption is a sibling in an
 * outer `Column`, never inside the tracked `Box`, keeping the distance math
 * `DraggableFamily` uses for its nearest-circle snap unaffected by the
 * caption's own height.
 */
@Composable
private fun CircleDropZone(index: Int, sum: Int, target: Int, isComplete: Boolean, onCenterChanged: (Offset) -> Unit, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.feeding_5000_gathering_crowd_circle_content_description, index + 1)

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(CIRCLE_SIZE)
                .clip(CircleShape)
                .background(if (isComplete) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                .onGloballyPositioned { coords -> onCenterChanged(coords.boundsInRoot().center) }
                .semantics { contentDescription = description },
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (isComplete) {
                    Icon(imageVector = Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                Text(
                    text = stringResource(R.string.feeding_5000_gathering_crowd_sum_label, sum, target),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
        Text(
            text = stringResource(R.string.feeding_5000_gathering_crowd_circle_label, index + 1),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun DraggableFamily(
    familyId: String,
    headcount: Int,
    circleCenters: List<Offset>,
    circleSums: List<Int>,
    circleTargets: List<Int>,
    snapRadiusPx: Float,
    onDropped: (familyId: String, circleIndex: Int) -> Unit,
) {
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    val snapOffset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val scaleAnim = remember { Animatable(1f) }
    var baseTopLeft by remember { mutableStateOf(Offset.Zero) }
    var itemSize by remember { mutableStateOf(IntSize.Zero) }
    val name = stringResource(R.string.feeding_5000_gathering_crowd_family_content_description, headcount)
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .onGloballyPositioned { coords ->
                baseTopLeft = coords.positionInRoot()
                itemSize = coords.size
            }
            .offset {
                IntOffset((dragOffset.x + snapOffset.value.x).roundToInt(), (dragOffset.y + snapOffset.value.y).roundToInt())
            }
            .size(FAMILY_TILE_SIZE)
            .pointerInput(familyId, circleCenters, circleSums) {
                detectDragGestures(
                    onDragEnd = {
                        val releasedCenter = baseTopLeft + dragOffset + Offset(itemSize.width / 2f, itemSize.height / 2f)
                        val nearestIndex = circleCenters.indices.minByOrNull { (releasedCenter - circleCenters[it]).getDistance() }
                        val distance = nearestIndex?.let { (releasedCenter - circleCenters[it]).getDistance() } ?: Float.MAX_VALUE
                        val fits = nearestIndex != null && circleSums[nearestIndex] + headcount <= circleTargets[nearestIndex]
                        if (nearestIndex != null && distance <= snapRadiusPx && fits) {
                            val target = circleCenters[nearestIndex] - releasedCenter
                            scope.launch {
                                snapOffset.animateTo(target, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
                                onDropped(familyId, nearestIndex)
                            }
                            scope.launch {
                                scaleAnim.animateTo(1.15f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                                scaleAnim.animateTo(1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                            }
                        } else {
                            // Near a circle but wouldn't fit — still notify so feedback text
                            // updates, but reset instantly (same as a radius miss), no penalty.
                            if (nearestIndex != null && distance <= snapRadiusPx) onDropped(familyId, nearestIndex)
                            dragOffset = Offset.Zero
                        }
                    },
                    onDragCancel = { dragOffset = Offset.Zero },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragOffset += dragAmount
                    },
                )
            }
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .semantics { contentDescription = name },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = headcount.toString(),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(4.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun Feeding5000GatheringCrowdPreview() {
    BibleAdventuresTheme {
        Feeding5000GatheringCrowdContent(
            groupFillState = GroupFillGameState(
                families = listOf(
                    FamilyGroup("family_0_0", 20),
                    FamilyGroup("family_0_1", 30),
                ),
                circleTargets = listOf(50, 50, 100),
            ),
            onFamilyDropped = { _, _ -> },
            onContinue = {},
        )
    }
}
