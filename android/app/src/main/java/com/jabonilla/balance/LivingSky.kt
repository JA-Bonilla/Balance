package com.jabonilla.balance

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.text.rememberTextMeasurer
import kotlin.math.sin

data class RGB(val r: Double, val g: Double, val b: Double) {
    constructor(hex: Int) : this((hex shr 16 and 0xFF) / 255.0, (hex shr 8 and 0xFF) / 255.0, (hex and 0xFF) / 255.0)

    fun color(opacity: Double = 1.0) = Color(r.toFloat(), g.toFloat(), b.toFloat(), opacity.toFloat())

    companion object {
        fun mix(a: RGB, b: RGB, t: Double) = RGB(a.r + (b.r - a.r) * t, a.g + (b.g - a.g) * t, a.b + (b.b - a.b) * t)
    }
}

/** Everything about the sky that changes with balance. Positions are in a 342x340 design box. */
data class SkyParams(
    val top: RGB, val bottom: RGB, val horizon: RGB, val sunColor: RGB, val sunGlow: RGB, val moonColor: RGB, val ground: RGB,
    val horizonA: Double, val stars: Double, val sunX: Double, val sunY: Double, val sunR: Double, val sunGlowA: Double,
    val moonX: Double, val moonY: Double, val moonR: Double, val moonA: Double, val rim: Double, val clouds: Double,
    val grass: Double, val desert: Double,
) {
    companion object {
        fun mix(a: SkyParams, b: SkyParams, t: Double): SkyParams {
            fun m(x: Double, y: Double) = x + (y - x) * t
            return SkyParams(
                RGB.mix(a.top, b.top, t), RGB.mix(a.bottom, b.bottom, t), RGB.mix(a.horizon, b.horizon, t),
                RGB.mix(a.sunColor, b.sunColor, t), RGB.mix(a.sunGlow, b.sunGlow, t),
                RGB.mix(a.moonColor, b.moonColor, t), RGB.mix(a.ground, b.ground, t),
                m(a.horizonA, b.horizonA), m(a.stars, b.stars), m(a.sunX, b.sunX), m(a.sunY, b.sunY),
                m(a.sunR, b.sunR), m(a.sunGlowA, b.sunGlowA), m(a.moonX, b.moonX), m(a.moonY, b.moonY),
                m(a.moonR, b.moonR), m(a.moonA, b.moonA), m(a.rim, b.rim), m(a.clouds, b.clouds),
                m(a.grass, b.grass), m(a.desert, b.desert),
            )
        }

        val eclipse = SkyParams(
            RGB(0x07061A), RGB(0x2A1F4D), RGB(0x6B4FA8), RGB(0xF2B84B), RGB(0xF2B84B), RGB(0x2B2447), RGB(0x17162A),
            0.3, 14.0, 171.0, 142.0, 46.0, 0.5, 176.0, 146.0, 44.0, 1.0, 1.0, 0.0, 0.0, 0.0)
        val dusk = SkyParams(
            RGB(0x2B2350), RGB(0xC0708A), RGB(0xFF9A6B), RGB(0xFFB45A), RGB(0xFF9A6B), RGB(0xCBB8FF), RGB(0x2A2438),
            0.45, 6.0, 120.0, 148.0, 40.0, 0.5, 158.0, 131.0, 36.0, 1.0, 0.0, 0.0, 0.0, 0.0)
        val golden = SkyParams(
            RGB(0x4B3F7A), RGB(0xF0B27A), RGB(0xFFD08A), RGB(0xF2B84B), RGB(0xFFD08A), RGB(0xCBB8FF), RGB(0x2C2F26),
            0.5, 3.0, 95.0, 134.0, 36.0, 0.5, 238.0, 130.0, 28.0, 0.95, 0.0, 0.0, 0.0, 0.0)
        val day = SkyParams(
            RGB(0x4FA8E8), RGB(0xD6F0FF), RGB(0xFFFFFF), RGB(0xFFD84F), RGB(0xFFE680), RGB(0xE8E0FF), RGB(0x3E7F4B),
            0.0, 0.0, 105.0, 152.0, 46.0, 0.6, 280.0, 134.0, 16.0, 0.5, 0.0, 1.0, 1.0, 0.0)
        val desertSky = SkyParams(
            RGB(0xF08A3C), RGB(0xFFE3A6), RGB(0xFFE3A6), RGB(0xFFF3B0), RGB(0xFFE680), RGB(0xE8E0FF), RGB(0xD9B26F),
            0.0, 0.0, 171.0, 156.0, 54.0, 0.7, 280.0, 134.0, 10.0, 0.0, 0.0, 0.0, 0.0, 1.0)

        /** -1 total eclipse, -0.5 dusk, 0 golden hour (on target), +0.5 bright day, +1 desert. */
        private val stops = listOf(-1.0 to eclipse, -0.5 to dusk, 0.0 to golden, 0.5 to day, 1.0 to desertSky)

        fun at(balance: Double): SkyParams {
            val b = balance.coerceIn(-1.0, 1.0)
            for (i in 1 until stops.size) {
                if (b <= stops[i].first) {
                    val (lo, hi) = stops[i - 1] to stops[i]
                    return mix(lo.second, hi.second, (b - lo.first) / (hi.first - lo.first))
                }
            }
            return desertSky
        }
    }
}

