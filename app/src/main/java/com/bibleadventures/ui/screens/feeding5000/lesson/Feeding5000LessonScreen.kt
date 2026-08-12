package com.bibleadventures.ui.screens.feeding5000.lesson

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bibleadventures.R
import com.bibleadventures.game.rewards.Feeding5000Reward
import com.bibleadventures.ui.LocalAudioController
import com.bibleadventures.ui.components.AdventureMenuButton
import com.bibleadventures.ui.components.ScriptureCardView
import com.bibleadventures.ui.theme.BibleAdventuresTheme

@Composable
fun Feeding5000LessonScreen(
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val audioController = LocalAudioController.current
    val narration = stringResource(R.string.feeding_5000_lesson_text)

    LaunchedEffect(Unit) { audioController.speak(narration) }

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.feeding_5000_lesson_title),
                    style = MaterialTheme.typography.headlineMedium,
                )
                IconButton(onClick = { audioController.speak(narration) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = stringResource(R.string.action_replay_narration),
                    )
                }
            }
            Text(
                text = stringResource(R.string.feeding_5000_lesson_text),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 12.dp, bottom = 24.dp),
            )

            ScriptureCardView(
                reference = Feeding5000Reward.scriptureCard.reference,
                text = stringResource(Feeding5000Reward.scriptureCard.textRes),
                modifier = Modifier.widthIn(max = 480.dp),
            )

            AdventureMenuButton(
                text = stringResource(R.string.action_continue),
                onClick = onContinue,
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .padding(top = 32.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun Feeding5000LessonPreview() {
    BibleAdventuresTheme {
        Feeding5000LessonScreen(onContinue = {})
    }
}
