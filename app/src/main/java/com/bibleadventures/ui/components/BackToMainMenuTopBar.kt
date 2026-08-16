package com.bibleadventures.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.bibleadventures.R

/**
 * Shown on a puzzle/scene screen only when the player is revisiting a
 * page they've already completed before — jumps straight to the Main
 * Menu (not a normal one-screen-back, which the system Back
 * button/gesture already covers), so it gets its own content description
 * rather than reusing the generic `action_back` string.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackToMainMenuTopBar(onBackToMainMenu: () -> Unit) {
    TopAppBar(
        title = {},
        navigationIcon = {
            IconButton(onClick = onBackToMainMenu) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_back_to_main_menu),
                )
            }
        },
    )
}
