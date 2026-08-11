package com.bibleadventures.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bibleadventures.R
import com.bibleadventures.ui.LocalAudioController
import com.bibleadventures.ui.theme.BibleAdventuresTheme

/**
 * A brief narrative/verse beat shown before a puzzle scene, giving the player
 * context for what they're about to do (and, implicitly, what doesn't belong)
 * before any decoy items show up. Deliberately lighter than the chapter's full
 * [com.bibleadventures.ui.screens.noahsark.intro.NoahsArkIntroScreen] — no
 * character render, just a title, a couple of lines, and Continue.
 *
 * Narrates its lines once via [LocalAudioController] on first composition
 * (respects the Settings narration toggle internally) — this one shared
 * composable is reused as every chapter's context card, so this single
 * addition covers narration for all of them at once.
 */
@Composable
fun StoryBeatScreen(
    titleRes: Int,
    lineRes: List<Int>,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val audioController = LocalAudioController.current
    val lines = lineRes.map { stringResource(it) }
    val narration = lines.joinToString(separator = " ")

    LaunchedEffect(lineRes) { audioController.speak(narration) }

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = stringResource(titleRes), style = MaterialTheme.typography.headlineMedium)
                IconButton(onClick = { audioController.speak(narration) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = stringResource(R.string.action_replay_narration),
                    )
                }
            }

            Column(
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .padding(top = 16.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                lines.forEach { line ->
                    Text(text = line, style = MaterialTheme.typography.bodyLarge)
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
private fun StoryBeatScreenPreview() {
    BibleAdventuresTheme {
        StoryBeatScreen(
            titleRes = R.string.noahs_ark_find_animals_context_title,
            lineRes = listOf(
                R.string.noahs_ark_find_animals_context_line_1,
                R.string.noahs_ark_find_animals_context_line_2,
            ),
            onContinue = {},
        )
    }
}
