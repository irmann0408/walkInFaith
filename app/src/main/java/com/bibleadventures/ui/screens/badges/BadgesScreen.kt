package com.bibleadventures.ui.screens.badges

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bibleadventures.R
import com.bibleadventures.game.rewards.RewardCatalog
import com.bibleadventures.game.stories.ChapterCatalog
import com.bibleadventures.ui.AppViewModelProvider
import com.bibleadventures.ui.components.BadgeView
import com.bibleadventures.ui.components.LockedNodeOverlay
import com.bibleadventures.ui.theme.BibleAdventuresTheme

@Composable
fun BadgesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BadgesViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    BadgesContent(badges = uiState.badges, onBack = onBack, modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BadgesContent(
    badges: List<BadgeUiState>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.badges_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(badges) { badgeUiState -> BadgeTile(badgeUiState) }
        }
    }
}

@Composable
private fun BadgeTile(state: BadgeUiState, modifier: Modifier = Modifier) {
    val title = stringResource(state.badge.titleRes)
    val description = stringResource(state.badge.descriptionRes)
    val tileContentDescription = if (state.earned) {
        title
    } else {
        val prerequisiteTitle = stringResource(
            ChapterCatalog.all.first { it.id == state.badge.chapterId }.titleRes,
        )
        stringResource(R.string.badge_locked_content_description, title, prerequisiteTitle)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = tileContentDescription },
        colors = CardDefaults.cardColors(
            containerColor = if (state.earned) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BadgeView(
                badge = state.badge,
                title = title,
                description = description,
                iconAlpha = if (state.earned) 1f else 0.4f,
                modifier = Modifier.weight(1f),
            )
            if (!state.earned) {
                LockedNodeOverlay()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BadgesPreview() {
    BibleAdventuresTheme {
        BadgesContent(
            badges = RewardCatalog.badges.mapIndexed { index, badge ->
                BadgeUiState(badge = badge, earned = index == 0)
            },
            onBack = {},
        )
    }
}
