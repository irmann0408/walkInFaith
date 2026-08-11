package com.bibleadventures.ui.screens.jericho.spiesescape

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bibleadventures.R
import com.bibleadventures.game.puzzles.slidingpuzzle.SlidingPuzzleGame
import com.bibleadventures.game.puzzles.slidingpuzzle.SlidingPuzzleGameState
import com.bibleadventures.ui.components.AdventureMenuButton
import com.bibleadventures.ui.screens.jericho.JerichoViewModel
import com.bibleadventures.ui.theme.BibleAdventuresTheme

/**
 * A 3x3 sliding-tile puzzle (the classic 15-puzzle's smaller, kid-tractable
 * cousin — confirmed with the user) — untangling the rope Rahab lowers the
 * spies with. No failure-state adaptation needed here at all: every legal
 * slide is fully reversible, so [com.bibleadventures.game.puzzles.slidingpuzzle.SlidingPuzzleGame]
 * has no "wrong move" concept to begin with.
 */
@Composable
fun JerichoSpiesEscapeScreen(
    viewModel: JerichoViewModel,
    onContinue: () -> Unit,
    previouslyCompleted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    JerichoSpiesEscapeContent(
        puzzleState = uiState.spiesEscapeState,
        onTileTapped = viewModel::onSpiesEscapeTileTapped,
        onContinue = onContinue,
        previouslyCompleted = previouslyCompleted,
        modifier = modifier,
    )
}

@Composable
private fun JerichoSpiesEscapeContent(
    puzzleState: SlidingPuzzleGameState,
    onTileTapped: (Int) -> Unit,
    onContinue: () -> Unit,
    previouslyCompleted: Boolean = false,
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
                text = stringResource(R.string.jericho_spies_escape_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = stringResource(R.string.jericho_spies_escape_instructions),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(puzzleState.size),
                modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                userScrollEnabled = false,
            ) {
                itemsIndexed(puzzleState.tiles) { index, number ->
                    SlidingTile(number = number, onClick = { onTileTapped(index) })
                }
            }

            if (previouslyCompleted && !puzzleState.isComplete) {
                Text(
                    text = stringResource(R.string.puzzle_already_completed_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            if (puzzleState.isComplete || previouslyCompleted) {
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
private fun SlidingTile(number: Int, onClick: () -> Unit) {
    if (number == 0) {
        val emptyDescription = stringResource(R.string.jericho_spies_escape_empty_content_description)
        Box(modifier = Modifier.padding(4.dp).aspectRatio(1f).semantics { contentDescription = emptyDescription })
        return
    }

    val tileDescription = stringResource(R.string.jericho_spies_escape_tile_content_description, number)
    Box(
        modifier = Modifier
            .padding(4.dp)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.primary)
            .clickable(onClickLabel = tileDescription, onClick = onClick)
            .semantics { contentDescription = tileDescription },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = number.toString(),
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.headlineSmall,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun JerichoSpiesEscapePreview() {
    BibleAdventuresTheme {
        JerichoSpiesEscapeContent(
            puzzleState = SlidingPuzzleGame.newShuffled(size = 3),
            onTileTapped = {},
            onContinue = {},
        )
    }
}
