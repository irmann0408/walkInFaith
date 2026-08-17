package com.bibleadventures.ui.screens.noahsark.findanimals

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bibleadventures.R
import com.bibleadventures.game.stories.AnimalDef
import com.bibleadventures.game.stories.DecoyItemDef
import com.bibleadventures.game.stories.NoahsArkContent
import com.bibleadventures.ui.LocalReducedMotion
import com.bibleadventures.ui.components.PuzzleTopBar
import com.bibleadventures.ui.screens.noahsark.DecoyTapOutcome
import com.bibleadventures.ui.screens.noahsark.NoahsArkViewModel
import com.bibleadventures.ui.theme.BibleAdventuresTheme

private val ANIMAL_TILE_ICON_SIZE = 88.dp
private const val ANIMAL_TILE_IMAGE_FRACTION = 72f / 88f
private val ANIMAL_TILE_LABEL_HEIGHT = 20.dp

@Composable
fun NoahsArkFindAnimalsScreen(
    viewModel: NoahsArkViewModel,
    onContinue: () -> Unit,
    onBackToMainMenu: () -> Unit,
    previouslyCompleted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    NoahsArkFindAnimalsContent(
        foundAnimalIds = uiState.foundAnimalIds,
        decoyOutcome = uiState.lastFindAnimalsDecoyOutcome,
        order = uiState.findAnimalsOrder,
        onAnimalTapped = viewModel::onAnimalFound,
        onDecoyTapped = viewModel::onFindAnimalsDecoyTapped,
        onContinue = onContinue,
        onBackToMainMenu = onBackToMainMenu,
        previouslyCompleted = previouslyCompleted,
        modifier = modifier,
    )
}

@Composable
private fun NoahsArkFindAnimalsContent(
    foundAnimalIds: Set<String>,
    decoyOutcome: DecoyTapOutcome,
    order: List<String>,
    onAnimalTapped: (String) -> Unit,
    onDecoyTapped: () -> Unit,
    onContinue: () -> Unit,
    onBackToMainMenu: () -> Unit,
    previouslyCompleted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val allFound = foundAnimalIds.size == NoahsArkContent.animals.size

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            if (previouslyCompleted || allFound) {
                PuzzleTopBar(
                    showBackButton = previouslyCompleted,
                    onBackToMainMenu = onBackToMainMenu,
                    showNextButton = allFound || previouslyCompleted,
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
                text = stringResource(R.string.noahs_ark_find_animals_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = stringResource(R.string.noahs_ark_find_animals_instructions),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp),
            )

            val feedback = if (decoyOutcome == DecoyTapOutcome.DECOY_TAPPED) {
                stringResource(R.string.feedback_not_an_animal)
            } else {
                ""
            }
            Box(modifier = Modifier.height(32.dp)) {
                Text(text = feedback, style = MaterialTheme.typography.titleLarge)
            }

            // `order` is shuffled once per fresh game (NoahsArkViewModel.createInitialState),
            // so the layout isn't the same every playthrough. Mixes in the decoy's
            // position too, not just which tiles are real animals.
            val tiles: List<@Composable (Dp) -> Unit> = buildList {
                order.forEach { id ->
                    val animal = NoahsArkContent.animals.find { it.id == id }
                    if (animal != null) {
                        add { iconSize ->
                            AnimalTile(
                                animal = animal,
                                iconSize = iconSize,
                                isFound = animal.id in foundAnimalIds,
                                onClick = { onAnimalTapped(animal.id) },
                            )
                        }
                    } else {
                        val decoy = NoahsArkContent.findAnimalsDecoys.first { it.id == id }
                        add { iconSize -> DecoyTile(decoy = decoy, iconSize = iconSize, onClick = onDecoyTapped) }
                    }
                }
            }

            // A static wrapped grid, not a lazily-virtualized one — every tile stays in
            // the tree regardless of scroll position (see Gather Supplies' history).
            // weight(1f, fill = true) hands this region exactly the space left over
            // after every other sibling above claims its natural size, and
            // BoxWithConstraints reads that resolved space to compute a tile icon
            // size that makes the whole grid (icons + their labels) fit — shrinking
            // below ANIMAL_TILE_ICON_SIZE only when there isn't room for it, never
            // overflowing.
            BoxWithConstraints(modifier = Modifier.fillMaxWidth().weight(1f, fill = true)) {
                val columns = 3
                val rows = ((tiles.size + columns - 1) / columns).coerceAtLeast(1)
                val spacing = 16.dp
                val iconSize = minOf(
                    (maxWidth - spacing * (columns - 1)) / columns,
                    (maxHeight - spacing * (rows - 1) - ANIMAL_TILE_LABEL_HEIGHT * rows) / rows,
                ).coerceIn(48.dp, ANIMAL_TILE_ICON_SIZE)

                Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
                    tiles.chunked(columns).forEach { rowTiles ->
                        Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                            rowTiles.forEach { tile -> tile(iconSize) }
                        }
                    }
                }
            }

            if (previouslyCompleted && !allFound) {
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
private fun AnimalTile(animal: AnimalDef, iconSize: Dp, isFound: Boolean, onClick: () -> Unit) {
    val reducedMotion = LocalReducedMotion.current
    val alpha by animateFloatAsState(
        targetValue = if (isFound) 0.5f else 1f,
        animationSpec = if (reducedMotion) snap() else spring(),
        label = "animalFoundAlpha",
    )
    val name = stringResource(animal.nameRes)

    Column(
        modifier = Modifier
            .width(iconSize)
            .clickable(enabled = !isFound, onClickLabel = name, onClick = onClick)
            .semantics { contentDescription = name },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.size(iconSize), contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(animal.iconRes),
                contentDescription = null,
                modifier = Modifier.size(iconSize * ANIMAL_TILE_IMAGE_FRACTION).alpha(alpha),
            )
            if (isFound) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
        TileLabel(name)
    }
}

/** Always tappable, never checked off — a decoy stays recoverable forever. */
@Composable
private fun DecoyTile(decoy: DecoyItemDef, iconSize: Dp, onClick: () -> Unit) {
    val name = stringResource(decoy.nameRes)

    Column(
        modifier = Modifier
            .width(iconSize)
            .clickable(onClickLabel = name, onClick = onClick)
            .semantics { contentDescription = name },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.size(iconSize), contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(decoy.iconRes),
                contentDescription = null,
                modifier = Modifier.size(iconSize * ANIMAL_TILE_IMAGE_FRACTION),
            )
        }
        TileLabel(name)
    }
}

/**
 * Visible name caption under a tile icon. Purely a reading aid, not a second
 * accessibility announcement — the tile's own `contentDescription` already
 * covers screen readers, so this is cleared from the semantics tree.
 */
@Composable
private fun TileLabel(name: String) {
    Text(
        text = name,
        style = MaterialTheme.typography.labelMedium,
        textAlign = TextAlign.Center,
        maxLines = 1,
        modifier = Modifier.clearAndSetSemantics {},
    )
}

@Preview(showBackground = true)
@Composable
private fun NoahsArkFindAnimalsPreview() {
    BibleAdventuresTheme {
        NoahsArkFindAnimalsContent(
            foundAnimalIds = emptySet(),
            decoyOutcome = DecoyTapOutcome.NONE,
            order = NoahsArkContent.animals.map { it.id } + NoahsArkContent.findAnimalsDecoys.map { it.id },
            onAnimalTapped = {},
            onDecoyTapped = {},
            onContinue = {},
            onBackToMainMenu = {},
        )
    }
}
