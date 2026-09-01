package com.bibleadventures.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bibleadventures.domain.model.CharacterCustomization

private val CHARACTER_CALLOUT_SIZE = 96.dp

/**
 * The player's own customized character, shown as a corner avatar — reused
 * everywhere the character personalizes a scene: [StoryVideoScreen]'s
 * narrated videos and, per the user's request, every Noah's Ark puzzle
 * screen too, where [message] carries that puzzle's existing feedback text
 * (e.g. "That's not a tool!") so the character is the one telling the
 * player about a mistake instead of a bare text row.
 *
 * [bubbleBelow] flips the avatar/bubble order (and the bubble's tail
 * direction) for callers anchoring the character to the *top* of a scene
 * instead of the bottom (e.g. Find the Tools, where a bottom corner
 * overlaps the puzzle's own hotspots) — the bubble still points toward the
 * character, just from below it instead of above.
 */
@Composable
fun CharacterCallout(
    characterCustomization: CharacterCustomization,
    message: String?,
    modifier: Modifier = Modifier,
    bubbleBelow: Boolean = false,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.Start) {
        if (!bubbleBelow && message != null) {
            SpeechBubble(
                text = message,
                modifier = Modifier.padding(bottom = 4.dp).widthIn(max = 220.dp),
            )
        }
        CharacterPreview(customization = characterCustomization, modifier = Modifier.size(CHARACTER_CALLOUT_SIZE))
        if (bubbleBelow && message != null) {
            SpeechBubble(
                text = message,
                tailOnTop = true,
                modifier = Modifier.padding(top = 4.dp).widthIn(max = 220.dp),
            )
        }
    }
}
