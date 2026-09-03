package com.bibleadventures.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Sizes [content] to the largest box matching [ratio] (width / height) that
 * still fits entirely within the space [modifier] resolves to — the same
 * "contain" behavior as `ContentScale.Fit` on an `Image`, letterboxed on
 * whichever axis has slack. `Modifier.aspectRatio` does *not* do this on its
 * own: by default it derives height from the available width regardless of
 * any height bound also in play, which is exactly what let a tall grid
 * overflow past a D-pad below it. Pair with `Modifier.weight(1f, fill =
 * true)` on [modifier] so the available space this fits into is itself
 * "whatever's left after every other sibling," not the whole screen.
 *
 * [alignment] defaults to [Alignment.Center] (matching every existing
 * caller) but can be overridden — e.g. [Alignment.TopCenter] for a caller
 * whose available height runs well past the fitted square's own size, so
 * the content hugs whatever's above it instead of floating in the middle
 * of a taller-than-necessary box with empty space evenly split above and
 * below.
 */
@Composable
fun AspectRatioFitBox(
    ratio: Float,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit,
) {
    BoxWithConstraints(modifier = modifier, contentAlignment = alignment) {
        val fittedWidth = minOf(maxWidth, maxHeight * ratio)
        val fittedHeight = fittedWidth / ratio
        Box(modifier = Modifier.width(fittedWidth).height(fittedHeight), content = content)
    }
}
