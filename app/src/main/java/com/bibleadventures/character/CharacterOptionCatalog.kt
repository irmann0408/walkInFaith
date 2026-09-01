package com.bibleadventures.character

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.bibleadventures.R
import com.bibleadventures.domain.model.Appearance
import com.bibleadventures.domain.model.ClothingColor
import com.bibleadventures.domain.model.Hairstyle

data class AppearanceOption(val value: Appearance, @StringRes val labelRes: Int)
data class HairstyleOption(val value: Hairstyle, @StringRes val labelRes: Int)
data class ClothingOption(val value: ClothingColor, @StringRes val labelRes: Int, val swatchColor: Color)

/** Static picker content for the Character screen. */
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
        HairstyleOption(Hairstyle.LONG, R.string.character_hairstyle_long),
    )

    val clothingOptions = listOf(
        ClothingOption(ClothingColor.TUNIC_BLUE, R.string.character_clothing_tunic_blue, Color(0xFF6EC6E8)),
        ClothingOption(ClothingColor.TUNIC_GREEN, R.string.character_clothing_tunic_green, Color(0xFF6FBE7C)),
        ClothingOption(ClothingColor.ROBE_RED, R.string.character_clothing_robe_red, Color(0xFFF08A6C)),
        ClothingOption(ClothingColor.VEST_YELLOW, R.string.character_clothing_vest_yellow, Color(0xFFF6C445)),
        ClothingOption(ClothingColor.ROBE_PURPLE, R.string.character_clothing_robe_purple, Color(0xFFA07CC9)),
        ClothingOption(ClothingColor.BROWN, R.string.character_clothing_brown, Color(0xFF9E592C)),
        ClothingOption(ClothingColor.PINK, R.string.character_clothing_pink, Color(0xFFB6888B)),
    )
}
