package com.zirakusa.calculadora.ui.convert

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Rows of amounts in different units (or currencies): the user types in the [active] row and every other row is that
 * amount converted. [convert] returns null when a pair cannot be converted (unknown currency).
 */
class ConverterState(codes: List<String>, private val decimals: Int,
    var convert: (BigDecimal, String, String) -> BigDecimal?) {
    var codes by mutableStateOf(codes); private set
    var active by mutableStateOf(0); private set
    var input by mutableStateOf("1"); private set

    fun type(text: String) {
        when {
            text == "," -> if (',' !in input) input += ","
            input == "0" -> input = text.trimStart('0').ifEmpty { "0" }
            input.replace(",", "").length < MAX_DIGITS -> input += text
        }
    }
    fun clear() { input = "0" }
    fun back() { input = input.dropLast(1).ifEmpty { "0" } }
    /** Tapping a row makes it the one being typed, starting from what it showed. */
    fun activate(row: Int) { if (row != active) { input = shown(row).takeIf { it != "—" } ?: "0"; active = row } }
    fun pick(row: Int, code: String) { codes = codes.toMutableList().also { it[row] = code } }

    /** What row [row] shows: the typed text, or the conversion with at most [decimals] decimals. */
    fun shown(row: Int): String {
        if (row == active) return input
        val amount = BigDecimal(input.replace(',', '.').ifEmpty { "0" }.let { if (it.endsWith('.')) it + "0" else it })
        val value = convert(amount, codes[active], codes[row]) ?: return "—"
        return value.setScale(decimals, RoundingMode.HALF_EVEN).stripTrailingZeros().let { if (it.scale() < 0) it.setScale(0) else it }
            .toPlainString().replace('.', ',')
    }

    private companion object { const val MAX_DIGITS = 15 }
}
