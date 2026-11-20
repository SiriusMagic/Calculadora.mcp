package com.zirakusa.calculadora.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.zirakusa.calculadora.data.Entry
import com.zirakusa.calculadora.data.HistoryStore
import com.zirakusa.calculadora.engine.CalcError
import com.zirakusa.calculadora.engine.Calculator

/** Every key of the keypad. */
enum class Key(val label: String) {
    CLEAR("AC"), PARENS("( )"), PERCENT("%"), DIVIDE("÷"),
    SEVEN("7"), EIGHT("8"), NINE("9"), TIMES("×"),
    FOUR("4"), FIVE("5"), SIX("6"), MINUS("−"),
    ONE("1"), TWO("2"), THREE("3"), PLUS("+"),
    BACK("⌫"), ZERO("0"), DOT(","), EQUALS("=");

    val isOperator get() = this in setOf(DIVIDE, TIMES, MINUS, PLUS)
}

/** The one owner of the screen: what is typed, the live result, the error and the history. */
class CalculatorState(private val store: HistoryStore, private val now: () -> Long = System::currentTimeMillis) {
    var expression by mutableStateOf(""); private set
    var history by mutableStateOf(store.read()); private set
    /** Code of the last «=» that failed (shown until the next key). */
    var error by mutableStateOf<String?>(null); private set
    /** True right after «=»: typing a digit starts over, typing an operator continues from the result. */
    private var justEvaluated = false

    /** Result while typing (null when the expression is not complete yet). */
    val preview: String? get() = runCatching { Calculator.format(Calculator.evaluate(expression)).replace('.', ',') }.getOrNull()
        ?.takeIf { expression.any { c -> !c.isDigit() && c != ',' } }

    fun press(key: Key) {
        error = null
        when (key) {
            Key.CLEAR -> expression = ""
            Key.BACK -> expression = expression.dropLast(1)
            Key.EQUALS -> return equals()
            Key.PARENS -> append(if (shouldClose()) ")" else if (endsWithValue()) "×(" else "(")
            Key.DOT -> if (!currentNumber().contains(',')) append(when {
                expression.isNotEmpty() && expression.last().isDigit() -> ","
                expression.isNotEmpty() && expression.last() in ")%" -> "×0,"
                else -> "0,"
            })
            Key.PERCENT -> if (endsWithValue()) append("%")
            else -> if (key.isOperator) operator(key.label) else digit(key.label)
        }
        justEvaluated = false
    }

    /** Reuses a past calculation (tap in the history). */
    fun reuse(entry: Entry) { expression = entry.expression; error = null; justEvaluated = false }
    fun clearHistory() { history = emptyList(); store.write(history) }

    private fun equals() {
        if (expression.isEmpty()) return
        try {
            val result = Calculator.format(Calculator.evaluate(expression))
            history = (listOf(Entry(expression, result, now())) + history).take(HistoryStore.MAX)
            store.write(history)
            expression = result.replace('.', ',')
            justEvaluated = true
        } catch (failure: CalcError) { error = failure.code }
    }

    private fun digit(d: String) {
        if (justEvaluated) expression = ""
        if (expression.endsWith(")") || expression.endsWith("%")) expression += "×"
        val number = currentNumber()
        expression = if (number == "0") expression.dropLast(1) + d else expression + d
    }

    private fun operator(op: String) {
        when {
            expression.isEmpty() -> if (op == "−") expression = op
            expression.last().toString() in OPERATORS -> if (expression.length > 1) expression = expression.dropLast(1) + op
            expression.last() == '(' -> if (op == "−") expression += op
            else -> expression += op
        }
    }

    private fun append(text: String) { if (justEvaluated && text.first().isDigit()) expression = ""; expression += text }
    private fun endsWithValue() = expression.isNotEmpty() && (expression.last().isDigit() || expression.last() in ")%,")
    private fun shouldClose() = expression.count { it == '(' } > expression.count { it == ')' } && endsWithValue()
    private fun currentNumber() = expression.takeLastWhile { it.isDigit() || it == ',' }

    private companion object { val OPERATORS = setOf("+", "−", "×", "÷") }
}
