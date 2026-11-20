package com.jabonilla.balance

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import kotlin.math.sin

enum class BonsiMood { HAPPY, SLEEPY, WORRIED }

private fun rgb(hex: Int, o: Double = 1.0) = RGB(hex).color(o)

private fun DrawScope.ellipse(x: Double, y: Double, w: Double, h: Double, color: Color) =
    drawOval(color, Offset((x - w / 2).toFloat(), (y - h / 2).toFloat()), Size(w.toFloat(), h.toFloat()))

private fun DrawScope.rrect(x: Double, y: Double, w: Double, h: Double, radius: Double, color: Color) =
    drawRoundRect(color, Offset(x.toFloat(), y.toFloat()), Size(w.toFloat(), h.toFloat()), CornerRadius(radius.toFloat()))

/** Rounded rect centred at (x, y), rotated `deg` counter-clockwise. */
private fun DrawScope.bar(x: Double, y: Double, w: Double, h: Double, deg: Double, color: Color) {
    rotate(-deg.toFloat(), Offset(x.toFloat(), y.toFloat())) { rrect(x - w / 2, y - h / 2, w, h, minOf(w, h) / 2, color) }
}

private fun mixHex(a: Int, b: Int, t: Double): Color = RGB.mix(RGB(a), RGB(b), t).color()

private fun DrawScope.curve(x0: Double, y0: Double, cx: Double, cy: Double, x1: Double, y1: Double, color: Color) {
    val p = Path().apply { moveTo(x0.toFloat(), y0.toFloat()); quadraticTo(cx.toFloat(), cy.toFloat(), x1.toFloat(), y1.toFloat()) }
    drawPath(p, color, style = Stroke(2.6f, cap = StrokeCap.Round))
}

/** The caricature bonsai: a potted tree with a face on its trunk. Draws into a 200x200 design box (pot base at y=190). */
fun DrawScope.drawBonsi(stage: Int, mood: BonsiMood, wilt: Double, t: Double, measurer: TextMeasurer) {
    val cx = 100.0
    val base = 190.0
    val dark = rgb(0x1C1D1A)
    // Pot (static)
    ellipse(cx, base + 2, 110.0, 14.0, Color.Black.copy(alpha = 0.25f))
    rrect(cx - 32, base - 30, 64.0, 30.0, 7.0, rgb(0xC97B55))
    rrect(cx - 39, base - 40, 78.0, 12.0, 6.0, rgb(0xDC8F68))
    rrect(cx - 22, base - 26, 6.0, 16.0, 3.0, rgb(0xE39A74))
    ellipse(cx, base - 40, 58.0, 8.0, rgb(0x4A3B2E))

    // Tree sways gently from the soil line and breathes.
    translate(cx.toFloat(), (base - 40).toFloat()) {
        rotate((1.6 * sin(t * 0.9)).toFloat(), Offset.Zero) {
            scale(1f, (1 + 0.012 * sin(t * 1.6)).toFloat(), pivot = Offset.Zero) {
                translate(-cx.toFloat(), -(base - 40).toFloat()) { tree(stage, mood, wilt, t, measurer, cx, base, dark) }
            }
        }
    }
}

