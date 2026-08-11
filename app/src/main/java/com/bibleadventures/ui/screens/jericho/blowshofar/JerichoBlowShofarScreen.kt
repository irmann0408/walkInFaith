package com.bibleadventures.ui.screens.jericho.blowshofar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bibleadventures.R
import com.bibleadventures.game.puzzles.sequence.SequenceGameState
import com.bibleadventures.game.puzzles.sequence.SequenceOutcome
import com.bibleadventures.game.stories.JerichoContent
import com.bibleadventures.game.stories.ShofarNoteDef
import com.bibleadventures.ui.components.AdventureMenuButton
import com.bibleadventures.ui.screens.jericho.JerichoViewModel
import com.bibleadventures.ui.theme.BibleAdventuresTheme

/**
 * Reuses `game/puzzles/sequence` exactly as-is (already Daniel's Lions'
 * Den mechanic) — tap the 5 colored notes in the right order to sound the
 * shofar. As in Lions' Den, the order isn't told upfront: an out-of-order
 * tap safely re-prompts without losing progress, so the order is
 * discovered by trying, never punished.
 */
@Composable
fun JerichoBlowShofarScreen(
    viewModel: JerichoViewModel,
    onContinue: () -> Unit,
    previouslyCompleted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    JerichoBlowShofarContent(
        shofarState = uiState.shofarState,
        onNoteTapped = viewModel::onShofarNoteTapped,
        onContinue = onContinue,
        previouslyCompleted = previouslyCompleted,
        modifier = modifier,
    )
}

private fun colorFor(noteId: String): Color = when (noteId) {
    "red" -> Color(0xFFE53935)
    "orange" -> Color(0xFFFB8C00)
    "yellow" -> Color(0xFFFDD835)
    "green" -> Color(0xFF43A047)
    "blue" -> Color(0xFF1E88E5)
    else -> Color.Gray
}

@Composable
private fun JerichoBlowShofarContent(
    shofarState: SequenceGameState,
    onNoteTapped: (String) -> Unit,
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
                text = stringResource(R.string.jericho_blow_shofar_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = stringResource(R.string.jericho_blow_shofar_instructions),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp),
            )

            val feedback = when (shofarState.lastOutcome) {
                SequenceOutcome.POINT_CONNECTED, SequenceOutcome.COMPLETE -> stringResource(R.string.feedback_great_job)
                SequenceOutcome.OUT_OF_ORDER -> stringResource(R.string.feedback_try_another_one)
                SequenceOutcome.NONE -> ""
            }
            Box(modifier = Modifier.height(32.dp)) {
                Text(text = feedback, style = MaterialTheme.typography.titleLarge)
            }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
            ) {
                val connectedOffsets = shofarState.connectedIds.map { id ->
                    JerichoContent.shofarNotes.first { it.id == id }.position
                }
                if (connectedOffsets.size > 1) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        for (i in 0 until connectedOffsets.size - 1) {
                            drawLine(
                                color = Color(0xFFFFD54A),
                                start = Offset(size.width * connectedOffsets[i].x, size.height * connectedOffsets[i].y),
                                end = Offset(size.width * connectedOffsets[i + 1].x, size.height * connectedOffsets[i + 1].y),
                                strokeWidth = 6f,
                            )
                        }
                    }
                }

                JerichoContent.shofarNotes.forEach { note ->
                    NoteTarget(
                        note = note,
                        isConnected = note.id in shofarState.connectedIds,
                        onClick = { onNoteTapped(note.id) },
                        modifier = Modifier.offset(x = maxWidth * note.position.x - 24.dp, y = maxHeight * note.position.y - 24.dp),
                    )
                }
            }

            if (previouslyCompleted && !shofarState.isComplete) {
                Text(
                    text = stringResource(R.string.puzzle_already_completed_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            if (shofarState.isComplete || previouslyCompleted) {
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
private fun NoteTarget(
    note: ShofarNoteDef,
    isConnected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val name = stringResource(note.nameRes)

    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(colorFor(note.id))
            .clickable(onClickLabel = name, onClick = onClick)
            .semantics { contentDescription = name },
        contentAlignment = Alignment.Center,
    ) {
        if (isConnected) {
            Icon(imageVector = Icons.Filled.Check, contentDescription = null, tint = Color.White)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun JerichoBlowShofarPreview() {
    BibleAdventuresTheme {
        JerichoBlowShofarContent(
            shofarState = SequenceGameState(pointIds = JerichoContent.shofarNotes.map { it.id }),
            onNoteTapped = {},
            onContinue = {},
        )
    }
}
