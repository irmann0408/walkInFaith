package com.bibleadventures.ui.screens.davidgoliath.dodge

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
import com.bibleadventures.ui.screens.davidgoliath.DavidGoliathViewModel
import com.bibleadventures.ui.theme.BibleAdventuresTheme
import kotlinx.coroutines.delay

/**
 * Discrete/self-paced, not real-time: once a hazard rolls into its lane it
 * just rests there until the player taps a side, with no clock — a
 * deliberate simplification versus Sling Practice, so "no reflex pressure"
 * stays unambiguous rather than relying on a generous tolerance. The roll-in
 * itself is a one-shot, bounded [tween] (not a looping animation), so it
 * plays out and settles on its own — Compose's idle-wait sync (used by the
 * instrumented test) advances through it automatically, unlike Sling
 * Practice's genuinely continuous mark, which needed the test clock frozen.
 *
 * David himself is rendered via [CharacterPreview] and slides to whichever
 * lane the player taps — added after on-device feedback that a rock rolling
 * in with no one reacting to it didn't read as "dodging" at all.
 */
@Composable
fun DavidGoliathDodgeScreen(
    viewModel: DavidGoliathViewModel,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val character by viewModel.characterCustomization.collectAsStateWithLifecycle()

    DavidGoliathDodgeContent(
        dodgeState = uiState.dodgeState,
        character = character,
        onLaneTapped = viewModel::onLaneTapped,
        onContinue = onContinue,
        modifier = modifier,
    )
}

@Composable
private fun DavidGoliathDodgeContent(
    dodgeState: DodgeGameState,
    character: CharacterCustomization,
    onLaneTapped: (DodgeLane) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Where David is currently standing, purely a presentation concern — the
    // engine only tracks outcomes, not the player's on-screen position.
    // -1 = left, 0 = center, 1 = right.
    val davidFraction = remember { Animatable(0f) }
    var lastTappedLane by remember { mutableStateOf<DodgeLane?>(null) }

    // What the rock currently shows — deliberately allowed to lag behind
    // dodgeState.currentBeat. The engine advances instantly on a tap, but if
    // the rock followed it directly it would reposition at the exact same
    // instant David starts his step animation, reading as "the rock moves
    // with David." Instead the rock only updates once David's full
    // step-hold-return sequence below has finished and he's back at center.
    var displayedBeat by remember { mutableStateOf(dodgeState.currentBeat) }

    // Sequenced, not two independent effects: stepping to a lane and
    // returning to center must happen one after another, not both triggered
    // by the same tap — an earlier version reset to center via a separate
    // effect keyed on the beat index, which fired on the very same tap that
    // advanced the beat (a correct dodge), canceling the step before it was
    // ever visible. David appeared to never leave center.
    LaunchedEffect(dodgeState) {
        val lane = lastTappedLane ?: return@LaunchedEffect
        davidFraction.animateTo(if (lane == DodgeLane.LEFT) -1f else 1f, animationSpec = tween(350))
        delay(450)
        davidFraction.animateTo(0f, animationSpec = tween(350))
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
                text = stringResource(R.string.david_goliath_dodge_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = stringResource(R.string.david_goliath_dodge_instructions),
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
                    painter = painterResource(R.drawable.bg_david_goliath_valley),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                if (!dodgeState.isComplete) {
                    val hazardLane = displayedBeat?.hazardLane
                    Row(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                            if (hazardLane == DodgeLane.LEFT) RockHazard(displayedBeat)
                        }
                        Box(modifier = Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                            if (hazardLane == DodgeLane.RIGHT) RockHazard(displayedBeat)
                        }
                    }
                }

                val laneDistance = maxWidth * 0.25f

                CharacterPreview(
                    customization = character,
                    modifier = Modifier
                        .size(100.dp)
                        .align(Alignment.BottomCenter)
                        .offset(x = laneDistance * davidFraction.value),
                )
            }

            if (!dodgeState.isComplete) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                ) {
                    AdventureMenuButton(
                        text = stringResource(R.string.david_goliath_dodge_lane_left),
                        onClick = { lastTappedLane = DodgeLane.LEFT; onLaneTapped(DodgeLane.LEFT) },
                        modifier = Modifier.weight(1f),
                    )
                    AdventureMenuButton(
                        text = stringResource(R.string.david_goliath_dodge_lane_right),
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

/**
 * Rolls into view whenever [beat] changes — i.e. only once a new hazard
 * genuinely appears, never on a wrong tap (the same beat is passed back
 * unchanged) and never in lockstep with David's own step animation, since
 * [beat] is the caller's deliberately-lagging `displayedBeat`, not the
 * engine's live current beat.
 */
@Composable
private fun RockHazard(beat: DodgeBeat?) {
    val name = stringResource(R.string.david_goliath_dodge_rock_content_description)
    val rollIn = remember { Animatable(0f) }
    LaunchedEffect(beat) {
        rollIn.snapTo(0f)
        rollIn.animateTo(1f, animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing))
    }
    Image(
        painter = painterResource(R.drawable.ic_rock_hazard),
        contentDescription = name,
        modifier = Modifier
            .size(56.dp)
            .graphicsLayer {
                translationY = rollIn.value * 60f
                rotationZ = rollIn.value * 540f
            },
    )
}

@Preview(showBackground = true)
@Composable
private fun DavidGoliathDodgePreview() {
    BibleAdventuresTheme {
        DavidGoliathDodgeContent(
            dodgeState = DodgeGameState(beats = emptyList()),
            character = CharacterCustomization(),
            onLaneTapped = {},
            onContinue = {},
        )
    }
}
