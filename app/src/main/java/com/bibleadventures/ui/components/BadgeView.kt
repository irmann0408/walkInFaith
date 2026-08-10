package com.bibleadventures.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.bibleadventures.domain.model.Badge

/**
 * Reusable earned-badge row (icon + title + description) — used on the
 * Reward screen right after it's awarded, and again in the Badges gallery.
 * Locked badges reuse this too at reduced [iconAlpha]; the caller pairs that
 * with a [LockedNodeOverlay]/content description, since dimming alone never
 * conveys lock state (spec section 13).
 */
@Composable
fun BadgeView(
    badge: Badge,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    iconAlpha: Float = 1f,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(badge.iconRes),
            contentDescription = null,
            modifier = Modifier.size(64.dp).alpha(iconAlpha),
        )
        Column(modifier = Modifier.padding(start = 16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleLarge)
            Text(text = description, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
