package com.bibleadventures.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.bibleadventures.R
import com.bibleadventures.character.CharacterOptionCatalog
import com.bibleadventures.domain.model.Appearance
import com.bibleadventures.domain.model.CharacterCustomization
import com.bibleadventures.domain.model.Hairstyle
import com.bibleadventures.ui.theme.BibleAdventuresTheme

/**
 * Simple Compose-primitive character render — placeholder art (spec
 * section 25). No per-combination image assets needed: swapping in final
 * character art later only touches this file and [CharacterOptionCatalog].
 */
@Composable
fun CharacterPreview(
    customization: CharacterCustomization,
    modifier: Modifier = Modifier,
) {
    val contentDescription = stringResource(R.string.character_preview_content_description)
    val skinColor = CharacterOptionCatalog.skinTones.first { it.value == customization.skinTone }.swatchColor
    val clothingColor = CharacterOptionCatalog.clothingOptions.first { it.value == customization.clothing }.swatchColor
    val hairColor = CharacterOptionCatalog.hairColor

    Canvas(
        modifier = modifier
            .size(160.dp)
            .semantics { this.contentDescription = contentDescription },
    ) {
        drawBody(clothingColor, customization.appearance)
        drawHead(skinColor)
        drawHair(hairColor, customization.hairstyle)
    }
}

private fun DrawScope.drawBody(color: androidx.compose.ui.graphics.Color, appearance: Appearance) {
    val bodyTop = size.height * 0.55f
    val bodyHeight = size.height * 0.4f
    when (appearance) {
        Appearance.BOY -> drawRoundRect(
            color = color,
            topLeft = Offset(size.width * 0.28f, bodyTop),
            size = Size(size.width * 0.44f, bodyHeight),
            cornerRadius = CornerRadius(size.width * 0.08f),
        )
        Appearance.GIRL -> {
            val path = Path().apply {
                moveTo(size.width * 0.4f, bodyTop)
                lineTo(size.width * 0.6f, bodyTop)
                lineTo(size.width * 0.78f, bodyTop + bodyHeight)
                lineTo(size.width * 0.22f, bodyTop + bodyHeight)
                close()
            }
            drawPath(path, color = color)
        }
    }
}

private fun DrawScope.drawHead(color: androidx.compose.ui.graphics.Color) {
    drawCircle(
        color = color,
        radius = size.width * 0.22f,
        center = Offset(size.width * 0.5f, size.height * 0.35f),
    )
}

private fun DrawScope.drawHair(color: androidx.compose.ui.graphics.Color, hairstyle: Hairstyle) {
    val headCenter = Offset(size.width * 0.5f, size.height * 0.35f)
    val headRadius = size.width * 0.22f
    when (hairstyle) {
        Hairstyle.SHORT -> drawArc(
            color = color,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(headCenter.x - headRadius, headCenter.y - headRadius),
            size = Size(headRadius * 2, headRadius * 2),
        )
        Hairstyle.CURLY -> {
            val bumpRadius = headRadius * 0.32f
            listOf(-0.6f, -0.2f, 0.2f, 0.6f).forEach { fraction ->
                drawCircle(
                    color = color,
                    radius = bumpRadius,
                    center = Offset(headCenter.x + headRadius * fraction, headCenter.y - headRadius * 0.75f),
                )
            }
        }
        Hairstyle.BRAIDED -> {
            drawArc(
                color = color,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = true,
                topLeft = Offset(headCenter.x - headRadius, headCenter.y - headRadius),
                size = Size(headRadius * 2, headRadius * 2),
            )
            listOf(-1f, 1f).forEach { side ->
                drawRoundRect(
                    color = color,
                    topLeft = Offset(headCenter.x + side * headRadius * 0.85f - headRadius * 0.12f, headCenter.y),
                    size = Size(headRadius * 0.24f, headRadius * 1.1f),
                    cornerRadius = CornerRadius(headRadius * 0.12f),
                )
            }
        }
        Hairstyle.PONYTAIL -> {
            drawArc(
                color = color,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = true,
                topLeft = Offset(headCenter.x - headRadius, headCenter.y - headRadius),
                size = Size(headRadius * 2, headRadius * 2),
            )
            drawCircle(
                color = color,
                radius = headRadius * 0.35f,
                center = Offset(headCenter.x + headRadius * 0.95f, headCenter.y + headRadius * 0.2f),
            )
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