private fun DrawScope.tree(stage: Int, mood: BonsiMood, wilt: Double, t: Double, measurer: TextMeasurer, cx: Double, base: Double, dark: Color) {
    val blink = t % 4.3 < 0.14

    if (stage == 0) {
        ellipse(cx, base - 52, 18.0, 24.0, rgb(0x8A6B4A))
        bar(cx + 3, base - 70, 8.0, 14.0, 25.0, rgb(0x8FD694))
        drawRect(dark, Offset((cx - 6).toFloat(), (base - 53).toFloat()), Size(5f, 2f))
        drawRect(dark, Offset((cx + 2).toFloat(), (base - 53).toFloat()), Size(5f, 2f))
        return
    }

    val w = intArrayOf(0, 26, 36, 46)[stage].toDouble()
    val h = intArrayOf(0, 40, 56, 70)[stage].toDouble()
    val tb = base - 40
    val tt = tb - h
    ellipse(cx, tb - 2, w * 1.5, 14.0, rgb(0x8A5E3C))
    rrect(cx - w / 2, tt, w, h, w / 2.4, rgb(0x9A6A44))
    rrect(cx + w / 2 - 10, tt + 10, 4.0, h * 0.6, 2.0, rgb(0xB07F58))
    if (stage >= 2) {
        bar(cx - w / 2 - 6, tt + 8, 7.0, 30.0, 40.0, rgb(0x8A5E3C))
        bar(cx + w / 2 + 6, tt + 8, 7.0, 30.0, -40.0, rgb(0x8A5E3C))
    }

    // Foliage (greens fade to dry yellow as `wilt` rises)
    val healthy = intArrayOf(0x6FBF7C, 0x7BC987, 0x8FD694, 0x5FA06B)
    val dry = intArrayOf(0xB8B86A, 0xC4B66B, 0xA89A55, 0xB0A860)
    val puffs = when (stage) {
        1 -> listOf(Triple(cx - 10, tt - 2, 14.0), Triple(cx + 10, tt - 6, 14.0))
        2 -> listOf(Triple(cx - 24, tt + 6, 20.0), Triple(cx + 24, tt + 2, 20.0), Triple(cx, tt - 12, 25.0))
        else -> listOf(
            Triple(cx - 44, tt + 20, 26.0), Triple(cx + 46, tt + 14, 26.0), Triple(cx - 20, tt - 8, 31.0),
            Triple(cx + 22, tt - 14, 31.0), Triple(cx, tt - 36, 30.0),
        )
    }
    puffs.forEachIndexed { i, (x, y, r) ->
        val dx = 1.2 * sin(t * 1.3 + i)
        val dy = 8 * wilt + 0.8 * sin(t * 1.1 + i * 2)
        ellipse(x + dx, y + dy, r * 2, r * 1.6, mixHex(healthy[i % 4], dry[i % 4], wilt))
        ellipse(x + dx - r * 0.3, y + dy - r * 0.3, r * 0.9, r * 0.6, Color.White.copy(alpha = 0.18f))
    }
    if (stage == 3) {
        val a = maxOf(0.0, 1 - wilt * 3)
        listOf(cx - 30 to tt - 12, cx + 34 to tt - 24, cx + 54 to tt + 12, cx - 48 to tt + 14).forEachIndexed { i, p ->
            ellipse(p.first, p.second, 10.0, 10.0, rgb(0xCBB8FF, a))
            ellipse(p.first, p.second, 4.0, 4.0, rgb(0xFFF3B0, a * (0.7 + 0.3 * sin(t * 2 + i))))
        }
    }

    // Face
    val fs = w / 46
    val fy = tt + h * 0.5
    for (dx in listOf(-10.0, 10.0)) {
        val ex = cx + dx * fs
        if (mood == BonsiMood.SLEEPY) {
            curve(ex - 6 * fs, fy, ex, fy + 5 * fs, ex + 6 * fs, fy, dark)
        } else {
            val open = if (blink) 0.12 else 1.0
            ellipse(ex, fy - 2, 14 * fs, 16 * fs * open, Color.White)
            ellipse(ex + (if (mood == BonsiMood.WORRIED) 0.0 else dx * 0.08 * fs), fy - 1, 7 * fs, 9 * fs * open, dark)
        }
    }
    val worried = mood == BonsiMood.WORRIED || wilt > 0.5
    val brow = rgb(0x5A3B22)
    bar(cx - 10 * fs, fy - (if (worried) 14 else 16) * fs, 13 * fs, 3.0, if (worried) 15.0 else 0.0, brow)
    bar(cx + 10 * fs, fy - (if (worried) 14 else 16) * fs, 13 * fs, 3.0, if (worried) -15.0 else 0.0, brow)
    val my = fy + 12 * fs
    when {
        mood == BonsiMood.SLEEPY -> ellipse(cx, my + 2, 6 * fs, 6 * fs, dark)
        worried -> curve(cx - 8 * fs, my + 5, cx, my - 4 * fs, cx + 8 * fs, my + 5, dark)
        else -> curve(cx - 9 * fs, my, cx, my + 11 * fs, cx + 9 * fs, my, dark)
    }
    ellipse(cx - 20 * fs, fy + 9 * fs, 10 * fs, 6.0, rgb(0xF4A7A0, 0.6))
    ellipse(cx + 20 * fs, fy + 9 * fs, 10 * fs, 6.0, rgb(0xF4A7A0, 0.6))
    if (wilt > 0.5) ellipse(cx + w / 2 + 10, tt + 14 + 3 * sin(t * 3), 8.0, 12.0, rgb(0x7FD6F5, minOf(1.0, (wilt - 0.5) * 4)))
    if (mood == BonsiMood.SLEEPY) {
        listOf(16.0, 11.0).forEachIndexed { i, size ->
            val drift = (t * 6 + i * 10) % 20
            val layout = measurer.measure("z", TextStyle(fontSize = size.toFloat().toSp(), fontWeight = FontWeight.Bold, color = rgb(0xCBB8FF, 1 - drift / 20)))
            val x = cx + w / 2 + 16 + i * 16
            val y = tt - 14 - i * 14 - drift
            drawText(layout, topLeft = Offset((x - layout.size.width / 2).toFloat(), (y - layout.size.height / 2).toFloat()))
        }
    }
}
