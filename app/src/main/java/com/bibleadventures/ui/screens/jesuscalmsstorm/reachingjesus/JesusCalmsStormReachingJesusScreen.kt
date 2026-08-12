package com.bibleadventures.ui.screens.jesuscalmsstorm.reachingjesus

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
import com.bibleadventures.game.stories.JesusCalmsStormContent
import com.bibleadventures.ui.components.AdventureMenuButton
import com.bibleadventures.ui.screens.jesuscalmsstorm.JesusCalmsStormViewModel
import com.bibleadventures.ui.theme.BibleAdventuresTheme

/**
 * "Jesus was in the stern, sleeping on a cushion. The disciples woke him"
 * (Mark 4:38) — a reuse of the generalized
 * [com.bibleadventures.game.puzzles.gridmaze] engine, mirroring
 * `DanielDariusMazeScreen`'s structure exactly: no collectible/checkpoint
 * tile, just PATH/WALL/GOAL. [JesusCalmsStormContent.reachingJesusMapLayout]
 * is a genuine perfect maze (real dead-end branches, single solution),
 * deliberately longer (30 moves) than Daniel's Darius maze (28).
 */
@Composable
fun JesusCalmsStormReachingJesusScreen(
    viewModel: JesusCalmsStormViewModel,
    onContinue: () -> Unit,
    previouslyCompleted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    JesusCalmsStormReachingJesusContent(
        gridMazeState = uiState.gridMazeState,
        onDirectionPressed = viewModel::onReachingJesusDirectionPressed,
        onContinue = onContinue,
        previouslyCompleted = previouslyCompleted,
        modifier = modifier,
    )
}

@Composable
private fun JesusCalmsStormReachingJesusContent(
    gridMazeState: GridMazeState,
    onDirectionPressed: (Direction) -> Unit,
    onContinue: () -> Unit,
    previouslyCompleted: Boolean = false,
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
                    text = stringResource(R.string.jesus_calms_storm_reaching_jesus_title),
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    text = stringResource(R.string.jesus_calms_storm_reaching_jesus_instructions),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 8.dp, bottom = 12.dp),
                )

                // Non-interactive grid, same reasoning as DanielDariusMazeScreen:
                // movement is via the D-pad below, not tap-on-tile.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .aspectRatio(7f / 9f),
                ) {
                    gridMazeState.grid.forEachIndexed { rowIndex, rowTiles ->
                        Row(modifier = Modifier.weight(1f)) {
                            rowTiles.forEachIndexed { colIndex, tile ->
                                ReachingJesusGridCell(
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
}

@Composable
private fun ReachingJesusGridCell(tile: GridTileType, isPlayer: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        when {
            isPlayer -> Image(
                painter = painterResource(R.drawable.ic_disciple_marker),
                contentDescription = stringResource(R.string.jesus_calms_storm_reaching_jesus_player_content_description),
                modifier = Modifier.fillMaxSize(0.8f),
            )
            tile == GridTileType.WALL -> Image(
                painter = painterResource(R.drawable.ic_cargo_obstacle),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
            tile == GridTileType.GOAL -> Image(
                painter = painterResource(R.drawable.ic_jesus_sleeping),
                contentDescription = stringResource(R.string.jesus_calms_storm_reaching_jesus_goal_content_description),
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
            contentDescription = stringResource(R.string.jesus_calms_storm_reaching_jesus_direction_up),
            onClick = { onDirectionPressed(Direction.UP) },
        )
        Row {
            DirectionButton(
                icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.jesus_calms_storm_reaching_jesus_direction_left),
                onClick = { onDirectionPressed(Direction.LEFT) },
            )
            Spacer(modifier = Modifier.width(56.dp))
            DirectionButton(
                icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(R.string.jesus_calms_storm_reaching_jesus_direction_right),
                onClick = { onDirectionPressed(Direction.RIGHT) },
            )
        }
        DirectionButton(
            icon = Icons.Filled.KeyboardArrowDown,
            contentDescription = stringResource(R.string.jesus_calms_storm_reaching_jesus_direction_down),
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
private fun JesusCalmsStormReachingJesusPreview() {
    BibleAdventuresTheme {
        val grid = JesusCalmsStormContent.reachingJesusMapLayout.map { row -> row.map { GridTileType.PATH } }
        JesusCalmsStormReachingJesusContent(
            gridMazeState = GridMazeState(grid = grid, playerPosition = GridPosition(0, 0)),
            onDirectionPressed = {},
            onContinue = {},
        )
    }
}
