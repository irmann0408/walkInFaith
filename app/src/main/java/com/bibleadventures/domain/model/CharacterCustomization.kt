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
 * Build-time gate for [CharacterStyle.ILLUSTRATED] — v1.0 ships Classic
 * only (the picker that could select Illustrated is hidden, see
 * `ui/screens/character/CharacterScreen.kt`), but a profile saved on a
 * device that explored Illustrated before this gate existed would still
 * have `characterStyle = ILLUSTRATED` persisted. Every place that reads
 * `characterStyle` must check this flag too, not just whether the picker
 * is reachable, so those devices render Classic exactly like a fresh
 * install rather than being stuck on stale Illustrated state with no way
 * back. Flip to `true` (and restore the picker row) for v2.0.
 */
const val CHARACTER_STYLE_ILLUSTRATED_ENABLED = false

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
