package com.bibleadventures.ui.screens.davidgoliath.choosestones

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bibleadventures.R
import com.bibleadventures.audio.CharacterVoiceLine
import com.bibleadventures.domain.model.CharacterCustomization
import com.bibleadventures.game.puzzles.connectfour.ConnectFourGameState
import com.bibleadventures.game.puzzles.connectfour.ConnectFourOutcome
import com.bibleadventures.game.puzzles.connectfour.Slot
import com.bibleadventures.ui.LocalAudioController
import com.bibleadventures.ui.components.CharacterCallout
import com.bibleadventures.ui.components.Posture
import com.bibleadventures.ui.components.PuzzleTopBar
import com.bibleadventures.ui.screens.davidgoliath.DavidGoliathViewModel
import com.bibleadventures.ui.theme.BibleAdventuresTheme
import kotlinx.coroutines.delay

private val CELL_SIZE = 64.dp
private val CELL_SPACING = 4.dp
private const val OPPONENT_THINK_DELAY_MS = 700L

/**
 * "Choose the Stones" — David already has one smooth stone; connect 4 more
 * in a row (horizontal, vertical, or diagonal) against a simple AI opponent
 * to gather the rest. This is the one mini-game in the app with a real
 * loss condition (a deliberate, confirmed exception to the project's
 * standing "no failure states" rule — see the architectural decisions
 * log), softened so a loss never feels punishing: a loss or a draw leaves
 * the finished board on screen (never auto-resets) with a "Try Again"
 * button, so the player decides when to start a fresh round, with
 * unlimited retries and no game-over screen.
 */
@Composable
fun DavidGoliathChooseStonesScreen(
    viewModel: DavidGoliathViewModel,
    onContinue: () -> Unit,
    onBackToMainMenu: () -> Unit,
    previouslyCompleted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val characterCustomization by viewModel.characterCustomization.collectAsStateWithLifecycle()

    DavidGoliathChooseStonesContent(
        connectFourState = uiState.connectFourState,
        characterCustomization = characterCustomization,
        onColumnTapped = viewModel::onConnectFourColumnTapped,
        onOpponentMove = viewModel::onConnectFourOpponentMove,
        onReset = viewModel::onConnectFourReset,
        onContinue = onContinue,
        onBackToMainMenu = onBackToMainMenu,
        previouslyCompleted = previouslyCompleted,
        modifier = modifier,
    )
}

