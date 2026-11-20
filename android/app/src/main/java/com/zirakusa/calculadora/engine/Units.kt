package com.zirakusa.calculadora.engine

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/** A unit: how many base units one of it is (temperature uses [toBase]/[fromBase] instead). */
class Measure(val symbol: String, val factor: BigDecimal = BigDecimal.ONE,
    val toBase: ((BigDecimal) -> BigDecimal)? = null, val fromBase: ((BigDecimal) -> BigDecimal)? = null)

/** Conversion categories of the unit converter (the ruler button). Factors are exact where the definition is exact. */
enum class Category(val units: List<Measure>) {
    LENGTH(listOf(u("km", "1000"), u("m", "1"), u("cm", "0.01"), u("mm", "0.001"), u("mi", "1609.344"), u("yd", "0.9144"),
        u("ft", "0.3048"), u("in", "0.0254"), u("nmi", "1852"))),
    MASS(listOf(u("t", "1000"), u("kg", "1"), u("g", "0.001"), u("mg", "0.000001"), u("lb", "0.45359237"), u("oz", "0.028349523125"),
        u("qq", "45.359237"))),
    TEMPERATURE(listOf(Measure("°C", toBase = { it }, fromBase = { it }),
        Measure("°F", toBase = { (it - n("32")) * n("5") / n("9") }, fromBase = { it * n("9") / n("5") + n("32") }),
        Measure("K", toBase = { it - n("273.15") }, fromBase = { it + n("273.15") }))),
    AREA(listOf(u("km²", "1000000"), u("ha", "10000"), u("m²", "1"), u("cm²", "0.0001"), u("mi²", "2589988.110336"),
        u("acre", "4046.8564224"), u("ft²", "0.09290304"), u("in²", "0.00064516"))),
    VOLUME(listOf(u("m³", "1000"), u("L", "1"), u("mL", "0.001"), u("gal", "3.785411784"), u("qt", "0.946352946"),
        u("cup", "0.2365882365"), u("fl oz", "0.0295735295625"))),
    SPEED(listOf(u("km/h", "1"), u("m/s", "3.6"), u("mph", "1.609344"), u("kn", "1.852"))),
    DATA(listOf(u("B", "1"), u("KB", "1024"), u("MB", "1048576"), u("GB", "1073741824"), u("TB", "1099511627776"), u("bit", "0.125"))),
    TIME(listOf(u("s", "1"), u("min", "60"), u("h", "3600"), u("d", "86400"), u("sem", "604800"), u("ms", "0.001")));

    fun unit(symbol: String) = units.first { it.symbol == symbol }
}

private val MC = MathContext(34, RoundingMode.HALF_EVEN)
private fun n(text: String) = BigDecimal(text)
private fun u(symbol: String, factor: String) = Measure(symbol, BigDecimal(factor))
private operator fun BigDecimal.div(other: BigDecimal): BigDecimal = divide(other, MC)

object Units {
    fun convert(value: BigDecimal, from: Measure, to: Measure): BigDecimal {
        val base = from.toBase?.invoke(value) ?: value.multiply(from.factor, MC)
        return to.fromBase?.invoke(base) ?: base.divide(to.factor, MC)
    }
}
