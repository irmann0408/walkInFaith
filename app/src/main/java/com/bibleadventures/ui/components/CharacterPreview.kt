package com.bibleadventures.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bibleadventures.R
import com.bibleadventures.character.CharacterOptionCatalog
import com.bibleadventures.domain.model.Appearance
import com.bibleadventures.domain.model.CharacterCustomization
import com.bibleadventures.domain.model.Hairstyle
import com.bibleadventures.ui.theme.BibleAdventuresTheme

/** A consistent warm dark-brown "cartoon sticker" outline used on every shape. */
private val OutlineColor = Color(0xFF4A2E1E)
private val EyeColor = Color(0xFF2E1A0F)
private val BlushColor = Color(0xFFFF8A80)

private const val HeadRadiusFraction = 0.30f
private const val HeadCenterYFraction = 0.34f

/**
 * Chibi-proportioned character render, entirely Compose `Canvas`
 * primitives — placeholder art (spec section 25), no per-combination
 * image assets needed: swapping in final character art later only
 * touches this file and [CharacterOptionCatalog]. Deliberately
 * big-headed/small-bodied with a simple face, limbs, and outline strokes
 * so it reads as a cartoon character rather than raw geometric shapes —
 * every measurement is a fraction of the canvas, never a fixed dp, since
 * this same composable renders at 160dp on story screens and ~72dp in
 * the lane mini-games (David & Goliath's Crossing the Valley, Daniel's
 * Hurrying to Pray, Jesus Calms the Storm's Bailing the Boat).
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
        // Back to front: legs and arms first so the body/head are drawn
        // on top of their inner edges — the head overlapping the body's
        // top and the body hem overlapping the legs' top is what gives
        // this a seamless "no visible neck/waist joints" chibi silhouette.
        drawLegs(skinColor)
        drawBody(clothingColor, customization.appearance)
        drawArms(skinColor)
        drawHead(skinColor)
        drawHair(hairColor, customization.hairstyle)
        drawFace()
    }
}

private fun DrawScope.headCenter() = Offset(size.width * 0.5f, size.height * HeadCenterYFraction)
private fun DrawScope.headRadius() = size.width * HeadRadiusFraction
private fun DrawScope.outlineWidth() = size.width * 0.02f

private fun DrawScope.drawLegs(color: Color) {
    val legWidth = size.width * 0.13f
    val legTop = size.height * 0.80f
    val legHeight = size.height * 0.16f
    val strokeWidth = outlineWidth()
    val cornerRadius = CornerRadius(legWidth / 2f)

    listOf(-0.11f, 0.11f).forEach { xFraction ->
        val topLeft = Offset(size.width * (0.5f + xFraction) - legWidth / 2f, legTop)
        val legSize = Size(legWidth, legHeight)
        drawRoundRect(color = color, topLeft = topLeft, size = legSize, cornerRadius = cornerRadius)
        drawRoundRect(color = OutlineColor, topLeft = topLeft, size = legSize, cornerRadius = cornerRadius, style = Stroke(strokeWidth))
    }
}

private fun DrawScope.drawArms(color: Color) {
    val armWidth = size.width * 0.11f
    val armTop = size.height * 0.60f
    val armHeight = size.height * 0.20f
    val strokeWidth = outlineWidth()
    val cornerRadius = CornerRadius(armWidth / 2f)

    listOf(-0.20f, 0.20f).forEach { xFraction ->
        val topLeft = Offset(size.width * (0.5f + xFraction) - armWidth / 2f, armTop)
        val armSize = Size(armWidth, armHeight)
        drawRoundRect(color = color, topLeft = topLeft, size = armSize, cornerRadius = cornerRadius)
        drawRoundRect(color = OutlineColor, topLeft = topLeft, size = armSize, cornerRadius = cornerRadius, style = Stroke(strokeWidth))
    }
}

/** A soft, rounded tunic/dress silhouette — curved sides via quadratic beziers instead of the old straight-edged rectangle/trapezoid, with a gentle flare at the hem (wider for the girl's dress). */
private fun DrawScope.drawBody(color: Color, appearance: Appearance) {
    val bodyTop = size.height * 0.58f
    val bodyBottom = size.height * 0.86f
    val bodyMidY = bodyTop + (bodyBottom - bodyTop) * 0.5f
    val topHalfWidth = size.width * 0.17f
    val bottomHalfWidth = when (appearance) {
        Appearance.BOY -> size.width * 0.20f
        Appearance.GIRL -> size.width * 0.27f
    }
    val centerX = size.width * 0.5f
    val strokeWidth = outlineWidth()

    val path = Path().apply {
        moveTo(centerX - topHalfWidth, bodyTop)
        lineTo(centerX + topHalfWidth, bodyTop)
        quadraticBezierTo(centerX + bottomHalfWidth, bodyMidY, centerX + bottomHalfWidth, bodyBottom)
        lineTo(centerX - bottomHalfWidth, bodyBottom)
        quadraticBezierTo(centerX - bottomHalfWidth, bodyMidY, centerX - topHalfWidth, bodyTop)
        close()
    }
    drawPath(path, color = color)
    drawPath(path, color = OutlineColor, style = Stroke(strokeWidth))
}

