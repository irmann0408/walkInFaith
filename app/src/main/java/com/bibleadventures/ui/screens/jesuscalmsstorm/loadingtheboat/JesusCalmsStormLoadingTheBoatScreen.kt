package com.bibleadventures.ui.screens.jesuscalmsstorm.loadingtheboat

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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import com.bibleadventures.game.puzzles.stackbuild.StackBuildOutcome
import com.bibleadventures.game.stories.JesusCalmsStormContent
import com.bibleadventures.ui.LocalReducedMotion
import com.bibleadventures.ui.components.AdventureMenuButton
import com.bibleadventures.ui.screens.jesuscalmsstorm.JesusCalmsStormViewModel
import com.bibleadventures.ui.theme.BibleAdventuresTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val ITEM_SIZE = 64.dp
private val SNAP_RADIUS = 72.dp
private val STACK_LEVEL_RISE = 20.dp
private val DROP_ZONE_WIDTH = 180.dp
private val DROP_ZONE_HEIGHT = 200.dp

/**
 * Six items loaded aboard before departure (Mark 4:36), each assigned a
 * random distinct weight 1-99 fresh every playthrough, dragged one at a
 * time onto a growing pile **heaviest first** —
 * [com.bibleadventures.game.puzzles.stackbuild.StackBuildGameState.nextExpectedId]
 * enforces which item is next; dragging the wrong one onto the boat just
 * re-prompts (no progress lost). The tray's on-screen order is a separate,
 * fixed shuffle ([JesusCalmsStormViewModel]'s `boatTrayOrder`),
 * deliberately independent of the required placement order, so the tray
 * layout itself never gives away the answer. Reuses the exact drag/snap
 * idiom already proven by `JerichoSettingUpCampScreen` — `detectDragGestures`
 * + `Modifier.offset` + `boundsInRoot()` hit-testing, with an [Animatable]
 * -driven snap once a drop is confirmed both close enough *and* the
 * correct next item — reskinned here with 6 distinct item icons instead of
 * one repeated stone image, since unlike Jericho's interchangeable stones
 * each boat item looks different. Each item's weight is shown as a visible
 * number badge on its icon (same role as Jericho's stones printing their
 * own number directly) — the icon alone can't tell a child which item is
 * heaviest, so the number is the actual thing being compared.
 */
@Composable
fun JesusCalmsStormLoadingTheBoatScreen(
    viewModel: JesusCalmsStormViewModel,
    onContinue: () -> Unit,
    previouslyCompleted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    JesusCalmsStormLoadingTheBoatContent(
        loadingState = uiState.loadingState,
        itemWeights = uiState.boatItemWeights,
        trayOrder = uiState.boatTrayOrder,
        onItemPlaced = viewModel::onBoatItemPlaced,
        onContinue = onContinue,
        previouslyCompleted = previouslyCompleted,
        modifier = modifier,
    )
}

