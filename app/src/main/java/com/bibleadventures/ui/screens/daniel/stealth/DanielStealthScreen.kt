package com.bibleadventures.ui.screens.daniel.stealth

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bibleadventures.R
import com.bibleadventures.domain.model.CharacterCustomization
import com.bibleadventures.game.puzzles.dodge.DodgeBeat
import com.bibleadventures.game.puzzles.dodge.DodgeGameState
import com.bibleadventures.game.puzzles.dodge.DodgeLane
import com.bibleadventures.game.puzzles.dodge.DodgeOutcome
import com.bibleadventures.ui.components.AdventureMenuButton
import com.bibleadventures.ui.components.CharacterPreview
import com.bibleadventures.ui.screens.daniel.DanielViewModel
import com.bibleadventures.ui.theme.BibleAdventuresTheme
import kotlinx.coroutines.delay

/**
 * A literal reskin of [com.bibleadventures.ui.screens.davidgoliath.dodge.DavidGoliathDodgeScreen] —
 * same [com.bibleadventures.game.puzzles.dodge.DodgeGame] engine, same
 * discrete/self-paced structure, only the art and copy change. An official
 * blocks one side of the hallway; Daniel steps to the clear side to keep
 * hurrying toward his prayer room — framed as getting past, not hiding,
 * since Daniel 6:10 has him praying openly once he arrives.
 */
@Composable
fun DanielStealthScreen(
    viewModel: DanielViewModel,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val character by viewModel.characterCustomization.collectAsStateWithLifecycle()

    DanielStealthContent(
        dodgeState = uiState.dodgeState,
        character = character,
        onLaneTapped = viewModel::onLaneTapped,
        onContinue = onContinue,
        modifier = modifier,
    )
}

@Composable
private fun DanielStealthContent(
    dodgeState: DodgeGameState,
    character: CharacterCustomization,
    onLaneTapped: (DodgeLane) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // -1 = left, 0 = center, 1 = right. Purely presentational, mirrors
    // DavidGoliathDodgeScreen's davidFraction exactly.
    val danielFraction = remember { Animatable(0f) }
    var lastTappedLane by remember { mutableStateOf<DodgeLane?>(null) }

    // Deliberately lags dodgeState.currentBeat — see DavidGoliathDodgeScreen's
    // displayedBeat for why (avoids the hazard appearing to move with Daniel).
    var displayedBeat by remember { mutableStateOf(dodgeState.currentBeat) }

    LaunchedEffect(dodgeState) {
        val lane = lastTappedLane ?: return@LaunchedEffect
        danielFraction.animateTo(if (lane == DodgeLane.LEFT) -1f else 1f, animationSpec = tween(350))
        delay(450)
        danielFraction.animateTo(0f, animationSpec = tween(350))
        displayedBeat = dodgeState.currentBeat
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

            val feedback = when (dodgeState.lastOutcome) {
                DodgeOutcome.DODGED -> stringResource(R.string.feedback_great_job)
                DodgeOutcome.TRY_AGAIN -> stringResource(R.string.feedback_try_another_one)
                DodgeOutcome.NONE -> ""
            }
            Box(modifier = Modifier.height(32.dp)) {
                Text(text = feedback, style = MaterialTheme.typography.titleLarge)
            }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.6f),
            ) {
                Image(
                    painter = painterResource(R.drawable.bg_daniel_hallway),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                if (!dodgeState.isComplete) {
                    val hazardLane = displayedBeat?.hazardLane
                    Row(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                            if (hazardLane == DodgeLane.LEFT) OfficialHazard(displayedBeat)
                        }
                        Box(modifier = Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                            if (hazardLane == DodgeLane.RIGHT) OfficialHazard(displayedBeat)
                        }
                    }
                }

                val laneDistance = maxWidth * 0.25f

                CharacterPreview(
                    customization = character,
                    modifier = Modifier
                        .size(100.dp)
                        .align(Alignment.BottomCenter)
                        .offset(x = laneDistance * danielFraction.value),
                )
            }

            if (!dodgeState.isComplete) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                ) {
                    AdventureMenuButton(
                        text = stringResource(R.string.daniel_stealth_lane_left),
                        onClick = { lastTappedLane = DodgeLane.LEFT; onLaneTapped(DodgeLane.LEFT) },
                        modifier = Modifier.weight(1f),
                    )
                    AdventureMenuButton(
                        text = stringResource(R.string.daniel_stealth_lane_right),
                        onClick = { lastTappedLane = DodgeLane.RIGHT; onLaneTapped(DodgeLane.RIGHT) },
                        modifier = Modifier.weight(1f),
                    )
                }
            } else {
                AdventureMenuButton(
                    text = stringResource(R.string.action_continue),
                    onClick = onContinue,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
        }
    }
}

/** Rolls into view whenever [beat] changes, mirroring DavidGoliathDodgeScreen's RockHazard. */
@Composable
private fun OfficialHazard(beat: DodgeBeat?) {
    val name = stringResource(R.string.daniel_stealth_official_content_description)
    val rollIn = remember { Animatable(0f) }
    LaunchedEffect(beat) {
        rollIn.snapTo(0f)
        rollIn.animateTo(1f, animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing))
    }
    Image(
        painter = painterResource(R.drawable.ic_official_marker),
        contentDescription = name,
        modifier = Modifier
            .size(56.dp)
            .graphicsLayer {
                translationY = rollIn.value * 60f
            },
    )
}

@Preview(showBackground = true)
@Composable
private fun DanielStealthPreview() {
    BibleAdventuresTheme {
        DanielStealthContent(
            dodgeState = DodgeGameState(beats = emptyList()),
            character = CharacterCustomization(),
            onLaneTapped = {},
            onContinue = {},
        )
    }
}
