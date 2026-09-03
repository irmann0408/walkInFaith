package com.bibleadventures.ui.screens.noahsark.matching

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bibleadventures.R
import com.bibleadventures.domain.model.CharacterCustomization
import com.bibleadventures.game.puzzles.matching.MatchItem
import com.bibleadventures.game.puzzles.matching.MatchOutcome
import com.bibleadventures.game.puzzles.matching.MatchingGameState
import com.bibleadventures.ui.components.CharacterCallout
import com.bibleadventures.ui.components.Posture
import com.bibleadventures.ui.components.PuzzleTopBar
import com.bibleadventures.ui.screens.noahsark.NoahsArkViewModel
import com.bibleadventures.ui.theme.BibleAdventuresTheme

private val MATCH_TILE_SIZE = 72.dp
private const val MATCH_TILE_ICON_FRACTION = 56f / 72f

@Composable
fun NoahsArkMatchingScreen(
    viewModel: NoahsArkViewModel,
    onContinue: () -> Unit,
    onBackToMainMenu: () -> Unit,
    previouslyCompleted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val characterCustomization by viewModel.characterCustomization.collectAsStateWithLifecycle()

    NoahsArkMatchingContent(
        matchingState = uiState.matchingState,
        characterCustomization = characterCustomization,
        onItemTapped = viewModel::onMatchItemTapped,
        onContinue = onContinue,
        onBackToMainMenu = onBackToMainMenu,
        previouslyCompleted = previouslyCompleted,
        modifier = modifier,
    )
}

@Composable
private fun NoahsArkMatchingContent(
    matchingState: MatchingGameState,
    characterCustomization: CharacterCustomization,
    onItemTapped: (String) -> Unit,
    onContinue: () -> Unit,
    onBackToMainMenu: () -> Unit,
    previouslyCompleted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            if (previouslyCompleted || matchingState.isComplete) {
                PuzzleTopBar(
                    showBackButton = previouslyCompleted,
                    onBackToMainMenu = onBackToMainMenu,
                    showNextButton = matchingState.isComplete || previouslyCompleted,
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
                text = stringResource(R.string.noahs_ark_matching_title),
                style = MaterialTheme.typography.headlineMedium,
            )

            val feedback = when (matchingState.lastOutcome) {
                MatchOutcome.CORRECT -> stringResource(R.string.feedback_great_job)
                MatchOutcome.TRY_AGAIN -> stringResource(R.string.feedback_try_another_one)
                MatchOutcome.NONE -> null
            }

            // A static wrapped grid, not a lazily-virtualized one — every tile stays in
            // the tree regardless of scroll position (see Gather Supplies' history).
            // weight(1f, fill = true) hands this region exactly the space left over
            // after every other sibling above claims its natural size, and
            // BoxWithConstraints reads that resolved space to compute a tile size
            // that makes the whole grid fit — shrinking below MATCH_TILE_SIZE only
            // when there isn't room for it, never overflowing.
            BoxWithConstraints(modifier = Modifier.fillMaxWidth().weight(1f, fill = true)) {
                val columns = 4
                val rows = ((matchingState.items.size + columns - 1) / columns).coerceAtLeast(1)
                val spacing = 12.dp
                val tileSize = minOf(
                    (maxWidth - spacing * (columns - 1)) / columns,
                    (maxHeight - spacing * (rows - 1)) / rows,
                ).coerceIn(48.dp, MATCH_TILE_SIZE)

                Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
                    matchingState.items.chunked(columns).forEach { rowItems ->
                        Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                            rowItems.forEach { item ->
                                key(item.id) {
                                    MatchTile(
                                        item = item,
                                        tileSize = tileSize,
                                        isFaceUp = matchingState.isFaceUp(item.id),
                                        isSelected = item.id in matchingState.selectedIds,
                                        isMatched = item.id in matchingState.matchedIds,
                                        onClick = { onItemTapped(item.id) },
                                    )
                                }
                            }
                        }
                    }
                }

                CharacterCallout(
                    characterCustomization = characterCustomization,
                    message = feedback ?: stringResource(R.string.noahs_ark_matching_instructions),
                    posture = if (matchingState.lastOutcome == MatchOutcome.CORRECT) Posture.THUMBS_UP else Posture.STANDING,
                    modifier = Modifier.align(Alignment.BottomStart),
                )
            }

            if (previouslyCompleted && !matchingState.isComplete) {
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
private fun MatchTile(item: MatchItem, tileSize: Dp, isFaceUp: Boolean, isSelected: Boolean, isMatched: Boolean, onClick: () -> Unit) {
    val name = stringResource(item.contentDescriptionRes)
    val borderColor = if (isSelected && !isMatched) MaterialTheme.colorScheme.primary else Color.Transparent

    Box(
        modifier = Modifier
            .size(tileSize)
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
            modifier = Modifier.size(tileSize * MATCH_TILE_ICON_FRACTION),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NoahsArkMatchingPreview() {
    BibleAdventuresTheme {
        NoahsArkMatchingContent(
            matchingState = MatchingGameState(items = emptyList()),
            characterCustomization = CharacterCustomization(),
            onItemTapped = {},
            onContinue = {},
            onBackToMainMenu = {},
        )
    }
}
