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
import com.zirakusa.calculadora.ui.McpScreen
import com.zirakusa.calculadora.mcp.McpMode
import com.zirakusa.calculadora.ui.convert.CurrencyScreen
import com.zirakusa.calculadora.ui.convert.UnitsScreen

private enum class Screen { CALCULATOR, CURRENCY, UNITS, MCP }

class MainActivity : ComponentActivity() {
    private fun copyLink(link: String) {
        getSystemService(android.content.ClipboardManager::class.java)?.setPrimaryClip(android.content.ClipData.newPlainText("Calculadora MCP", link))
        android.widget.Toast.makeText(this, R.string.copied, android.widget.Toast.LENGTH_SHORT).show()
    }

    /** «Ir al Hub»: copies the hub's link (in case it must be pasted) and opens the Gateway Hub app. */
    private fun goToHub() {
        getSystemService(android.content.ClipboardManager::class.java)?.setPrimaryClip(android.content.ClipData.newPlainText("Calculadora › Gateway Hub", McpMode.hubLink()))
        val open = packageManager.getLaunchIntentForPackage("com.zirakusa.gatewayhub")
        if (open != null) startActivity(open) else android.widget.Toast.makeText(this, R.string.mcp_paste_in_hub, android.widget.Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppIcon.load(this)
        enableEdgeToEdge()
        val history = PrefsHistory(this)
        val rates = RatesSource(this)
        McpMode.load(this)
        setContent {
            CalcTheme {
                val state = remember { CalculatorState(history) }
                // calc_type (Modo MCP) writes on this screen while it is open.
                DisposableEffect(state) { McpMode.screen = state; onDispose { McpMode.screen = null } }
                var screen by rememberSaveable { mutableStateOf(Screen.CALCULATOR) }
                LifecycleResumeEffect(Unit) { state.reloadHistory(); onPauseOrDispose { state.keepUnsaved() } } // the floating window adds to it too; leaving keeps an unsaved result
                BackHandler(screen != Screen.CALCULATOR) { screen = Screen.CALCULATOR }
                when (screen) {
                    Screen.CALCULATOR -> CalculatorScreen(state,
                        // Like the phone: the calculator becomes a small window and you go back to what you were doing.
                        onFloat = { if (FloatingService.open(this)) moveTaskToBack(true) },
                        onCurrency = { screen = Screen.CURRENCY }, onUnits = { screen = Screen.UNITS }, onMcp = { screen = Screen.MCP })
                    Screen.CURRENCY -> CurrencyScreen(rates) { screen = Screen.CALCULATOR }
                    Screen.UNITS -> UnitsScreen { screen = Screen.CALCULATOR }
                    Screen.MCP -> McpScreen(::copyLink, ::goToHub) { screen = Screen.CALCULATOR }
                }
            }
        }
    }
}
