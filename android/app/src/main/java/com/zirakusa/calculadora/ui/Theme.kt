package com.zirakusa.calculadora.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Dark with Stone's yellow, so the apps of the same developer look like a family. */
object CalcColors {
    val Background = Color(0xFF111015)
    val Key = Color(0xFF23212B)
    val Operator = Color(0xFF3A3420)
    val Yellow = Color(0xFFF5C518)
    val Text = Color(0xFFF3F0FA)
    val Muted = Color(0xFFA49EB3)
    val Error = Color(0xFFFF7A7A)
}

@Composable fun CalcTheme(content: @Composable () -> Unit) = MaterialTheme(darkColorScheme(primary = CalcColors.Yellow,
    background = CalcColors.Background, surface = CalcColors.Background, onBackground = CalcColors.Text, onSurface = CalcColors.Text),
    content = content)
