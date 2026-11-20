package com.zirakusa.calculadora.ui.convert

import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import com.zirakusa.calculadora.R
import com.zirakusa.calculadora.data.RatesSource
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Currency
import java.util.Date
import java.util.Locale

/** Conversor de divisas: three currencies, rates kept offline, «Actualizar tasa de cambio» and «moneda local». */
@Composable fun CurrencyScreen(source: RatesSource, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var rates by remember { mutableStateOf(source.cached()) }
    var failed by remember { mutableStateOf(false) }
    val local = remember { runCatching { Currency.getInstance(Locale.getDefault()).currencyCode }.getOrNull() ?: "USD" }
    val state = remember {
        ConverterState(source.chosen.takeIf { it.size == 3 } ?: listOf(local, "USD", "EUR").distinct().let { if (it.size < 3) it + "MXN" else it }, 4) { a, f, t ->
            rates?.convert(a, f, t)
        }
    }
    LaunchedEffect(rates) { state.convert = { a, f, t -> rates?.convert(a, f, t) } }
    LaunchedEffect(state.codes) { source.chosen = state.codes }
    fun refresh() = scope.launch { source.refresh().onSuccess { rates = it; failed = false }.onFailure { failed = true } }
    LaunchedEffect(Unit) { if (rates == null || System.currentTimeMillis() / 1000 - rates!!.updatedAt > 12 * 3600) refresh() }
    val codes = rates?.let { listOf(it.base) + it.rates.keys }?.distinct()?.sorted() ?: listOf("USD", "EUR")
    val footer = when {
        rates == null && failed -> stringResource(R.string.rates_none)
        rates == null -> stringResource(R.string.rates_loading)
        else -> stringResource(R.string.rates_updated, DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
            .format(Date(rates!!.updatedAt * 1000))) + if (failed) " · " + stringResource(R.string.rates_offline) else ""
    }
    ConverterScreen(stringResource(R.string.currency), state, codes.map { Choice(it, currencyName(it), flag(it)) }, footer,
        listOf(stringResource(R.string.rates_refresh) to { refresh(); Unit }, stringResource(R.string.rates_local) to { state.pick(0, local) }),
        onBack)
}

private fun currencyName(code: String) = runCatching { Currency.getInstance(code).getDisplayName(Locale.getDefault()) }.getOrDefault(code)

/** 🇺🇸 from «USD» (the first two letters are the country); the euro has its own flag. */
fun flag(code: String): String {
    if (code == "EUR") return "🇪🇺"
    if (code.length != 3 || !code.all { it in 'A'..'Z' } || code.startsWith("X")) return "💱"
    return code.take(2).map { String(Character.toChars(0x1F1E6 + (it - 'A'))) }.joinToString("")
}

