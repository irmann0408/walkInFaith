package com.bibleadventures.ui.screens.noahsark.findanimals

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bibleadventures.R
import com.bibleadventures.game.stories.AnimalDef
import com.bibleadventures.game.stories.NoahsArkContent
import com.bibleadventures.ui.components.AdventureMenuButton
import com.bibleadventures.ui.screens.noahsark.NoahsArkViewModel
import com.bibleadventures.ui.theme.BibleAdventuresTheme

@Composable
fun NoahsArkFindAnimalsScreen(
    viewModel: NoahsArkViewModel,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    NoahsArkFindAnimalsContent(
        foundAnimalIds = uiState.foundAnimalIds,
        onAnimalTapped = viewModel::onAnimalFound,
        onContinue = onContinue,
        modifier = modifier,
    )
}

@Composable
private fun NoahsArkFindAnimalsContent(
    foundAnimalIds: Set<String>,
    onAnimalTapped: (String) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val allFound = foundAnimalIds.size == NoahsArkContent.animals.size

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.noahs_ark_find_animals_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = stringResource(R.string.noahs_ark_find_animals_instructions),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f, fill = false),
            ) {
                items(NoahsArkContent.animals) { animal ->
                    AnimalTile(
                        animal = animal,
                        isFound = animal.id in foundAnimalIds,
                        onClick = { onAnimalTapped(animal.id) },
                    )
                }
            }

            if (allFound) {
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
private fun AnimalTile(animal: AnimalDef, isFound: Boolean, onClick: () -> Unit) {
    val alpha by animateFloatAsState(targetValue = if (isFound) 0.5f else 1f, label = "animalFoundAlpha")
    val name = stringResource(animal.nameRes)

    Box(
        modifier = Modifier
            .size(88.dp)
            .clickable(enabled = !isFound, onClickLabel = name, onClick = onClick)
            .semantics { contentDescription = name },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(animal.iconRes),
            contentDescription = null,
            modifier = Modifier.size(72.dp).alpha(alpha),
        )
        if (isFound) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NoahsArkFindAnimalsPreview() {
    BibleAdventuresTheme {
        NoahsArkFindAnimalsContent(foundAnimalIds = emptySet(), onAnimalTapped = {}, onContinue = {})
    }
}
