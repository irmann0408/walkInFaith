package com.bibleadventures.ui.screens.mainmenu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bibleadventures.R
import com.bibleadventures.ui.components.AdventureMenuButton
import com.bibleadventures.ui.navigation.MenuItemId
import com.bibleadventures.ui.theme.BibleAdventuresTheme

private data class MenuItem(val id: MenuItemId, val label: String, val enabled: Boolean = true)

@Composable
fun MainMenuScreen(
    onMenuItemClick: (MenuItemId) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MainMenuViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    MainMenuContent(
        hasAdventureInProgress = uiState.hasAdventureInProgress,
        onMenuItemClick = onMenuItemClick,
        modifier = modifier,
    )
}

@Composable
private fun MainMenuContent(
    hasAdventureInProgress: Boolean,
    onMenuItemClick: (MenuItemId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val menuItems = listOf(
        MenuItem(
            MenuItemId.CONTINUE_ADVENTURE,
            stringResource(R.string.menu_continue_adventure),
            enabled = hasAdventureInProgress,
        ),
        MenuItem(MenuItemId.ADVENTURES, stringResource(R.string.menu_adventures)),
        MenuItem(MenuItemId.BADGES, stringResource(R.string.menu_badges)),
        MenuItem(MenuItemId.SCRIPTURE_CARDS, stringResource(R.string.menu_scripture_cards)),
        MenuItem(MenuItemId.CHARACTER, stringResource(R.string.menu_character)),
        MenuItem(MenuItemId.SETTINGS, stringResource(R.string.menu_settings)),
        MenuItem(MenuItemId.PARENT_AREA, stringResource(R.string.menu_parent_area)),
    )

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(bottom = 24.dp),
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.widthIn(max = 480.dp),
            ) {
                items(menuItems) { item ->
                    AdventureMenuButton(
                        text = item.label,
                        enabled = item.enabled,
                        onClick = { onMenuItemClick(item.id) },
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MainMenuPreview() {
    BibleAdventuresTheme {
        MainMenuContent(hasAdventureInProgress = false, onMenuItemClick = {})
    }
}
