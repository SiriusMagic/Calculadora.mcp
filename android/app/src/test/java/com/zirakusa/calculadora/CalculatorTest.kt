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

    @Test fun scientificFunctionsInDegrees() {
        assertEquals("1024", calc("2^10"))
        assertEquals("-4", calc("−2^2"))                     // the power first, then the sign
        assertEquals("0.25", calc("2^−2"))
        assertEquals("120", calc("5!"))
        assertEquals("3", calc("√9"))
        assertEquals("0.5", calc("sin(30)"))
        assertEquals("0", calc("sin(180)"))                  // no 1.2E-16 dust
        assertEquals("1", calc("tan(45)"))
        assertEquals("2", calc("log(100)"))
        assertEquals("1", calc("ln(e)"))
        assertEquals("6.28318530718", calc("2×π"))
        assertEquals("1.414213562373", calc("2^0,5"))
        assertEquals("4.023872600771E+2567", calc("1000!"))
    }

    @Test fun badInputGivesACodeNeverACrash() {
        assertEquals("DOMAIN", fails("√(−4)"))
        assertEquals("DOMAIN", fails("ln(0)"))
        assertEquals("DOMAIN", fails("tan(90)"))
        assertEquals("DOMAIN", fails("2,5!"))
        assertEquals("TOO_BIG", fails("1001!"))
        assertEquals("TOO_BIG", fails("10^99999"))
        assertEquals("DOMAIN", fails("(−8)^0,5"))
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