@Composable
private fun DavidGoliathChooseStonesContent(
    connectFourState: ConnectFourGameState,
    characterCustomization: CharacterCustomization,
    onColumnTapped: (Int) -> Unit,
    onOpponentMove: () -> Unit,
    onReset: () -> Unit,
    onContinue: () -> Unit,
    onBackToMainMenu: () -> Unit,
    previouslyCompleted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val audioController = LocalAudioController.current
    LaunchedEffect(Unit) { audioController.playCharacterLine(CharacterVoiceLine.DAVID_CHOOSE_STONES_INTRO) }

    // The opponent's own move is a plain screen-owned timing concern (a brief
    // "thinking" pause), same split as every other engine in this app.
    LaunchedEffect(connectFourState.isPlayerTurn, connectFourState.outcome) {
        if (!connectFourState.isPlayerTurn && connectFourState.outcome == ConnectFourOutcome.NONE) {
            delay(OPPONENT_THINK_DELAY_MS)
            onOpponentMove()
        }
    }
    val isComplete = connectFourState.outcome == ConnectFourOutcome.PLAYER_WON
    // A loss or a draw is never a dead end, but it's also never forced on the
    // player — the finished board stays on screen exactly as it landed until
    // they tap Try Again. Only a real win (isComplete above) unlocks Next Page.
    val canTryAgain = connectFourState.outcome == ConnectFourOutcome.OPPONENT_WON || connectFourState.outcome == ConnectFourOutcome.DRAW
    // The character introduces the game only before the first stone is
    // dropped — once play is underway, the turn/outcome text above already
    // carries ongoing status, so the character falls silent (just posture)
    // rather than repeating or competing with it.
    val boardIsEmpty = connectFourState.grid.all { row -> row.all { it == Slot.EMPTY } }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            if (previouslyCompleted || isComplete) {
                PuzzleTopBar(
                    showBackButton = previouslyCompleted,
                    onBackToMainMenu = onBackToMainMenu,
                    showNextButton = isComplete || previouslyCompleted,
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
                text = stringResource(R.string.david_goliath_choose_stones_title),
                style = MaterialTheme.typography.headlineMedium,
            )

            val feedback = when (connectFourState.outcome) {
                ConnectFourOutcome.PLAYER_WON -> stringResource(R.string.david_goliath_choose_stones_player_won)
                ConnectFourOutcome.OPPONENT_WON -> stringResource(R.string.david_goliath_choose_stones_opponent_won)
                ConnectFourOutcome.DRAW -> stringResource(R.string.david_goliath_choose_stones_draw)
                ConnectFourOutcome.NONE -> {
                    if (connectFourState.isPlayerTurn) {
                        stringResource(R.string.david_goliath_choose_stones_your_turn)
                    } else {
                        stringResource(R.string.david_goliath_choose_stones_opponent_turn)
                    }
                }
            }
            Box(modifier = Modifier.height(32.dp).padding(top = 4.dp)) {
                Text(text = feedback, style = MaterialTheme.typography.titleMedium)
            }

            BoxWithConstraints(
                modifier = Modifier.weight(1f, fill = true).fillMaxSize(),
                // TopCenter, not Center: this box got taller once the
                // character's own row (and the old standalone instructions
                // line) were removed, and a vertically-centered grid just
                // grows equal empty margins above and below it instead of
                // actually moving closer to the title/status text above.
                contentAlignment = Alignment.TopCenter,
            ) {
                val cellSize = minOf(
                    (maxWidth - CELL_SPACING * (connectFourState.columns - 1)) / connectFourState.columns,
                    (maxHeight - CELL_SPACING * (connectFourState.rows - 1)) / connectFourState.rows,
                ).coerceIn(36.dp, CELL_SIZE)
                val canDrop = connectFourState.isPlayerTurn && connectFourState.outcome == ConnectFourOutcome.NONE

                Row(horizontalArrangement = Arrangement.spacedBy(CELL_SPACING)) {
                    for (column in 0 until connectFourState.columns) {
                        val columnFull = connectFourState.isColumnFull(column)
                        val columnDescription = stringResource(R.string.david_goliath_choose_stones_column_content_description, column + 1)

                        Column(
                            verticalArrangement = Arrangement.spacedBy(CELL_SPACING),
                            modifier = Modifier
                                .clickable(
                                    enabled = canDrop && !columnFull,
                                    onClickLabel = columnDescription,
                                    onClick = { onColumnTapped(column) },
                                )
                                .semantics { contentDescription = columnDescription },
                        ) {
                            // Row (rows-1) renders at the top of the screen, row 0 at the
                            // bottom — matching gravity, since stones settle downward.
                            for (row in connectFourState.rows - 1 downTo 0) {
                                ConnectFourCell(slot = connectFourState.grid[row][column], size = cellSize, column = column, row = row)
                            }
                        }
                    }
                }

                // Anchored inside the grid's own box (not a separate row
                // below it) so its bubble — which points *up* toward the
                // character, per the default bubbleBelow=false — has real
                // screen space to grow into, and so this box gets to claim
                // the vertical room a separate character row used to take,
                // letting the grid itself render larger (see CELL_SIZE).
                // Bottom-*start*, not end: the bubble's own anchor is its
                // top-left corner (it grows rightward from there, up to
                // 220dp wide) — anchoring the character at the right edge
                // left no room for that growth and clipped the bubble.
                CharacterCallout(
                    characterCustomization = characterCustomization,
                    message = if (boardIsEmpty && connectFourState.outcome == ConnectFourOutcome.NONE) {
                        stringResource(R.string.david_goliath_choose_stones_instructions)
                    } else {
                        null
                    },
                    posture = if (isComplete) Posture.THUMBS_UP else Posture.STANDING,
                    modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
                    bubbleAboveClearance = 76.dp,
                )
            }

            if (canTryAgain) {
                Button(onClick = onReset, modifier = Modifier.padding(top = 8.dp)) {
                    Text(text = stringResource(R.string.david_goliath_choose_stones_try_again))
                }
            }

            if (previouslyCompleted && !isComplete) {
                Text(
                    text = stringResource(R.string.puzzle_already_completed_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

/**
 * Carries its own content description (row/column/state) separate from the
 * clickable column's — not read aloud in ordinary play (there's nothing
 * actionable about an individual cell), but lets an instrumented test read
 * the live board back without a shared mutable test hook.
 */
@Composable
private fun ConnectFourCell(slot: Slot, size: Dp, column: Int, row: Int) {
    val (background, border) = when (slot) {
        Slot.EMPTY -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.outline
        Slot.PLAYER -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.primary
        Slot.OPPONENT -> MaterialTheme.colorScheme.error to MaterialTheme.colorScheme.error
    }
    val slotLabel = when (slot) {
        Slot.EMPTY -> stringResource(R.string.david_goliath_choose_stones_cell_empty)
        Slot.PLAYER -> stringResource(R.string.david_goliath_choose_stones_cell_player)
        Slot.OPPONENT -> stringResource(R.string.david_goliath_choose_stones_cell_opponent)
    }
    val cellDescription = stringResource(R.string.david_goliath_choose_stones_cell_content_description, column + 1, row + 1, slotLabel)
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(background)
            .border(1.dp, border, CircleShape)
            .semantics { contentDescription = cellDescription },
    )
}

@Preview(showBackground = true)
@Composable
private fun DavidGoliathChooseStonesPreview() {
    BibleAdventuresTheme {
        DavidGoliathChooseStonesContent(
            connectFourState = ConnectFourGameState(),
            characterCustomization = CharacterCustomization(),
            onColumnTapped = {},
            onOpponentMove = {},
            onReset = {},
            onContinue = {},
            onBackToMainMenu = {},
        )
    }
}
