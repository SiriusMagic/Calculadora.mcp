package com.zirakusa.calculadora

import com.zirakusa.calculadora.engine.Category
import com.zirakusa.calculadora.engine.Rates
import com.zirakusa.calculadora.engine.Units
import com.zirakusa.calculadora.ui.convert.ConverterState
import com.zirakusa.calculadora.ui.convert.flag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.math.BigDecimal

@RunWith(RobolectricTestRunner::class) // org.json is Android's
@Config(sdk = [34])
class ConvertersTest {
    private fun units(category: Category, value: String, from: String, to: String) =
        Units.convert(BigDecimal(value), category.unit(from), category.unit(to)).stripTrailingZeros().toPlainString()

    @Test fun unitsConvertExactly() {
        assertEquals("1.609344", units(Category.LENGTH, "1", "mi", "km"))
        assertEquals("212", units(Category.TEMPERATURE, "100", "°C", "°F"))
        assertEquals("-40", units(Category.TEMPERATURE, "-40", "°F", "°C"))
        assertEquals("0", units(Category.TEMPERATURE, "273.15", "K", "°C"))
        assertEquals("1024", units(Category.DATA, "1", "GB", "MB"))
        assertEquals("100", units(Category.MASS, "1", "qq", "lb"))
    }

    private val sample = """{"result":"success","base_code":"USD","time_last_update_unix":1763596800,"rates":{"USD":1,"EUR":0.912101,
        "MXN":18.3,"GTQ":7.7,"COP":4000,"PEN":3.75,"ARS":1000,"BRL":5.4,"CLP":930,"BOB":6.9,"PYG":7800,"bad":-1,"X1":2}}"""

    @Test fun ratesComeFromTheProviderReplyAndConvertBothWays() {
        val rates = Rates.parse(sample)
        assertEquals("USD", rates.base)
        assertNull(rates.rates["bad"])                         // odd codes and values are dropped
        assertEquals("91.2101", rates.convert(BigDecimal(100), "USD", "EUR")!!.stripTrailingZeros().toPlainString())
        assertEquals("1000", rates.convert(BigDecimal(1), "USD", "ARS")!!.stripTrailingZeros().toPlainString())
        assertNull(rates.convert(BigDecimal.ONE, "USD", "ZZZ"))
        assertThrows(IllegalArgumentException::class.java) { Rates.parse("""{"result":"error","error-type":"quota"}""") }
    }

    @Test fun theActiveRowIsTypedAndTheOthersFollow() {
        val rates = Rates.parse(sample)
        val state = ConverterState(listOf("USD", "USD", "EUR"), 4) { a, f, t -> rates.convert(a, f, t) }
        state.clear(); listOf("1", "0", "0").forEach(state::type)
        assertEquals(listOf("100", "100", "91,2101"), (0..2).map(state::shown))
        state.activate(2)                                      // typing in EUR now
        state.back(); state.back()
        assertEquals("91,21", state.shown(2))
        assertEquals("99,9999", state.shown(0))
        state.pick(1, "ZZZ")
        assertEquals("—", state.shown(1))
        state.type(","); state.type(",")
        assertEquals("91,21", state.shown(2))                  // a second comma is ignored
        assertEquals("🇺🇸", flag("USD")); assertEquals("🇪🇺", flag("EUR")); assertEquals("💱", flag("XAU"))
    }
}
