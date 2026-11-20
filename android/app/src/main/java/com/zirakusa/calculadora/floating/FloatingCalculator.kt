package com.zirakusa.calculadora.floating

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.OpenInFull
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zirakusa.calculadora.R
import com.zirakusa.calculadora.ui.CalcColors
import com.zirakusa.calculadora.ui.CalculatorState
import com.zirakusa.calculadora.ui.Display
import com.zirakusa.calculadora.ui.Key
import com.zirakusa.calculadora.ui.Keypad
import com.zirakusa.calculadora.ui.describe

/** The small window over other apps: drag it by its top bar; open the full app or close it. Same history as the app. */
@Composable fun FloatingCalculator(state: CalculatorState, onDrag: (Float, Float) -> Unit, onOpen: () -> Unit, onClose: () -> Unit) {
    val spoken = Key.entries.associateWith { describe(it) }
    Column(Modifier.fillMaxSize().clip(RoundedCornerShape(24.dp)).background(CalcColors.Background)
        .border(1.dp, Color.White.copy(alpha = .12f), RoundedCornerShape(24.dp)).padding(10.dp)) {
        Row(Modifier.fillMaxWidth().pointerInput(Unit) { detectDragGestures { change, drag -> change.consume(); onDrag(drag.x, drag.y) } },
            verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f).height(36.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.size(40.dp, 5.dp).clip(RoundedCornerShape(3.dp)).background(CalcColors.Muted))
            }
            IconButton(onOpen, Modifier.size(36.dp)) { Icon(Icons.Outlined.OpenInFull, stringResource(R.string.open_app), tint = CalcColors.Text) }
            IconButton(onClose, Modifier.size(36.dp)) { Icon(Icons.Outlined.Close, stringResource(R.string.close), tint = CalcColors.Text) }
        }
        Display(state, Modifier.fillMaxWidth().weight(.32f), big = 30)
        Keypad(Key.BASIC, state::press, spoken::getValue, Modifier.fillMaxWidth().weight(1f), gap = 6, small = true, round = false)
    }
}
