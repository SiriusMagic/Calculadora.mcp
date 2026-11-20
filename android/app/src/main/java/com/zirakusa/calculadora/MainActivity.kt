package com.zirakusa.calculadora

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import com.zirakusa.calculadora.data.PrefsHistory
import com.zirakusa.calculadora.ui.CalcTheme
import com.zirakusa.calculadora.ui.CalculatorScreen
import com.zirakusa.calculadora.ui.CalculatorState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val history = PrefsHistory(this)
        setContent {
            CalcTheme {
                val state = remember { CalculatorState(history) }
                CalculatorScreen(state)
            }
        }
    }
}
