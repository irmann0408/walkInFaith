package com.bibleadventures.ui.screens.daniel.stealth

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bibleadventures.R
import com.bibleadventures.domain.model.CharacterCustomization
import com.bibleadventures.game.puzzles.rhythmlane.NoteJudgment
import com.bibleadventures.game.puzzles.rhythmlane.RhythmLaneChart
import com.bibleadventures.game.puzzles.rhythmlane.RhythmLaneGameState
import com.bibleadventures.game.stories.DanielContent
import com.bibleadventures.ui.LocalReducedMotion
import com.bibleadventures.ui.components.AdventureMenuButton
import com.bibleadventures.ui.components.CharacterPreview
import com.bibleadventures.ui.screens.daniel.DanielViewModel
import com.bibleadventures.ui.theme.BibleAdventuresTheme
import kotlinx.coroutines.isActive

private const val LANE_COUNT = 3
private val OFFICIAL_SIZE = 40.dp
private val CHARACTER_SIZE = 72.dp
private const val TRAVEL_DURATION_MS = 1500L
private const val NOTE_GRACE_MS = 300L

/**
 * A literal reskin of [com.bibleadventures.ui.screens.davidgoliath.dodge.DavidGoliathDodgeScreen] —
 * same `rhythmlane`-via-`onLaneAvoided` shape, only the art and copy
 * change. An official blocks one of the 3 lanes at a time; Daniel steers
 * himself out of the way to keep hurrying toward his prayer room — framed
 * as getting past, not hiding, since Daniel 6:10 has him praying openly
 * once he arrives. `requiredHits = 3`.
 */
@Composable
fun DanielStealthScreen(
    viewModel: DanielViewModel,
    onContinue: () -> Unit,
    previouslyCompleted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val character by viewModel.characterCustomization.collectAsStateWithLifecycle()

    DanielStealthContent(
        hurryToPrayState = uiState.hurryToPrayState,
        characterLane = uiState.characterLane,
        character = character,
        onLaneMoved = viewModel::onHurryToPrayLaneMoved,
        onTimeAdvanced = viewModel::onHurryToPrayTimeAdvanced,
        onContinue = onContinue,
        previouslyCompleted = previouslyCompleted,
        modifier = modifier,
    )
}

@Composable
private fun DanielStealthContent(
    hurryToPrayState: RhythmLaneGameState,
    characterLane: Int,
    character: CharacterCustomization,
    onLaneMoved: (Int) -> Unit,
    onTimeAdvanced: (Long) -> Unit,
    onContinue: () -> Unit,
    previouslyCompleted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var elapsedMs by remember { mutableLongStateOf(0L) }
    val isComplete = hurryToPrayState.isComplete

    LaunchedEffect(isComplete) {
        if (isComplete) return@LaunchedEffect
        var startFrameNanos = -1L
        while (isActive) {
            withFrameNanos { frameNanos ->
                if (startFrameNanos < 0) startFrameNanos = frameNanos
                elapsedMs = (frameNanos - startFrameNanos) / 1_000_000
            }
            onTimeAdvanced(elapsedMs)
        }
    }

    val feedback = when (hurryToPrayState.lastJudgment) {
        NoteJudgment.PERFECT, NoteJudgment.GREAT -> stringResource(R.string.feedback_great_job)
        NoteJudgment.MISSED -> stringResource(R.string.feedback_try_another_one)
        null -> ""
    }

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.daniel_stealth_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = stringResource(R.string.daniel_stealth_instructions),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = stringResource(R.string.daniel_stealth_progress_label, hurryToPrayState.hits, hurryToPrayState.requiredHits),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
            )

            Box(modifier = Modifier.height(28.dp).padding(top = 4.dp)) {
                Text(text = feedback, style = MaterialTheme.typography.titleMedium)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .height(20.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(hurryToPrayState.progressFraction)
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primary),
                )
            }

            if (!isComplete) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(modifier = Modifier.fillMaxWidth().aspectRatio(1.6f)) {
                        Image(
                            painter = painterResource(R.drawable.bg_daniel_hallway),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().weight(1f, fill = true),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                repeat(LANE_COUNT) { lane ->
                                    FallingOfficialLane(
                                        lane = lane,
                                        chart = hurryToPrayState.chart,
                                        judgedNoteKeys = hurryToPrayState.judgedNoteKeys,
                                        elapsedMs = elapsedMs,
                                        modifier = Modifier.weight(1f).fillMaxHeight(),
                                    )
                                }
                            }

                            SingleCharacterTrack(
                                characterLane = characterLane,
                                character = character,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            )
                        }
                    }

                    LaneMoveControls(onLaneMoved = onLaneMoved, modifier = Modifier.padding(top = 12.dp))
                }
            }

            if (previouslyCompleted && !isComplete) {
                Text(
                    text = stringResource(R.string.puzzle_already_completed_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            if (isComplete || previouslyCompleted) {
                AdventureMenuButton(
                    text = stringResource(R.string.action_continue),
                    onClick = onContinue,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
        }
    }
}

/** Officials only — no character, no click handling; the single character in [SingleCharacterTrack] is what dodges. */
@Composable
private fun FallingOfficialLane(
    lane: Int,
    chart: RhythmLaneChart,
    judgedNoteKeys: Set<String>,
    elapsedMs: Long,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier) {
        val trackHeight = maxHeight
        visibleNotes(chart, lane, judgedNoteKeys, elapsedMs).forEach { msUntilHit ->
            val fraction = (1f - msUntilHit.toFloat() / TRAVEL_DURATION_MS).coerceIn(0f, 1f)
            Image(
                painter = painterResource(R.drawable.ic_official_marker),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (trackHeight - OFFICIAL_SIZE) * fraction)
                    .size(OFFICIAL_SIZE),
            )
        }
    }
}

