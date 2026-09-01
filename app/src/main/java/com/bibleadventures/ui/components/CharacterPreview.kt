package com.bibleadventures.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bibleadventures.R
import com.bibleadventures.domain.model.Appearance
import com.bibleadventures.domain.model.CharacterCustomization
import com.bibleadventures.domain.model.ClothingColor
import com.bibleadventures.domain.model.Hairstyle
import com.bibleadventures.ui.theme.BibleAdventuresTheme

/**
 * Renders the player's chosen character as one real illustrated image per
 * (appearance, hairstyle, clothing color) combination — see
 * [illustratedDrawableRes]. Boy art is always a tunic, girl art is always a
 * robe, regardless of which color is picked; only the color of that one
 * garment shape changes. `Modifier.size(160.dp)` here is a default only —
 * callers that need a smaller preview (this composable is also used at
 * ~72dp in lane mini-games) pass their own `Modifier.size(...)`, which
 * further constrains this default since a fixed outer size always wins
 * over a looser inner one.
 */
@Composable
fun CharacterPreview(
    customization: CharacterCustomization,
    modifier: Modifier = Modifier,
) {
    val contentDescription = stringResource(R.string.character_preview_content_description)

    Image(
        painter = painterResource(
            illustratedDrawableRes(customization.appearance, customization.hairstyle, customization.clothing),
        ),
        contentDescription = null,
        modifier = modifier
            .size(160.dp)
            .semantics { this.contentDescription = contentDescription },
    )
}

