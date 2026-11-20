package com.zirakusa.calculadora

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.zirakusa.calculadora.data.PrefsHistory
import com.zirakusa.calculadora.data.RatesSource
import com.zirakusa.calculadora.floating.FloatingService
import com.zirakusa.calculadora.ui.CalcTheme
import com.zirakusa.calculadora.ui.CalculatorScreen
import com.zirakusa.calculadora.ui.CalculatorState
import com.zirakusa.calculadora.ui.convert.CurrencyScreen
import com.zirakusa.calculadora.ui.convert.UnitsScreen

private enum class Screen { CALCULATOR, CURRENCY, UNITS }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val history = PrefsHistory(this)
        val rates = RatesSource(this)
        setContent {
            CalcTheme {
                val state = remember { CalculatorState(history) }
                var screen by rememberSaveable { mutableStateOf(Screen.CALCULATOR) }
                LifecycleResumeEffect(Unit) { state.reloadHistory(); onPauseOrDispose { } } // the floating window adds to it too
                BackHandler(screen != Screen.CALCULATOR) { screen = Screen.CALCULATOR }
                when (screen) {
                    Screen.CALCULATOR -> CalculatorScreen(state,
                        // Like the phone: the calculator becomes a small window and you go back to what you were doing.
                        onFloat = { if (FloatingService.open(this)) moveTaskToBack(true) },
                        onCurrency = { screen = Screen.CURRENCY }, onUnits = { screen = Screen.UNITS })
                    Screen.CURRENCY -> CurrencyScreen(rates) { screen = Screen.CALCULATOR }
                    Screen.UNITS -> UnitsScreen { screen = Screen.CALCULATOR }
                }
            }
        }
    }
}
