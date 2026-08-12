package com.bibleadventures.game.stories

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.geometry.Offset

/**
 * Content shapes shared across chapters (unlike `AnimalDef`/`SupplyDef`-style
 * types, which stay chapter-local until a second chapter actually needs that
 * exact domain concept). Extracted once a second chapter needed them, not
 * ahead of time.
 */
data class HiddenItemDef(
    val id: String,
    @DrawableRes val iconRes: Int,
    @StringRes val nameRes: Int,
    /** Fractional (0..1) position within the scene area. */
    val position: Offset,
)

/** A tappable item that doesn't belong — never required, never penalized, always retryable. */
data class DecoyItemDef(val id: String, @DrawableRes val iconRes: Int, @StringRes val nameRes: Int)

/**
 * A purely visual, hand-positioned distractor in a hidden-object scene —
 * never registered as a [HiddenItemDef]/`HiddenItem`, so tapping one is a
 * screen-level no-op by construction, not an engine concern (never wired to
 * a click handler at all). Distinct from [DecoyItemDef] (no fixed position
 * of its own — shuffled into a shared pool with the real items instead) —
 * extracted here once a second chapter (Esther's Royal Attire) needed this
 * exact "own position, no name" shape, first established by Feeding the
 * 5,000's crowd/basket decoys.
 */
data class DecoyItem(val id: String, val position: Offset, @DrawableRes val iconRes: Int)
