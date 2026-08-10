package com.bibleadventures.ui.screens.noahsark.matching

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import com.bibleadventures.ui.screens.noahsark.NoahsArkViewModel
import com.bibleadventures.ui.theme.BibleAdventuresTheme

@Composable
fun NoahsArkMatchingScreen(
    viewModel: NoahsArkViewModel,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    NoahsArkMatchingContent(
        matchingState = uiState.matchingState,
        onItemTapped = viewModel::onMatchItemTapped,
        onContinue = onContinue,
        modifier = modifier,
    )
}

@Composable
private fun NoahsArkMatchingContent(
    matchingState: MatchingGameState,
    onItemTapped: (String) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.noahs_ark_matching_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = stringResource(R.string.noahs_ark_matching_instructions),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp),
            )

            val feedback = when (matchingState.lastOutcome) {
                MatchOutcome.CORRECT -> stringResource(R.string.feedback_great_job)
                MatchOutcome.TRY_AGAIN -> stringResource(R.string.feedback_try_another_one)
                MatchOutcome.NONE -> ""
            }
            Box(modifier = Modifier.height(32.dp)) {
                Text(text = feedback, style = MaterialTheme.typography.titleLarge)
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f, fill = false),
            ) {
                items(matchingState.items, key = { it.id }) { item ->
                    MatchTile(
                        item = item,
                        isSelected = item.id == matchingState.selectedId,
                        isMatched = item.id in matchingState.matchedIds,
                        onClick = { onItemTapped(item.id) },
                    )
                }
            }

            if (matchingState.isComplete) {
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
private fun MatchTile(item: MatchItem, isSelected: Boolean, isMatched: Boolean, onClick: () -> Unit) {
    val name = stringResource(item.contentDescriptionRes)
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent

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
            painter = painterResource(item.iconRes),
            contentDescription = null,
            modifier = Modifier.size(56.dp).alpha(if (isMatched) 0.35f else 1f),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NoahsArkMatchingPreview() {
    BibleAdventuresTheme {
        NoahsArkMatchingContent(
            matchingState = MatchingGameState(items = emptyList()),
            onItemTapped = {},
            onContinue = {},
        )
    }
}
