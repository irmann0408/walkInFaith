package com.bibleadventures.ui.screens.noahsark.organizeark

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.bibleadventures.domain.model.CharacterCustomization
import com.bibleadventures.game.puzzles.groupfill.FamilyGroup
import com.bibleadventures.game.puzzles.groupfill.GroupFillGame
import com.bibleadventures.game.puzzles.groupfill.GroupFillGameState
import com.bibleadventures.game.puzzles.groupfill.GroupFillOutcome
import com.bibleadventures.game.stories.NoahsArkContent
import com.bibleadventures.ui.LocalReducedMotion
import com.bibleadventures.ui.components.CharacterCallout
import com.bibleadventures.ui.components.PuzzleTopBar
import com.bibleadventures.ui.screens.noahsark.NoahsArkViewModel
import com.bibleadventures.ui.theme.BibleAdventuresTheme
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

private val BASKET_TILE_SIZE = 64.dp
private const val BASKET_ICON_FRACTION = 36f / 64f
private val DECK_SIZE = 96.dp
private val SNAP_RADIUS = 60.dp

/**
 * "Load the Ark" — reuses the exact `groupfill` running-sum engine already
 * proven by Feeding the 5000's "Gathering the Crowd" scene (same
 * `detectDragGestures` + `Animatable` snap idiom, including its
 * `pointerInput` keying fix), reframed as loading numbered supply baskets
 * onto 3 ark decks until each hits its exact capacity. Replaces the old
 * "Organize the Ark" (drag into a labeled category, no real reasoning
 * required) with genuine running-sum arithmetic appropriate for the app's
 * 7+ target age. A basket's supply icon (bread/fruit/grain/water) is purely
 * cosmetic — [NoahsArkViewModel.loadArkBasketSupplyKinds] — the engine only
 * ever judges the basket's count.
 */
@Composable
fun NoahsArkOrganizeArkScreen(
    viewModel: NoahsArkViewModel,
    onContinue: () -> Unit,
    onBackToMainMenu: () -> Unit,
    previouslyCompleted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val characterCustomization by viewModel.characterCustomization.collectAsStateWithLifecycle()

    NoahsArkOrganizeArkContent(
        groupFillState = uiState.groupFillState,
        basketSupplyKinds = uiState.loadArkBasketSupplyKinds,
        characterCustomization = characterCustomization,
        onBasketDropped = viewModel::onBasketDropped,
        onContinue = onContinue,
        onBackToMainMenu = onBackToMainMenu,
        previouslyCompleted = previouslyCompleted,
        modifier = modifier,
    )
}

private val DECK_LABEL_RES = listOf(
    R.string.noahs_ark_load_ark_deck_lower,
    R.string.noahs_ark_load_ark_deck_middle,
    R.string.noahs_ark_load_ark_deck_upper,
)

