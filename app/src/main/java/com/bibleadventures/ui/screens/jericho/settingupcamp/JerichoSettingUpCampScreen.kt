package com.bibleadventures.ui.screens.jericho.settingupcamp

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bibleadventures.R
import com.bibleadventures.game.puzzles.stackbuild.StackBuildGameState
import com.bibleadventures.game.puzzles.stackbuild.StackBuildOutcome
import com.bibleadventures.game.stories.JerichoContent
import com.bibleadventures.ui.LocalReducedMotion
import com.bibleadventures.ui.components.PuzzleTopBar
import com.bibleadventures.ui.screens.jericho.JerichoViewModel
import com.bibleadventures.ui.theme.BibleAdventuresTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val STONE_SIZE = 64.dp
private val SNAP_RADIUS = 72.dp
private val STACK_LEVEL_RISE = 20.dp
private val DROP_ZONE_WIDTH = 160.dp
private val DROP_ZONE_HEIGHT = 200.dp

/**
 * Twelve memorial stones (Joshua 4:1-9), each randomly assigned a distinct
 * number 1-99 fresh every playthrough, dragged one at a time onto a growing
 * monument **in ascending order** — [com.bibleadventures.game.puzzles.stackbuild.StackBuildGameState.nextExpectedId]
 * enforces which stone is next; dragging the wrong one onto the drop zone
 * just re-prompts (no progress lost, matches this app's no-failure-state
 * rule), only the correct next stone animates into the stack. The tray's
 * on-screen order is a separate, fixed shuffle (`JerichoViewModel`'s
 * `campTrayOrder`) — deliberately independent of the required placement
 * order, so the tray layout itself never gives away the answer. A drop
 * within [SNAP_RADIUS] of the monument is the forgiving target (no
 * punishing precision); a miss (or a correct-but-out-of-radius stone)
 * resets instantly. Reuses the drag idiom already proven twice in this app
 * (`NoahsArkOrganizeArkScreen`, `DavidGoliathSlingPracticeScreen`) —
 * `detectDragGestures` + `Modifier.offset` + `boundsInRoot()` hit-testing
 * — with an [Animatable]-driven snap animation once a drop is confirmed
 * both close enough *and* the correct next stone.
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
    onBackToMainMenu: () -> Unit,
    previouslyCompleted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    JerichoSettingUpCampContent(
        campState = uiState.campState,
        stoneValues = uiState.campStoneValues,
        trayOrder = uiState.campTrayOrder,
        onStonePlaced = viewModel::onCampStonePlaced,
        onContinue = onContinue,
        onBackToMainMenu = onBackToMainMenu,
        previouslyCompleted = previouslyCompleted,
        modifier = modifier,
    )
}

@Composable
private fun JerichoSettingUpCampContent(
    campState: StackBuildGameState,
    stoneValues: Map<String, Int>,
    trayOrder: List<String>,
    onStonePlaced: (String) -> Unit,
    onContinue: () -> Unit,
    onBackToMainMenu: () -> Unit,
    previouslyCompleted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var dropZoneCenter by remember { mutableStateOf(Offset.Zero) }
    val snapRadiusPx = with(LocalDensity.current) { SNAP_RADIUS.toPx() }
    val dropZoneDescription = stringResource(R.string.jericho_camp_dropzone_content_description)
    val remainingStoneIds = trayOrder.filter { it in campState.remainingIds }

    // All 12 stones stacking at the full STACK_LEVEL_RISE would need
    // 64dp + 20dp*11 = 284dp of height — more than DROP_ZONE_HEIGHT, so the
    // last several stones were rendering above the frame's top edge,
    // clipped invisible by the Box's own .clip(). Shrinks the per-level
    // rise just enough that the full stack always fits inside the frame,
    // regardless of how many stones campState.itemIds ends up holding —
    // self-adjusting rather than a magic number tied to today's count of
    // 12, so this can't silently re-break if that count ever changes.
    val stoneCount = campState.itemIds.size
    val stackLevelRise = if (stoneCount > 1) {
        ((DROP_ZONE_HEIGHT - STONE_SIZE) / (stoneCount - 1)).coerceAtMost(STACK_LEVEL_RISE)
    } else {
        STACK_LEVEL_RISE
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            if (previouslyCompleted || campState.isComplete) {
                PuzzleTopBar(
                    showBackButton = previouslyCompleted,
                    onBackToMainMenu = onBackToMainMenu,
                    showNextButton = campState.isComplete || previouslyCompleted,
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

            val feedback = when (campState.lastOutcome) {
                StackBuildOutcome.PLACED, StackBuildOutcome.COMPLETE -> stringResource(R.string.feedback_great_job)
                StackBuildOutcome.WRONG_ORDER -> stringResource(R.string.feedback_try_another_one)
                StackBuildOutcome.NONE -> ""
            }
            Box(modifier = Modifier.height(28.dp)) {
                Text(text = feedback, style = MaterialTheme.typography.titleMedium)
            }

            Box(
                modifier = Modifier
                    .width(DROP_ZONE_WIDTH)
                    .height(DROP_ZONE_HEIGHT)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                    .onGloballyPositioned { coords -> dropZoneCenter = coords.boundsInRoot().center }
                    .semantics { contentDescription = dropZoneDescription },
                contentAlignment = Alignment.BottomCenter,
            ) {
                campState.placedOrder.forEachIndexed { level, stoneId ->
                    key(stoneId) {
                        StoneTile(
                            value = stoneValues.getValue(stoneId),
                            modifier = Modifier
                                .size(STONE_SIZE)
                                .offset(y = -(stackLevelRise * level)),
                        )
                    }
                }
            }

            // Every remaining stone is visible at once. weight(1f, fill = true)
            // hands this region exactly the space left over after every other
            // sibling above claims its natural size, and BoxWithConstraints reads
            // that resolved space to compute a stone size that makes the whole
            // tray fit — shrinking below STONE_SIZE only when there isn't room for
            // it, never overflowing. Drag/drop detection reads live positions via
            // onGloballyPositioned, so a smaller stone size doesn't affect it.
            BoxWithConstraints(modifier = Modifier.fillMaxWidth().weight(1f, fill = true).padding(top = 16.dp)) {
                val columns = 4
                val rows = ((remainingStoneIds.size + columns - 1) / columns).coerceAtLeast(1)
                val spacing = 8.dp
                val traySize = minOf(
                    (maxWidth - spacing * (columns - 1)) / columns,
                    (maxHeight - spacing * (rows - 1)) / rows,
                ).coerceIn(48.dp, STONE_SIZE)

                Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
                    remainingStoneIds.chunked(columns).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(spacing, Alignment.CenterHorizontally),
                        ) {
                            row.forEach { stoneId ->
                                key(stoneId) {
                                    DraggableStone(
                                        stoneId = stoneId,
                                        value = stoneValues.getValue(stoneId),
                                        stoneSize = traySize,
                                        isNextExpected = stoneId == campState.nextExpectedId,
                                        dropZoneCenter = dropZoneCenter,
                                        snapRadiusPx = snapRadiusPx,
                                        onSnapped = onStonePlaced,
                                    )
                                }
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
        }
    }
}

@Composable
private fun DraggableStone(
    stoneId: String,
    value: Int,
    stoneSize: Dp,
    isNextExpected: Boolean,
    dropZoneCenter: Offset,
    snapRadiusPx: Float,
    onSnapped: (String) -> Unit,
) {
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    val snapOffset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val scaleAnim = remember { Animatable(1f) }
    var baseTopLeft by remember { mutableStateOf(Offset.Zero) }
    var itemSize by remember { mutableStateOf(IntSize.Zero) }
    val name = stringResource(R.string.jericho_camp_stone_content_description, value)
    val scope = rememberCoroutineScope()
    val reducedMotion = LocalReducedMotion.current

    Box(
        modifier = Modifier
            .onGloballyPositioned { coords ->
                baseTopLeft = coords.positionInRoot()
                itemSize = coords.size
            }
            .offset {
                IntOffset((dragOffset.x + snapOffset.value.x).roundToInt(), (dragOffset.y + snapOffset.value.y).roundToInt())
            }
            .size(stoneSize)
            .pointerInput(stoneId, isNextExpected, dropZoneCenter) {
                detectDragGestures(
                    onDragEnd = {
                        val releasedCenter = baseTopLeft + dragOffset + Offset(itemSize.width / 2f, itemSize.height / 2f)
                        val distance = (releasedCenter - dropZoneCenter).getDistance()
                        if (distance <= snapRadiusPx && isNextExpected) {
                            val target = dropZoneCenter - releasedCenter
                            scope.launch {
                                snapOffset.animateTo(
                                    target,
                                    animationSpec = if (reducedMotion) snap() else spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                                )
                                onSnapped(stoneId)
                            }
                            scope.launch {
                                val pulseSpec: AnimationSpec<Float> = if (reducedMotion) snap() else spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                                scaleAnim.animateTo(1.15f, animationSpec = pulseSpec)
                                scaleAnim.animateTo(1f, animationSpec = pulseSpec)
                            }
                        } else {
                            // Either outside the snap radius, or the right spot but the wrong
                            // stone — either way, reset instantly, no penalty. A same-radius
                            // wrong-order drop still notifies the ViewModel so feedback text
                            // updates (see `onSnapped`'s WRONG_ORDER handling), just without
                            // the accept animation.
                            if (distance <= snapRadiusPx) onSnapped(stoneId)
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
            modifier = Modifier.size(stoneSize * (56f / 64f)).scale(scaleAnim.value),
        )
        Text(text = value.toString(), style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun StoneTile(value: Int, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Image(painter = painterResource(R.drawable.ic_stone_smooth), contentDescription = null, modifier = Modifier.fillMaxSize())
        Text(text = value.toString(), style = MaterialTheme.typography.titleMedium)
    }
}

@Preview(showBackground = true)
@Composable
private fun JerichoSettingUpCampPreview() {
    val previewValues = JerichoContent.campStoneIds.zip((1..99).shuffled().take(JerichoContent.campStoneIds.size)).toMap()
    BibleAdventuresTheme {
        JerichoSettingUpCampContent(
            campState = StackBuildGameState(itemIds = previewValues.entries.sortedBy { it.value }.map { it.key }),
            stoneValues = previewValues,
            trayOrder = JerichoContent.campStoneIds,
            onStonePlaced = {},
            onContinue = {},
            onBackToMainMenu = {},
        )
    }
}
