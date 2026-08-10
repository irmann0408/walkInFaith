package com.bibleadventures.ui.screens.davidgoliath.dodge

import androidx.compose.foundation.Image
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bibleadventures.R
import com.bibleadventures.game.puzzles.dodge.DodgeGameState
import com.bibleadventures.game.puzzles.dodge.DodgeLane
import com.bibleadventures.game.puzzles.dodge.DodgeOutcome
import com.bibleadventures.ui.components.AdventureMenuButton
import com.bibleadventures.ui.screens.davidgoliath.DavidGoliathViewModel
import com.bibleadventures.ui.theme.BibleAdventuresTheme

/**
 * Discrete/self-paced, not real-time: the hazard just rests visibly in one
 * lane until the player taps a side, with no clock or continuous animation —
 * a deliberate simplification versus Sling Practice, so "no reflex pressure"
 * stays unambiguous rather than relying on a generous tolerance.
 */
@Composable
fun DavidGoliathDodgeScreen(
    viewModel: DavidGoliathViewModel,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DavidGoliathDodgeContent(
        dodgeState = uiState.dodgeState,
        onLaneTapped = viewModel::onLaneTapped,
        onContinue = onContinue,
        modifier = modifier,
    )
}

@Composable
private fun DavidGoliathDodgeContent(
    dodgeState: DodgeGameState,
    onLaneTapped: (DodgeLane) -> Unit,
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

            Box(
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
                    val hazardLane = dodgeState.currentBeat?.hazardLane
                    Row(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) {
                            if (hazardLane == DodgeLane.LEFT) RockHazard()
                        }
                        Box(modifier = Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) {
                            if (hazardLane == DodgeLane.RIGHT) RockHazard()
                        }
                    }
                }
            }

            if (!dodgeState.isComplete) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                ) {
                    AdventureMenuButton(
                        text = stringResource(R.string.david_goliath_dodge_lane_left),
                        onClick = { onLaneTapped(DodgeLane.LEFT) },
                        modifier = Modifier.weight(1f),
                    )
                    AdventureMenuButton(
                        text = stringResource(R.string.david_goliath_dodge_lane_right),
                        onClick = { onLaneTapped(DodgeLane.RIGHT) },
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

@Composable
private fun RockHazard() {
    val name = stringResource(R.string.david_goliath_dodge_rock_content_description)
    Image(
        painter = painterResource(R.drawable.ic_rock_hazard),
        contentDescription = name,
        modifier = Modifier.size(56.dp),
    )
}

@Preview(showBackground = true)
@Composable
private fun DavidGoliathDodgePreview() {
    BibleAdventuresTheme {
        DavidGoliathDodgeContent(
            dodgeState = DodgeGameState(beats = emptyList()),
            onLaneTapped = {},
            onContinue = {},
        )
    }
}
