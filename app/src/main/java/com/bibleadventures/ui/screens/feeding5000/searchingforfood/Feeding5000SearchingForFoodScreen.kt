package com.bibleadventures.ui.screens.feeding5000.searchingforfood

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bibleadventures.R
import com.bibleadventures.game.puzzles.hiddenobject.HiddenItem
import com.bibleadventures.game.puzzles.hiddenobject.HiddenObjectGameState
import com.bibleadventures.game.stories.DecoyItem
import com.bibleadventures.game.stories.Feeding5000Content
import com.bibleadventures.ui.components.AspectRatioFitBox
import com.bibleadventures.ui.components.PuzzleTopBar
import com.bibleadventures.ui.screens.feeding5000.Feeding5000ViewModel
import com.bibleadventures.ui.theme.BibleAdventuresTheme

/**
 * Reuses `hiddenobject` exactly as Esther's Royal Attire does — a single
 * real target (the boy) — now surrounded by 20 non-interactive crowd
 * figures ([Feeding5000Content.searchingForFoodDecoys], 5 robe-color
 * variants) so the boy isn't simply the only person on screen. The basket
 * ([R.drawable.ic_boy_with_basket] vs. the crowd's plain robes) is the only
 * thing that tells him apart.
 */
@Composable
fun Feeding5000SearchingForFoodScreen(
    viewModel: Feeding5000ViewModel,
    onContinue: () -> Unit,
    onBackToMainMenu: () -> Unit,
    previouslyCompleted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Feeding5000SearchingForFoodContent(
        hiddenObjectState = uiState.searchingState,
        onItemTapped = viewModel::onBoyFound,
        onContinue = onContinue,
        onBackToMainMenu = onBackToMainMenu,
        previouslyCompleted = previouslyCompleted,
        modifier = modifier,
    )
}

@Composable
private fun Feeding5000SearchingForFoodContent(
    hiddenObjectState: HiddenObjectGameState,
    onItemTapped: (String) -> Unit,
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
                text = stringResource(R.string.feeding_5000_searching_for_food_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = stringResource(R.string.feeding_5000_searching_for_food_instructions),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
            )

            // weight(1f, fill = true) hands this element exactly the space left
            // over after every other (naturally-sized) sibling in this Column,
            // and AspectRatioFitBox letterbox-fits within that bounded box, so
            // nothing here ever needs to scroll. The nested BoxWithConstraints
            // re-reads the fitted box's own size so item positions below can
            // still be placed as fractions of it.
            AspectRatioFitBox(ratio = 1f, modifier = Modifier.weight(1f, fill = true).fillMaxSize()) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(R.drawable.bg_feeding_hillside),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )

                    // Crowd fill renders first, underneath, and is never wired to
                    // onItemTapped — a harmless no-op by construction, same
                    // screen-level-only decoy pattern as The Boy's Gift.
                    Feeding5000Content.searchingForFoodDecoys.forEach { decoy ->
                        CrowdDecoyTarget(
                            decoy = decoy,
                            modifier = Modifier.offset(x = maxWidth * decoy.position.x, y = maxHeight * decoy.position.y),
                        )
                    }

                    hiddenObjectState.items.forEach { item ->
                        SearchItemTarget(
                            item = item,
                            isFound = item.id in hiddenObjectState.foundIds,
                            onClick = { onItemTapped(item.id) },
                            modifier = Modifier.offset(x = maxWidth * item.position.x, y = maxHeight * item.position.y),
                        )
                    }
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
private fun SearchItemTarget(
    item: HiddenItem,
    isFound: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val name = stringResource(item.contentDescriptionRes)

    Box(
        modifier = modifier
            .size(56.dp)
            .clickable(enabled = !isFound, onClickLabel = name, onClick = onClick)
            .semantics { contentDescription = name },
        contentAlignment = Alignment.Center,
    ) {
        if (isFound) {
            Icon(imageVector = Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        } else {
            Image(
                painter = painterResource(item.iconRes),
                contentDescription = null,
                modifier = Modifier.size(40.dp).alpha(0.9f),
            )
        }
    }
}

/** Purely decorative — no click handling at all, so a tap here can never register as a find. */
@Composable
private fun CrowdDecoyTarget(decoy: DecoyItem, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(decoy.iconRes),
        contentDescription = null,
        modifier = modifier.size(40.dp).alpha(0.9f),
    )
}

@Preview(showBackground = true)
@Composable
private fun Feeding5000SearchingForFoodPreview() {
    BibleAdventuresTheme {
        Feeding5000SearchingForFoodContent(
            hiddenObjectState = HiddenObjectGameState(
                items = listOf(
                    HiddenItem("boy", Offset(0.5f, 0.5f), R.drawable.ic_boy_with_basket, R.string.feeding_5000_searching_for_food_boy_content_description),
                ),
            ),
            onItemTapped = {},
            onContinue = {},
            onBackToMainMenu = {},
        )
    }
}
