package com.zirakusa.calculadora.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Five rows of four keys; «=» in yellow, operators tinted. */
@Composable fun Keypad(onKey: (Key) -> Unit, describe: (Key) -> String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Key.entries.chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { key ->
                    val (background, text) = when {
                        key == Key.EQUALS -> CalcColors.Yellow to Color.Black
                        key.isOperator || key == Key.PERCENT || key == Key.PARENS -> CalcColors.Operator to CalcColors.Yellow
                        key == Key.CLEAR -> CalcColors.Operator to CalcColors.Error
                        else -> CalcColors.Key to CalcColors.Text
                    }
                    Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(22.dp)).background(background)
                        .clickable { onKey(key) }.semantics { contentDescription = describe(key) }, contentAlignment = Alignment.Center) {
                        Text(key.label, color = text, fontSize = if (key.label.length > 2) 22.sp else 30.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}
