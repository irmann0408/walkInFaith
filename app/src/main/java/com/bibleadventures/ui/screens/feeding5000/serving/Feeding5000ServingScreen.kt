package com.bibleadventures.ui.screens.feeding5000.serving

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bibleadventures.R
import com.bibleadventures.game.puzzles.gridmaze.Direction
import com.bibleadventures.game.puzzles.gridmaze.GridMazeOutcome
import com.bibleadventures.game.puzzles.gridmaze.GridMazeState
import com.bibleadventures.game.puzzles.gridmaze.GridPosition
import com.bibleadventures.game.puzzles.gridmaze.GridTileType
import com.bibleadventures.game.stories.Feeding5000Content
import com.bibleadventures.ui.components.AdventureMenuButton
import com.bibleadventures.ui.screens.feeding5000.Feeding5000ViewModel
import com.bibleadventures.ui.theme.BibleAdventuresTheme

/**
 * Phase A of the Grand Feast finale — reuses `gridmaze` (Good Samaritan's/
 * Daniel's D-pad grid-walk engine), replacing an earlier `rhythmlane`
 * version where the disciple stood still and bread fell into a basket —
 * that read as *receiving* food, the opposite of serving it. Walking out to
 * each of the 7 groups instead reads as actually distributing the meal.
 * Mirrors [com.bibleadventures.ui.screens.goodsamaritan.explore.GoodSamaritanExploreScreen]'s
 * structure but simpler — no checkpoint/goal tile, just collectibles, using
 * `GridMazeState`'s newly-generalized "no goal tile -> complete once every
 * collectible is gathered" mode, since serving all 7 groups has no single
 * finish line or required order.
 */
@Composable
fun Feeding5000ServingScreen(
    viewModel: Feeding5000ViewModel,
    onContinue: () -> Unit,
    previouslyCompleted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Feeding5000ServingContent(
        gridMazeState = uiState.servingState,
        onDirectionPressed = viewModel::onServingDirectionPressed,
        onContinue = onContinue,
        previouslyCompleted = previouslyCompleted,
        modifier = modifier,
    )
}

@Composable
private fun Feeding5000ServingContent(
    gridMazeState: GridMazeState,
    onDirectionPressed: (Direction) -> Unit,
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
                text = stringResource(R.string.feeding_5000_serving_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = stringResource(R.string.feeding_5000_serving_instructions),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = stringResource(
                    R.string.feeding_5000_serving_progress_label,
                    gridMazeState.collectedPositions.size,
                    Feeding5000Content.SERVING_GROUP_COUNT,
                ),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )

            // Visible feedback + a screen-reader announcement after each move (a
            // wall bump or a group served, since the grid's other tiles have no
            // per-cell content description — narrating up to 64 non-interactive
            // cells on every recomposition would be noisy for a D-pad-only maze
            // where the player never touches a tile directly).
            Box(modifier = Modifier.height(28.dp)) {
                Text(
                    text = mazeFeedbackText(gridMazeState),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }

            // Non-interactive: an 8x8 grid can't give each cell a legible 48dp tap
            // target on a phone screen, which is exactly why movement is via the
            // D-pad below, not tap-on-tile — same reasoning as every other
            // gridmaze chapter.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
            ) {
                gridMazeState.grid.forEachIndexed { rowIndex, rowTiles ->
                    Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        rowTiles.forEachIndexed { colIndex, tile ->
                            val position = GridPosition(rowIndex, colIndex)
                            ServingGridCell(
                                tile = tile,
                                isPlayer = gridMazeState.playerPosition == position,
                                isServed = position in gridMazeState.collectedPositions,
                                modifier = Modifier.weight(1f).fillMaxSize(),
                            )
                        }
                    }
                }
            }

            DirectionalPad(
                onDirectionPressed = onDirectionPressed,
                modifier = Modifier.padding(top = 16.dp),
            )

            if (previouslyCompleted && !gridMazeState.isComplete) {
                Text(
                    text = stringResource(R.string.puzzle_already_completed_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            if (gridMazeState.isComplete || previouslyCompleted) {
                AdventureMenuButton(
                    text = stringResource(R.string.action_continue),
                    onClick = onContinue,
                    modifier = Modifier.widthIn(max = 320.dp).padding(top = 16.dp),
                )
            }
        }
    }
}

/** Reads as "Blocked" / "Collected!" / "You reached the goal!" — a live-region announcement plus visible feedback text, this screen's only feedback of any kind beyond the progress label. */
@Composable
private fun mazeFeedbackText(gridMazeState: GridMazeState): String = when {
    gridMazeState.isComplete -> stringResource(R.string.grid_maze_feedback_goal_reached)
    gridMazeState.lastOutcome == GridMazeOutcome.COLLECTED -> stringResource(R.string.grid_maze_feedback_collected)
    gridMazeState.lastOutcome == GridMazeOutcome.BLOCKED -> stringResource(R.string.grid_maze_feedback_blocked)
    else -> ""
}

@Composable
private fun ServingGridCell(tile: GridTileType, isPlayer: Boolean, isServed: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        when {
            isPlayer -> Image(
                painter = painterResource(R.drawable.ic_player_marker),
                contentDescription = stringResource(R.string.feeding_5000_serving_player_content_description),
                modifier = Modifier.fillMaxSize(0.8f),
            )
            tile == GridTileType.WALL -> Image(
                painter = painterResource(R.drawable.ic_wall_rock),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
            tile == GridTileType.COLLECTIBLE && !isServed -> Image(
                painter = painterResource(R.drawable.ic_crowd_group),
                contentDescription = stringResource(R.string.feeding_5000_serving_group_content_description),
                modifier = Modifier.fillMaxSize(0.8f),
            )
            else -> Unit
        }
    }
}

@Composable
private fun DirectionalPad(onDirectionPressed: (Direction) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        DirectionButton(
            icon = Icons.Filled.KeyboardArrowUp,
            contentDescription = stringResource(R.string.feeding_5000_serving_direction_up),
            onClick = { onDirectionPressed(Direction.UP) },
        )
        Row {
            DirectionButton(
                icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.feeding_5000_serving_direction_left),
                onClick = { onDirectionPressed(Direction.LEFT) },
            )
            Spacer(modifier = Modifier.width(56.dp))
            DirectionButton(
                icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(R.string.feeding_5000_serving_direction_right),
                onClick = { onDirectionPressed(Direction.RIGHT) },
            )
        }
        DirectionButton(
            icon = Icons.Filled.KeyboardArrowDown,
            contentDescription = stringResource(R.string.feeding_5000_serving_direction_down),
            onClick = { onDirectionPressed(Direction.DOWN) },
        )
    }
}

@Composable
private fun DirectionButton(icon: ImageVector, contentDescription: String, onClick: () -> Unit) {
    FilledTonalIconButton(
        onClick = onClick,
        modifier = Modifier.size(56.dp),
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription, modifier = Modifier.size(32.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun Feeding5000ServingPreview() {
    BibleAdventuresTheme {
        val grid = Feeding5000Content.servingMapLayout.map { row -> row.map { GridTileType.PATH } }
        Feeding5000ServingContent(
            gridMazeState = GridMazeState(grid = grid, playerPosition = GridPosition(0, 0)),
            onDirectionPressed = {},
            onContinue = {},
        )
    }
}