@Composable
private fun NoahsArkOrganizeArkContent(
    groupFillState: GroupFillGameState,
    basketSupplyKinds: Map<String, String>,
    characterCustomization: CharacterCustomization,
    onBasketDropped: (basketId: String, deckIndex: Int) -> Unit,
    onContinue: () -> Unit,
    onBackToMainMenu: () -> Unit,
    previouslyCompleted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var deckCenters by remember { mutableStateOf(List(groupFillState.circleTargets.size) { Offset.Zero }) }
    val snapRadiusPx = with(LocalDensity.current) { SNAP_RADIUS.toPx() }
    val deckSums = groupFillState.circleTargets.indices.map { groupFillState.circleSum(it) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            if (previouslyCompleted || groupFillState.isComplete) {
                PuzzleTopBar(
                    showBackButton = previouslyCompleted,
                    onBackToMainMenu = onBackToMainMenu,
                    showNextButton = groupFillState.isComplete || previouslyCompleted,
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
                text = stringResource(R.string.noahs_ark_organize_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = stringResource(R.string.noahs_ark_organize_instructions),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = stringResource(
                    R.string.noahs_ark_load_ark_progress_label,
                    groupFillState.circleTargets.indices.count { groupFillState.isCircleComplete(it) },
                    groupFillState.circleTargets.size,
                ),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
            )

            val feedback = when (groupFillState.lastOutcome) {
                GroupFillOutcome.ADDED, GroupFillOutcome.CIRCLE_COMPLETE, GroupFillOutcome.ALL_COMPLETE -> stringResource(R.string.feedback_great_job)
                GroupFillOutcome.REJECTED_OVERSHOOT, GroupFillOutcome.REJECTED_UNREACHABLE -> stringResource(R.string.feedback_try_another_one)
                GroupFillOutcome.NONE -> null
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            ) {
                groupFillState.circleTargets.forEachIndexed { index, target ->
                    key(index) {
                        DeckDropZone(
                            labelRes = DECK_LABEL_RES.getOrElse(index) { R.string.noahs_ark_load_ark_deck_lower },
                            sum = deckSums[index],
                            target = target,
                            isComplete = groupFillState.isCircleComplete(index),
                            onCenterChanged = { center ->
                                deckCenters = deckCenters.toMutableList().also { it[index] = center }
                            },
                        )
                    }
                }
            }

            // Every remaining basket is visible at once. weight(1f, fill = true)
            // hands this region exactly the space left over after every other
            // sibling above claims its natural size, and BoxWithConstraints reads
            // that resolved space to compute a tile size that makes the whole
            // grid fit — shrinking below BASKET_TILE_SIZE only when there isn't
            // room for it, never overflowing. Drag/drop detection reads live
            // positions via onGloballyPositioned, so a smaller tile size doesn't
            // affect it.
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true)
                    .padding(top = 24.dp),
            ) {
                val columns = 5
                val rows = ((groupFillState.remainingFamilyIds.size + columns - 1) / columns).coerceAtLeast(1)
                val spacing = 8.dp
                val tileSize = minOf(
                    (maxWidth - spacing * (columns - 1)) / columns,
                    (maxHeight - spacing * (rows - 1)) / rows,
                ).coerceIn(48.dp, BASKET_TILE_SIZE)

                Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
                    groupFillState.remainingFamilyIds.chunked(columns).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(spacing, Alignment.CenterHorizontally),
                        ) {
                            row.forEach { basketId ->
                                key(basketId) {
                                    val count = groupFillState.families.first { it.id == basketId }.headcount
                                    val supplyKindId = basketSupplyKinds.getValue(basketId)
                                    val iconRes = NoahsArkContent.loadArkSupplyKinds.first { it.id == supplyKindId }.iconRes
                                    DraggableBasket(
                                        basketId = basketId,
                                        count = count,
                                        iconRes = iconRes,
                                        tileSize = tileSize,
                                        deckCenters = deckCenters,
                                        canAccept = { deckIndex -> GroupFillGame.canAccept(groupFillState, basketId, deckIndex) },
                                        snapRadiusPx = snapRadiusPx,
                                        onDropped = onBasketDropped,
                                    )
                                }
                            }
                        }
                    }
                }

                CharacterCallout(
                    characterCustomization = characterCustomization,
                    message = feedback,
                    modifier = Modifier.align(Alignment.BottomStart),
                )
            }

            if (previouslyCompleted && !groupFillState.isComplete) {
                Text(
                    text = stringResource(R.string.puzzle_already_completed_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

/**
 * The deck's own drag-target bounds ([onCenterChanged], read via
 * `onGloballyPositioned`) must track only the deck box itself, not its
 * label below — so that label is a sibling in an outer `Column`, never
 * inside the tracked `Box`, keeping the distance math `DraggableBasket`
 * uses for its nearest-deck snap unaffected by the label's own height.
 */
@Composable
private fun DeckDropZone(labelRes: Int, sum: Int, target: Int, isComplete: Boolean, onCenterChanged: (Offset) -> Unit, modifier: Modifier = Modifier) {
    val label = stringResource(labelRes)

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(DECK_SIZE)
                .clip(RoundedCornerShape(12.dp))
                .background(if (isComplete) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                .onGloballyPositioned { coords -> onCenterChanged(coords.boundsInRoot().center) }
                .semantics { contentDescription = label },
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (isComplete) {
                    Icon(imageVector = Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                Text(
                    text = stringResource(R.string.noahs_ark_load_ark_sum_label, sum, target),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun DraggableBasket(
    basketId: String,
    count: Int,
    iconRes: Int,
    tileSize: Dp,
    deckCenters: List<Offset>,
    canAccept: (deckIndex: Int) -> Boolean,
    snapRadiusPx: Float,
    onDropped: (basketId: String, deckIndex: Int) -> Unit,
) {
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    val snapOffset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val scaleAnim = remember { Animatable(1f) }
    var baseTopLeft by remember { mutableStateOf(Offset.Zero) }
    var itemSize by remember { mutableStateOf(IntSize.Zero) }
    val name = stringResource(R.string.noahs_ark_load_ark_basket_content_description, count)
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
            .size(tileSize)
            .pointerInput(basketId, deckCenters, canAccept) {
                detectDragGestures(
                    onDragEnd = {
                        val releasedCenter = baseTopLeft + dragOffset + Offset(itemSize.width / 2f, itemSize.height / 2f)
                        val nearestIndex = deckCenters.indices.minByOrNull { (releasedCenter - deckCenters[it]).getDistance() }
                        val distance = nearestIndex?.let { (releasedCenter - deckCenters[it]).getDistance() } ?: Float.MAX_VALUE
                        val fits = nearestIndex != null && canAccept(nearestIndex)
                        if (nearestIndex != null && distance <= snapRadiusPx && fits) {
                            val target = deckCenters[nearestIndex] - releasedCenter
                            scope.launch {
                                snapOffset.animateTo(
                                    target,
                                    animationSpec = if (reducedMotion) snap() else spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                                )
                                onDropped(basketId, nearestIndex)
                            }
                            scope.launch {
                                val pulseSpec: AnimationSpec<Float> = if (reducedMotion) snap() else spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                                scaleAnim.animateTo(1.15f, animationSpec = pulseSpec)
                                scaleAnim.animateTo(1f, animationSpec = pulseSpec)
                            }
                        } else {
                            // Near a deck but wouldn't fit — still notify so feedback text
                            // updates, but reset instantly (same as a radius miss), no penalty.
                            if (nearestIndex != null && distance <= snapRadiusPx) onDropped(basketId, nearestIndex)
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
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(painter = painterResource(iconRes), contentDescription = null, modifier = Modifier.size(tileSize * BASKET_ICON_FRACTION))
            Text(text = count.toString(), style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NoahsArkOrganizeArkPreview() {
    BibleAdventuresTheme {
        NoahsArkOrganizeArkContent(
            groupFillState = GroupFillGameState(
                families = listOf(FamilyGroup("basket_0_0", 4), FamilyGroup("basket_0_1", 6)),
                circleTargets = NoahsArkContent.loadArkDeckTargets,
            ),
            basketSupplyKinds = mapOf(
                "basket_0_0" to "bread",
                "basket_0_1" to "fruit",
            ),
            characterCustomization = CharacterCustomization(),
            onBasketDropped = { _, _ -> },
            onContinue = {},
            onBackToMainMenu = {},
        )
    }
}
