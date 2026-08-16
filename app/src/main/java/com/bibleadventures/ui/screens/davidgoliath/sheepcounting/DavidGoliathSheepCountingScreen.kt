package com.bibleadventures.ui.screens.davidgoliath.sheepcounting

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bibleadventures.R
import com.bibleadventures.game.puzzles.matching.MatchItem
import com.bibleadventures.game.puzzles.matching.MatchOutcome
import com.bibleadventures.game.puzzles.matching.MatchingGameState
import com.bibleadventures.ui.components.AdventureMenuButton
import com.bibleadventures.ui.components.BackToMainMenuTopBar
import com.bibleadventures.ui.screens.davidgoliath.DavidGoliathViewModel
import com.bibleadventures.ui.theme.BibleAdventuresTheme

/**
 * A themed reuse of the same memory/concentration engine as Animal Matching —
 * pairs a numeral card with a same-count sheep-group card via a shared
 * pairKey. Duplicates NoahsArkMatchingScreen's tile/grid composables rather
 * than extracting a shared component; this codebase only extracts once a
 * genuine third consumer appears.
 */
@Composable
fun DavidGoliathSheepCountingScreen(
    viewModel: DavidGoliathViewModel,
    onContinue: () -> Unit,
    onBackToMainMenu: () -> Unit,
    previouslyCompleted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DavidGoliathSheepCountingContent(
        sheepCountingState = uiState.sheepCountingState,
        onItemTapped = viewModel::onSheepCountingItemTapped,
        onContinue = onContinue,
        onBackToMainMenu = onBackToMainMenu,
        previouslyCompleted = previouslyCompleted,
        modifier = modifier,
    )
}

@Composable
private fun DavidGoliathSheepCountingContent(
    sheepCountingState: MatchingGameState,
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
                text = stringResource(R.string.david_goliath_sheep_counting_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = stringResource(R.string.david_goliath_sheep_counting_instructions),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp),
            )

            val feedback = when (sheepCountingState.lastOutcome) {
                MatchOutcome.CORRECT -> stringResource(R.string.feedback_great_job)
                MatchOutcome.TRY_AGAIN -> stringResource(R.string.feedback_try_another_one)
                MatchOutcome.NONE -> ""
            }
            Box(modifier = Modifier.height(32.dp)) {
                Text(text = feedback, style = MaterialTheme.typography.titleLarge)
            }

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                sheepCountingState.items.chunked(4).forEach { rowItems ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        rowItems.forEach { item ->
                            key(item.id) {
                                SheepCountTile(
                                    item = item,
                                    isFaceUp = sheepCountingState.isFaceUp(item.id),
                                    isSelected = item.id in sheepCountingState.selectedIds,
                                    isMatched = item.id in sheepCountingState.matchedIds,
                                    onClick = { onItemTapped(item.id) },
                                )
                            }
                        }
                    }
                }
            }

            if (previouslyCompleted && !sheepCountingState.isComplete) {
                Text(
                    text = stringResource(R.string.puzzle_already_completed_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            if (sheepCountingState.isComplete || previouslyCompleted) {
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
private fun SheepCountTile(item: MatchItem, isFaceUp: Boolean, isSelected: Boolean, isMatched: Boolean, onClick: () -> Unit) {
    val name = stringResource(item.contentDescriptionRes)
    val borderColor = if (isSelected && !isMatched) MaterialTheme.colorScheme.primary else Color.Transparent

    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(3.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(enabled = !isMatched, onClickLabel = name, onClick = onClick)
            .semantics { contentDescription = name },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = if (isFaceUp) painterResource(item.iconRes) else painterResource(R.drawable.ic_card_back),
            contentDescription = null,
            modifier = Modifier.size(56.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DavidGoliathSheepCountingPreview() {
    BibleAdventuresTheme {
        DavidGoliathSheepCountingContent(
            sheepCountingState = MatchingGameState(items = emptyList()),
            onItemTapped = {},
            onContinue = {},
            onBackToMainMenu = {},
        )
    }
}
