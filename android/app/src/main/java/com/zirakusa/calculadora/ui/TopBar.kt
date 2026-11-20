package com.zirakusa.calculadora.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

/** A round button, and a pill holding several, as on the phone's calculator. */
@Composable fun Round(icon: ImageVector, label: Int, tint: androidx.compose.ui.graphics.Color = CalcColors.Text, onClick: () -> Unit) {
    IconButton(onClick, Modifier.size(56.dp).clip(CircleShape).background(CalcColors.Chip)) { Icon(icon, stringResource(label), tint = tint) }
}

@Composable fun PillButtons(vararg buttons: Triple<ImageVector, Int, Pair<Boolean, () -> Unit>>) {
    Row(Modifier.clip(CircleShape).background(CalcColors.Chip).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        buttons.forEach { (icon, label, state) ->
            val (active, onClick) = state
            IconButton(onClick, Modifier.size(56.dp)) { Icon(icon, stringResource(label), tint = if (active) CalcColors.Accent else CalcColors.Text) }
        }
    }
}
