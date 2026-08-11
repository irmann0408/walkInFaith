package com.bibleadventures.ui.screens.esther.messengersudoku

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bibleadventures.R
import com.bibleadventures.game.puzzles.sudoku.SudokuGameState
import com.bibleadventures.game.puzzles.sudoku.SudokuOutcome
import com.bibleadventures.game.stories.EstherContent
import com.bibleadventures.game.stories.SudokuIconDef
import com.bibleadventures.ui.components.AdventureMenuButton
import com.bibleadventures.ui.screens.esther.EstherViewModel
import com.bibleadventures.ui.theme.BibleAdventuresTheme

@Composable
fun EstherMessengerSudokuScreen(
    viewModel: EstherViewModel,
    onContinue: () -> Unit,
    previouslyCompleted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    EstherMessengerSudokuContent(
        sudokuState = uiState.sudokuState,
        selectedCell = uiState.selectedSudokuCell,
        onCellSelected = viewModel::onSudokuCellSelected,
        onIconTapped = viewModel::onSudokuIconTapped,
        onContinue = onContinue,
        previouslyCompleted = previouslyCompleted,
        modifier = modifier,
    )
}

@Composable
private fun EstherMessengerSudokuContent(
    sudokuState: SudokuGameState,
    selectedCell: Pair<Int, Int>?,
    onCellSelected: (Int, Int) -> Unit,
    onIconTapped: (String) -> Unit,
    onContinue: () -> Unit,
    previouslyCompleted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.esther_threat_sudoku_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = stringResource(R.string.esther_threat_sudoku_instructions),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp),
            )

            val feedback = when (sudokuState.lastOutcome) {
                SudokuOutcome.CONFLICT -> stringResource(R.string.esther_threat_conflict_feedback)
                SudokuOutcome.ROW_COMPLETE, SudokuOutcome.COMPLETE -> stringResource(R.string.esther_threat_row_complete_feedback)
                else -> ""
            }
            Box(modifier = Modifier.height(28.dp)) {
                Text(text = feedback, style = MaterialTheme.typography.titleMedium)
            }

            MessengerTracker(completedRowCount = sudokuState.completedRows.size, totalRows = sudokuState.size)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            ) {
                for (row in 0 until sudokuState.size) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (col in 0 until sudokuState.size) {
                            SudokuCell(
                                row = row,
                                col = col,
                                value = sudokuState.valueAt(row, col),
                                isGiven = sudokuState.givens.containsKey(row to col),
                                isSelected = selectedCell == (row to col),
                                onClick = { onCellSelected(row, col) },
                                modifier = Modifier.weight(1f).aspectRatio(1f),
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                EstherContent.sudokuIcons.forEach { icon ->
                    IconPaletteButton(
                        icon = icon,
                        enabled = selectedCell != null,
                        onClick = { onIconTapped(icon.key) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            if (previouslyCompleted && !sudokuState.isComplete) {
                Text(
                    text = stringResource(R.string.puzzle_already_completed_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            if (sudokuState.isComplete || previouslyCompleted) {
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
private fun MessengerTracker(completedRowCount: Int, totalRows: Int) {
    val messengerDescription = stringResource(R.string.esther_threat_messenger_content_description)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(totalRows) { index ->
            Image(
                painter = painterResource(R.drawable.ic_messenger),
                contentDescription = if (index == 0) messengerDescription else null,
                modifier = Modifier
                    .size(28.dp)
                    .alpha(if (index < completedRowCount) 1f else 0.25f),
            )
        }
    }
}

@Composable
private fun SudokuCell(
    row: Int,
    col: Int,
    value: String?,
    isGiven: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val icon = EstherContent.sudokuIcons.find { it.key == value }
    val iconName = icon?.let { stringResource(it.nameRes) }
    val positionLabel = stringResource(R.string.esther_threat_sudoku_cell_content_description, row + 1, col + 1)
    val cellDescription = if (iconName != null) "$positionLabel: $iconName" else positionLabel
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val background = if (isGiven) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface

    Box(
        modifier = modifier
            .padding(2.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(background)
            .border(2.dp, borderColor, RoundedCornerShape(6.dp))
            .clickable(enabled = !isGiven, onClickLabel = cellDescription, onClick = onClick)
            .semantics { contentDescription = cellDescription },
        contentAlignment = Alignment.Center,
    ) {
        if (icon != null) {
            // No contentDescription here — the outer Box's cellDescription already
            // carries both the position and the icon name, and duplicating the bare
            // icon name here would collide with the palette buttons below, which use
            // that exact same bare name as their own contentDescription.
            Image(
                painter = painterResource(icon.iconRes),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth(0.7f).aspectRatio(1f),
            )
        }
    }
}

@Composable
private fun IconPaletteButton(
    icon: SudokuIconDef,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val name = stringResource(icon.nameRes)
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(enabled = enabled, onClickLabel = name, onClick = onClick)
            .alpha(if (enabled) 1f else 0.4f)
            .semantics { contentDescription = name },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(icon.iconRes),
            contentDescription = null,
            modifier = Modifier.fillMaxWidth(0.6f).aspectRatio(1f),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EstherMessengerSudokuPreview() {
    BibleAdventuresTheme {
        EstherMessengerSudokuContent(
            sudokuState = SudokuGameState(size = 5, givens = EstherContent.sudokuGivens),
            selectedCell = null,
            onCellSelected = { _, _ -> },
            onIconTapped = {},
            onContinue = {},
        )
    }
}
