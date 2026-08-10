package com.bibleadventures.ui.screens.noahsark.findanimals

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bibleadventures.R
import com.bibleadventures.game.stories.AnimalDef
import com.bibleadventures.game.stories.DecoyItemDef
import com.bibleadventures.game.stories.NoahsArkContent
import com.bibleadventures.ui.components.AdventureMenuButton
import com.bibleadventures.ui.screens.noahsark.DecoyTapOutcome
import com.bibleadventures.ui.screens.noahsark.NoahsArkViewModel
import com.bibleadventures.ui.theme.BibleAdventuresTheme

@Composable
fun NoahsArkFindAnimalsScreen(
    viewModel: NoahsArkViewModel,
    onContinue: () -> Unit,
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
    modifier: Modifier = Modifier,
) {
    val allFound = foundAnimalIds.size == NoahsArkContent.animals.size

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
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
            val tiles: List<@Composable () -> Unit> = buildList {
                order.forEach { id ->
                    val animal = NoahsArkContent.animals.find { it.id == id }
                    if (animal != null) {
                        add {
                            AnimalTile(
                                animal = animal,
                                isFound = animal.id in foundAnimalIds,
                                onClick = { onAnimalTapped(animal.id) },
                            )
                        }
                    } else {
                        val decoy = NoahsArkContent.findAnimalsDecoys.first { it.id == id }
                        add { DecoyTile(decoy = decoy, onClick = onDecoyTapped) }
                    }
                }
            }

            // A static wrapped grid, not a lazily-virtualized one — every tile stays in
            // the tree regardless of scroll position (see Gather Supplies' history).
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                tiles.chunked(3).forEach { rowTiles ->
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        rowTiles.forEach { tile -> tile() }
                    }
                }
            }

            if (allFound) {
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
private fun AnimalTile(animal: AnimalDef, isFound: Boolean, onClick: () -> Unit) {
    val alpha by animateFloatAsState(targetValue = if (isFound) 0.5f else 1f, label = "animalFoundAlpha")
    val name = stringResource(animal.nameRes)

    Column(
        modifier = Modifier
            .width(88.dp)
            .clickable(enabled = !isFound, onClickLabel = name, onClick = onClick)
            .semantics { contentDescription = name },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.size(88.dp), contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(animal.iconRes),
                contentDescription = null,
                modifier = Modifier.size(72.dp).alpha(alpha),
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
private fun DecoyTile(decoy: DecoyItemDef, onClick: () -> Unit) {
    val name = stringResource(decoy.nameRes)

    Column(
        modifier = Modifier
            .width(88.dp)
            .clickable(onClickLabel = name, onClick = onClick)
            .semantics { contentDescription = name },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.size(88.dp), contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(decoy.iconRes),
                contentDescription = null,
                modifier = Modifier.size(72.dp),
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
        )
    }
}
