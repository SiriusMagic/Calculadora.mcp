package com.zirakusa.calculadora.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CurrencyExchange
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.PictureInPictureAlt
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.calculadora.R

/** Floating window · (history, currency, units) on top; the display; the keypad (with the scientific rows when on). */
@Composable fun CalculatorScreen(state: CalculatorState, onFloat: () -> Unit, onCurrency: () -> Unit, onUnits: () -> Unit) {
    var showHistory by remember { mutableStateOf(false) }
    val spoken = Key.entries.associateWith { describe(it) }
    Column(Modifier.fillMaxSize().background(CalcColors.Background).systemBarsPadding().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Round(Icons.Outlined.PictureInPictureAlt, R.string.floating, onClick = onFloat)
            Spacer(Modifier.weight(1f))
            PillButtons(Triple(Icons.Outlined.History, R.string.history, showHistory to { showHistory = !showHistory; state.reloadHistory() }),
                Triple(Icons.Outlined.CurrencyExchange, R.string.currency, false to onCurrency),
                Triple(Icons.Outlined.Straighten, R.string.units, false to onUnits))
        }
        Display(state, Modifier.fillMaxWidth().weight(if (showHistory) .5f else 1f))
        if (showHistory) HistoryPanel(state, spoken::getValue, Modifier.weight(1.4f)) { showHistory = false }
        else {
            if (state.scientific) Keypad(Key.EXTRA + Key.BASIC, state::press, spoken::getValue, Modifier.fillMaxWidth().fillMaxHeight(.8f),
                gap = 8, small = true, round = false)
            else Keypad(Key.BASIC, state::press, spoken::getValue, Modifier.fillMaxWidth())
        }
    }
}

/** What is typed (big, right-aligned) and the live result or the error. */
@Composable fun Display(state: CalculatorState, modifier: Modifier, big: Int = 52) {
    Column(modifier.padding(horizontal = 8.dp), verticalArrangement = Arrangement.Bottom, horizontalAlignment = Alignment.End) {
        Text(state.expression, Modifier.fillMaxWidth(), textAlign = TextAlign.End, fontSize = big.sp, lineHeight = (big + 6).sp,
            maxLines = 3, overflow = TextOverflow.StartEllipsis, color = CalcColors.Text)
        val error = state.error?.let { errorText(it) }
        Text(error ?: state.preview?.let { "= $it" } ?: "", Modifier.padding(top = 4.dp).heightIn(min = 30.dp), fontSize = (big / 2).sp,
            color = if (error != null) CalcColors.Error else CalcColors.Muted, maxLines = 1)
    }
}

@Composable fun errorText(code: String) = stringResource(when (code) {
    "DIVISION_BY_ZERO" -> R.string.error_zero
    "PARENTHESES" -> R.string.error_parentheses
    "INCOMPLETE" -> R.string.error_incomplete
    "DOMAIN" -> R.string.error_domain
    "TOO_BIG" -> R.string.error_too_big
    else -> R.string.error_syntax
})

@Composable fun describe(key: Key) = when (key) {
    Key.CLEAR -> stringResource(R.string.key_clear)
    Key.BACK -> stringResource(R.string.key_back)
    Key.PARENS -> stringResource(R.string.key_parens)
    Key.EQUALS -> stringResource(R.string.key_equals)
    Key.SCIENTIFIC -> stringResource(R.string.key_scientific)
    else -> key.label
}
