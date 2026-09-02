package com.bibleadventures.ui.screens.noahsark.findtools

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bibleadventures.R
import com.bibleadventures.domain.model.CharacterCustomization
import com.bibleadventures.game.puzzles.hiddenobject.HiddenObjectGameState
import com.bibleadventures.game.stories.NoahsArkContent
import com.bibleadventures.ui.components.AspectRatioFitBox
import com.bibleadventures.ui.components.CharacterCallout
import com.bibleadventures.ui.components.Posture
import com.bibleadventures.ui.components.PuzzleTopBar
import com.bibleadventures.ui.screens.noahsark.DecoyTapOutcome
import com.bibleadventures.ui.screens.noahsark.NoahsArkViewModel
import com.bibleadventures.ui.theme.BibleAdventuresTheme

/**
 * Unlike every other hidden-object scene in this app, the 10 tools here
 * aren't rendered as their own floating icons — they're already baked into
 * the single background image (`ic_noahs_ark_find_tools_scene`), invisible
 * to the player except by looking closely. Each hotspot's hit-box is sized
 * per-tool from [NoahsArkContent.findToolsHotspots] (pixel-matched against
 * the source art, not uniform), floored at 48dp so a tool with a small
 * visual footprint still stays comfortably tappable (spec section 9).
 */
@Composable
fun NoahsArkFindToolsScreen(
    viewModel: NoahsArkViewModel,
    onContinue: () -> Unit,
    onBackToMainMenu: () -> Unit,
    previouslyCompleted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val characterCustomization by viewModel.characterCustomization.collectAsStateWithLifecycle()

    NoahsArkFindToolsContent(
        hiddenObjectState = uiState.hiddenObjectState,
        wrongTapOutcome = uiState.lastFindToolsWrongTapOutcome,
        tapWasCorrect = uiState.lastFindToolsTapWasCorrect,
        characterCustomization = characterCustomization,
        onItemTapped = viewModel::onHiddenItemTapped,
        onBackgroundTapped = viewModel::onFindToolsBackgroundTapped,
        onContinue = onContinue,
        onBackToMainMenu = onBackToMainMenu,
        previouslyCompleted = previouslyCompleted,
        modifier = modifier,
    )
}

@Composable
private fun NoahsArkFindToolsContent(
    hiddenObjectState: HiddenObjectGameState,
    wrongTapOutcome: DecoyTapOutcome,
    tapWasCorrect: Boolean,
    characterCustomization: CharacterCustomization,
    onItemTapped: (String) -> Unit,
    onBackgroundTapped: () -> Unit,
    onContinue: () -> Unit,
    onBackToMainMenu: () -> Unit,
    previouslyCompleted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            if (previouslyCompleted || hiddenObjectState.isComplete) {
                PuzzleTopBar(
                    showBackButton = previouslyCompleted,
                    onBackToMainMenu = onBackToMainMenu,
                    showNextButton = hiddenObjectState.isComplete || previouslyCompleted,
                    onNext = onContinue,
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 8.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.noahs_ark_find_tools_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Text(
                text = stringResource(R.string.noahs_ark_find_tools_instructions),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = 16.dp).padding(top = 8.dp),
            )

            AspectRatioFitBox(
                ratio = NoahsArkContent.FIND_TOOLS_SCENE_ASPECT_RATIO,
                modifier = Modifier.weight(1f, fill = true).fillMaxSize(),
            ) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(R.drawable.ic_noahs_ark_find_tools_scene),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("find_tools_background")
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onBackgroundTapped,
                            ),
                    )
                    NoahsArkContent.findToolsHotspots.forEach { hotspot ->
                        val isFound = hotspot.id in hiddenObjectState.foundIds
                        val name = stringResource(hotspot.nameRes)
                        val hotspotWidth = (maxWidth * hotspot.size.x).coerceAtLeast(48.dp)
                        val hotspotHeight = (maxHeight * hotspot.size.y).coerceAtLeast(48.dp)

                        Box(
                            modifier = Modifier
                                .offset(
                                    x = maxWidth * hotspot.position.x - hotspotWidth / 2,
                                    y = maxHeight * hotspot.position.y - hotspotHeight / 2,
                                )
                                .size(width = hotspotWidth, height = hotspotHeight)
                                .clickable(enabled = !isFound, onClickLabel = name, onClick = { onItemTapped(hotspot.id) })
                                .semantics { contentDescription = name },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isFound) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }

                    CharacterCallout(
                        characterCustomization = characterCustomization,
                        message = if (wrongTapOutcome == DecoyTapOutcome.DECOY_TAPPED) stringResource(R.string.feedback_not_a_tool) else null,
                        posture = if (tapWasCorrect) Posture.THUMBS_UP else Posture.STANDING,
                        modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
                    )
                }
            }

            if (previouslyCompleted && !hiddenObjectState.isComplete) {
                Text(
                    text = stringResource(R.string.puzzle_already_completed_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp).padding(top = 8.dp),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NoahsArkFindToolsPreview() {
    BibleAdventuresTheme {
        NoahsArkFindToolsContent(
            hiddenObjectState = HiddenObjectGameState(items = emptyList()),
            wrongTapOutcome = DecoyTapOutcome.NONE,
            tapWasCorrect = false,
            characterCustomization = CharacterCustomization(),
            onItemTapped = {},
            onBackgroundTapped = {},
            onContinue = {},
            onBackToMainMenu = {},
        )
    }
}
