package com.bibleadventures.ui.screens.goodsamaritan.explore

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.bibleadventures.game.stories.GoodSamaritanContent
import com.bibleadventures.ui.components.AdventureMenuButton
import com.bibleadventures.ui.screens.goodsamaritan.GoodSamaritanViewModel
import com.bibleadventures.ui.theme.BibleAdventuresTheme

@Composable
fun GoodSamaritanExploreScreen(
    viewModel: GoodSamaritanViewModel,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    GoodSamaritanExploreContent(
        gridMazeState = uiState.gridMazeState,
        helpingBeatAcknowledged = uiState.helpingBeatAcknowledged,
        onDirectionPressed = viewModel::onDirectionPressed,
        onHelpingBeatAcknowledged = viewModel::onHelpingBeatAcknowledged,
        onContinue = onContinue,
        modifier = modifier,
    )
}

@Composable
private fun GoodSamaritanExploreContent(
    gridMazeState: GridMazeState,
    helpingBeatAcknowledged: Boolean,
    onDirectionPressed: (Direction) -> Unit,
    onHelpingBeatAcknowledged: () -> Unit,
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
                    text = stringResource(R.string.good_samaritan_explore_title),
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    text = stringResource(R.string.good_samaritan_explore_instructions),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 8.dp, bottom = 12.dp),
                )

                // Non-interactive: a 10x10 grid can't give each cell a legible 48dp tap
                // target on a phone screen, which is exactly why movement is via the
                // D-pad below, not tap-on-tile.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .aspectRatio(1f),
                ) {
                    GoodSamaritanContent.mapLayout.forEachIndexed { rowIndex, rowChars ->
                        Row(modifier = Modifier.weight(1f)) {
                            rowChars.forEachIndexed { colIndex, tileChar ->
                                GridCell(
                                    tileChar = tileChar,
                                    isPlayer = gridMazeState.playerPosition == GridPosition(rowIndex, colIndex),
                                    isMedicineCollected = GridPosition(rowIndex, colIndex) in gridMazeState.medicineCollected,
                                    isTravelerTreated = gridMazeState.travelerTreated,
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

            if (gridMazeState.travelerTreated && !helpingBeatAcknowledged) {
                HelpingBeatOverlay(onDismiss = onHelpingBeatAcknowledged)
            }
        }
    }
}

@Composable
private fun GridCell(
    tileChar: Char,
    isPlayer: Boolean,
    isMedicineCollected: Boolean,
    isTravelerTreated: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        when {
            isPlayer -> Image(
                painter = painterResource(R.drawable.ic_player_marker),
                contentDescription = stringResource(R.string.good_samaritan_player_content_description),
                modifier = Modifier.fillMaxSize(0.8f),
            )
            tileChar == '#' -> Image(
                painter = painterResource(R.drawable.ic_wall_rock),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
            tileChar == 'X' -> Image(
                painter = painterResource(R.drawable.ic_wall_bandit),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
            tileChar == 'M' && !isMedicineCollected -> Image(
                painter = painterResource(R.drawable.ic_medicine),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(0.8f),
            )
            tileChar == 'T' && !isTravelerTreated -> Image(
                painter = painterResource(R.drawable.ic_traveler_injured),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(0.8f),
            )
            tileChar == 'I' -> Image(
                painter = painterResource(R.drawable.ic_inn),
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
            contentDescription = stringResource(R.string.good_samaritan_direction_up),
            onClick = { onDirectionPressed(Direction.UP) },
        )
        Row {
            DirectionButton(
                icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.good_samaritan_direction_left),
                onClick = { onDirectionPressed(Direction.LEFT) },
            )
            Spacer(modifier = Modifier.width(56.dp))
            DirectionButton(
                icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(R.string.good_samaritan_direction_right),
                onClick = { onDirectionPressed(Direction.RIGHT) },
            )
        }
        DirectionButton(
            icon = Icons.Filled.KeyboardArrowDown,
            contentDescription = stringResource(R.string.good_samaritan_direction_down),
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

/**
 * An automatic story beat, not a Choice scene — Luke 10:34 describes a
 * specific, non-branching sequence of care, so there's nothing real to pick.
 * Consumes all touches so the D-pad underneath can't be pressed while it's up.
 */
@Composable
private fun HelpingBeatOverlay(onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        contentAlignment = Alignment.Center,
    ) {
        ElevatedCard(
            modifier = Modifier.widthIn(max = 400.dp).padding(24.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.good_samaritan_helping_beat_title),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Column(
                    modifier = Modifier.padding(top = 12.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    GoodSamaritanContent.helpingBeatLines.forEach { lineRes ->
                        Text(text = stringResource(lineRes), style = MaterialTheme.typography.bodyLarge)
                    }
                }
                AdventureMenuButton(
                    text = stringResource(R.string.action_continue),
                    onClick = onDismiss,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GoodSamaritanExplorePreview() {
    BibleAdventuresTheme {
        val grid = GoodSamaritanContent.mapLayout.map { row -> row.map { com.bibleadventures.game.puzzles.gridmaze.GridTileType.PATH } }
        GoodSamaritanExploreContent(
            gridMazeState = GridMazeState(grid = grid, playerPosition = GridPosition(0, 0)),
            helpingBeatAcknowledged = false,
            onDirectionPressed = {},
            onHelpingBeatAcknowledged = {},
            onContinue = {},
        )
    }
}
