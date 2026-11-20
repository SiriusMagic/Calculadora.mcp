package com.zirakusa.calculadora

import com.zirakusa.calculadora.data.Entry
import com.zirakusa.calculadora.data.HistoryStore
import com.zirakusa.calculadora.ui.CalculatorState
import com.zirakusa.calculadora.ui.Key
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class) // org.json is Android's
@Config(sdk = [34])
class CalculatorStateTest {
    private class Memory : HistoryStore {
        var saved = emptyList<Entry>()
        override fun read() = saved
        override fun write(entries: List<Entry>) { saved = entries }
    }

    private fun CalculatorState.type(vararg keys: Key) = keys.forEach(::press)

    @Test fun typingShowsALiveResultAndEqualsSavesIt() {
        val store = Memory()
        val state = CalculatorState(store) { 42 }
        state.type(Key.ONE, Key.TWO, Key.PLUS, Key.THREE, Key.DOT, Key.FIVE)
        assertEquals("12+3,5", state.expression)
        assertEquals("15,5", state.preview)
        state.press(Key.EQUALS)
        assertEquals("15,5", state.expression)
        assertEquals(listOf(Entry("12+3,5", "15.5", 42)), store.saved)
        state.press(Key.TWO)                      // a digit after «=» starts over
        assertEquals("2", state.expression)
    }

    @Test fun operatorsReplaceEachOtherAndKeysStaySensible() {
        val state = CalculatorState(Memory())
        state.type(Key.PLUS)                      // nothing to add to yet
        assertEquals("", state.expression)
        state.type(Key.ZERO, Key.ZERO, Key.SEVEN, Key.PLUS, Key.TIMES)
        assertEquals("7×", state.expression)
        state.type(Key.PARENS, Key.TWO, Key.PARENS, Key.DOT, Key.FIVE)       // «( )» lives in the scientific rows
        assertEquals("7×(2)×0,5", state.expression)
        state.type(Key.DOT)                       // a number has one comma at most
        assertEquals("7×(2)×0,5", state.expression)
        state.type(Key.BACK, Key.BACK, Key.BACK, Key.BACK)
        assertEquals("7×(2)", state.expression)
    }

    @Test fun scientificKeysAddWhatTheyMean() {
        val state = CalculatorState(Memory())
        state.press(Key.SCIENTIFIC)
        state.type(Key.TWO, Key.SIN, Key.THREE, Key.ZERO, Key.PARENS)
        assertEquals("2×sin(30)", state.expression)
        assertEquals("1", state.preview)
        state.press(Key.BACK)                                   // ⌫ removes «)», then the 0…
        state.type(Key.BACK, Key.BACK, Key.BACK)                // …the 3 and the whole «sin(»
        assertEquals("2×", state.expression)
        state.type(Key.PI, Key.SQUARE, Key.FACTORIAL)
        assertEquals("2×π^2!", state.expression)
        state.type(Key.CLEAR, Key.THREE, Key.POWER, Key.MINUS, Key.TWO)
        assertEquals("3^−2", state.expression)
    }

    @Test fun anAiTypesOnTheScreenWithTheCalculatorsSymbols() {
        val store = Memory()
        val state = CalculatorState(store) { 9 }
        assertEquals(null, state.typeFromAi("12.5*2-1", equals = false))
        assertEquals("12,5×2−1", state.expression)
        assertEquals("24", state.typeFromAi("12.5*2-1", equals = true))
        assertEquals(listOf(Entry("12,5×2−1", "24", 9)), store.saved)
    }

    @Test fun anErrorIsShownUntilTheNextKeyAndNothingIsSaved() {
        val store = Memory()
        val state = CalculatorState(store)
        state.type(Key.FIVE, Key.DIVIDE, Key.ZERO, Key.EQUALS)
        assertEquals("DIVISION_BY_ZERO", state.error)
        assertEquals(emptyList<Entry>(), store.saved)
        state.press(Key.BACK)
        assertNull(state.error)
    }

    @Test fun historyKeepsTheNewestHundredAndCanBeReusedOrCleared() {
        val store = Memory()
        val state = CalculatorState(store)
        repeat(105) { state.type(Key.CLEAR, Key.ONE, Key.PLUS, Key.ONE, Key.EQUALS) }
        assertEquals(HistoryStore.MAX, store.saved.size)
        state.reuse(state.history.first())
        assertEquals("1+1", state.expression)
        assertEquals(store.saved, HistoryStore.parse(HistoryStore.serialize(store.saved)))
        assertEquals(emptyList<Entry>(), HistoryStore.parse("no es json"))
        state.clearHistory()
        assertEquals(emptyList<Entry>(), store.saved)
    }
}
