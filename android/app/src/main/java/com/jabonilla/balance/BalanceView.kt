package com.jabonilla.balance

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** True when the system animator duration scale is 0 ("Remove animations"). */
val LocalReduceMotion = compositionLocalOf { false }

fun balanceCaption(growth: Int, fun_: Int): String {
    if (growth + fun_ == 0) return "Log some minutes to wake your Bonsi"
    val b = AppData.balance(growth, fun_)
    return when {
        b < -0.8 -> "Deep night · the moon covers the sun"
        b < -0.45 -> "Dusk · the moon is taking over"
        b < -0.15 -> "Moon is rising"
        b <= 0.15 -> "Golden hour · in balance"
        b <= 0.45 -> "Sun is rising"
        b <= 0.8 -> "Clear skies · lots of sun"
        else -> "Heat wave · time to rest and play"
    }
}

/** Caption block under the sky: totals and a one-line read on the balance. */
@Composable
fun BalanceCaption(growth: Int, fun_: Int, modifier: Modifier = Modifier) {
    val caption = balanceCaption(growth, fun_)
    val shadow = Shadow(Color.Black.copy(alpha = 0.55f), Offset(0f, 1f), 3f) // keeps text legible over bright sand/sky
    val small = TextStyle(color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, shadow = shadow)
    Column(
        modifier.clearAndSetSemantics {
            contentDescription = caption
            stateDescription = "$growth growth minutes, $fun_ fun minutes this week"
        },
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.ic_sunny), null, Modifier.size(14.dp), tint = GrowthColor)
            Spacer(Modifier.width(4.dp))
            Text("Growth ${growth.minutesShort}", style = small)
            Spacer(Modifier.weight(1f))
            Icon(painterResource(R.drawable.ic_dark_mode), null, Modifier.size(14.dp), tint = FunColor)
            Spacer(Modifier.width(4.dp))
            Text("Fun ${fun_.minutesShort}", style = small)
        }
        Text(
            caption, Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
            style = TextStyle(color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, shadow = shadow),
        )
    }
}

/** The hero graphic. Balanced (1 growth : 5 fun) is golden hour; more fun slides into night, more growth scorches into desert. */
@Composable
fun SkyBackdrop(growth: Int, fun_: Int, stage: Int, modifier: Modifier = Modifier) {
    val reduceMotion = LocalReduceMotion.current
    val target = if (growth + fun_ == 0) 0f else AppData.balance(growth, fun_).toFloat()
    val balance by animateFloatAsState(target, if (reduceMotion) snap() else tween(1400, easing = FastOutSlowInEasing), label = "balance")
    Box(modifier.fillMaxWidth()) {
        LivingSky(balance, stage, reduceMotion)
        // Bright skies (lots of growth) get a charcoal scrim behind the status bar and buttons for contrast.
        val scrim = 0.1f + 0.27f * balance.coerceIn(0f, 1f)
        Box(Modifier.fillMaxWidth().height(140.dp).background(Brush.verticalGradient(listOf(AppBackground.copy(alpha = scrim), Color.Transparent))))
    }
}
