package com.bibleadventures.ui.screens.noahsark.organizeark

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bibleadventures.R
import com.bibleadventures.game.puzzles.dragsort.DragSortGameState
import com.bibleadventures.game.puzzles.dragsort.SortCategory
import com.bibleadventures.game.puzzles.dragsort.SortableItem
import com.bibleadventures.ui.components.AdventureMenuButton
import com.bibleadventures.ui.screens.noahsark.NoahsArkViewModel
import com.bibleadventures.ui.theme.BibleAdventuresTheme
import kotlin.math.roundToInt

@Composable
fun NoahsArkOrganizeArkScreen(
    viewModel: NoahsArkViewModel,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    NoahsArkOrganizeArkContent(
        dragSortState = uiState.dragSortState,
        onItemDropped = viewModel::onSortItemDropped,
        onContinue = onContinue,
        modifier = modifier,
    )
}

@Composable
private fun NoahsArkOrganizeArkContent(
    dragSortState: DragSortGameState,
    onItemDropped: (itemId: String, categoryKey: String) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val categoryBounds = remember { mutableStateMapOf<String, Rect>() }
    val unplacedItems = dragSortState.items.filter { it.id !in dragSortState.placedItems.keys }

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
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
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
            )

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

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(unplacedItems, key = { it.id }) { item ->
                    DraggableSortItem(
                        item = item,
                        categoryBounds = categoryBounds,
                        onDropped = { categoryKey -> onItemDropped(item.id, categoryKey) },
                    )
                }
            }

            if (dragSortState.isComplete) {
                AdventureMenuButton(
                    text = stringResource(R.string.action_continue),
                    onClick = onContinue,
                    modifier = Modifier.padding(top = 32.dp),
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
    categoryBounds: Map<String, Rect>,
    onDropped: (categoryKey: String) -> Unit,
) {
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var baseTopLeft by remember { mutableStateOf(Offset.Zero) }
    var itemSize by remember { mutableStateOf(IntSize.Zero) }
    val name = stringResource(item.contentDescriptionRes)

    Box(
        modifier = Modifier
            .onGloballyPositioned { coords ->
                baseTopLeft = coords.positionInRoot()
                itemSize = coords.size
            }
            .offset { IntOffset(dragOffset.x.roundToInt(), dragOffset.y.roundToInt()) }
            .size(64.dp)
            .pointerInput(item.id) {
                detectDragGestures(
                    onDragEnd = {
                        val center = baseTopLeft + dragOffset +
                            Offset(itemSize.width / 2f, itemSize.height / 2f)
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
        Image(painter = painterResource(item.iconRes), contentDescription = null, modifier = Modifier.size(56.dp))
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
        )
    }
}
