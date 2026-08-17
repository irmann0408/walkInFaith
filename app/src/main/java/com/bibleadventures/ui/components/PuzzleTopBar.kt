package com.bibleadventures.ui.components

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bibleadventures.R

/**
 * Puzzle/scene screens' top bar: a back-to-main-menu icon on the left (only
 * when revisiting an already-completed page — a normal one-screen-back is
 * already covered by the system Back button/gesture) and a "Next Page"
 * action on the right (only once the puzzle can be advanced past). Both
 * live together in one bar so a puzzle's body never needs its own pinned
 * footer button — every screen's content can claim the full remaining space
 * below this bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PuzzleTopBar(
    showBackButton: Boolean,
    onBackToMainMenu: () -> Unit,
    showNextButton: Boolean,
    onNext: () -> Unit,
) {
    TopAppBar(
        title = {},
        navigationIcon = {
            if (showBackButton) {
                IconButton(onClick = onBackToMainMenu) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.action_back_to_main_menu),
                    )
                }
            }
        },
        actions = {
            if (showNextButton) {
                TextButton(onClick = onNext, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.action_next_page))
                }
            }
        },
    )
}
