package com.bibleadventures.ui.screens.jericho.settingupcamp

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bibleadventures.R
import com.bibleadventures.game.puzzles.stackbuild.StackBuildGameState
import com.bibleadventures.game.stories.JerichoContent
import com.bibleadventures.ui.components.AdventureMenuButton
import com.bibleadventures.ui.screens.jericho.JerichoViewModel
import com.bibleadventures.ui.theme.BibleAdventuresTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val STONE_SIZE = 64.dp
private val SNAP_RADIUS = 72.dp
private val STACK_LEVEL_RISE = 20.dp

/**
 * Twelve memorial stones (Joshua 4:1-9), dragged one at a time onto a
 * growing monument — any stone, any order (the source material doesn't
 * rank the tribes' stones), so [com.bibleadventures.game.puzzles.stackbuild.StackBuildGameState.placedOrder]
 * only tracks the *act* of stacking, not which named stone fills which
 * level. A drop within [SNAP_RADIUS] of the monument slides smoothly into
 * place (a gentle, forgiving target — no punishing precision, matching
 * this app's no-failure-state rule); a miss just resets instantly, no
 * penalty, try again. Reuses the drag idiom already proven twice in this
 * app (`NoahsArkOrganizeArkScreen`, `DavidGoliathSlingPracticeScreen`) —
 * `detectDragGestures` + `Modifier.offset` + `boundsInRoot()` hit-testing
 * — with one addition new to this codebase: an [Animatable]-driven snap
 * animation once a drop is confirmed close enough, instead of an instant
 * placement.
 *
 * A real bug found on-device during verification, worth flagging since it's
 * an easy trap for the next real-drag screen: keying the gesture detector
 * only on `pointerInput(stoneId)` captured the drop zone's center in a
 * closure at first launch and never saw it update once the drop zone's
 * real position was measured (the gesture-detector coroutine only
 * relaunches when its keys change, and `stoneId` never does) — every drop
 * silently measured its distance against `Offset.Zero`. Fixed by keying
 * `pointerInput` on the drop zone center too, so the detector relaunches
 * (and recaptures the fresh value) once the real position is known.
 */
@Composable
fun JerichoSettingUpCampScreen(
    viewModel: JerichoViewModel,
    onContinue: () -> Unit,
    previouslyCompleted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    JerichoSettingUpCampContent(
        campState = uiState.campState,
        onStonePlaced = viewModel::onCampStonePlaced,
        onContinue = onContinue,
        previouslyCompleted = previouslyCompleted,
        modifier = modifier,
    )
}

@Composable
private fun JerichoSettingUpCampContent(
    campState: StackBuildGameState,
    onStonePlaced: (String) -> Unit,
    onContinue: () -> Unit,
    previouslyCompleted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var dropZoneCenter by remember { mutableStateOf(Offset.Zero) }
    val snapRadiusPx = with(LocalDensity.current) { SNAP_RADIUS.toPx() }
    val dropZoneDescription = stringResource(R.string.jericho_camp_dropzone_content_description)
    val remainingStones = JerichoContent.campStones.filter { it.id in campState.remainingIds }

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.jericho_camp_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = stringResource(R.string.jericho_camp_instructions),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = stringResource(R.string.jericho_camp_progress_label, campState.placedOrder.size, campState.itemIds.size),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
            )

            Box(
                modifier = Modifier
                    .width(160.dp)
                    .height(200.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                    .onGloballyPositioned { coords -> dropZoneCenter = coords.boundsInRoot().center }
                    .semantics { contentDescription = dropZoneDescription },
                contentAlignment = Alignment.BottomCenter,
            ) {
                campState.placedOrder.forEachIndexed { level, stoneId ->
                    key(stoneId) {
                        Image(
                            painter = painterResource(R.drawable.ic_stone_smooth),
                            contentDescription = null,
                            modifier = Modifier
                                .size(STONE_SIZE)
                                .offset(y = -(STACK_LEVEL_RISE * level)),
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                remainingStones.chunked(4).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    ) {
                        row.forEach { stone ->
                            key(stone.id) {
                                DraggableStone(
                                    stoneId = stone.id,
                                    nameRes = stone.nameRes,
                                    dropZoneCenter = dropZoneCenter,
                                    snapRadiusPx = snapRadiusPx,
                                    onSnapped = onStonePlaced,
                                )
                            }
                        }
                    }
                }
            }

            if (previouslyCompleted && !campState.isComplete) {
                Text(
                    text = stringResource(R.string.puzzle_already_completed_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            if (campState.isComplete || previouslyCompleted) {
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
private fun DraggableStone(
    stoneId: String,
    nameRes: Int,
    dropZoneCenter: Offset,
    snapRadiusPx: Float,
    onSnapped: (String) -> Unit,
) {
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    val snapOffset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val scaleAnim = remember { Animatable(1f) }
    var baseTopLeft by remember { mutableStateOf(Offset.Zero) }
    var itemSize by remember { mutableStateOf(IntSize.Zero) }
    val name = stringResource(nameRes)
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
            .size(STONE_SIZE)
            .pointerInput(stoneId, dropZoneCenter) {
                detectDragGestures(
                    onDragEnd = {
                        val releasedCenter = baseTopLeft + dragOffset + Offset(itemSize.width / 2f, itemSize.height / 2f)
                        val distance = (releasedCenter - dropZoneCenter).getDistance()
                        if (distance <= snapRadiusPx) {
                            val target = dropZoneCenter - releasedCenter
                            scope.launch {
                                snapOffset.animateTo(target, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
                                onSnapped(stoneId)
                            }
                            scope.launch {
                                scaleAnim.animateTo(1.15f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                                scaleAnim.animateTo(1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                            }
                        } else {
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
            .semantics { contentDescription = name },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_stone_smooth),
            contentDescription = null,
            modifier = Modifier.size(56.dp).scale(scaleAnim.value),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun JerichoSettingUpCampPreview() {
    BibleAdventuresTheme {
        JerichoSettingUpCampContent(
            campState = StackBuildGameState(itemIds = JerichoContent.campStones.map { it.id }),
            onStonePlaced = {},
            onContinue = {},
        )
    }
}
