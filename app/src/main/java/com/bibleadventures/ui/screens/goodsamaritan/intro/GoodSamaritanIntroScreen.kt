package com.bibleadventures.ui.screens.goodsamaritan.intro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bibleadventures.R
import com.bibleadventures.domain.model.CharacterCustomization
import com.bibleadventures.game.stories.GoodSamaritanContent
import com.bibleadventures.ui.components.AdventureMenuButton
import com.bibleadventures.ui.components.CharacterPreview
import com.bibleadventures.ui.screens.goodsamaritan.GoodSamaritanViewModel
import com.bibleadventures.ui.theme.BibleAdventuresTheme

@Composable
fun GoodSamaritanIntroScreen(
    viewModel: GoodSamaritanViewModel,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val character by viewModel.characterCustomization.collectAsStateWithLifecycle()

    GoodSamaritanIntroContent(
        character = character,
        onContinue = onContinue,
        modifier = modifier,
    )
}

@Composable
private fun GoodSamaritanIntroContent(
    character: CharacterCustomization,
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
            verticalArrangement = Arrangement.Center,
        ) {
            CharacterPreview(customization = character)

            Column(
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .padding(top = 24.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                GoodSamaritanContent.introDialogueLines.forEach { lineRes ->
                    Text(text = stringResource(lineRes), style = MaterialTheme.typography.bodyLarge)
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
private fun GoodSamaritanIntroPreview() {
    BibleAdventuresTheme {
        GoodSamaritanIntroContent(character = CharacterCustomization(), onContinue = {})
    }
}
