package com.bibleadventures.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bibleadventures.R
import com.bibleadventures.domain.model.CharacterCustomization

private val CHARACTER_CALLOUT_SIZE = 96.dp

/**
 * Default clearance reserved above the character for the bubble to float
 * into, tuned for the short single-line feedback strings the Noah's Ark
 * puzzle screens show (e.g. "Great job!"). Fixed rather than measured from
 * the bubble's own real height (which would need a two-pass layout) — a
 * caller whose [CharacterCallout.message] runs noticeably longer (wraps to
 * 2+ lines) should pass a bigger [CharacterCallout.bubbleAboveClearance] so
 * the bubble doesn't grow down into the character (see [StoryVideoScreen],
 * whose reflection sentences are full sentences, not short phrases).
 */
private val DEFAULT_BUBBLE_ABOVE_CLEARANCE = 60.dp

/**
 * The player's own customized character, shown as a corner avatar — reused
 * everywhere the character personalizes a scene: [StoryVideoScreen]'s
 * narrated videos and, per the user's request, every Noah's Ark puzzle
 * screen too, where [message] carries that puzzle's existing feedback text
 * (e.g. "That's not a tool!") so the character is the one telling the
 * player about a mistake instead of a bare text row.
 *
 * The character sits in a fixed-size [Box] whose position never changes
 * based on whether [message] is present — the bubble is a sibling
 * positioned with a plain [Modifier.offset] entirely outside that box
 * (above or below it, per [bubbleBelow]), so it never pushes the character
 * around the way stacking it in a `Column` above the character would (a
 * real bug this app hit: a top-anchored callout's character visibly
 * dropped position whenever a bubble appeared above it in the stack).
 *
 * [bubbleBelow] puts the bubble below the character instead of above it
 * (and flips the bubble's tail direction to match) for callers anchoring
 * the character to the *top* of a scene instead of the bottom (e.g. Find
 * the Tools, where a bottom corner overlaps the puzzle's own hotspots) —
 * the bubble still points toward the character either way. The "below"
 * case never risks overlapping the character no matter how tall the bubble
 * grows (it only extends further away), so only the "above" case needs
 * [bubbleAboveClearance] tuned per caller.
 *
 * [posture] lets a caller show the character reacting to its own last
 * outcome (e.g. [Posture.THUMBS_UP] on a correct answer) — see
 * [CharacterPreview] for why that's a transient parameter here rather than
 * part of [characterCustomization] itself.
 *
 * [onClick], when non-null, makes the character itself tappable (e.g.
 * [StoryVideoScreen] lets the player tap the character to hear its own
 * recorded line early, interrupting the video's narration). [CharacterPreview]
 * already carries its own `character_preview_content_description` semantics
 * internally — wrapping it in `.clickable` without overriding the
 * description would leave two nodes reporting the same generic text, so
 * this explicitly replaces it with [onClickContentDescription] (defaulting
 * to [R.string.character_tap_to_hear_content_description], right for every
 * existing caller) whenever a click handler is supplied. A caller whose tap
 * means something other than "hear the character speak" — e.g.
 * [com.bibleadventures.ui.screens.goodsamaritan.passingby.GoodSamaritanPassingByScreen]'s
 * "tap to advance to the next puzzle" — should pass its own description
 * here instead, so a screen reader announces what the tap actually does.
 */
@Composable
fun CharacterCallout(
    characterCustomization: CharacterCustomization,
    message: String?,
    modifier: Modifier = Modifier,
    bubbleBelow: Boolean = false,
    posture: Posture = Posture.STANDING,
    bubbleAboveClearance: Dp = DEFAULT_BUBBLE_ABOVE_CLEARANCE,
    onClick: (() -> Unit)? = null,
    onClickContentDescription: String? = null,
) {
    Box(modifier = modifier.size(CHARACTER_CALLOUT_SIZE)) {
        var characterModifier = Modifier.size(CHARACTER_CALLOUT_SIZE)
        if (onClick != null) {
            val tapDescription = onClickContentDescription ?: stringResource(R.string.character_tap_to_hear_content_description)
            characterModifier = characterModifier
                .clickable(onClickLabel = tapDescription, onClick = onClick)
                .semantics(mergeDescendants = true) { contentDescription = tapDescription }
        }
        CharacterPreview(
            customization = characterCustomization,
            posture = posture,
            modifier = characterModifier,
        )
        if (message != null) {
            SpeechBubble(
                text = message,
                tailOnTop = bubbleBelow,
                // The bubble lives inside a Box fixed at CHARACTER_CALLOUT_SIZE
                // (96dp) — without wrapContentSize(unbounded = true), it would be
                // measured against that same 96dp cap despite the offset visually
                // moving it outside, truncating longer messages. unbounded lets it
                // measure/lay out at its real (up to 220dp-wide) size while still
                // reporting a placeable the Box's offset can freely reposition —
                // align = TopStart is required too, since wrapContentSize's own
                // default (Center) would center the now-larger bubble around the
                // 96dp box's midpoint instead of anchoring its top-left corner,
                // pushing it sideways away from the character and off-screen.
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .wrapContentSize(align = Alignment.TopStart, unbounded = true)
                    .offset(y = if (bubbleBelow) CHARACTER_CALLOUT_SIZE + 4.dp else -bubbleAboveClearance)
                    .widthIn(max = 220.dp),
            )
        }
    }
}
