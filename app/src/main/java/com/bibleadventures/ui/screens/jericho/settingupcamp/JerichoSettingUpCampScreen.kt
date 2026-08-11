package com.bibleadventures.ui.screens.jericho.settingupcamp

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.bibleadventures.game.puzzles.hiddenobject.HiddenItem
import com.bibleadventures.game.puzzles.hiddenobject.HiddenObjectGameState
import com.bibleadventures.game.stories.JerichoContent
import com.bibleadventures.ui.components.AdventureMenuButton
import com.bibleadventures.ui.screens.jericho.JerichoViewModel
import com.bibleadventures.ui.theme.BibleAdventuresTheme

/**
 * Twelve memorial stones (Joshua 4:1-9), collected order-independently —
 * reuses `hiddenobject` exactly as Royal Attire does, just with a plain
 * static tray instead of a search scene, since these stones start in
 * plain view rather than hidden.
 */
@Composable
fun JerichoSettingUpCampScreen(
    viewModel: JerichoViewModel,
    onContinue: () -> Unit,
    previouslyCompleted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    JerichoSettingUpCampContent(
        campState = uiState.campState,
        onStoneTapped = viewModel::onCampStoneTapped,
        onContinue = onContinue,
        previouslyCompleted = previouslyCompleted,
        modifier = modifier,
    )
}

@Composable
private fun JerichoSettingUpCampContent(
    campState: HiddenObjectGameState,
    onStoneTapped: (String) -> Unit,
    onContinue: () -> Unit,
    previouslyCompleted: Boolean = false,
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
                text = stringResource(R.string.jericho_camp_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = stringResource(R.string.jericho_camp_instructions),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = "${campState.foundIds.size} / ${campState.items.size}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                campState.items.chunked(4).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    ) {
                        row.forEach { item ->
                            StoneTile(item = item, isPlaced = item.id in campState.foundIds, onClick = { onStoneTapped(item.id) })
                        }
                    }
                }
            }

            if (previouslyCompleted && !campState.isComplete) {
                Text(
                    text = stringResource(R.string.puzzle_already_completed_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            if (campState.isComplete || previouslyCompleted) {
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
private fun StoneTile(item: HiddenItem, isPlaced: Boolean, onClick: () -> Unit) {
    val name = stringResource(item.contentDescriptionRes)

    Box(
        modifier = Modifier
            .size(64.dp)
            .clickable(enabled = !isPlaced, onClickLabel = name, onClick = onClick)
            .semantics { contentDescription = name },
        contentAlignment = Alignment.Center,
    ) {
        if (isPlaced) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(imageVector = Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(text = name, style = MaterialTheme.typography.labelSmall)
            }
        } else {
            Image(
                painter = painterResource(item.iconRes),
                contentDescription = null,
                modifier = Modifier.size(48.dp).alpha(0.9f),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun JerichoSettingUpCampPreview() {
    BibleAdventuresTheme {
        val items = JerichoContent.campStones.map {
            HiddenItem(id = it.id, position = androidx.compose.ui.geometry.Offset.Zero, iconRes = R.drawable.ic_stone_smooth, contentDescriptionRes = it.nameRes)
        }
        JerichoSettingUpCampContent(
            campState = HiddenObjectGameState(items = items),
            onStoneTapped = {},
            onContinue = {},
        )
    }
}
