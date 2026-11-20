package com.zirakusa.calculadora.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.Functions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Rows of keys; [rows] decides the layout. [round]: true circles sized by the width (basic keypad, as on the phone);
 * false fills the given height (scientific mode and the floating window, where 8 rows must fit).
 */
@Composable fun Keypad(rows: List<List<Key>>, onKey: (Key) -> Unit, describe: (Key) -> String, modifier: Modifier = Modifier,
    gap: Int = 12, small: Boolean = false, round: Boolean = true) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(gap.dp)) {
        rows.forEach { row ->
            Row((if (round) Modifier.fillMaxWidth() else Modifier.fillMaxWidth().weight(1f)), horizontalArrangement = Arrangement.spacedBy(gap.dp)) {
                row.forEach { key ->
                    val shape = if (round) Modifier.weight(1f).aspectRatio(1f) else Modifier.weight(1f).fillMaxHeight()
                    RoundKey(key, shape, describe(key), if (small) 20.sp else 32.sp) { onKey(key) }
                }
            }
        }
    }
}

@Composable fun RoundKey(key: Key, modifier: Modifier, spoken: String, size: TextUnit, onClick: () -> Unit) {
    val background = when {
        key == Key.EQUALS -> CalcColors.Accent
        key.isOperator || key in setOf(Key.CLEAR, Key.PERCENT, Key.BACK) -> CalcColors.Operator
        else -> CalcColors.Key
    }
    Box(modifier.clip(CircleShape).background(background).clickable(onClick = onClick).semantics { contentDescription = spoken },
        contentAlignment = Alignment.Center) {
        when (key) {
            Key.BACK -> Icon(Icons.AutoMirrored.Outlined.Backspace, null, tint = Color.White)
            Key.SCIENTIFIC -> Icon(Icons.Outlined.Functions, null, tint = Color.White)
            else -> Text(key.label, color = Color.White, fontSize = if (key.label.length > 2) size * 0.65f else size)
        }
    }
}
