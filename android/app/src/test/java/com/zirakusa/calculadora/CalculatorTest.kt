package com.zirakusa.calculadora

import com.zirakusa.calculadora.engine.CalcError
import com.zirakusa.calculadora.engine.Calculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CalculatorTest {
    private fun calc(text: String) = Calculator.format(Calculator.evaluate(text))
    private fun fails(text: String) = assertThrows(CalcError::class.java) { Calculator.evaluate(text) }.code

    @Test fun precedenceParenthesesAndSigns() {
        assertEquals("14", calc("2+3×4"))
        assertEquals("20", calc("(2+3)×4"))
        assertEquals("-6", calc("−(2+4)"))
        assertEquals("5", calc("2−−3"))
        assertEquals("2.5", calc("10÷4"))
        assertEquals("0.3", calc("0,1+0,2"))                 // exact decimals: not 0.30000000000000004
    }

    @Test fun percentLikeAPocketCalculator() {
        assertEquals("110", calc("100+10%"))
        assertEquals("90", calc("100−10%"))
        assertEquals("20", calc("200×10%"))
        assertEquals("0.5", calc("50%"))
        assertEquals("5", calc("50%×10"))
    }

    @Test fun repeatingDecimalsAreRoundedForTheScreen() {
        assertEquals("0.333333333333", calc("1÷3"))
        assertEquals("1", calc("1÷3×3"))
        assertEquals("100000000000000000000", calc("10000000000×10000000000"))
    }

    @Test fun badInputGivesACodeNeverACrash() {
        assertEquals("DIVISION_BY_ZERO", fails("5÷0"))
        assertEquals("DIVISION_BY_ZERO", fails("5÷(2−2)"))
        assertEquals("PARENTHESES", fails("(2+3"))
        assertEquals("INCOMPLETE", fails("2+"))
        assertEquals("SYNTAX", fails("2+3)"))
        assertEquals("SYNTAX", fails("1,2,3"))
        assertEquals("SYNTAX", fails("abc"))
        assertEquals("EMPTY", fails("  "))
        assertEquals("TOO_LONG", fails("1+".repeat(300) + "1"))
        assertEquals("TOO_DEEP", fails("(".repeat(60) + "1" + ")".repeat(60)))
        assertEquals("TOO_DEEP", fails("−".repeat(60) + "1"))
    }
}
