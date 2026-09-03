package com.bibleadventures.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.bibleadventures.ui.LocalReducedMotion
import kotlinx.coroutines.delay

/** Caps the bubble at roughly 2 lines of bodyMedium text — longer messages (e.g. a puzzle's full instructions) auto-scroll within this fixed height instead of growing the bubble tall enough to overlap the character above/below it. */
private val TEXT_MAX_HEIGHT = 44.dp
private const val AUTO_SCROLL_READ_PAUSE_MS = 1600L
private const val AUTO_SCROLL_TOP_PAUSE_MS = 700L
private const val AUTO_SCROLL_DURATION_MS = 1200

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
 *
 * [text] is capped to [TEXT_MAX_HEIGHT] (about 2 lines) and auto-scrolls
 * within that fixed height if it runs longer — a puzzle's full instructions
 * can be several times longer than a short feedback phrase like "Great
 * job!", and letting the bubble itself grow to fit the longest possible
 * message would risk it overlapping the character it's anchored to. The
 * scroll (down, pause, back to top, repeat) only starts if the text
 * actually overflows, and is skipped entirely under [LocalReducedMotion] —
 * the full text is always present for a screen reader regardless of the
 * visible scroll position.
 */
@Composable
fun SpeechBubble(text: String, modifier: Modifier = Modifier, tailOnTop: Boolean = false) {
    val scrollState = rememberScrollState()
    val reducedMotion = LocalReducedMotion.current

    LaunchedEffect(text, reducedMotion) {
        scrollState.scrollTo(0)
        if (reducedMotion) return@LaunchedEffect
        while (true) {
            delay(AUTO_SCROLL_READ_PAUSE_MS)
            if (scrollState.maxValue > 0) {
                scrollState.animateScrollTo(scrollState.maxValue, animationSpec = tween(AUTO_SCROLL_DURATION_MS, easing = LinearEasing))
                delay(AUTO_SCROLL_TOP_PAUSE_MS)
                scrollState.animateScrollTo(0, animationSpec = tween(AUTO_SCROLL_DURATION_MS, easing = LinearEasing))
            }
        }
    }

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
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.heightIn(max = TEXT_MAX_HEIGHT).verticalScroll(scrollState),
            )
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
