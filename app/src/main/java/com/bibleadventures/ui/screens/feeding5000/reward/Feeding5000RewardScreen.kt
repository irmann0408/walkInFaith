package com.bibleadventures.ui.screens.feeding5000.reward

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bibleadventures.R
import com.bibleadventures.game.rewards.Feeding5000Reward
import com.bibleadventures.game.rewards.RewardCalculator
import com.bibleadventures.ui.components.AdventureMenuButton
import com.bibleadventures.ui.components.BadgeView
import com.bibleadventures.ui.components.ScriptureCardView
import com.bibleadventures.ui.screens.feeding5000.Feeding5000RewardResult
import com.bibleadventures.ui.screens.feeding5000.Feeding5000ViewModel
import com.bibleadventures.ui.theme.BibleAdventuresTheme

@Composable
fun Feeding5000RewardScreen(
    viewModel: Feeding5000ViewModel,
    onReturnToMap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.onChapterFinished() }

    Feeding5000RewardContent(
        reward = uiState.reward,
        onReturnToMap = onReturnToMap,
        modifier = modifier,
    )
}

@Composable
private fun Feeding5000RewardContent(
    reward: Feeding5000RewardResult?,
    onReturnToMap: () -> Unit,
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
            Text(text = stringResource(R.string.reward_title), style = MaterialTheme.typography.headlineLarge)

            AnimatedVisibility(visible = reward != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val stars = reward?.stars ?: 0
                    val starsContentDescription = stringResource(
                        R.string.reward_stars_content_description,
                        stars,
                        RewardCalculator.MAX_STARS,
                    )
                    Text(
                        text = "⭐".repeat(stars),
                        style = MaterialTheme.typography.headlineLarge,
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .semantics { contentDescription = starsContentDescription },
                    )

                    BadgeView(
                        badge = Feeding5000Reward.badge,
                        title = stringResource(Feeding5000Reward.badge.titleRes),
                        description = stringResource(Feeding5000Reward.badge.descriptionRes),
                        modifier = Modifier.padding(top = 24.dp),
                    )

                    ScriptureCardView(
                        reference = Feeding5000Reward.scriptureCard.reference,
                        text = stringResource(Feeding5000Reward.scriptureCard.textRes),
                        modifier = Modifier
                            .widthIn(max = 480.dp)
                            .padding(top = 24.dp),
                    )

                    AdventureMenuButton(
                        text = stringResource(R.string.action_return_to_map),
                        onClick = onReturnToMap,
                        modifier = Modifier
                            .widthIn(max = 320.dp)
                            .padding(top = 32.dp),
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun Feeding5000RewardPreview() {
    BibleAdventuresTheme {
        Feeding5000RewardContent(reward = Feeding5000RewardResult(stars = 3), onReturnToMap = {})
    }
}
