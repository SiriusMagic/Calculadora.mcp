package com.zirakusa.calculadora.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.zirakusa.calculadora.data.Entry
import com.zirakusa.calculadora.data.HistoryStore
import com.zirakusa.calculadora.engine.CalcError
import com.zirakusa.calculadora.engine.Calculator

/** Every key. [text] is what it adds to the expression (functions open their parenthesis). */
enum class Key(val label: String, val text: String = label) {
    CLEAR("AC"), PERCENT("%"), BACK("⌫"), DIVIDE("÷"),
    SEVEN("7"), EIGHT("8"), NINE("9"), TIMES("×"),
    FOUR("4"), FIVE("5"), SIX("6"), MINUS("−"),
    ONE("1"), TWO("2"), THREE("3"), PLUS("+"),
    SCIENTIFIC("√π"), ZERO("0"), DOT(","), EQUALS("="),
    PARENS("( )"), FACTORIAL("x!", "!"), SQRT("√", "√("), POWER("xʸ", "^"),
    SIN("sin", "sin("), COS("cos", "cos("), TAN("tan", "tan("), SQUARE("x²", "^2"),
    LN("ln", "ln("), LOG("log", "log("), PI("π"), E("e");

    val isOperator get() = this in OPERATORS
    val isFunction get() = this in setOf(SIN, COS, TAN, LN, LOG, SQRT)

    companion object {
        val OPERATORS = setOf(DIVIDE, TIMES, MINUS, PLUS)
        val BASIC = entries.subList(0, 20).chunked(4)
        val EXTRA = entries.subList(20, entries.size).chunked(4)
    }
}

/** The one owner of a calculator: what is typed, the live result, the error and the shared history. */
class CalculatorState(private val store: HistoryStore, private val now: () -> Long = System::currentTimeMillis) {
    var expression by mutableStateOf(""); private set
    var history by mutableStateOf(store.read()); private set
    /** Code of the last «=» that failed (shown until the next key). */
    var error by mutableStateOf<String?>(null); private set
    var scientific by mutableStateOf(false); private set
    /** True right after «=»: typing a digit starts over, typing an operator continues from the result. */
    private var justEvaluated = false

    /** Result while typing (null when the expression is not complete yet). */
    val preview: String? get() = runCatching { shown(Calculator.format(Calculator.evaluate(expression))) }.getOrNull()
        ?.takeIf { expression.any { c -> !c.isDigit() && c != ',' } && expression.trimStart('−').any { c -> !c.isDigit() && c != ',' } }

    fun press(key: Key) {
        error = null
        val wasResult = justEvaluated
        justEvaluated = false
        when {
            key == Key.CLEAR -> { if (!wasResult) keepUnsaved(); expression = "" }
            key == Key.BACK -> expression = dropToken(expression)
            key == Key.EQUALS -> equals()
            key == Key.SCIENTIFIC -> scientific = !scientific
            key == Key.PARENS -> append(if (shouldClose()) ")" else if (endsWithValue()) "×(" else "(")
            key == Key.DOT -> if (!currentNumber().contains(',')) append(when {
                wasResult -> { expression = ""; "0," }
                expression.isNotEmpty() && expression.last().isDigit() -> ","
                endsWithValue() -> "×0,"
                else -> "0,"
            })
            key in setOf(Key.PERCENT, Key.FACTORIAL, Key.POWER, Key.SQUARE) -> if (endsWithValue()) append(key.text)
            key.isFunction || key == Key.PI || key == Key.E -> {
                if (wasResult && !key.isFunction) expression = ""
                append(if (endsWithValue()) "×" + key.text else key.text)
            }
            key.isOperator -> operator(key.label)
            else -> digit(key.label, wasResult)
        }
    }

    /**
     * An AI typed [text] through Modo MCP (calc_type): it appears on the screen in the calculator's own symbols, and
     * with [equals] «=» is pressed (saved in the history like a user's). Returns the result, or null.
     */
    fun typeFromAi(text: String, equals: Boolean): String? {
        expression = text.replace('*', '×').replace('/', '÷').replace('-', '−').replace('.', ',').filterNot { it.isWhitespace() }
        error = null; justEvaluated = false
        if (!equals) return null
        equals()
        return if (error == null) expression else null
    }

    /** Reuses a past calculation (tap in the history). */
    fun reuse(entry: Entry) { if (!justEvaluated) keepUnsaved(); expression = entry.expression; error = null; justEvaluated = false }

    /**
     * A calculation with a live result that is about to be lost without «=» (AC, leaving the app, closing the floating
     * window, reusing another one) goes to the history anyway, marked [Entry.auto]. Nothing is saved twice in a row.
     */
    fun keepUnsaved() {
        if (justEvaluated) return
        val result = runCatching { Calculator.format(Calculator.evaluate(expression)) }.getOrNull() ?: return
        if (preview == null || history.firstOrNull()?.expression == expression) return
        history = (listOf(Entry(expression, result, now(), auto = true)) + history).take(HistoryStore.MAX)
        store.write(history)
    }
    fun clearHistory() { history = emptyList(); store.write(history) }
    /** History written elsewhere (the floating window shares it). */
    fun reloadHistory() { history = store.read() }

    private fun equals() {
        if (expression.isEmpty()) return
        try {
            val result = Calculator.format(Calculator.evaluate(expression))
            history = (listOf(Entry(expression, result, now())) + history).take(HistoryStore.MAX)
            store.write(history)
            expression = shown(result)
            justEvaluated = true
        } catch (failure: CalcError) { error = failure.code }
    }

    private fun digit(d: String, wasResult: Boolean) {
        if (wasResult) expression = ""
        if (expression.isNotEmpty() && expression.last() in ")%!πe") expression += "×"
        val number = currentNumber()
        expression = if (number == "0") expression.dropLast(1) + d else expression + d
    }

    private fun operator(op: String) {
        val last = expression.lastOrNull()
        when {
            last == null -> if (op == "−") expression = op
            last.toString() in OPERATOR_TEXT -> if (expression.length > 1) expression = expression.dropLast(1) + op
            last == '(' || last == '^' -> if (op == "−") expression += op
            else -> expression += op
        }
    }

    private fun append(text: String) { expression += text }
    private fun endsWithValue() = expression.isNotEmpty() && (expression.last().isDigit() || expression.last() in ")%,!πe")
    private fun shouldClose() = expression.count { it == '(' } > expression.count { it == ')' } && endsWithValue()
    private fun currentNumber() = expression.takeLastWhile { it.isDigit() || it == ',' }

    companion object {
        private val OPERATOR_TEXT = setOf("+", "−", "×", "÷")
        private val NAMES = listOf("sin(", "cos(", "tan(", "ln(", "log(", "√(")
        /** ⌫ removes a whole function name («sin(») at once. */
        fun dropToken(text: String) = NAMES.firstOrNull { text.endsWith(it) }?.let { text.dropLast(it.length) } ?: text.dropLast(1)
        /** Engine format (1234.5, 1.2E+21) to the screen (1234,5). */
        fun shown(result: String) = result.replace('.', ',')
    }
}