private fun DrawScope.drawHead(color: Color) {
    drawCircle(color = color, radius = headRadius(), center = headCenter())
    drawCircle(color = OutlineColor, radius = headRadius(), center = headCenter(), style = Stroke(outlineWidth()))
}

private fun DrawScope.drawFace() {
    val center = headCenter()
    val radius = headRadius()

    val eyeRadius = radius * 0.10f
    val eyeY = center.y - radius * 0.05f
    listOf(-0.34f, 0.34f).forEach { xFraction ->
        drawCircle(color = EyeColor, radius = eyeRadius, center = Offset(center.x + radius * xFraction, eyeY))
    }

    val blushRadius = radius * 0.18f
    val blushY = center.y + radius * 0.20f
    listOf(-0.55f, 0.55f).forEach { xFraction ->
        drawCircle(
            color = BlushColor.copy(alpha = 0.45f),
            radius = blushRadius,
            center = Offset(center.x + radius * xFraction, blushY),
        )
    }

    // A short, upward-curving smile.
    val mouthWidth = radius * 0.6f
    val mouthHeight = radius * 0.35f
    drawArc(
        color = EyeColor,
        startAngle = 20f,
        sweepAngle = 140f,
        useCenter = false,
        topLeft = Offset(center.x - mouthWidth / 2f, center.y + radius * 0.18f),
        size = Size(mouthWidth, mouthHeight),
        style = Stroke(width = radius * 0.07f, cap = StrokeCap.Round),
    )
}

private fun DrawScope.drawHair(color: Color, hairstyle: Hairstyle) {
    val headCenter = headCenter()
    val headRadius = headRadius()
    val strokeWidth = outlineWidth()
    val capTopLeft = Offset(headCenter.x - headRadius, headCenter.y - headRadius)
    val capSize = Size(headRadius * 2, headRadius * 2)

    fun drawCap() {
        drawArc(color = color, startAngle = 180f, sweepAngle = 180f, useCenter = true, topLeft = capTopLeft, size = capSize)
        drawArc(color = OutlineColor, startAngle = 180f, sweepAngle = 180f, useCenter = false, topLeft = capTopLeft, size = capSize, style = Stroke(strokeWidth))
    }

    when (hairstyle) {
        Hairstyle.SHORT -> drawCap()
        Hairstyle.CURLY -> {
            val bumpRadius = headRadius * 0.36f
            listOf(-0.62f, -0.22f, 0.22f, 0.62f).forEach { fraction ->
                val bumpCenter = Offset(headCenter.x + headRadius * fraction, headCenter.y - headRadius * 0.78f)
                drawCircle(color = color, radius = bumpRadius, center = bumpCenter)
                drawCircle(color = OutlineColor, radius = bumpRadius, center = bumpCenter, style = Stroke(strokeWidth))
            }
        }
        Hairstyle.BRAIDED -> {
            drawCap()
            val braidSize = Size(headRadius * 0.28f, headRadius * 1.15f)
            val braidCorner = CornerRadius(headRadius * 0.14f)
            listOf(-1f, 1f).forEach { side ->
                val braidTopLeft = Offset(headCenter.x + side * headRadius * 0.88f - headRadius * 0.14f, headCenter.y)
                drawRoundRect(color = color, topLeft = braidTopLeft, size = braidSize, cornerRadius = braidCorner)
                drawRoundRect(color = OutlineColor, topLeft = braidTopLeft, size = braidSize, cornerRadius = braidCorner, style = Stroke(strokeWidth))
            }
        }
        // Kept the PONYTAIL enum name (renaming it would silently lose this
        // choice from existing saves), but drawn as twin pigtail buns now —
        // a single off-center bump read as confusing/lopsided once the face
        // was added, sitting right next to one cheek. Two symmetric bumps
        // read clearly as a hairstyle at any size.
        Hairstyle.PONYTAIL -> {
            drawCap()
            val pigtailRadius = headRadius * 0.32f
            val pigtailY = headCenter.y + headRadius * 0.15f
            listOf(-1.05f, 1.05f).forEach { xFraction ->
                val pigtailCenter = Offset(headCenter.x + headRadius * xFraction, pigtailY)
                drawCircle(color = color, radius = pigtailRadius, center = pigtailCenter)
                drawCircle(color = OutlineColor, radius = pigtailRadius, center = pigtailCenter, style = Stroke(strokeWidth))
            }
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
