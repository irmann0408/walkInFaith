package com.bibleadventures.ui.screens.noahsark.organizeark

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bibleadventures.R
import com.bibleadventures.game.puzzles.dragsort.DragSortGameState
import com.bibleadventures.game.puzzles.dragsort.SortCategory
import com.bibleadventures.game.puzzles.dragsort.SortOutcome
import com.bibleadventures.game.puzzles.dragsort.SortableItem
import com.bibleadventures.ui.components.PuzzleTopBar
import com.bibleadventures.ui.screens.noahsark.NoahsArkViewModel
import com.bibleadventures.ui.theme.BibleAdventuresTheme
import kotlin.math.roundToInt

private val SORT_ITEM_SIZE = 64.dp
private const val SORT_ITEM_IMAGE_FRACTION = 56f / 64f
private val SORT_ITEM_LABEL_HEIGHT = 20.dp

@Composable
fun NoahsArkOrganizeArkScreen(
    viewModel: NoahsArkViewModel,
    onContinue: () -> Unit,
    onBackToMainMenu: () -> Unit,
    previouslyCompleted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    NoahsArkOrganizeArkContent(
        dragSortState = uiState.dragSortState,
        onItemDropped = viewModel::onSortItemDropped,
        onContinue = onContinue,
        onBackToMainMenu = onBackToMainMenu,
        previouslyCompleted = previouslyCompleted,
        modifier = modifier,
    )
}

@Composable
private fun NoahsArkOrganizeArkContent(
    dragSortState: DragSortGameState,
    onItemDropped: (itemId: String, categoryKey: String) -> Unit,
    onContinue: () -> Unit,
    onBackToMainMenu: () -> Unit,
    previouslyCompleted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val categoryBounds = remember { mutableStateMapOf<String, Rect>() }
    val unplacedItems = dragSortState.items.filter { it.id !in dragSortState.placedItems.keys }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            if (previouslyCompleted || dragSortState.isComplete) {
                PuzzleTopBar(
                    showBackButton = previouslyCompleted,
                    onBackToMainMenu = onBackToMainMenu,
                    showNextButton = dragSortState.isComplete || previouslyCompleted,
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

            val feedback = when (dragSortState.lastOutcome) {
                SortOutcome.CORRECT -> stringResource(R.string.feedback_great_job)
                SortOutcome.TRY_AGAIN -> stringResource(R.string.feedback_try_another_one)
                SortOutcome.NOT_SORTABLE -> stringResource(R.string.feedback_doesnt_belong)
                SortOutcome.NONE -> ""
            }
            Box(modifier = Modifier.height(32.dp).padding(bottom = 8.dp)) {
                Text(text = feedback, style = MaterialTheme.typography.titleLarge)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                dragSortState.categories.forEach { category ->
                    CategoryDropZone(
                        category = category,
                        modifier = Modifier
                            .weight(1f)
                            .onGloballyPositioned { coords ->
                                categoryBounds[category.key] = coords.boundsInRoot()
                            },
                    )
                }
            }

            // A static wrapped grid — every item is visible at once (spec section
            // 13's "simple, discoverable navigation"). weight(1f, fill = true)
            // hands this region exactly the space left over after every other
            // sibling above claims its natural size, and BoxWithConstraints reads
            // that resolved space to compute an item size that makes the whole
            // tray fit — shrinking below SORT_ITEM_SIZE only when there isn't room
            // for it, never overflowing. Drag/drop detection reads live positions
            // via onGloballyPositioned, so a smaller item size doesn't affect it.
            BoxWithConstraints(modifier = Modifier.fillMaxWidth().weight(1f, fill = true).padding(top = 32.dp)) {
                val columns = 4
                val rows = ((unplacedItems.size + columns - 1) / columns).coerceAtLeast(1)
                val spacing = 16.dp
                val itemSize = minOf(
                    (maxWidth - spacing * (columns - 1)) / columns,
                    (maxHeight - spacing * (rows - 1) - SORT_ITEM_LABEL_HEIGHT * rows) / rows,
                ).coerceIn(48.dp, SORT_ITEM_SIZE)

                Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
                    unplacedItems.chunked(columns).forEach { rowItems ->
                        Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                            rowItems.forEach { item ->
                                key(item.id) {
                                    DraggableSortItem(
                                        item = item,
                                        itemSize = itemSize,
                                        categoryBounds = categoryBounds,
                                        onDropped = { categoryKey -> onItemDropped(item.id, categoryKey) },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (previouslyCompleted && !dragSortState.isComplete) {
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
private fun CategoryDropZone(category: SortCategory, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(96.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = stringResource(category.labelRes), style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun DraggableSortItem(
    item: SortableItem,
    itemSize: Dp,
    categoryBounds: Map<String, Rect>,
    onDropped: (categoryKey: String) -> Unit,
) {
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var baseTopLeft by remember { mutableStateOf(Offset.Zero) }
    var boxSizePx by remember { mutableStateOf(IntSize.Zero) }
    val name = stringResource(item.contentDescriptionRes)

    // The label sits below the draggable box rather than inside it, so it doesn't
    // affect the drag/drop hit-testing math (which uses the box's own center).
    Column(modifier = Modifier.width(itemSize), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .onGloballyPositioned { coords ->
                    baseTopLeft = coords.positionInRoot()
                    boxSizePx = coords.size
                }
                .offset { IntOffset(dragOffset.x.roundToInt(), dragOffset.y.roundToInt()) }
                .size(itemSize)
                .pointerInput(item.id) {
                    detectDragGestures(
                        onDragEnd = {
                            val center = baseTopLeft + dragOffset +
                                Offset(boxSizePx.width / 2f, boxSizePx.height / 2f)
                            val targetCategory = categoryBounds.entries.firstOrNull { (_, rect) -> rect.contains(center) }?.key
                            if (targetCategory != null) {
                                onDropped(targetCategory)
                            }
                            dragOffset = Offset.Zero
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
            Image(painter = painterResource(item.iconRes), contentDescription = null, modifier = Modifier.size(itemSize * SORT_ITEM_IMAGE_FRACTION))
        }
        Text(
            text = name,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.clearAndSetSemantics {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NoahsArkOrganizeArkPreview() {
    BibleAdventuresTheme {
        NoahsArkOrganizeArkContent(
            dragSortState = DragSortGameState(items = emptyList(), categories = emptyList()),
            onItemDropped = { _, _ -> },
            onContinue = {},
            onBackToMainMenu = {},
        )
    }
}
