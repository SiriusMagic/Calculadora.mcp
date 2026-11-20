package com.zirakusa.calculadora.engine

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/** Why an expression has no result; [code] is stable (the MCP phase gives it to the AI as is). */
class CalcError(val code: String, message: String) : Exception(message)

/**
 * Exact decimal arithmetic for what the keypad can type: numbers, + − × ÷, %, parentheses and unary minus.
 * A small recursive-descent parser: nothing is ever executed as code, and the input size and depth are capped.
 *
 * Grammar (× and ÷ bind tighter than + and −):
 *   expr   := term (('+' | '-') term)*
 *   term   := unary (('*' | '/') unary)*
 *   unary  := '-' unary | postfix
 *   postfix:= primary '%'*
 *   primary:= number | '(' expr ')'
 * «a + b%» and «a − b%» mean «a ± (a·b/100)», as on pocket calculators; elsewhere «b%» is b/100.
 */
object Calculator {
    const val MAX_LENGTH = 500
    private const val MAX_DEPTH = 50
    private val CONTEXT = MathContext(34, RoundingMode.HALF_EVEN)

    fun evaluate(expression: String): BigDecimal {
        val text = normalize(expression)
        if (text.isEmpty()) throw CalcError("EMPTY", "Escribe una operación.")
        if (text.length > MAX_LENGTH) throw CalcError("TOO_LONG", "La operación es demasiado larga.")
        val parser = Parser(text)
        val value = parser.expr(0)
        if (!parser.done) throw CalcError("SYNTAX", "No se entiende «${text.substring(parser.pos)}».")
        return value.stripTrailingZeros().let { if (it.scale() < 0) it.setScale(0) else it }
    }

    /** What the screen shows: plain digits (no 1E+3), at most 12 decimals. */
    fun format(value: BigDecimal): String {
        val rounded = value.setScale(12, RoundingMode.HALF_EVEN).stripTrailingZeros()
        return (if (rounded.scale() < 0) rounded.setScale(0) else rounded).toPlainString().let { if (it == "-0") "0" else it }
    }

    /** Keypad symbols to plain ASCII; spaces and thousands separators are ignored. */
    fun normalize(expression: String): String = expression.replace('×', '*').replace('÷', '/').replace('−', '-')
        .replace(',', '.').filterNot { it.isWhitespace() }

    private class Parser(val text: String) {
        var pos = 0
        val done get() = pos == text.length

        fun expr(depth: Int): BigDecimal {
            if (depth > MAX_DEPTH) throw CalcError("TOO_DEEP", "Demasiados paréntesis.")
            var value = term(depth).value
            while (!done && (peek() == '+' || peek() == '-')) {
                val plus = text[pos++] == '+'
                val right = term(depth)
                // «a + b%»: the percent is of a (right.value is already b/100).
                val operand = if (right.percent) value.multiply(right.value, CONTEXT) else right.value
                value = if (plus) value.add(operand, CONTEXT) else value.subtract(operand, CONTEXT)
            }
            return value
        }

        /** A term's value, and whether it was just «number%» (for the pocket-calculator percent rule). */
        class Operand(val value: BigDecimal, val percent: Boolean)

        fun term(depth: Int): Operand {
            var operand = unary(depth)
            while (!done && (peek() == '*' || peek() == '/')) {
                val times = text[pos++] == '*'
                val right = unary(depth).value
                operand = Operand(if (times) operand.value.multiply(right, CONTEXT) else {
                    if (right.signum() == 0) throw CalcError("DIVISION_BY_ZERO", "No se puede dividir entre cero.")
                    operand.value.divide(right, CONTEXT)
                }, false)
            }
            return operand
        }

        fun unary(depth: Int): Operand {
            if (depth > MAX_DEPTH) throw CalcError("TOO_DEEP", "Demasiados signos seguidos.")
            if (!done && peek() == '-') { pos++; return Operand(unary(depth + 1).value.negate(), false) }
            if (!done && peek() == '+') { pos++; return Operand(unary(depth + 1).value, false) }
            var value = primary(depth)
            var percents = 0
            while (!done && peek() == '%') { pos++; percents++; value = value.divide(BigDecimal(100), CONTEXT) }
            return Operand(value, percents == 1)
        }

        private fun primary(depth: Int): BigDecimal {
            if (done) throw CalcError("INCOMPLETE", "Falta un número al final.")
            if (peek() == '(') {
                pos++
                val value = expr(depth + 1)
                if (done || peek() != ')') throw CalcError("PARENTHESES", "Falta cerrar un paréntesis.")
                pos++
                return value
            }
            val start = pos
            while (!done && (peek().isDigit() || peek() == '.')) pos++
            val number = text.substring(start, pos)
            if (number.isEmpty()) throw CalcError("SYNTAX", "No se entiende «${text.substring(start)}».")
            if (number.count { it == '.' } > 1 || number == ".") throw CalcError("SYNTAX", "Número mal escrito: $number")
            return BigDecimal(number)
        }

        private fun peek() = text[pos]
    }
}
