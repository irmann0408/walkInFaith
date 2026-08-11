package com.bibleadventures.ui.screens.noahsark.intro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bibleadventures.R
import com.bibleadventures.audio.MusicTrack
import com.bibleadventures.domain.model.CharacterCustomization
import com.bibleadventures.game.stories.NoahsArkContent
import com.bibleadventures.ui.LocalAudioController
import com.bibleadventures.ui.components.AdventureMenuButton
import com.bibleadventures.ui.components.CharacterPreview
import com.bibleadventures.ui.screens.noahsark.NoahsArkViewModel
import com.bibleadventures.ui.theme.BibleAdventuresTheme

@Composable
fun NoahsArkIntroScreen(
    viewModel: NoahsArkViewModel,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val character by viewModel.characterCustomization.collectAsStateWithLifecycle()

    NoahsArkIntroContent(
        character = character,
        onContinue = onContinue,
        modifier = modifier,
    )
}

@Composable
private fun NoahsArkIntroContent(
    character: CharacterCustomization,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val audioController = LocalAudioController.current
    val lines = NoahsArkContent.introDialogueLines.map { stringResource(it) }
    val narration = lines.joinToString(separator = " ")

    LaunchedEffect(Unit) {
        audioController.speak(narration)
        audioController.playMusic(MusicTrack.ADVENTURE)
    }

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CharacterPreview(customization = character)

            Row(
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .padding(top = 24.dp, bottom = 32.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    lines.forEach { line ->
                        Text(text = line, style = MaterialTheme.typography.bodyLarge)
                    }
                }
                IconButton(onClick = { audioController.speak(narration) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = stringResource(R.string.action_replay_narration),
                    )
                }
            }

            AdventureMenuButton(
                text = stringResource(R.string.action_continue),
                onClick = onContinue,
                modifier = Modifier.widthIn(max = 320.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NoahsArkIntroPreview() {
    BibleAdventuresTheme {
        NoahsArkIntroContent(character = CharacterCustomization(), onContinue = {})
    }
}
