package com.zirakusa.calculadora.engine

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/** Why an expression has no result; [code] is stable (the MCP phase gives it to the AI as is). */
class CalcError(val code: String, message: String) : Exception(message)

/**
 * Decimal arithmetic for what the keypad can type: numbers, + − × ÷, %, parentheses, unary minus and, in scientific
 * mode, ^, !, √, sin cos tan (degrees), ln, log, π and e. A small recursive-descent parser: nothing is ever executed
 * as code, and the input size and depth are capped. + − × ÷ and integer powers are exact; functions use doubles,
 * rounded to 15 significant digits (so sin(180) is 0, not 1.2E-16).
 *
 * Grammar (^ binds tighter than × ÷, which bind tighter than + −):
 *   expr   := term (('+' | '-') term)*
 *   term   := unary (('*' | '/') unary)*
 *   unary  := '-' unary | power
 *   power  := postfix ('^' unary)?
 *   postfix:= primary ('%' | '!')*
 *   primary:= number | 'π' | 'e' | '(' expr ')' | ('sin'|'cos'|'tan'|'ln'|'log'|'√') primary
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

    /** What the screen shows: plain digits with at most 12 decimals; very big numbers as 1.234567890123E+20. */
    fun format(value: BigDecimal): String {
        if (value.abs() >= BIG) return value.round(MathContext(13, RoundingMode.HALF_EVEN)).stripTrailingZeros().toString()
        val rounded = value.setScale(12, RoundingMode.HALF_EVEN).stripTrailingZeros()
        return (if (rounded.scale() < 0) rounded.setScale(0) else rounded).toPlainString().let { if (it == "-0") "0" else it }
    }

    private val BIG = BigDecimal("1E21")
    private val FUNCTIONS = listOf("sin", "cos", "tan", "ln", "log", "√")

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
            val base = postfix(depth)
            if (done || peek() != '^') return base
            pos++
            return Operand(power(base.value, unary(depth + 1).value), false)
        }

        private fun postfix(depth: Int): Operand {
            var value = primary(depth)
            var percents = 0
            var other = false
            while (!done && (peek() == '%' || peek() == '!')) {
                if (text[pos++] == '%') { percents++; value = value.divide(BigDecimal(100), CONTEXT) } else { other = true; value = factorial(value) }
            }
            return Operand(value, percents == 1 && !other)
        }

        private fun primary(depth: Int): BigDecimal {
            if (done) throw CalcError("INCOMPLETE", "Falta un número al final.")
            if (peek() == 'π') { pos++; return BigDecimal(Math.PI, CONTEXT) }
            FUNCTIONS.firstOrNull { text.startsWith(it, pos) }?.let { name ->
                pos += name.length
                return function(name, primary(depth + 1))
            }
            if (peek() == 'e') { pos++; return BigDecimal(Math.E, CONTEXT) }
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

    /** Integer exponents up to 9 999 are exact; others (roots, fractions) go through doubles. */
    private fun power(base: BigDecimal, exponent: BigDecimal): BigDecimal {
        val whole = runCatching { exponent.intValueExact() }.getOrNull()
        if (whole != null && kotlin.math.abs(whole) <= 9_999) {
            if (whole < 0 && base.signum() == 0) throw CalcError("DIVISION_BY_ZERO", "No se puede dividir entre cero.")
            val digits = base.abs().precision() - base.abs().scale()
            if (digits.toLong() * kotlin.math.abs(whole) > 10_000) throw CalcError("TOO_BIG", "El resultado es demasiado grande.")
            return if (whole >= 0) base.pow(whole, CONTEXT) else BigDecimal.ONE.divide(base.pow(-whole, CONTEXT), CONTEXT)
        }
        if (base.signum() < 0) throw CalcError("DOMAIN", "Una potencia no entera de un número negativo no es real.")
        return fromDouble(Math.pow(base.toDouble(), exponent.toDouble()))
    }

    private fun factorial(n: BigDecimal): BigDecimal {
        val k = runCatching { n.intValueExact() }.getOrNull()
        if (k == null || k < 0) throw CalcError("DOMAIN", "El factorial es solo de enteros desde 0.")
        if (k > 1_000) throw CalcError("TOO_BIG", "El resultado es demasiado grande.")
        var result = java.math.BigInteger.ONE
        for (i in 2..k) result = result.multiply(java.math.BigInteger.valueOf(i.toLong()))
        return BigDecimal(result)
    }

    private fun function(name: String, x: BigDecimal): BigDecimal {
        val d = x.toDouble()
        val radians = Math.toRadians(d)
        return fromDouble(when (name) {
            "sin" -> Math.sin(radians)
            "cos" -> Math.cos(radians)
            "tan" -> if (Math.floorMod(Math.round(d * 1e9), Math.round(180e9)) == Math.round(90e9))
                throw CalcError("DOMAIN", "La tangente de 90° no existe.") else Math.tan(radians)
            "ln" -> if (d <= 0) throw CalcError("DOMAIN", "El logaritmo es solo de números positivos.") else Math.log(d)
            "log" -> if (d <= 0) throw CalcError("DOMAIN", "El logaritmo es solo de números positivos.") else Math.log10(d)
            else -> if (d < 0) throw CalcError("DOMAIN", "La raíz de un número negativo no es real.") else Math.sqrt(d)
        })
    }

    /** 15 significant digits; dust below 1E-14 is zero (so sin(180) is 0). */
    private fun fromDouble(d: Double): BigDecimal {
        if (d.isNaN() || d.isInfinite()) throw CalcError("TOO_BIG", "El resultado es demasiado grande.")
        if (kotlin.math.abs(d) < 1e-14) return BigDecimal.ZERO
        return BigDecimal(d).round(MathContext(15, RoundingMode.HALF_EVEN))
    }
}
