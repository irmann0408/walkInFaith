package com.bibleadventures.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class Appearance { BOY, GIRL }

@Serializable
enum class Hairstyle { SHORT, CURLY, BRAIDED, PONYTAIL, LONG }

/**
 * Named after the original Classic-style clothing pieces (a `TUNIC`/`ROBE`/
 * `VEST` per color) from when this app had two rendering styles — Classic
 * was removed once real illustrated art replaced it, but these constant
 * names are persisted by name in save data, so they're kept as-is rather
 * than renamed to avoid silently losing existing players' clothing choice.
 * `BROWN`/`PINK` were added after Classic's removal and are named by color
 * only, since "clothing type" no longer has any rendering meaning — every
 * appearance always wears its one illustrated outfit shape, just recolored.
 */
@Serializable
enum class ClothingColor { TUNIC_BLUE, TUNIC_GREEN, ROBE_RED, VEST_YELLOW, ROBE_PURPLE, BROWN, PINK }

/**
 * Player's chosen appearance. Intentionally simple for the MVP (spec
 * section 8) — no `accessories` field yet, added only once there's a real
 * accessories feature to back it.
 */
@Serializable
data class CharacterCustomization(
    val appearance: Appearance = Appearance.BOY,
    val hairstyle: Hairstyle = Hairstyle.SHORT,
    val clothing: ClothingColor = ClothingColor.TUNIC_BLUE,
)
