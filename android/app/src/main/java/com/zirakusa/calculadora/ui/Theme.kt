package com.zirakusa.calculadora.ui

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Black with round grey keys and an orange «=», like the phone's own calculator. */
object CalcColors {
    val Background = Color(0xFF000000)
    val Key = Color(0xFF2C2C2C)
    val Operator = Color(0xFF5A5A5A)
    val Accent = Color(0xFFF7931E)
    val Chip = Color(0xFF1E1E1E)
    val Text = Color(0xFFF2F2F2)
    val Muted = Color(0xFF9A9A9A)
    val Error = Color(0xFFFF6B6B)
}

@Composable fun CalcTheme(content: @Composable () -> Unit) = MaterialTheme(darkColorScheme(primary = CalcColors.Accent,
    background = CalcColors.Background, surface = CalcColors.Chip, onBackground = CalcColors.Text, onSurface = CalcColors.Text)) {
    // Text is light everywhere, also outside a Surface (the converters, the floating window).
    CompositionLocalProvider(LocalContentColor provides CalcColors.Text, content = content)
}
