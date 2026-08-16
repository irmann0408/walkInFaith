package com.bibleadventures.ui.screens.esther.royalattire

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.bibleadventures.game.stories.DecoyItem
import com.bibleadventures.game.stories.EstherContent
import com.bibleadventures.ui.components.AdventureMenuButton
import com.bibleadventures.ui.components.BackToMainMenuTopBar
import com.bibleadventures.ui.screens.esther.EstherViewModel
import com.bibleadventures.ui.theme.BibleAdventuresTheme

@Composable
fun EstherRoyalAttireScreen(
    viewModel: EstherViewModel,
    onContinue: () -> Unit,
    onBackToMainMenu: () -> Unit,
    previouslyCompleted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    EstherRoyalAttireContent(
        hiddenObjectState = uiState.hiddenObjectState,
        onItemTapped = viewModel::onAttireItemTapped,
        onContinue = onContinue,
        onBackToMainMenu = onBackToMainMenu,
        previouslyCompleted = previouslyCompleted,
        modifier = modifier,
    )
}

@Composable
private fun EstherRoyalAttireContent(
    hiddenObjectState: HiddenObjectGameState,
    onItemTapped: (String) -> Unit,
    onContinue: () -> Unit,
    onBackToMainMenu: () -> Unit,
    previouslyCompleted: Boolean = false,
    modifier: Modifier = Modifier,
) {
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
                text = stringResource(R.string.esther_new_queen_royal_attire_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = stringResource(R.string.esther_new_queen_royal_attire_instructions),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
            )

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
            ) {
                Image(
                    painter = painterResource(R.drawable.bg_esther_new_queen_chamber),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )

                // Decoys render first, underneath, and are never wired to
                // onItemTapped — a harmless no-op by construction, not
                // something the engine defends against (same pattern as
                // Feeding the 5,000's crowd/basket decoys).
                EstherContent.royalAttireDecoys.forEach { decoy ->
                    RoyalAttireDecoyTarget(
                        decoy = decoy,
                        modifier = Modifier.offset(x = maxWidth * decoy.position.x, y = maxHeight * decoy.position.y),
                    )
                }

                hiddenObjectState.items.forEach { item ->
                    AttireItemTarget(
                        item = item,
                        isFound = item.id in hiddenObjectState.foundIds,
                        onClick = { onItemTapped(item.id) },
                        modifier = Modifier.offset(x = maxWidth * item.position.x, y = maxHeight * item.position.y),
                    )
                }
            }

            RemainingAttireChecklist(
                items = hiddenObjectState.items,
                foundIds = hiddenObjectState.foundIds,
                modifier = Modifier.padding(top = 16.dp),
            )

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
private fun AttireItemTarget(
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
            Image(
                painter = painterResource(item.iconRes),
                contentDescription = null,
                modifier = Modifier.size(32.dp).alpha(0.85f),
            )
        }
    }
}

/** Purely decorative — no click handling at all, so a tap here can never register as a find. */
@Composable
private fun RoyalAttireDecoyTarget(decoy: DecoyItem, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(decoy.iconRes),
        contentDescription = null,
        modifier = modifier.size(32.dp).alpha(0.85f),
    )
}

/**
 * A live checklist of what's still hidden, named up front with its icon
 * (unlike the on-image labels, which stay hidden until found so they don't
 * give away *where* to look) — the icon is what a young player is actually
 * scanning the scene for, so pairing it with the word (not just the word
 * alone) is what makes the checklist usable. Each entry disappears the
 * instant that item is found, derived straight from
 * [HiddenObjectGameState.foundIds] with no new engine/ViewModel state
 * needed.
 */
@Composable
private fun RemainingAttireChecklist(items: List<HiddenItem>, foundIds: Set<String>, modifier: Modifier = Modifier) {
    val remaining = items.filter { it.id !in foundIds }
    if (remaining.isEmpty()) return

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.esther_new_queen_royal_attire_checklist_title),
            style = MaterialTheme.typography.titleMedium,
        )
        remaining.forEach { item ->
            val name = stringResource(item.contentDescriptionRes)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .padding(top = 4.dp)
                    .semantics(mergeDescendants = true) {},
            ) {
                Image(
                    painter = painterResource(item.iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EstherRoyalAttirePreview() {
    BibleAdventuresTheme {
        EstherRoyalAttireContent(
            hiddenObjectState = HiddenObjectGameState(items = emptyList()),
            onItemTapped = {},
            onContinue = {},
            onBackToMainMenu = {},
        )
    }
}
