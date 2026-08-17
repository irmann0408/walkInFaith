package com.bibleadventures.ui.screens.davidgoliath.choosestones

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
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
import com.bibleadventures.game.puzzles.hiddenobject.HiddenItem
import com.bibleadventures.game.puzzles.hiddenobject.HiddenObjectGameState
import com.bibleadventures.ui.components.AspectRatioFitBox
import com.bibleadventures.ui.components.PuzzleTopBar
import com.bibleadventures.ui.screens.davidgoliath.DavidGoliathViewModel
import com.bibleadventures.ui.screens.noahsark.DecoyTapOutcome
import com.bibleadventures.ui.theme.BibleAdventuresTheme

@Composable
fun DavidGoliathChooseStonesScreen(
    viewModel: DavidGoliathViewModel,
    onContinue: () -> Unit,
    onBackToMainMenu: () -> Unit,
    previouslyCompleted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DavidGoliathChooseStonesContent(
        hiddenObjectState = uiState.hiddenObjectState,
        decoyPosition = uiState.riverbedDecoyPosition,
        decoyOutcome = uiState.lastRiverbedDecoyOutcome,
        onStoneTapped = viewModel::onStoneFound,
        onDecoyTapped = viewModel::onRiverbedDecoyTapped,
        onContinue = onContinue,
        onBackToMainMenu = onBackToMainMenu,
        previouslyCompleted = previouslyCompleted,
        modifier = modifier,
    )
}

@Composable
private fun DavidGoliathChooseStonesContent(
    hiddenObjectState: HiddenObjectGameState,
    decoyPosition: Offset,
    decoyOutcome: DecoyTapOutcome,
    onStoneTapped: (String) -> Unit,
    onDecoyTapped: () -> Unit,
    onContinue: () -> Unit,
    onBackToMainMenu: () -> Unit,
    previouslyCompleted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            if (previouslyCompleted || hiddenObjectState.isComplete) {
                PuzzleTopBar(
                    showBackButton = previouslyCompleted,
                    onBackToMainMenu = onBackToMainMenu,
                    showNextButton = hiddenObjectState.isComplete || previouslyCompleted,
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
                text = stringResource(R.string.david_goliath_choose_stones_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = stringResource(R.string.david_goliath_choose_stones_instructions),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp),
            )

            val feedback = if (decoyOutcome == DecoyTapOutcome.DECOY_TAPPED) {
                stringResource(R.string.feedback_not_a_stone)
            } else {
                ""
            }
            Box(modifier = Modifier.height(32.dp)) {
                Text(text = feedback, style = MaterialTheme.typography.titleLarge)
            }

            // weight(1f, fill = true) hands this element exactly the space left
            // over after every other (naturally-sized) sibling in this Column,
            // and AspectRatioFitBox letterbox-fits within that bounded box, so
            // nothing here ever needs to scroll. The nested BoxWithConstraints
            // re-reads the fitted box's own size so item positions below can
            // still be placed as fractions of it.
            AspectRatioFitBox(ratio = 1f, modifier = Modifier.weight(1f, fill = true).fillMaxSize()) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(R.drawable.bg_david_goliath_riverbed),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    hiddenObjectState.items.forEach { item ->
                        HiddenStoneTarget(
                            item = item,
                            isFound = item.id in hiddenObjectState.foundIds,
                            onClick = { onStoneTapped(item.id) },
                            modifier = Modifier.offset(x = maxWidth * item.position.x, y = maxHeight * item.position.y),
                        )
                    }
                    RiverbedDecoyTarget(
                        onClick = onDecoyTapped,
                        modifier = Modifier.offset(x = maxWidth * decoyPosition.x, y = maxHeight * decoyPosition.y),
                    )
                }
            }

            if (previouslyCompleted && !hiddenObjectState.isComplete) {
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
private fun HiddenStoneTarget(
    item: HiddenItem,
    isFound: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val name = stringResource(item.contentDescriptionRes)

    Box(
        modifier = modifier
            .size(48.dp)
            .clickable(enabled = !isFound, onClickLabel = name, onClick = onClick)
            .semantics { contentDescription = name },
        contentAlignment = Alignment.Center,
    ) {
        if (isFound) {
            // Label only appears once found — showing it upfront would give away what
            // to search for and undo the point of hiding it in the first place.
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(imageVector = Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = name,
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.clearAndSetSemantics {},
                )
            }
        } else {
            // Smaller and slightly translucent so items blend into the busy background
            // (spec section 9's "avoid frustrating pixel-hunting" still holds: only the
            // visual icon shrinks, the 48dp tap target above does not).
            Image(
                painter = painterResource(item.iconRes),
                contentDescription = null,
                modifier = Modifier.size(32.dp).alpha(0.85f),
            )
        }
    }
}

/**
 * Camouflaged the same way as the real stones and never "solved" — always
 * tappable, no checkmark, no label reveal (revealing its name would make it
 * stand out from the still-hidden stones and give away which item is fake).
 */
@Composable
private fun RiverbedDecoyTarget(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val name = stringResource(R.string.decoy_boot)

    Box(
        modifier = modifier
            .size(48.dp)
            .clickable(onClickLabel = name, onClick = onClick)
            .semantics { contentDescription = name },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_decoy_boot),
            contentDescription = null,
            modifier = Modifier.size(32.dp).alpha(0.85f),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DavidGoliathChooseStonesPreview() {
    BibleAdventuresTheme {
        DavidGoliathChooseStonesContent(
            hiddenObjectState = HiddenObjectGameState(items = emptyList()),
            decoyPosition = Offset(0.5f, 0.5f),
            decoyOutcome = DecoyTapOutcome.NONE,
            onStoneTapped = {},
            onDecoyTapped = {},
            onContinue = {},
            onBackToMainMenu = {},
        )
    }
}