/** Daniel, sliding to whichever of the 3 lanes [characterLane] names — same width as the [FallingOfficialLane] row above it, so his standing spot lines up under each lane. */
@Composable
private fun SingleCharacterTrack(characterLane: Int, character: CharacterCustomization, modifier: Modifier = Modifier) {
    val characterDescription = stringResource(R.string.daniel_stealth_character_content_description, characterLane + 1)

    val reducedMotion = LocalReducedMotion.current
    BoxWithConstraints(modifier = modifier.height(96.dp)) {
        val laneWidth = maxWidth / LANE_COUNT
        val characterOffsetX by animateDpAsState(
            targetValue = laneWidth * characterLane + (laneWidth - CHARACTER_SIZE) / 2,
            animationSpec = if (reducedMotion) snap() else spring(),
            label = "hurryToPrayCharacterOffsetX",
        )
        Box(
            modifier = Modifier
                .offset(x = characterOffsetX)
                .align(Alignment.BottomStart)
                .semantics { contentDescription = characterDescription },
        ) {
            CharacterPreview(customization = character, modifier = Modifier.size(CHARACTER_SIZE))
        }
    }
}

@Composable
private fun LaneMoveControls(onLaneMoved: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(56.dp)) {
        MoveButton(
            icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            contentDescription = stringResource(R.string.daniel_stealth_move_left_content_description),
            onClick = { onLaneMoved(-1) },
        )
        MoveButton(
            icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = stringResource(R.string.daniel_stealth_move_right_content_description),
            onClick = { onLaneMoved(1) },
        )
    }
}

@Composable
private fun MoveButton(icon: ImageVector, contentDescription: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .clickable(onClickLabel = contentDescription, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription, modifier = Modifier.size(32.dp))
    }
}

private fun visibleNotes(chart: RhythmLaneChart, lane: Int, judgedNoteKeys: Set<String>, elapsedMs: Long): List<Long> {
    val currentLoopIndex = elapsedMs / chart.loopDurationMs
    return chart.notes
        .filter { it.lane == lane }
        .flatMap { note ->
            (currentLoopIndex..currentLoopIndex + 1).mapNotNull { loopIndex ->
                if ("$loopIndex:${note.id}" in judgedNoteKeys) return@mapNotNull null
                val msUntilHit = (loopIndex * chart.loopDurationMs + note.hitTimeMs) - elapsedMs
                msUntilHit.takeIf { it in -NOTE_GRACE_MS..TRAVEL_DURATION_MS }
            }
        }
}

@Preview(showBackground = true)
@Composable
private fun DanielStealthPreview() {
    BibleAdventuresTheme {
        DanielStealthContent(
            hurryToPrayState = RhythmLaneGameState(
                chart = DanielContent.hurryToPrayChart,
                requiredHits = DanielContent.HURRY_TO_PRAY_REQUIRED_AVOIDS,
            ),
            characterLane = 1,
            character = CharacterCustomization(),
            onLaneMoved = {},
            onTimeAdvanced = {},
            onContinue = {},
        )
    }
}
