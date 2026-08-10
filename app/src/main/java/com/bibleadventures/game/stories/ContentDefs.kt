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
