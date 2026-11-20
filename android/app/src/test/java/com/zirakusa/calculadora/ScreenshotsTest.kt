package com.zirakusa.calculadora

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.zirakusa.calculadora.data.Entry
import com.zirakusa.calculadora.data.HistoryStore
import com.zirakusa.calculadora.ui.CalcTheme
import com.zirakusa.calculadora.ui.CalculatorScreen
import com.zirakusa.calculadora.ui.CalculatorState
import com.zirakusa.calculadora.ui.convert.UnitsScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "es-w411dp-h891dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScreenshotsTest {
    @get:Rule val compose = createComposeRule()

    private val store = object : HistoryStore {
        var list = listOf(Entry("720+100", "820", 0), Entry("60×12", "720", 0), Entry("24×2,50", "60", 0))
        override fun read() = list
        override fun write(entries: List<Entry>) { list = entries }
    }

    @Test fun keypadScientificAndHistory() {
        val state = CalculatorState(store)
        compose.setContent { CalcTheme { CalculatorScreen(state, {}, {}, {}) } }
        listOf("1", "2", "×", "3", "+", "4").forEach { compose.onNodeWithText(it).performClick() }
        compose.onNodeWithText("= 40").assertExists()
        onView(isRoot()).captureRoboImage("build/screenshots/calc-keypad.png")
        compose.onNodeWithContentDescription("Científica").performClick()
        compose.onNodeWithText("sin").assertExists()
        onView(isRoot()).captureRoboImage("build/screenshots/calc-scientific.png")
        compose.onNodeWithContentDescription("Historial").performClick()
        compose.onNodeWithText("720+100").assertExists()
        onView(isRoot()).captureRoboImage("build/screenshots/calc-history.png")
    }

    @Test fun unitConverter() {
        compose.setContent { CalcTheme { UnitsScreen {} } }
        compose.onNodeWithText("Longitud").assertExists()
        compose.onNodeWithText("1000").assertExists()           // 1 km = 1000 m
        onView(isRoot()).captureRoboImage("build/screenshots/calc-units.png")
    }
}
