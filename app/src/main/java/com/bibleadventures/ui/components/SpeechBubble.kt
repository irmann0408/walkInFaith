package com.bibleadventures.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

private val TailDownShape = GenericShape { size, _ ->
    moveTo(0f, 0f)
    lineTo(size.width, 0f)
    lineTo(size.width / 2f, size.height)
    close()
}

private val TailUpShape = GenericShape { size, _ ->
    moveTo(0f, size.height)
    lineTo(size.width, size.height)
    lineTo(size.width / 2f, 0f)
    close()
}

/**
 * A simple rounded-rect callout with a small triangular tail — deliberately
 * two separate shapes (body + tail) rather than one combined `Path`, since
 * `GenericShape`'s pixel-space coordinates make a standalone triangle
 * trivial but a rounded-rect-plus-tail union fiddlier than this effect is
 * worth. No engine/state coupling — purely presentational, meant to sit
 * next to a [CharacterPreview] avatar but reusable anywhere a short
 * speech/thought callout is needed.
 *
 * [tailOnTop] points the tail up instead of down, for callers (like
 * [CharacterCallout]'s `bubbleBelow`) whose avatar sits above the bubble
 * rather than below it — the tail still points toward the character, just
 * from the other side.
 */
@Composable
fun SpeechBubble(text: String, modifier: Modifier = Modifier, tailOnTop: Boolean = false) {
    Column(modifier = modifier) {
        if (tailOnTop) {
            Box(
                modifier = Modifier
                    .padding(start = 20.dp)
                    .size(width = 14.dp, height = 8.dp)
                    .clip(TailUpShape)
                    .background(MaterialTheme.colorScheme.surface),
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text(text = text, style = MaterialTheme.typography.bodyMedium)
        }
        if (!tailOnTop) {
            Box(
                modifier = Modifier
                    .padding(start = 20.dp)
                    .size(width = 14.dp, height = 8.dp)
                    .clip(TailDownShape)
                    .background(MaterialTheme.colorScheme.surface),
            )
        }
    }
}
