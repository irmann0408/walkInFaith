package com.bibleadventures.ui.screens.feeding5000.boysgift

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.bibleadventures.ui.components.AdventureMenuButton
import com.bibleadventures.ui.components.BackToMainMenuTopBar
import com.bibleadventures.ui.screens.feeding5000.Feeding5000ViewModel
import com.bibleadventures.ui.theme.BibleAdventuresTheme

/**
 * Reuses `hiddenobject` again, at a new parameterization: instead of blindly
 * dragging items until a quota is filled (too close to the old, rejected
 * Setting Up Camp shape), this is a small search among decoys — find exactly
 * 5 barley loaves among similar-looking stones, and 2 fish among frog
 * decoys. Decoys ([Feeding5000Content.boysGiftDecoys]) are rendered but
 * never wired to [onItemTapped] — tapping one is a harmless no-op.
 */
@Composable
fun Feeding5000BoysGiftScreen(
    viewModel: Feeding5000ViewModel,
    onContinue: () -> Unit,
    onBackToMainMenu: () -> Unit,
    previouslyCompleted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Feeding5000BoysGiftContent(
        hiddenObjectState = uiState.boysGiftState,
        onItemTapped = viewModel::onBoysGiftItemTapped,
        onContinue = onContinue,
        onBackToMainMenu = onBackToMainMenu,
        previouslyCompleted = previouslyCompleted,
        modifier = modifier,
    )
}

@Composable
private fun Feeding5000BoysGiftContent(
    hiddenObjectState: HiddenObjectGameState,
    onItemTapped: (String) -> Unit,
    onContinue: () -> Unit,
    onBackToMainMenu: () -> Unit,
    previouslyCompleted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val loavesFound = hiddenObjectState.foundIds.count { it.startsWith("loaf_") }
    val fishFound = hiddenObjectState.foundIds.count { it.startsWith("fish_") }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { if (previouslyCompleted) BackToMainMenuTopBar(onBackToMainMenu) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.feeding_5000_boys_gift_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = stringResource(R.string.feeding_5000_boys_gift_instructions),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = stringResource(R.string.feeding_5000_boys_gift_progress_label, loavesFound, fishFound),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
            )

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
            ) {
                Image(
                    painter = painterResource(R.drawable.bg_feeding_basket),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )

                // Decoys render first, underneath, and are never wired to onItemTapped —
                // a harmless no-op by construction, not something the engine defends against.
                Feeding5000Content.boysGiftDecoys.forEach { decoy ->
                    DecoyTarget(
                        decoy = decoy,
                        modifier = Modifier.offset(x = maxWidth * decoy.position.x, y = maxHeight * decoy.position.y),
                    )
                }

                hiddenObjectState.items.forEach { item ->
                    RealItemTarget(
                        item = item,
                        isFound = item.id in hiddenObjectState.foundIds,
                        onClick = { onItemTapped(item.id) },
                        modifier = Modifier.offset(x = maxWidth * item.position.x, y = maxHeight * item.position.y),
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

            if (hiddenObjectState.isComplete || previouslyCompleted) {
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
private fun RealItemTarget(
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
            Icon(imageVector = Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        } else {
            Image(
                painter = painterResource(item.iconRes),
                contentDescription = null,
                modifier = Modifier.size(36.dp).alpha(0.9f),
            )
        }
    }
}

/** Purely decorative — no click handling at all, so a tap here can never register as a find. */
@Composable
private fun DecoyTarget(decoy: DecoyItem, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(decoy.iconRes),
        contentDescription = null,
        modifier = modifier.size(36.dp).alpha(0.9f),
    )
}

@Preview(showBackground = true)
@Composable
private fun Feeding5000BoysGiftPreview() {
    BibleAdventuresTheme {
        Feeding5000BoysGiftContent(
            hiddenObjectState = HiddenObjectGameState(items = Feeding5000ViewModel.boysGiftRealItems),
            onItemTapped = {},
            onContinue = {},
            onBackToMainMenu = {},
        )
    }
}
