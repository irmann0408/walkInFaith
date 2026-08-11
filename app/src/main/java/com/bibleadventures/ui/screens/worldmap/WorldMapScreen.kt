package com.bibleadventures.ui.screens.worldmap

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bibleadventures.R
import com.bibleadventures.audio.MusicTrack
import com.bibleadventures.domain.model.ChapterId
import com.bibleadventures.domain.model.ChapterStatus
import com.bibleadventures.game.stories.ChapterCatalog
import com.bibleadventures.ui.AppViewModelProvider
import com.bibleadventures.ui.LocalAudioController
import com.bibleadventures.ui.components.LockedNodeOverlay
import com.bibleadventures.ui.theme.BibleAdventuresTheme

@Composable
fun WorldMapScreen(
    onBack: () -> Unit,
    onChapterSelected: (ChapterId) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WorldMapViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    WorldMapContent(
        nodes = uiState.nodes,
        onBack = onBack,
        onChapterSelected = onChapterSelected,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorldMapContent(
    nodes: List<WorldMapNodeUiState>,
    onBack: () -> Unit,
    onChapterSelected: (ChapterId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val audioController = LocalAudioController.current
    LaunchedEffect(Unit) { audioController.playMusic(MusicTrack.WORLD_MAP) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.world_map_title)) },
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
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .testTag("world_map_chapter_list"),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text(
                    text = stringResource(R.string.world_map_home_village),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            items(nodes) { node ->
                ChapterNode(node = node, onClick = { onChapterSelected(node.chapter.id) })
            }
        }
    }
}

@Composable
private fun ChapterNode(
    node: WorldMapNodeUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isLocked = node.status == ChapterStatus.LOCKED
    val title = stringResource(node.chapter.titleRes)
    val nodeContentDescription = if (isLocked && node.chapter.requiredChapter != null) {
        val prerequisiteTitle = stringResource(
            ChapterCatalog.all.first { it.id == node.chapter.requiredChapter }.titleRes,
        )
        stringResource(R.string.world_map_locked_content_description, title, prerequisiteTitle)
    } else {
        title
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = !isLocked, onClickLabel = nodeContentDescription, onClick = onClick)
            .semantics { contentDescription = nodeContentDescription },
        colors = CardDefaults.cardColors(
            containerColor = if (isLocked) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surface
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
            Column {
                Text(text = title, style = MaterialTheme.typography.titleLarge)
                if (node.status == ChapterStatus.COMPLETED) {
                    Text(
                        text = stringResource(R.string.world_map_stars_earned_content_description, node.stars),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            if (isLocked) {
                LockedNodeOverlay()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WorldMapPreview() {
    BibleAdventuresTheme {
        WorldMapContent(
            nodes = ChapterCatalog.all.mapIndexed { index, chapter ->
                WorldMapNodeUiState(
                    chapter = chapter,
                    status = if (index == 0) ChapterStatus.UNLOCKED else ChapterStatus.LOCKED,
                    stars = 0,
                )
            },
            onBack = {},
            onChapterSelected = {},
        )
    }
}
