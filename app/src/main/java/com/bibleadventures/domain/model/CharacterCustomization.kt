package com.bibleadventures.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class Appearance { BOY, GIRL }

@Serializable
enum class Hairstyle { SHORT, CURLY, BRAIDED, PONYTAIL }

@Serializable
enum class SkinTone { TONE_1, TONE_2, TONE_3, TONE_4 }

@Serializable
enum class Clothing { TUNIC_BLUE, TUNIC_GREEN, ROBE_RED, VEST_YELLOW, ROBE_PURPLE }

/**
 * `CLASSIC` is the original Compose `Canvas`-drawn placeholder look, driven
 * by [Hairstyle]/[SkinTone]/[Clothing]. `ILLUSTRATED` renders real static
 * art per [Appearance]/[Clothing] combination instead — that art has no
 * separable hair/skin-tone layers, so [Hairstyle]/[SkinTone] are ignored in
 * this style (see `ui/components/CharacterPreview.kt`).
 */
@Serializable
enum class CharacterStyle { CLASSIC, ILLUSTRATED }

/**
 * Player's chosen appearance. Intentionally simple for the MVP (spec
 * section 8) — no `accessories` field yet, added only once there's a real
 * accessories feature to back it.
 */
@Serializable
data class CharacterCustomization(
    val appearance: Appearance = Appearance.BOY,
    val hairstyle: Hairstyle = Hairstyle.SHORT,
    val skinTone: SkinTone = SkinTone.TONE_1,
    val clothing: Clothing = Clothing.TUNIC_BLUE,
    val characterStyle: CharacterStyle = CharacterStyle.CLASSIC,
)
