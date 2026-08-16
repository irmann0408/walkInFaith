package com.bibleadventures.ui.screens.esther.courtyardstealth

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bibleadventures.R
import com.bibleadventures.game.puzzles.gridmaze.Direction
import com.bibleadventures.game.puzzles.gridmaze.GridPosition
import com.bibleadventures.game.puzzles.stealth.StealthGameState
import com.bibleadventures.game.puzzles.stealth.StealthOutcome
import com.bibleadventures.game.puzzles.stealth.StealthTileType
import com.bibleadventures.ui.components.AdventureMenuButton
import com.bibleadventures.ui.components.BackToMainMenuTopBar
import com.bibleadventures.ui.screens.esther.EstherViewModel
import com.bibleadventures.ui.theme.BibleAdventuresTheme

@Composable
fun EstherCourtyardStealthScreen(
    viewModel: EstherViewModel,
    onContinue: () -> Unit,
    onBackToMainMenu: () -> Unit,
    previouslyCompleted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    EstherCourtyardStealthContent(
        stealthState = uiState.stealthState,
        onDirectionPressed = viewModel::onCourtyardDirectionPressed,
        onContinue = onContinue,
        onBackToMainMenu = onBackToMainMenu,
        previouslyCompleted = previouslyCompleted,
        modifier = modifier,
    )
}

@Composable
private fun EstherCourtyardStealthContent(
    stealthState: StealthGameState,
    onDirectionPressed: (Direction) -> Unit,
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
                text = stringResource(R.string.esther_secret_plot_stealth_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = stringResource(R.string.esther_secret_plot_stealth_instructions),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
            )

            val feedback = if (stealthState.lastOutcome == StealthOutcome.SPOTTED) {
                stringResource(R.string.esther_secret_plot_spotted_feedback)
            } else {
                ""
            }
            Box(modifier = Modifier.height(28.dp)) {
                Text(text = feedback, style = MaterialTheme.typography.titleMedium)
            }

            // Non-interactive grid, same reasoning as GoodSamaritanExploreScreen:
            // movement is via the D-pad below, not tap-on-tile.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .aspectRatio(3f / 5f),
            ) {
                stealthState.grid.forEachIndexed { rowIndex, rowTiles ->
                    Row(modifier = Modifier.weight(1f)) {
                        rowTiles.forEachIndexed { colIndex, tile ->
                            CourtyardCell(
                                tile = tile,
                                isPlayer = stealthState.playerPosition == GridPosition(rowIndex, colIndex),
                                isGuard = GridPosition(rowIndex, colIndex) in stealthState.watchedCells,
                                modifier = Modifier.weight(1f).fillMaxSize(),
                            )
                        }
                    }
                }
            }

            if (previouslyCompleted && !stealthState.isComplete) {
                Text(
                    text = stringResource(R.string.puzzle_already_completed_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            if (!stealthState.isComplete) {
                DirectionalPad(
                    onDirectionPressed = onDirectionPressed,
                    modifier = Modifier.padding(top = 16.dp),
                )
                if (previouslyCompleted) {
                    AdventureMenuButton(
                        text = stringResource(R.string.action_continue),
                        onClick = onContinue,
                        modifier = Modifier.widthIn(max = 320.dp).padding(top = 8.dp),
                    )
                }
            } else {
                AdventureMenuButton(
                    text = stringResource(R.string.action_continue),
                    onClick = onContinue,
                    modifier = Modifier.widthIn(max = 320.dp).padding(top = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun CourtyardCell(tile: StealthTileType, isPlayer: Boolean, isGuard: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        when {
            isPlayer -> Image(
                painter = painterResource(R.drawable.ic_mordecai_marker),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(0.8f),
            )
            isGuard -> Image(
                painter = painterResource(R.drawable.ic_guard_marker),
                contentDescription = stringResource(R.string.esther_secret_plot_guard_content_description),
                modifier = Modifier.fillMaxSize(0.8f),
            )
            tile == StealthTileType.WALL -> Image(
                painter = painterResource(R.drawable.ic_wall_courtyard),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
            tile == StealthTileType.GOAL -> Image(
                painter = painterResource(R.drawable.ic_esther_waiting_goal),
                contentDescription = null,
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
            contentDescription = stringResource(R.string.esther_secret_plot_direction_up),
            onClick = { onDirectionPressed(Direction.UP) },
        )
        Row {
            DirectionButton(
                icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.esther_secret_plot_direction_left),
                onClick = { onDirectionPressed(Direction.LEFT) },
            )
            Spacer(modifier = Modifier.width(56.dp))
            DirectionButton(
                icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(R.string.esther_secret_plot_direction_right),
                onClick = { onDirectionPressed(Direction.RIGHT) },
            )
        }
        DirectionButton(
            icon = Icons.Filled.KeyboardArrowDown,
            contentDescription = stringResource(R.string.esther_secret_plot_direction_down),
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
private fun EstherCourtyardStealthPreview() {
    BibleAdventuresTheme {
        val grid = listOf(listOf(StealthTileType.PATH, StealthTileType.PATH, StealthTileType.GOAL))
        EstherCourtyardStealthContent(
            stealthState = StealthGameState(
                grid = grid,
                startPosition = GridPosition(0, 0),
                playerPosition = GridPosition(0, 0),
                guards = emptyList(),
            ),
            onDirectionPressed = {},
            onContinue = {},
            onBackToMainMenu = {},
        )
    }
}
