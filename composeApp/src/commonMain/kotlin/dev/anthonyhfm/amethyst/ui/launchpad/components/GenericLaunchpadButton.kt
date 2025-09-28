package dev.anthonyhfm.amethyst.ui.launchpad.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.dp
import dev.anthonyhfm.amethyst.core.engine.heaven.RawLEDUpdate
import dev.anthonyhfm.amethyst.core.engine.heaven.mix

@Composable
fun GenericLaunchpadButton(
    effect: RawLEDUpdate = RawLEDUpdate(0, Color.Black),
    sizeModifier: Modifier,
    enableLightSpot: Boolean = true,
    shape: Shape = RoundedCornerShape(10)
) {
    val backgroundColor = computeColor(effect)

    Canvas(
        modifier = sizeModifier
            .clip(shape)
            .background(backgroundColor)
            .innerShadow(
                RectangleShape,
                Shadow(
                    radius = 12.dp,
                    spread = 8.dp,
                    color = run {
                        val darkFactor = 0.22f
                        val alpha = 0.6f
                        darkenColor(backgroundColor, darkFactor).copy(alpha = alpha)
                    },
                )
            )
    ) {
        if (enableLightSpot) {
            val buttonWidth = size.width
            val buttonHeight = size.height

            val luminance = 0.2126f * effect.color.red +
                    0.7152f * effect.color.green +
                    0.0722f * effect.color.blue

            val threshold = 0.03f
            if (luminance > threshold) {
                val baseAlpha = (luminance * 0.9f).coerceAtMost(1f)
                val innerAlpha = baseAlpha * 0.30f * (0.7f + 0.3f * luminance)

                val innerColor = Color.White.copy(alpha = innerAlpha)
                val tintColor = effect.color.copy(alpha = (innerAlpha * 0.6f).coerceIn(0f,1f))

                val gradient = Brush.radialGradient(
                    colors = listOf(
                        innerColor,
                        tintColor,
                        Color.Transparent
                    ),
                    center = Offset(buttonWidth / 2f, buttonHeight / 2f),
                    radius = size.minDimension / 2f
                )

                drawCircle(
                    brush = gradient,
                    radius = size.minDimension / 2f,
                    center = Offset(buttonWidth / 2f, buttonHeight / 2f)
                )
            }
        }
    }
}

private fun computeColor(effectData: RawLEDUpdate): Color {
    val background = Color(91, 91, 91)

    val base = effectData.color.mix(background)

    val luminance = 0.2126f * base.red +
            0.7152f * base.green +
            0.0722f * base.blue

    val darkThreshold = 0.10f
    if (luminance <= darkThreshold) {
        return base
    }

    val maxBrighten = 0.60f

    val t = ((luminance - darkThreshold) / (1f - darkThreshold)).coerceIn(0f, 1f)
    val curve = t * t
    val amount = curve * maxBrighten

    val rBright = (base.red + (1f - base.red) * amount).coerceIn(0f, 1f)
    val gBright = (base.green + (1f - base.green) * amount).coerceIn(0f, 1f)
    val bBright = (base.blue + (1f - base.blue) * amount).coerceIn(0f, 1f)

    val (_, sOrig, _) = rgbToHsl(base.red, base.green, base.blue)

    val maxSaturationBoost = 3f
    var satAmount = curve * maxSaturationBoost
    val satThreshold = 0.1f

    val (h, s, l) = rgbToHsl(rBright, gBright, bBright)
    val hueBoostFactor = when {
        h < 0.08f || h > 0.92f -> 1.25f
        h in 0.70f..0.92f -> 1.25f
        else -> 1.0f
    }
    satAmount *= hueBoostFactor

    val newS = if (sOrig <= satThreshold) {
        s
    } else {
        val vibranceBoost = satAmount * (1f - s)
        val multiplicative = s * (1f + satAmount * 0.45f * (0.6f + 0.4f * (1f - sOrig)))
        val towardsOne = s + (1f - s) * (satAmount * 0.5f)
        maxOf((s + vibranceBoost).coerceIn(0f,1f), multiplicative.coerceIn(0f,1f), towardsOne.coerceIn(0f,1f))
    }

    val contrastStrength = 0.20f
    val contrastMultiplier = 1f + contrastStrength * curve
    var newL = ((l - 0.5f) * contrastMultiplier + 0.5f).coerceIn(0f, 1f)

    val highlightBoost = 0.08f
    if (newL > 0.5f) {
        newL = (newL + (1f - newL) * highlightBoost * curve).coerceIn(0f, 1f)
    }

    val (rFinal, gFinal, bFinal) = hslToRgb(h, newS, newL)

    return Color(rFinal, gFinal, bFinal, base.alpha)
}

private fun rgbToHsl(r: Float, g: Float, b: Float): Triple<Float, Float, Float> {
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val l = (max + min) / 2f

    if (max == min) {
        return Triple(0f, 0f, l) // achromatisch
    }

    val d = max - min
    val s = if (l > 0.5f) d / (2f - max - min) else d / (max + min)

    val h = when (max) {
        r -> ((g - b) / d + (if (g < b) 6f else 0f)) / 6f
        g -> ((b - r) / d + 2f) / 6f
        else -> ((r - g) / d + 4f) / 6f
    }

    return Triple(h.coerceIn(0f, 1f), s.coerceIn(0f, 1f), l.coerceIn(0f, 1f))
}

private fun hueToRgb(p: Float, q: Float, tIn: Float): Float {
    var t = tIn
    if (t < 0f) t += 1f
    if (t > 1f) t -= 1f
    return when {
        t < 1f / 6f -> p + (q - p) * 6f * t
        t < 1f / 2f -> q
        t < 2f / 3f -> p + (q - p) * (2f / 3f - t) * 6f
        else -> p
    }
}

private fun hslToRgb(h: Float, s: Float, l: Float): Triple<Float, Float, Float> {
    if (s == 0f) {
        return Triple(l, l, l)
    }
    val q = if (l < 0.5f) l * (1f + s) else l + s - l * s
    val p = 2f * l - q
    val r = hueToRgb(p, q, h + 1f / 3f)
    val g = hueToRgb(p, q, h)
    val b = hueToRgb(p, q, h - 1f / 3f)
    return Triple(r.coerceIn(0f, 1f), g.coerceIn(0f, 1f), b.coerceIn(0f, 1f))
}

private fun darkenColor(c: Color, factor: Float): Color {
    return Color(
        (c.red * factor).coerceIn(0f, 1f),
        (c.green * factor).coerceIn(0f, 1f),
        (c.blue * factor).coerceIn(0f, 1f),
        c.alpha
    )
}
