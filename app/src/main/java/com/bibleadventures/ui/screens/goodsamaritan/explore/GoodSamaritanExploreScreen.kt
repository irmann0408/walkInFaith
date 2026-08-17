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
import androidx.compose.foundation.layout.fillMaxSize
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
import com.bibleadventures.game.stories.GoodSamaritanContent
import com.bibleadventures.ui.components.AdventureMenuButton
import com.bibleadventures.ui.components.AspectRatioFitBox
import com.bibleadventures.ui.components.PuzzleTopBar
import com.bibleadventures.ui.screens.goodsamaritan.GoodSamaritanViewModel
import com.bibleadventures.ui.theme.BibleAdventuresTheme

@Composable
fun GoodSamaritanExploreScreen(
    viewModel: GoodSamaritanViewModel,
    onContinue: () -> Unit,
    onBackToMainMenu: () -> Unit,
    previouslyCompleted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    GoodSamaritanExploreContent(
        gridMazeState = uiState.gridMazeState,
        helpingBeatAcknowledged = uiState.helpingBeatAcknowledged,
        onDirectionPressed = viewModel::onDirectionPressed,
        onHelpingBeatAcknowledged = viewModel::onHelpingBeatAcknowledged,
        onContinue = onContinue,
        onBackToMainMenu = onBackToMainMenu,
        previouslyCompleted = previouslyCompleted,
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
    onBackToMainMenu: () -> Unit,
    previouslyCompleted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    // The helping-beat overlay below has its own Continue button while it's up —
    // never show the early-skip Continue at the same time, or two "Continue"
    // nodes would exist at once.
    val helpingBeatOverlayShowing = gridMazeState.checkpointActivated && !helpingBeatAcknowledged

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            if ((previouslyCompleted || gridMazeState.isComplete) && !helpingBeatOverlayShowing) {
                PuzzleTopBar(
                    showBackButton = previouslyCompleted,
                    onBackToMainMenu = onBackToMainMenu,
                    showNextButton = gridMazeState.isComplete || previouslyCompleted,
                    onNext = onContinue,
                )
            }
        },
    ) { innerPadding ->
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

                // Visible feedback + a screen-reader announcement after each move (a
                // wall bump, collecting the medicine, treating the traveler, or
                // reaching the inn), since the grid's other tiles have no per-cell
                // content description — narrating up to 100 non-interactive cells on
                // every recomposition would be noisy for a D-pad-only maze where the
                // player never touches a tile directly.
                Box(modifier = Modifier.height(28.dp)) {
                    Text(
                        text = mazeFeedbackText(gridMazeState),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }

                // Non-interactive: a 10x10 grid can't give each cell a legible 48dp tap
                // target on a phone screen, which is exactly why movement is via the
                // D-pad below, not tap-on-tile. weight(1f, fill = true) hands this
                // element exactly the space left over after every other (naturally-
                // sized) sibling in this Column, and AspectRatioFitBox letterbox-fits
                // within that bounded box — shrinking on cramped viewports instead of
                // overflowing, so nothing here ever needs to scroll.
                AspectRatioFitBox(ratio = 1f, modifier = Modifier.weight(1f, fill = true).fillMaxSize()) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        GoodSamaritanContent.mapLayout.forEachIndexed { rowIndex, rowChars ->
                            Row(modifier = Modifier.weight(1f)) {
                                rowChars.forEachIndexed { colIndex, tileChar ->
                                    GridCell(
                                        tileChar = tileChar,
                                        isPlayer = gridMazeState.playerPosition == GridPosition(rowIndex, colIndex),
                                        isMedicineCollected = GridPosition(rowIndex, colIndex) in gridMazeState.collectedPositions,
                                        isTravelerTreated = gridMazeState.checkpointActivated,
                                        modifier = Modifier.weight(1f).fillMaxSize(),
                                    )
                                }
                            }
                        }
                    }
                }

                DirectionalPad(
                    onDirectionPressed = onDirectionPressed,
                    modifier = Modifier.padding(top = 16.dp),
                )

                if (previouslyCompleted && !gridMazeState.isComplete && !helpingBeatOverlayShowing) {
                    Text(
                        text = stringResource(R.string.puzzle_already_completed_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }

            if (helpingBeatOverlayShowing) {
                HelpingBeatOverlay(onDismiss = onHelpingBeatAcknowledged)
            }
        }
    }
}

/** Reads as "Blocked" / "Collected!" / "Checkpoint activated!" / "You reached the goal!" — a live-region announcement plus visible feedback text, this screen's only feedback of any kind. */
@Composable
private fun mazeFeedbackText(gridMazeState: GridMazeState): String = when {
    gridMazeState.isComplete -> stringResource(R.string.grid_maze_feedback_goal_reached)
    gridMazeState.lastOutcome == GridMazeOutcome.CHECKPOINT_ACTIVATED -> stringResource(R.string.grid_maze_feedback_checkpoint_activated)
    gridMazeState.lastOutcome == GridMazeOutcome.CHECKPOINT_NEEDS_COLLECTIBLE -> stringResource(R.string.grid_maze_feedback_checkpoint_needs_collectible)
    gridMazeState.lastOutcome == GridMazeOutcome.COLLECTED -> stringResource(R.string.grid_maze_feedback_collected)
    gridMazeState.lastOutcome == GridMazeOutcome.BLOCKED -> stringResource(R.string.grid_maze_feedback_blocked)
    else -> ""
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
            onBackToMainMenu = {},
        )
    }
}
