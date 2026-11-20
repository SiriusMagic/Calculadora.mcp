package com.zirakusa.calculadora.engine

import org.json.JSONObject
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/** Exchange rates against [base] (1 base = rates[code]) and when the provider updated them (seconds). */
data class Rates(val base: String, val rates: Map<String, BigDecimal>, val updatedAt: Long) {
    fun has(code: String) = code == base || code in rates

    /** amount in [from] → [to]; null when a currency is unknown. */
    fun convert(amount: BigDecimal, from: String, to: String): BigDecimal? {
        val a = if (from == base) BigDecimal.ONE else rates[from] ?: return null
        val b = if (to == base) BigDecimal.ONE else rates[to] ?: return null
        return amount.multiply(b, MC).divide(a, MC)
    }

    companion object {
        private val MC = MathContext(34, RoundingMode.HALF_EVEN)
        private val CODE = Regex("^[A-Z]{3}$")

        /**
         * The reply of ExchangeRate-API's open endpoint ({"result":"success","base_code":"USD","rates":{…}}). Anything
         * else (an error page, odd values) is refused instead of shown as a rate.
         */
        fun parse(json: String): Rates {
            val o = JSONObject(json)
            require(o.optString("result") == "success") { "provider error" }
            val base = o.getString("base_code").also { require(CODE.matches(it)) }
            val list = o.getJSONObject("rates")
            val rates = list.keys().asSequence().filter(CODE::matches).mapNotNull { code ->
                list.optDouble(code).takeIf { it.isFinite() && it > 0 }?.let { code to BigDecimal(it.toString()) }
            }.toMap()
            require(rates.size >= 10) { "too few rates" }
            return Rates(base, rates, o.optLong("time_last_update_unix"))
        }
    }
}
