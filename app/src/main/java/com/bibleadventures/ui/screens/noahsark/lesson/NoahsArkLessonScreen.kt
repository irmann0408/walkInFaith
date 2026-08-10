package com.bibleadventures.ui.screens.noahsark.lesson

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bibleadventures.R
import com.bibleadventures.game.rewards.NoahsArkReward
import com.bibleadventures.ui.components.AdventureMenuButton
import com.bibleadventures.ui.components.ScriptureCardView
import com.bibleadventures.ui.theme.BibleAdventuresTheme

@Composable
fun NoahsArkLessonScreen(
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
                text = stringResource(R.string.noahs_ark_lesson_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = stringResource(R.string.noahs_ark_lesson_text),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 12.dp, bottom = 24.dp),
            )

            ScriptureCardView(
                reference = NoahsArkReward.scriptureCard.reference,
                text = stringResource(NoahsArkReward.scriptureCard.textRes),
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
private fun NoahsArkLessonPreview() {
    BibleAdventuresTheme {
        NoahsArkLessonScreen(onContinue = {})
    }
}
