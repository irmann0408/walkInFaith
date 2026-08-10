package com.bibleadventures.character

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.bibleadventures.R
import com.bibleadventures.domain.model.Appearance
import com.bibleadventures.domain.model.Clothing
import com.bibleadventures.domain.model.Hairstyle
import com.bibleadventures.domain.model.SkinTone

data class AppearanceOption(val value: Appearance, @StringRes val labelRes: Int)
data class HairstyleOption(val value: Hairstyle, @StringRes val labelRes: Int)
data class SkinToneOption(val value: SkinTone, @StringRes val labelRes: Int, val swatchColor: Color)
data class ClothingOption(val value: Clothing, @StringRes val labelRes: Int, val swatchColor: Color)

/**
 * Static picker content for the Character screen. Placeholder swatch
 * colors stand in for real art (spec section 25) — swapping in final
 * character art later only touches this catalog and [CharacterPreview],
 * not the screen or persistence logic.
 */
object CharacterOptionCatalog {
    val appearances = listOf(
        AppearanceOption(Appearance.BOY, R.string.character_option_boy),
        AppearanceOption(Appearance.GIRL, R.string.character_option_girl),
    )

    val hairstyles = listOf(
        HairstyleOption(Hairstyle.SHORT, R.string.character_hairstyle_short),
        HairstyleOption(Hairstyle.CURLY, R.string.character_hairstyle_curly),
        HairstyleOption(Hairstyle.BRAIDED, R.string.character_hairstyle_braided),
        HairstyleOption(Hairstyle.PONYTAIL, R.string.character_hairstyle_ponytail),
    )

    val skinTones = listOf(
        SkinToneOption(SkinTone.TONE_1, R.string.character_skin_tone_1, Color(0xFFFFE0B2)),
        SkinToneOption(SkinTone.TONE_2, R.string.character_skin_tone_2, Color(0xFFE3A970)),
        SkinToneOption(SkinTone.TONE_3, R.string.character_skin_tone_3, Color(0xFFB37746)),
        SkinToneOption(SkinTone.TONE_4, R.string.character_skin_tone_4, Color(0xFF7A4B28)),
    )

    val clothingOptions = listOf(
        ClothingOption(Clothing.TUNIC_BLUE, R.string.character_clothing_tunic_blue, Color(0xFF6EC6E8)),
        ClothingOption(Clothing.TUNIC_GREEN, R.string.character_clothing_tunic_green, Color(0xFF6FBE7C)),
        ClothingOption(Clothing.ROBE_RED, R.string.character_clothing_robe_red, Color(0xFFF08A6C)),
        ClothingOption(Clothing.VEST_YELLOW, R.string.character_clothing_vest_yellow, Color(0xFFF6C445)),
    )

    val hairColor = Color(0xFF6B4A2E)
}
