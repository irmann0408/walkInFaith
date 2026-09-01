package com.bibleadventures.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.bibleadventures.R
import com.bibleadventures.domain.model.Appearance
import com.bibleadventures.domain.model.CharacterCustomization
import com.bibleadventures.domain.model.ClothingColor
import com.bibleadventures.domain.model.Hairstyle
import com.bibleadventures.ui.theme.BibleAdventuresTheme
import androidx.compose.ui.tooling.preview.Preview

/**
 * Renders the player's chosen character as 3 stacked layers — body, costume,
 * then hair — instead of one flattened image per (appearance, hairstyle,
 * clothing) combination. That flat-image approach doesn't scale: a new
 * posture would need a fresh image for every one of the 70 combinations.
 * Layering means a new hairstyle only needs 2 new images (one per
 * appearance) that work with every existing costume color for free, and
 * likewise a new costume color only needs 2 new images that work with
 * every existing hairstyle.
 *
 * The body and costume images share one fixed pixel canvas per appearance
 * (576×1024 for girls, 256×1024 for boys — see [bodyAspectRatio]) and are
 * pixel-for-pixel aligned, so both render as a plain `fillMaxSize()` inside
 * an [AspectRatioFitBox] locked to that canvas's own aspect ratio — the
 * same "fit, don't distort, letterbox on whichever axis has slack" behavior
 * `Modifier.aspectRatio` alone doesn't give you (see that component's own
 * doc comment). Hair art doesn't share the body's proportions or position,
 * so each [Hairstyle] × [Appearance] combination carries its own [HairFit]:
 * a scale factor and an (x, y) offset, both as fractions of that same
 * canvas — found by compositing real art with Python/PIL, looking at the
 * result, and nudging by eye until the hair actually sat on the head
 * correctly, not by any formula. `Modifier.size()`/`.offset()` inside a
 * [BoxWithConstraints] convert those fractions back into this composable's
 * own actual rendered size, so the fit holds at any size a caller passes in
 * (160dp default here, 96dp in [CharacterCallout], 72dp in mini-games).
 */
@Composable
fun CharacterPreview(
    customization: CharacterCustomization,
    modifier: Modifier = Modifier,
) {
    val contentDescription = stringResource(R.string.character_preview_content_description)
    val hairFit = hairFit(customization.appearance, customization.hairstyle)

    AspectRatioFitBox(
        ratio = bodyAspectRatio(customization.appearance),
        modifier = modifier
            .size(160.dp)
            .semantics { this.contentDescription = contentDescription },
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(bodyRes(customization.appearance)),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
            Image(
                painter = painterResource(costumeRes(customization.appearance, customization.clothing)),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
            Image(
                painter = painterResource(hairFit.res),
                contentDescription = null,
                modifier = Modifier
                    .size(width = maxWidth * hairFit.scale, height = maxHeight * hairFit.scale)
                    .offset(x = maxWidth * hairFit.offsetXFraction, y = maxHeight * hairFit.offsetYFraction),
            )
        }
    }
}

private fun bodyAspectRatio(appearance: Appearance): Float = when (appearance) {
    Appearance.BOY -> 256f / 1024f
    Appearance.GIRL -> 576f / 1024f
}

private fun bodyRes(appearance: Appearance): Int = when (appearance) {
    Appearance.BOY -> R.drawable.character_body_boy
    Appearance.GIRL -> R.drawable.character_body_girl
}

private fun costumeRes(appearance: Appearance, clothing: ClothingColor): Int = when (appearance) {
    Appearance.BOY -> when (clothing) {
        ClothingColor.TUNIC_BLUE -> R.drawable.character_costume_boy_blue
        ClothingColor.TUNIC_GREEN -> R.drawable.character_costume_boy_green
        ClothingColor.ROBE_RED -> R.drawable.character_costume_boy_red
        ClothingColor.VEST_YELLOW -> R.drawable.character_costume_boy_yellow
        ClothingColor.ROBE_PURPLE -> R.drawable.character_costume_boy_purple
        ClothingColor.BROWN -> R.drawable.character_costume_boy_brown
        ClothingColor.PINK -> R.drawable.character_costume_boy_pink
    }
    Appearance.GIRL -> when (clothing) {
        ClothingColor.TUNIC_BLUE -> R.drawable.character_costume_girl_blue
        ClothingColor.TUNIC_GREEN -> R.drawable.character_costume_girl_green
        ClothingColor.ROBE_RED -> R.drawable.character_costume_girl_red
        ClothingColor.VEST_YELLOW -> R.drawable.character_costume_girl_yellow
        ClothingColor.ROBE_PURPLE -> R.drawable.character_costume_girl_purple
        ClothingColor.BROWN -> R.drawable.character_costume_girl_brown
        ClothingColor.PINK -> R.drawable.character_costume_girl_pink
    }
}

/**
 * [scale]/[offsetXFraction]/[offsetYFraction] are all fractions of the
 * body/costume canvas's own width or height (see [bodyAspectRatio]) — found
 * by fitting real art in a Python/PIL script (scale the hair, position it,
 * render a preview, look at it, adjust), not computed from any rule. A new
 * hairstyle needs its own pass through that same process, not a formula
 * plugged in here.
 */
private data class HairFit(
    @DrawableRes val res: Int,
    val scale: Float,
    val offsetXFraction: Float,
    val offsetYFraction: Float,
)

private fun hairFit(appearance: Appearance, hairstyle: Hairstyle): HairFit = when (appearance) {
    Appearance.GIRL -> when (hairstyle) {
        Hairstyle.SHORT -> HairFit(R.drawable.character_hair_girl_short, 0.9363077f, 25f / 576f, -222f / 1024f)
        Hairstyle.CURLY -> HairFit(R.drawable.character_hair_girl_curly, 0.9502793f, 13f / 576f, -134f / 1024f)
        Hairstyle.BRAIDED -> HairFit(R.drawable.character_hair_girl_braided, 1.0104839f, -6f / 576f, -265f / 1024f)
        Hairstyle.PONYTAIL -> HairFit(R.drawable.character_hair_girl_ponytail, 1.0132075f, 1f / 576f, -3f / 1024f)
        Hairstyle.LONG -> HairFit(R.drawable.character_hair_girl_long, 1.0042904f, -4f / 576f, -5f / 1024f)
    }
    Appearance.BOY -> when (hairstyle) {
        Hairstyle.SHORT -> HairFit(R.drawable.character_hair_boy_short, 1.0316742f, -6f / 256f, -138f / 1024f)
        Hairstyle.CURLY -> HairFit(R.drawable.character_hair_boy_curly, 0.9870130f, 0f, -134f / 1024f)
        Hairstyle.BRAIDED -> HairFit(R.drawable.character_hair_boy_braided, 0.9963415f, 0f, -38f / 1024f)
        Hairstyle.PONYTAIL -> HairFit(R.drawable.character_hair_boy_ponytail, 1.0469388f, 1f / 256f, -176f / 1024f)
        Hairstyle.LONG -> HairFit(R.drawable.character_hair_boy_long, 0.9686275f, 11f / 256f, -20f / 1024f)
    }
}

@Preview(showBackground = true)
@Composable
private fun CharacterPreviewPreview() {
    BibleAdventuresTheme {
        CharacterPreview(customization = CharacterCustomization())
    }
}
