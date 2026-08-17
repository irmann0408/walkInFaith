package com.bibleadventures.ui.screens.jericho.shout

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bibleadventures.R
import com.bibleadventures.game.stories.JerichoContent
import com.bibleadventures.ui.components.PuzzleTopBar
import com.bibleadventures.ui.screens.jericho.JerichoViewModel
import com.bibleadventures.ui.theme.BibleAdventuresTheme

/**
 * The grand-finale button mash: not a new engine, just a plain tap counter
 * on [com.bibleadventures.ui.screens.jericho.JerichoUiState] (`shoutTaps`)
 * — matches this app's own precedent for genuinely trivial counters, a
 * third micro-engine would be overkill for "tap N times." Every tap always
 * makes progress; reaching the target swaps the wall art and plays the
 * trumpet fanfare exactly once.
 */
@Composable
fun JerichoShoutScreen(
    viewModel: JerichoViewModel,
    onContinue: () -> Unit,
    onBackToMainMenu: () -> Unit,
    previouslyCompleted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    JerichoShoutContent(
        shoutTaps = uiState.shoutTaps,
        requiredTaps = JerichoContent.SHOUT_REQUIRED_TAPS,
        isComplete = uiState.isShoutComplete,
        onShoutTapped = viewModel::onShoutTapped,
        onContinue = onContinue,
        onBackToMainMenu = onBackToMainMenu,
        previouslyCompleted = previouslyCompleted,
        modifier = modifier,
    )
}

@Composable
private fun JerichoShoutContent(
    shoutTaps: Int,
    requiredTaps: Int,
    isComplete: Boolean,
    onShoutTapped: () -> Unit,
    onContinue: () -> Unit,
    onBackToMainMenu: () -> Unit,
    previouslyCompleted: Boolean = false,
    modifier: Modifier = Modifier,
) {
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
                text = stringResource(R.string.jericho_shout_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = stringResource(R.string.jericho_shout_instructions),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp),
            )

            Image(
                painter = painterResource(if (isComplete) R.drawable.ic_jericho_wall_fallen else R.drawable.ic_jericho_wall_intact),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(120.dp).padding(vertical = 16.dp),
            )

            val progressFraction = (shoutTaps.toFloat() / requiredTaps).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progressFraction)
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primary),
                )
            }

            // weight(1f, fill = true) hands this element exactly the space left
            // over after every other (naturally-sized) sibling in this Column, so
            // nothing here ever needs to scroll; the tap circle itself is sized
            // relative to whatever's available, capped so it doesn't balloon on
            // tall/tablet screens.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true),
                contentAlignment = Alignment.Center,
            ) {
                if (!isComplete) {
                    val shoutDescription = stringResource(R.string.jericho_shout_button_content_description)
                    Box(
                        modifier = Modifier
                            .fillMaxHeight(0.8f)
                            .aspectRatio(1f)
                            .sizeIn(maxWidth = 200.dp, maxHeight = 200.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error)
                            .clickable(onClickLabel = shoutDescription, onClick = onShoutTapped)
                            .semantics { contentDescription = shoutDescription },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.jericho_shout_title),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onError,
                        )
                    }
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

@Preview(showBackground = true)
@Composable
private fun JerichoShoutPreview() {
    BibleAdventuresTheme {
        JerichoShoutContent(
            shoutTaps = 5,
            requiredTaps = 15,
            isComplete = false,
            onShoutTapped = {},
            onContinue = {},
            onBackToMainMenu = {},
        )
    }
}
