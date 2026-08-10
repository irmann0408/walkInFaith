package com.bibleadventures.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Lock indicator for a not-yet-unlocked node. Locking is never conveyed by
 * color alone (spec section 13) — callers pair this with a content
 * description on the node itself and disable its click target.
 */
@Composable
fun LockedNodeOverlay(modifier: Modifier = Modifier) {
    Icon(
        imageVector = Icons.Filled.Lock,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}
