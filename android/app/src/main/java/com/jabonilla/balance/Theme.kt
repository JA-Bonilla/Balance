package com.jabonilla.balance

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface as M3Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** "Sun & Moon" tokens (dark only). Growth is the sun, fun is the moon, life is the plant. */
val GrowthColor = Color(0xFFF2B84B)
val FunColor = Color(0xFFB79CFF)
val LifeColor = Color(0xFF8FD694)
val AccentInk = Color(0xFFA7A8FF)
val AppBackground = Color(0xFF1D1C22)
val Surface = Color(0xFF27262E)

fun sideColor(side: Side) = if (side == Side.GROWTH) GrowthColor else FunColor

@Composable
fun BalanceTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(primary = GrowthColor, background = AppBackground, surface = Surface, onPrimary = Color(0xFF1D1C22)),
        // Surface supplies LocalContentColor (default is black) so bare Text is readable on the dark UI.
        content = { M3Surface(color = AppBackground, content = content) },
    )
}