@Composable
private fun JesusCalmsStormLoadingTheBoatContent(
    loadingState: StackBuildGameState,
    itemWeights: Map<String, Int>,
    trayOrder: List<String>,
    onItemPlaced: (String) -> Unit,
    onContinue: () -> Unit,
    previouslyCompleted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var dropZoneCenter by remember { mutableStateOf(Offset.Zero) }
    val snapRadiusPx = with(LocalDensity.current) { SNAP_RADIUS.toPx() }
    val dropZoneDescription = stringResource(R.string.jesus_calms_storm_loading_dropzone_content_description)
    val remainingItemIds = trayOrder.filter { it in loadingState.remainingIds }

    // Self-adjusting per-level rise so the full stack always fits inside
    // DROP_ZONE_HEIGHT regardless of item count, instead of a fixed
    // STACK_LEVEL_RISE that could clip the top items invisible above the
    // frame — the same overflow trap Jericho's Setting Up Camp hit and
    // fixed (see JerichoSettingUpCampScreen.kt).
    val itemCount = loadingState.itemIds.size
    val stackLevelRise = if (itemCount > 1) {
        ((DROP_ZONE_HEIGHT - ITEM_SIZE) / (itemCount - 1)).coerceAtMost(STACK_LEVEL_RISE)
    } else {
        STACK_LEVEL_RISE
    }

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.jesus_calms_storm_loading_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = stringResource(R.string.jesus_calms_storm_loading_instructions),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = stringResource(R.string.jesus_calms_storm_loading_progress_label, loadingState.placedOrder.size, loadingState.itemIds.size),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
            )

            val feedback = when (loadingState.lastOutcome) {
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
                loadingState.placedOrder.forEachIndexed { level, itemId ->
                    key(itemId) {
                        Box(
                            modifier = Modifier
                                .size(ITEM_SIZE)
                                .offset(y = -(stackLevelRise * level)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Image(painter = painterResource(itemIconRes(itemId)), contentDescription = null, modifier = Modifier.fillMaxSize())
                            WeightBadge(weight = itemWeights.getValue(itemId), modifier = Modifier.align(Alignment.TopEnd))
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                remainingItemIds.chunked(3).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    ) {
                        row.forEach { itemId ->
                            key(itemId) {
                                DraggableBoatItem(
                                    itemId = itemId,
                                    weight = itemWeights.getValue(itemId),
                                    isNextExpected = itemId == loadingState.nextExpectedId,
                                    dropZoneCenter = dropZoneCenter,
                                    snapRadiusPx = snapRadiusPx,
                                    onSnapped = onItemPlaced,
                                )
                            }
                        }
                    }
                }
            }

            if (previouslyCompleted && !loadingState.isComplete) {
                Text(
                    text = stringResource(R.string.puzzle_already_completed_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            if (loadingState.isComplete || previouslyCompleted) {
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
private fun DraggableBoatItem(
    itemId: String,
    weight: Int,
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
    val name = stringResource(R.string.jesus_calms_storm_loading_item_content_description, stringResource(itemNameRes(itemId)), weight)
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
            .size(ITEM_SIZE)
            .pointerInput(itemId, isNextExpected, dropZoneCenter) {
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
                                onSnapped(itemId)
                            }
                            scope.launch {
                                val pulseSpec: AnimationSpec<Float> = if (reducedMotion) snap() else spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                                scaleAnim.animateTo(1.15f, animationSpec = pulseSpec)
                                scaleAnim.animateTo(1f, animationSpec = pulseSpec)
                            }
                        } else {
                            // Either outside the snap radius, or the right spot but the wrong
                            // item — either way, reset instantly, no penalty. A same-radius
                            // wrong-order drop still notifies the ViewModel so feedback text
                            // updates, just without the accept animation.
                            if (distance <= snapRadiusPx) onSnapped(itemId)
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
            painter = painterResource(itemIconRes(itemId)),
            contentDescription = null,
            modifier = Modifier.size(56.dp).scale(scaleAnim.value),
        )
        WeightBadge(weight = weight, modifier = Modifier.align(Alignment.TopEnd))
    }
}

/**
 * A small always-visible number badge — the actual thing a child compares
 * to decide what's heaviest, since the icon alone can't communicate weight.
 * Placed at the corner so it never obscures the item's own icon.
 */
@Composable
private fun WeightBadge(weight: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .size(22.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = weight.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

private fun itemIconRes(itemId: String): Int = when (itemId) {
    "anchor" -> R.drawable.ic_anchor
    "water_jars" -> R.drawable.ic_water_jars
    "fishing_nets" -> R.drawable.ic_fishing_nets
    "food_basket" -> R.drawable.ic_leftover_basket
    "oars" -> R.drawable.ic_oars
    "cushion" -> R.drawable.ic_cushion
    else -> R.drawable.ic_anchor
}

private fun itemNameRes(itemId: String): Int = when (itemId) {
    "anchor" -> R.string.jesus_calms_storm_loading_item_anchor
    "water_jars" -> R.string.jesus_calms_storm_loading_item_water_jars
    "fishing_nets" -> R.string.jesus_calms_storm_loading_item_fishing_nets
    "food_basket" -> R.string.jesus_calms_storm_loading_item_food_basket
    "oars" -> R.string.jesus_calms_storm_loading_item_oars
    "cushion" -> R.string.jesus_calms_storm_loading_item_cushion
    else -> R.string.jesus_calms_storm_loading_item_anchor
}

@Preview(showBackground = true)
@Composable
private fun JesusCalmsStormLoadingTheBoatPreview() {
    val previewWeights = JesusCalmsStormContent.boatItemIds.zip((1..99).shuffled().take(JesusCalmsStormContent.boatItemIds.size)).toMap()
    BibleAdventuresTheme {
        JesusCalmsStormLoadingTheBoatContent(
            loadingState = StackBuildGameState(itemIds = previewWeights.entries.sortedByDescending { it.value }.map { it.key }),
            itemWeights = previewWeights,
            trayOrder = JesusCalmsStormContent.boatItemIds,
            onItemPlaced = {},
            onContinue = {},
        )
    }
}
