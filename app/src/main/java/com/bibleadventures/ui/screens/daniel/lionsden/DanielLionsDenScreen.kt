package com.bibleadventures.ui.screens.daniel.lionsden

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bibleadventures.R
import com.bibleadventures.game.puzzles.sequence.SequenceGameState
import com.bibleadventures.game.puzzles.sequence.SequenceOutcome
import com.bibleadventures.game.stories.DanielContent
import com.bibleadventures.ui.components.AdventureMenuButton
import com.bibleadventures.ui.screens.daniel.DanielViewModel
import com.bibleadventures.ui.theme.BibleAdventuresTheme

/**
 * The "connect in order" puzzle for the Lions' Den — a new small pure engine
 * ([com.bibleadventures.game.puzzles.sequence]), chosen over reusing an
 * existing one. Tapping the 5 lights in order draws a polyline between them
 * (mirroring Sling Practice's dashed trajectory line, solid here since it
 * marks real progress rather than a live aim) and, once complete, calms the
 * lions — a shape change (ic_lion_pacing -> ic_lion_calm), never color-only.
 */
@Composable
fun DanielLionsDenScreen(
    viewModel: DanielViewModel,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DanielLionsDenContent(
        sequenceState = uiState.sequenceState,
        onPointTapped = viewModel::onLightPointTapped,
        onContinue = onContinue,
        modifier = modifier,
    )
}

@Composable
private fun DanielLionsDenContent(
    sequenceState: SequenceGameState,
    onPointTapped: (String) -> Unit,
    onContinue: () -> Unit,
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
                text = stringResource(R.string.daniel_lions_den_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = stringResource(R.string.daniel_lions_den_instructions),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp),
            )

            val feedback = when (sequenceState.lastOutcome) {
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
                Image(
                    painter = painterResource(R.drawable.bg_daniel_den),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )

                val lionIcon = if (sequenceState.isComplete) R.drawable.ic_lion_calm else R.drawable.ic_lion_pacing
                val lionsDescription = stringResource(R.string.daniel_lions_den_lions_content_description)
                Row(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    Image(painter = painterResource(lionIcon), contentDescription = lionsDescription, modifier = Modifier.size(72.dp))
                    Image(painter = painterResource(lionIcon), contentDescription = null, modifier = Modifier.size(72.dp))
                }

                val connectedOffsets = sequenceState.connectedIds.map { id ->
                    DanielContent.lionsDenPoints.first { it.id == id }.position
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

                DanielContent.lionsDenPoints.forEach { point ->
                    LightPointTarget(
                        nameRes = point.nameRes,
                        isConnected = point.id in sequenceState.connectedIds,
                        onClick = { onPointTapped(point.id) },
                        modifier = Modifier.offset(x = maxWidth * point.position.x - 24.dp, y = maxHeight * point.position.y - 24.dp),
                    )
                }
            }

            if (sequenceState.isComplete) {
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
private fun LightPointTarget(
    nameRes: Int,
    isConnected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val name = stringResource(nameRes)

    Box(
        modifier = modifier
            .size(48.dp)
            .clickable(onClickLabel = name, onClick = onClick)
            .semantics { contentDescription = name },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(if (isConnected) R.drawable.ic_light_point_lit else R.drawable.ic_light_point),
            contentDescription = null,
            modifier = Modifier.size(40.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DanielLionsDenPreview() {
    BibleAdventuresTheme {
        DanielLionsDenContent(
            sequenceState = SequenceGameState(pointIds = DanielContent.lionsDenPointIds),
            onPointTapped = {},
            onContinue = {},
        )
    }
}
