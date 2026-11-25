package com.zirakusa.calculadora.mcp

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import com.zirakusa.calculadora.BuildConfig
import com.zirakusa.calculadora.data.Entry
import com.zirakusa.calculadora.data.PrefsHistory
import com.zirakusa.calculadora.data.RatesSource
import com.zirakusa.calculadora.engine.Rates
import com.zirakusa.calculadora.ui.CalculatorState

/**
 * The Modo MCP, run by Arka (same developer, through [HubLinkProvider]): «connect» turns the public link on (the relay,
 * with Arka's own token, so the hub decides who may do what) and «disconnect» turns it off. Lucía calls the tools on the
 * phone with [local], without internet. Off by default; nothing to set up in this app.
 */
object McpMode {
    var remote by mutableStateOf(false); private set
    var state by mutableStateOf(RelayLink.State.OFFLINE); private set
    /** The calculator on screen (set by the activity), so calc_type writes where the user sees it. */
    var screen: CalculatorState? = null

    private var prefs: android.content.SharedPreferences? = null
    private var link: RelayLink? = null
    private var device = ""
    private var relaySecret = ""
    private var token = Token("")
    internal var relayUrl: String = BuildConfig.RELAY_URL

    fun load(context: Context) {
        if (prefs != null) return
        val app = context.applicationContext
        val p = app.getSharedPreferences("mcp", Context.MODE_PRIVATE)
        prefs = p
        remote = p.getBoolean("redirect", false)
        token = Token(SecretBox.open(p.getString("arka", "") ?: "").ifBlank {
            (Legacy.redirectSecret(SecretBox.open(p.getString("tokens", "") ?: "")) ?: Token.secret())
                .also { p.edit { putString("arka", SecretBox.seal(it)); remove("tokens"); remove("enabled") } }
        })
        device = p.getString("device", null) ?: Token.id().repeat(2).also { p.edit { putString("device", it) } }
        relaySecret = SecretBox.open(p.getString("relay", "") ?: "").ifBlank {
            Token.secret().also { p.edit { putString("relay", SecretBox.seal(it)) } }
        }
        data = AppData(app)
        apply()
    }

    /** Arka's «Activar MCP» for this app: the public link on or off. */
    fun turnRemote(on: Boolean) { remote = on; prefs?.edit { putBoolean("redirect", on) }; apply() }

    fun hubLink() = "${relayUrl.trimEnd('/')}/d/$device/mcp?token=${token.secret}"

    private var data: CalcData? = null

    /** One JSON-RPC request from Lucía on this phone, through the same handler as the relay. Null before [load]. */
    fun local(body: String): String? {
        val d = data ?: return null
        return kotlinx.coroutines.runBlocking {
            handler(d).handle("POST", "/mcp", mapOf("Authorization" to "Bearer ${token.secret}"), emptyMap(), body).body
        }
    }

    private fun handler(d: CalcData) = McpHandler({ listOf(token) }, d, BuildConfig.VERSION_NAME)

    private fun apply() {
        val d = data ?: return
        if (remote && relayUrl.isNotBlank()) {
            if (link == null) link = RelayLink(relayUrl, device, relaySecret, handler(d)) { state = it }.also { it.start() }
        } else { link?.stop(); link = null; state = RelayLink.State.OFFLINE }
    }

    /** The real calculator: its history, the saved exchange rates and the screen. */
    private class AppData(private val context: Context) : CalcData {
        private val history = PrefsHistory(context)
        private val rates = RatesSource(context)
        override fun history(): List<Entry> = history.read()
        override fun clearHistory() { history.write(emptyList()); screen?.let { s -> android.os.Handler(context.mainLooper).post { s.reloadHistory() } } }
        override suspend fun rates(fresh: Boolean): Rates? {
            val saved = rates.cached()
            if (fresh && (saved == null || System.currentTimeMillis() / 1000 - saved.updatedAt > 86_400)) rates.refresh().getOrNull()?.let { return it }
            return saved
        }
        override fun type(expression: String, equals: Boolean): String? {
            val result = java.util.concurrent.FutureTask { (screen ?: CalculatorState(history)).typeFromAi(expression, equals) }
            android.os.Handler(context.mainLooper).post(result) // the screen's state lives on the main thread
            return result.get(5, java.util.concurrent.TimeUnit.SECONDS)
        }
    }
}
