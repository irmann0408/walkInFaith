package com.bibleadventures.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

private val JOYSTICK_BASE_SIZE = 96.dp
private val JOYSTICK_KNOB_SIZE = 56.dp

/**
 * A self-centering virtual analog stick — the shared continuous-movement
 * input for every real-time joystick puzzle in this app (Good Samaritan's
 * "mini dungeon" and Daniel's "Race to the Den" maze). Extracted from
 * `GoodSamaritanExploreScreen.kt`'s original `private fun Joystick` once a
 * second consumer needed it, same "share it once a second chapter needs it"
 * precedent as [com.bibleadventures.game.stories.MathProblem].
 *
 * [knobOffsetState] is a hoisted [MutableState] *object* — not a plain
 * [Offset] value — read and written directly (`.value`) inside the
 * `pointerInput` gesture closure below. That distinction matters:
 * `pointerInput(Unit)` is keyed on `Unit`, so it never restarts once
 * attached, meaning `detectDragGestures` is only ever called a single time.
 * A plain `Offset` *parameter* captured by that one long-lived closure would
 * permanently see whatever value this composable had on its *first*
 * composition — every subsequent `onDrag` would compute
 * `candidate = <that stale value> + dragAmount` instead of properly
 * accumulating the held knob displacement, which is exactly what produced a
 * "nudge and stop" feel instead of a real held-stick one. Since
 * [knobOffsetState] is the same object reference across every
 * recomposition, `.value` inside the closure always reads/writes the live,
 * current offset regardless of when the closure was created.
 */
@Composable
fun Joystick(
    knobOffsetState: MutableState<Offset>,
    maxTravelPx: Float,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val knobOffset = knobOffsetState.value

    Box(
        modifier = modifier
            .size(JOYSTICK_BASE_SIZE)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val candidate = knobOffsetState.value + dragAmount
                        val distance = candidate.getDistance()
                        knobOffsetState.value = if (distance > maxTravelPx) candidate * (maxTravelPx / distance) else candidate
                    },
                    onDragEnd = { knobOffsetState.value = Offset.Zero },
                    onDragCancel = { knobOffsetState.value = Offset.Zero },
                )
            }
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(knobOffset.x.roundToInt(), knobOffset.y.roundToInt()) }
                .size(JOYSTICK_KNOB_SIZE)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}