private val stars = listOf(
    30f to 30f, 80f to 55f, 130f to 22f, 200f to 40f, 260f to 28f, 310f to 60f, 50f to 100f,
    300f to 110f, 110f to 80f, 230f to 90f, 20f to 150f, 320f to 160f, 160f to 50f, 70f to 20f,
)

private fun DrawScope.dot(x: Double, y: Double, r: Double, color: Color) =
    drawCircle(color, r.toFloat(), Offset(x.toFloat(), y.toFloat()))

/** A soft blob: a radial gradient from the color to transparent, standing in for a Gaussian blur (works on API 26). */
private fun DrawScope.glow(x: Double, y: Double, rx: Double, ry: Double, blur: Double, color: Color) {
    val big = (rx + blur).toFloat()
    val c = Offset(x.toFloat(), y.toFloat())
    scale(1f, ((ry + blur) / (rx + blur)).toFloat(), pivot = c) {
        drawCircle(
            Brush.radialGradient(0f to color, (rx / (rx + blur)).toFloat() to color.copy(alpha = color.alpha * 0.55f), 1f to color.copy(alpha = 0f), center = c, radius = big),
            big, c,
        )
    }
}

fun DrawScope.drawLivingSky(balance: Double, stage: Int, t: Double, measurer: androidx.compose.ui.text.TextMeasurer) {
    val p = SkyParams.at(balance)
    scale(size.width / 342f, pivot = Offset.Zero) {
        drawRect(
            Brush.verticalGradient(listOf(p.top.color(), p.bottom.color()), startY = 0f, endY = 340f),
            size = Size(342f, 380f),
        )
        stars.forEachIndexed { i, s ->
            val a = (p.stars - i).coerceIn(0.0, 1.0) * (0.7 + 0.3 * sin(t * 1.7 + i * 1.3))
            if (a > 0.01) dot(s.first.toDouble(), s.second.toDouble(), if (i % 3 == 0) 1.8 else 1.3, Color.White.copy(alpha = (a * 0.9).toFloat()))
        }
        if (p.horizonA > 0.01) glow(171.0, 310.0, 210.0, 65.0, 40.0, p.horizon.color(p.horizonA))
        if (p.clouds > 0.01) {
            for ((x0, y, w, speed) in listOf(listOf(250.0, 70.0, 90.0, 4.0), listOf(70.0, 140.0, 70.0, 2.5))) {
                val x = -60 + (x0 + 60 + t * speed) % 460
                drawOval(Color.White.copy(alpha = (0.8 * p.clouds).toFloat()), Offset((x - w / 2).toFloat(), (y - w * 0.175).toFloat()), Size(w.toFloat(), (w * 0.35).toFloat()))
            }
        }

        // Sun (growth)
        val pulse = 1 + 0.03 * sin(t * 1.4)
        glow(p.sunX, p.sunY, p.sunR * 1.7 * pulse, p.sunR * 1.7 * pulse, p.sunR * 0.7, p.sunGlow.color(p.sunGlowA))
        if (p.desert > 0.01) {
            for (k in listOf(1.5, 2.0)) {
                val r = p.sunR * k * (1 + 0.02 * sin(t * 1.2 + k))
                drawCircle(RGB(0xFFF3B0).color(0.35 / k * p.desert), r.toFloat(), Offset(p.sunX.toFloat(), p.sunY.toFloat()), style = Stroke(2f))
            }
        }
        dot(p.sunX, p.sunY, p.sunR, p.sunColor.color())

        // Moon (fun)
        if (p.moonA > 0.01) {
            val mx = p.moonX + 3 * sin(t * 0.4)
            val my = p.moonY + 2 * sin(t * 0.55)
            val lavender = RGB(0xB79CFF)
            glow(mx, my, p.moonR * 1.5, p.moonR * 1.5, p.moonR * 0.6, lavender.color(0.25 * p.moonA))
            dot(mx, my, p.moonR, p.moonColor.color(p.moonA))
            if (p.rim > 0.01) drawCircle(lavender.color(p.rim), p.moonR.toFloat(), Offset(mx.toFloat(), my.toFloat()), style = Stroke(2f))
            val craterA = (1 - p.rim) * 0.55 * p.moonA
            if (craterA > 0.01) {
                for ((dx, dy, d) in listOf(listOf(-8.0, -6.0, 9.0), listOf(7.0, 5.0, 6.0), listOf(-2.0, 10.0, 5.0))) {
                    dot(mx + dx * p.moonR / 28, my + dy * p.moonR / 28, d / 2, RGB(0x9C86D9).color(craterA))
                }
            }
        }

        // Ground
        drawOval(p.ground.color(), Offset(-59f, 275f), Size(460f, 140f))
        if (p.grass > 0.01) {
            for ((x, y) in listOf(60f to 292f, 95f to 298f, 250f to 294f, 285f to 290f, 215f to 302f)) {
                drawOval(RGB(0x6FBF7C).color(p.grass), Offset(x - 7, y - 4), Size(14f, 8f))
            }
        }
        if (p.desert > 0.01) {
            val a = p.desert
            for ((x, y, w, h) in listOf(listOf(70f, 302f, 160f, 18f), listOf(270f, 304f, 170f, 20f))) {
                drawOval(RGB(0xE6C384).color(a), Offset(x - w / 2, y - h / 2), Size(w, h))
            }
            for ((x, y, l, deg) in listOf(listOf(120f, 316f, 40f, -20f), listOf(210f, 320f, 36f, 15f), listOf(250f, 310f, 30f, -30f))) {
                rotate(-deg, Offset(x + l / 2, y)) {
                    drawRect(RGB(0xB88E4D).color(a), Offset(x, y - 1), Size(l, 2f))
                }
            }
            val cactus = RGB(0x5C8F4F).color(a)
            for ((x, y, w, h) in listOf(
                listOf(288f, 254f, 12f, 44f), listOf(276f, 268f, 8f, 6f), listOf(276f, 262f, 6f, 18f),
                listOf(300f, 276f, 8f, 6f), listOf(302f, 264f, 6f, 16f),
            )) {
                val r = minOf(w, h) / 2
                drawRoundRect(cactus, Offset(x, y), Size(w, h), CornerRadius(r, r))
            }
        }

        // Bonsi: sleepy under a moon-heavy sky, worried as the sun scorches the garden.
        val mood = if (balance < -0.3) BonsiMood.SLEEPY else if (p.desert > 0.5) BonsiMood.WORRIED else BonsiMood.HAPPY
        translate(171f - 110f, 326f - 209f) {
            scale(1.1f, pivot = Offset.Zero) { drawBonsi(stage, mood, p.desert, t, measurer) }
        }
    }
}

/** Sun = growth, moon = fun, a potted Bonsi in the middle. Decorative, so no semantics. */
@Composable
fun LivingSky(balance: Float, stage: Int, reduceMotion: Boolean) {
    val time = remember { mutableDoubleStateOf(0.0) }
    // Time is read only inside the draw lambda, so frames redraw without recomposing.
    LaunchedEffect(reduceMotion) {
        if (reduceMotion) { time.doubleValue = 0.0; return@LaunchedEffect }
        while (true) androidx.compose.runtime.withFrameNanos { time.doubleValue = it / 1e9 }
    }
    val measurer = rememberTextMeasurer()
    Canvas(
        Modifier
            .fillMaxWidth()
            .aspectRatio(342f / 380f)
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            // Full-bleed at the sides and top; only the ground melts into the app background.
            .drawWithContent {
                drawContent()
                drawRect(Brush.verticalGradient(0f to Color.Black, 0.82f to Color.Black, 1f to Color.Transparent), blendMode = BlendMode.DstIn)
            },
    ) { drawLivingSky(balance.toDouble(), stage, time.doubleValue, measurer) }
}
