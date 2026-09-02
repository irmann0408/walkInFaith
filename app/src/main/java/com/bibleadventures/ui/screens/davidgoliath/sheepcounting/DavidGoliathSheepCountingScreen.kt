package com.bibleadventures.ui.screens.davidgoliath.sheepcounting

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
import com.bibleadventures.ui.screens.davidgoliath.DavidGoliathViewModel
import com.bibleadventures.ui.theme.BibleAdventuresTheme

private val SHEEP_TILE_SIZE = 72.dp
private const val SHEEP_TILE_ICON_FRACTION = 56f / 72f

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
    val characterCustomization by viewModel.characterCustomization.collectAsStateWithLifecycle()

    DavidGoliathSheepCountingContent(
        sheepCountingState = uiState.sheepCountingState,
        characterCustomization = characterCustomization,
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
            if (previouslyCompleted || sheepCountingState.isComplete) {
                PuzzleTopBar(
                    showBackButton = previouslyCompleted,
                    onBackToMainMenu = onBackToMainMenu,
                    showNextButton = sheepCountingState.isComplete || previouslyCompleted,
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
                MatchOutcome.NONE -> null
            }

            // weight(1f, fill = true) hands this region exactly the space left over
            // after every other sibling above claims its natural size, and
            // BoxWithConstraints reads that resolved space to compute a tile size
            // that makes the whole grid fit — shrinking below SHEEP_TILE_SIZE only
            // when there isn't room for it, never overflowing.
            BoxWithConstraints(modifier = Modifier.fillMaxWidth().weight(1f, fill = true)) {
                val columns = 4
                val rows = ((sheepCountingState.items.size + columns - 1) / columns).coerceAtLeast(1)
                val spacing = 12.dp
                val tileSize = minOf(
                    (maxWidth - spacing * (columns - 1)) / columns,
                    (maxHeight - spacing * (rows - 1)) / rows,
                ).coerceIn(48.dp, SHEEP_TILE_SIZE)

                Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
                    sheepCountingState.items.chunked(columns).forEach { rowItems ->
                        Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                            rowItems.forEach { item ->
                                key(item.id) {
                                    SheepCountTile(
                                        item = item,
                                        tileSize = tileSize,
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

                CharacterCallout(
                    characterCustomization = characterCustomization,
                    message = feedback,
                    posture = if (sheepCountingState.lastOutcome == MatchOutcome.CORRECT) Posture.THUMBS_UP else Posture.STANDING,
                    modifier = Modifier.align(Alignment.BottomStart),
                )
            }

            if (previouslyCompleted && !sheepCountingState.isComplete) {
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
private fun SheepCountTile(item: MatchItem, tileSize: Dp, isFaceUp: Boolean, isSelected: Boolean, isMatched: Boolean, onClick: () -> Unit) {
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
            modifier = Modifier.size(tileSize * SHEEP_TILE_ICON_FRACTION),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DavidGoliathSheepCountingPreview() {
    BibleAdventuresTheme {
        DavidGoliathSheepCountingContent(
            sheepCountingState = MatchingGameState(items = emptyList()),
            characterCustomization = CharacterCustomization(),
            onItemTapped = {},
            onContinue = {},
            onBackToMainMenu = {},
        )
    }
}
