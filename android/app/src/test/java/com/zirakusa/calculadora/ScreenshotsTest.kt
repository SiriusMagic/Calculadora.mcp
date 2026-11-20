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

    @Test fun keypadAndHistory() {
        val store = object : HistoryStore {
            var list = listOf(Entry("100+10%", "110", 0))
            override fun read() = list
            override fun write(entries: List<Entry>) { list = entries }
        }
        val state = CalculatorState(store)
        compose.setContent { CalcTheme { CalculatorScreen(state) } }
        listOf("1", "2", "×", "( )", "3", "+", "4", "( )").forEach { compose.onNodeWithText(it).performClick() }
        compose.onNodeWithText("= 84").assertExists()
        onView(isRoot()).captureRoboImage("build/screenshots/calc-keypad.png")
        compose.onNodeWithContentDescription("Historial").performClick()
        compose.onNodeWithText("100+10%").assertExists()
    }
}
