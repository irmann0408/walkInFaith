package com.bibleadventures.ui.screens.scripturecards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bibleadventures.R
import com.bibleadventures.game.rewards.RewardCatalog
import com.bibleadventures.game.stories.ChapterCatalog
import com.bibleadventures.ui.AppViewModelProvider
import com.bibleadventures.ui.components.LockedNodeOverlay
import com.bibleadventures.ui.components.ScriptureCardView
import com.bibleadventures.ui.theme.BibleAdventuresTheme

@Composable
fun ScriptureCardsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ScriptureCardsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ScriptureCardsContent(cards = uiState.cards, onBack = onBack, modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScriptureCardsContent(
    cards: List<ScriptureCardUiState>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.scripture_cards_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(cards) { cardUiState -> ScriptureCardTile(cardUiState) }
        }
    }
}

@Composable
private fun ScriptureCardTile(state: ScriptureCardUiState, modifier: Modifier = Modifier) {
    if (state.earned) {
        ScriptureCardView(
            reference = state.card.reference,
            text = stringResource(state.card.textRes),
            modifier = modifier.fillMaxWidth(),
        )
    } else {
        // Reference and text are the collectible content, so a locked card shows
        // neither — only the badge's title is shown locked, since a title isn't
        // much of a spoiler while a scripture verse's text is.
        val prerequisiteTitle = stringResource(
            ChapterCatalog.all.first { it.id == state.card.chapterId }.titleRes,
        )
        val tileContentDescription = stringResource(R.string.scripture_card_locked_content_description, prerequisiteTitle)

        Card(
            modifier = modifier
                .fillMaxWidth()
                .semantics { contentDescription = tileContentDescription },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.scripture_card_locked_placeholder),
                    style = MaterialTheme.typography.bodyLarge,
                )
                LockedNodeOverlay()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ScriptureCardsPreview() {
    BibleAdventuresTheme {
        ScriptureCardsContent(
            cards = RewardCatalog.scriptureCards.mapIndexed { index, card ->
                ScriptureCardUiState(card = card, earned = index == 0)
            },
            onBack = {},
        )
    }
}
