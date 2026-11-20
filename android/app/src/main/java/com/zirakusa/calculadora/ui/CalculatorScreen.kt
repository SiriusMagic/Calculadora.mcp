package com.zirakusa.calculadora.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.calculadora.R

/** Display (what is typed, live result or the error), history button and the keypad. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun CalculatorScreen(state: CalculatorState) {
    var showHistory by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(CalcColors.Background).systemBarsPadding().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.app_name), Modifier.weight(1f), color = CalcColors.Yellow, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            IconButton({ showHistory = true }) { Icon(Icons.Default.History, stringResource(R.string.history), tint = CalcColors.Text) }
        }
        Column(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.Bottom, horizontalAlignment = Alignment.End) {
            Text(state.expression.ifEmpty { "0" }, Modifier.fillMaxWidth(), textAlign = TextAlign.End, fontSize = 48.sp,
                lineHeight = 54.sp, maxLines = 3, overflow = TextOverflow.Ellipsis, color = CalcColors.Text)
            val error = state.error?.let { errorText(it) }
            Text(error ?: state.preview?.let { "= $it" } ?: "", Modifier.padding(top = 6.dp).heightIn(min = 34.dp), fontSize = 26.sp,
                color = if (error != null) CalcColors.Error else CalcColors.Muted)
        }
        val spoken = Key.entries.associateWith { describe(it) }
        Keypad(state::press, spoken::getValue, Modifier.fillMaxWidth().fillMaxHeight(.58f).padding(top = 12.dp))
    }
    if (showHistory) ModalBottomSheet({ showHistory = false }, containerColor = CalcColors.Key) {
        HistoryList(state, onPick = { state.reuse(it); showHistory = false })
    }
}

@Composable private fun HistoryList(state: CalculatorState, onPick: (com.zirakusa.calculadora.data.Entry) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.history), Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 18.sp)
            if (state.history.isNotEmpty()) TextButton(state::clearHistory) { Text(stringResource(R.string.history_clear), color = CalcColors.Error) }
        }
        if (state.history.isEmpty()) Text(stringResource(R.string.history_empty), Modifier.padding(vertical = 24.dp), color = CalcColors.Muted)
        else LazyColumn(Modifier.heightIn(max = 420.dp)) {
            items(state.history) { entry ->
                Column(Modifier.fillMaxWidth().clickable { onPick(entry) }.padding(vertical = 10.dp), horizontalAlignment = Alignment.End) {
                    Text(entry.expression, color = CalcColors.Muted, fontSize = 16.sp)
                    Text("= ${entry.result.replace('.', ',')}", fontSize = 22.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable private fun errorText(code: String) = stringResource(when (code) {
    "DIVISION_BY_ZERO" -> R.string.error_zero
    "PARENTHESES" -> R.string.error_parentheses
    "INCOMPLETE" -> R.string.error_incomplete
    else -> R.string.error_syntax
})

@Composable private fun describe(key: Key) = when (key) {
    Key.CLEAR -> stringResource(R.string.key_clear)
    Key.BACK -> stringResource(R.string.key_back)
    Key.PARENS -> stringResource(R.string.key_parens)
    Key.EQUALS -> stringResource(R.string.key_equals)
    else -> key.label
}
