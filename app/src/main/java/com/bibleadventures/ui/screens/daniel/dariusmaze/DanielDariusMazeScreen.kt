package com.bibleadventures.ui.screens.daniel.dariusmaze

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bibleadventures.R
import com.bibleadventures.game.puzzles.gridmaze.Direction
import com.bibleadventures.game.puzzles.gridmaze.GridMazeState
import com.bibleadventures.game.puzzles.gridmaze.GridPosition
import com.bibleadventures.game.puzzles.gridmaze.GridTileType
import com.bibleadventures.game.stories.DanielContent
import com.bibleadventures.ui.components.AdventureMenuButton
import com.bibleadventures.ui.screens.daniel.DanielViewModel
import com.bibleadventures.ui.theme.BibleAdventuresTheme

/**
 * King Darius's dawn hurry through the palace to the lions' den (Daniel
 * 6:19) — a reuse of the generalized [com.bibleadventures.game.puzzles.gridmaze]
 * engine, mirroring
 * [com.bibleadventures.ui.screens.goodsamaritan.explore.GoodSamaritanExploreScreen]'s
 * structure, but simpler: no collectible/checkpoint tile, just PATH/WALL/GOAL.
 */
@Composable
fun DanielDariusMazeScreen(
    viewModel: DanielViewModel,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DanielDariusMazeContent(
        gridMazeState = uiState.gridMazeState,
        onDirectionPressed = viewModel::onDirectionPressed,
        onContinue = onContinue,
        modifier = modifier,
    )
}

@Composable
private fun DanielDariusMazeContent(
    gridMazeState: GridMazeState,
    onDirectionPressed: (Direction) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.daniel_darius_maze_title),
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    text = stringResource(R.string.daniel_darius_maze_instructions),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 8.dp, bottom = 12.dp),
                )

                // Non-interactive grid, same reasoning as GoodSamaritanExploreScreen:
                // movement is via the D-pad below, not tap-on-tile.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .aspectRatio(1f),
                ) {
                    gridMazeState.grid.forEachIndexed { rowIndex, rowTiles ->
                        Row(modifier = Modifier.weight(1f)) {
                            rowTiles.forEachIndexed { colIndex, tile ->
                                DariusGridCell(
                                    tile = tile,
                                    isPlayer = gridMazeState.playerPosition == GridPosition(rowIndex, colIndex),
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

                if (gridMazeState.isComplete) {
                    AdventureMenuButton(
                        text = stringResource(R.string.action_continue),
                        onClick = onContinue,
                        modifier = Modifier.widthIn(max = 320.dp).padding(top = 16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun DariusGridCell(tile: GridTileType, isPlayer: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        when {
            isPlayer -> Image(
                painter = painterResource(R.drawable.ic_darius_marker),
                contentDescription = stringResource(R.string.daniel_darius_player_content_description),
                modifier = Modifier.fillMaxSize(0.8f),
            )
            tile == GridTileType.WALL -> Image(
                painter = painterResource(R.drawable.ic_wall_palace),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
            tile == GridTileType.GOAL -> Image(
                painter = painterResource(R.drawable.ic_den_goal),
                contentDescription = stringResource(R.string.daniel_darius_den_content_description),
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
            contentDescription = stringResource(R.string.daniel_darius_direction_up),
            onClick = { onDirectionPressed(Direction.UP) },
        )
        Row {
            DirectionButton(
                icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.daniel_darius_direction_left),
                onClick = { onDirectionPressed(Direction.LEFT) },
            )
            Spacer(modifier = Modifier.width(56.dp))
            DirectionButton(
                icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(R.string.daniel_darius_direction_right),
                onClick = { onDirectionPressed(Direction.RIGHT) },
            )
        }
        DirectionButton(
            icon = Icons.Filled.KeyboardArrowDown,
            contentDescription = stringResource(R.string.daniel_darius_direction_down),
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
private fun DanielDariusMazePreview() {
    BibleAdventuresTheme {
        val grid = DanielContent.dariusMapLayout.map { row -> row.map { GridTileType.PATH } }
        DanielDariusMazeContent(
            gridMazeState = GridMazeState(grid = grid, playerPosition = GridPosition(0, 0)),
            onDirectionPressed = {},
            onContinue = {},
        )
    }
}
