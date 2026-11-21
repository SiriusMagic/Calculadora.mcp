package com.zirakusa.calculadora.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.calculadora.R

/** As on the phone: past calculations on the left (tap = reuse), the operators on the right, back and clear below. */
@Composable fun HistoryPanel(state: CalculatorState, describe: (Key) -> String, modifier: Modifier, onClose: () -> Unit) {
    Row(modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f).fillMaxHeight()) {
            if (state.history.isEmpty()) Text(stringResource(R.string.history_empty), Modifier.padding(8.dp).weight(1f), color = CalcColors.Muted)
            else LazyColumn(Modifier.weight(1f)) {
                items(state.history) { entry ->
                    Column(Modifier.fillMaxWidth().clickable { state.reuse(entry); onClose() }.padding(vertical = 12.dp, horizontal = 4.dp)) {
                        Text(entry.expression, color = CalcColors.Muted, fontSize = 17.sp)
                        Text(CalculatorState.shown(entry.result), color = CalcColors.Text, fontSize = 26.sp)
                        if (entry.auto) Text(stringResource(R.string.history_auto), color = CalcColors.Muted, fontSize = 12.sp)
                    }
                    HorizontalDivider(color = CalcColors.Chip)
                }
            }
            Row(Modifier.padding(top = 12.dp)) {
                Round(Icons.AutoMirrored.Outlined.ArrowBack, R.string.back, onClick = onClose)
                Spacer(Modifier.weight(1f))
                if (state.history.isNotEmpty()) Round(Icons.Outlined.Delete, R.string.history_clear, onClick = state::clearHistory)
            }
        }
        Keypad(listOf(Key.BACK, Key.DIVIDE, Key.TIMES, Key.MINUS, Key.PLUS, Key.EQUALS).map { listOf(it) }, state::press, describe,
            Modifier.padding(start = 16.dp).width(84.dp), gap = 10)
    }
}