private fun illustratedDrawableRes(appearance: Appearance, hairstyle: Hairstyle, clothing: ClothingColor): Int = when (appearance) {
    Appearance.BOY -> when (hairstyle) {
        Hairstyle.SHORT -> when (clothing) {
            ClothingColor.TUNIC_BLUE -> R.drawable.character_boy_blue_short
            ClothingColor.TUNIC_GREEN -> R.drawable.character_boy_green_short
            ClothingColor.ROBE_RED -> R.drawable.character_boy_red_short
            ClothingColor.VEST_YELLOW -> R.drawable.character_boy_yellow_short
            ClothingColor.ROBE_PURPLE -> R.drawable.character_boy_purple_short
            ClothingColor.BROWN -> R.drawable.character_boy_brown_short
            ClothingColor.PINK -> R.drawable.character_boy_pink_short
        }
        Hairstyle.CURLY -> when (clothing) {
            ClothingColor.TUNIC_BLUE -> R.drawable.character_boy_blue_curly
            ClothingColor.TUNIC_GREEN -> R.drawable.character_boy_green_curly
            ClothingColor.ROBE_RED -> R.drawable.character_boy_red_curly
            ClothingColor.VEST_YELLOW -> R.drawable.character_boy_yellow_curly
            ClothingColor.ROBE_PURPLE -> R.drawable.character_boy_purple_curly
            ClothingColor.BROWN -> R.drawable.character_boy_brown_curly
            ClothingColor.PINK -> R.drawable.character_boy_pink_curly
        }
        Hairstyle.BRAIDED -> when (clothing) {
            ClothingColor.TUNIC_BLUE -> R.drawable.character_boy_blue_pigtail
            ClothingColor.TUNIC_GREEN -> R.drawable.character_boy_green_pigtail
            ClothingColor.ROBE_RED -> R.drawable.character_boy_red_pigtail
            ClothingColor.VEST_YELLOW -> R.drawable.character_boy_yellow_pigtail
            ClothingColor.ROBE_PURPLE -> R.drawable.character_boy_purple_pigtail
            ClothingColor.BROWN -> R.drawable.character_boy_brown_pigtail
            ClothingColor.PINK -> R.drawable.character_boy_pink_pigtail
        }
        Hairstyle.PONYTAIL -> when (clothing) {
            ClothingColor.TUNIC_BLUE -> R.drawable.character_boy_blue_ponytail
            ClothingColor.TUNIC_GREEN -> R.drawable.character_boy_green_ponytail
            ClothingColor.ROBE_RED -> R.drawable.character_boy_red_ponytail
            ClothingColor.VEST_YELLOW -> R.drawable.character_boy_yellow_ponytail
            ClothingColor.ROBE_PURPLE -> R.drawable.character_boy_purple_ponytail
            ClothingColor.BROWN -> R.drawable.character_boy_brown_ponytail
            ClothingColor.PINK -> R.drawable.character_boy_pink_ponytail
        }
        Hairstyle.LONG -> when (clothing) {
            ClothingColor.TUNIC_BLUE -> R.drawable.character_boy_blue_long
            ClothingColor.TUNIC_GREEN -> R.drawable.character_boy_green_long
            ClothingColor.ROBE_RED -> R.drawable.character_boy_red_long
            ClothingColor.VEST_YELLOW -> R.drawable.character_boy_yellow_long
            ClothingColor.ROBE_PURPLE -> R.drawable.character_boy_purple_long
            ClothingColor.BROWN -> R.drawable.character_boy_brown_long
            ClothingColor.PINK -> R.drawable.character_boy_pink_long
        }
    }
    Appearance.GIRL -> when (hairstyle) {
        Hairstyle.SHORT -> when (clothing) {
            ClothingColor.TUNIC_BLUE -> R.drawable.character_girl_blue_short
            ClothingColor.TUNIC_GREEN -> R.drawable.character_girl_green_short
            ClothingColor.ROBE_RED -> R.drawable.character_girl_red_short
            ClothingColor.VEST_YELLOW -> R.drawable.character_girl_yellow_short
            ClothingColor.ROBE_PURPLE -> R.drawable.character_girl_purple_short
            ClothingColor.BROWN -> R.drawable.character_girl_brown_short
            ClothingColor.PINK -> R.drawable.character_girl_pink_short
        }
        Hairstyle.CURLY -> when (clothing) {
            ClothingColor.TUNIC_BLUE -> R.drawable.character_girl_blue_curly
            ClothingColor.TUNIC_GREEN -> R.drawable.character_girl_green_curly
            ClothingColor.ROBE_RED -> R.drawable.character_girl_red_curly
            ClothingColor.VEST_YELLOW -> R.drawable.character_girl_yellow_curly
            ClothingColor.ROBE_PURPLE -> R.drawable.character_girl_purple_curly
            ClothingColor.BROWN -> R.drawable.character_girl_brown_curly
            ClothingColor.PINK -> R.drawable.character_girl_pink_curly
        }
        Hairstyle.BRAIDED -> when (clothing) {
            ClothingColor.TUNIC_BLUE -> R.drawable.character_girl_blue_pigtail
            ClothingColor.TUNIC_GREEN -> R.drawable.character_girl_green_pigtail
            ClothingColor.ROBE_RED -> R.drawable.character_girl_red_pigtail
            ClothingColor.VEST_YELLOW -> R.drawable.character_girl_yellow_pigtail
            ClothingColor.ROBE_PURPLE -> R.drawable.character_girl_purple_pigtail
            ClothingColor.BROWN -> R.drawable.character_girl_brown_pigtail
            ClothingColor.PINK -> R.drawable.character_girl_pink_pigtail
        }
        Hairstyle.PONYTAIL -> when (clothing) {
            ClothingColor.TUNIC_BLUE -> R.drawable.character_girl_blue_ponytail
            ClothingColor.TUNIC_GREEN -> R.drawable.character_girl_green_ponytail
            ClothingColor.ROBE_RED -> R.drawable.character_girl_red_ponytail
            ClothingColor.VEST_YELLOW -> R.drawable.character_girl_yellow_ponytail
            ClothingColor.ROBE_PURPLE -> R.drawable.character_girl_purple_ponytail
            ClothingColor.BROWN -> R.drawable.character_girl_brown_ponytail
            ClothingColor.PINK -> R.drawable.character_girl_pink_ponytail
        }
        Hairstyle.LONG -> when (clothing) {
            ClothingColor.TUNIC_BLUE -> R.drawable.character_girl_blue_long
            ClothingColor.TUNIC_GREEN -> R.drawable.character_girl_green_long
            ClothingColor.ROBE_RED -> R.drawable.character_girl_red_long
            ClothingColor.VEST_YELLOW -> R.drawable.character_girl_yellow_long
            ClothingColor.ROBE_PURPLE -> R.drawable.character_girl_purple_long
            ClothingColor.BROWN -> R.drawable.character_girl_brown_long
            ClothingColor.PINK -> R.drawable.character_girl_pink_long
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CharacterPreviewPreview() {
    BibleAdventuresTheme {
        CharacterPreview(customization = CharacterCustomization())
    }
}
