package com.zirakusa.calculadora.data

import android.content.Context
import androidx.core.content.edit
import com.zirakusa.calculadora.engine.Rates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Exchange rates: the last ones are kept on the phone (the converter works offline with them) and «Actualizar tasa de
 * cambio» asks one fixed address, ExchangeRate-API's free open endpoint (no key, nothing about the user is sent).
 */
class RatesSource(context: Context) {
    private val prefs = context.getSharedPreferences("rates", Context.MODE_PRIVATE)

    fun cached(): Rates? = prefs.getString("json", null)?.let { runCatching { Rates.parse(it) }.getOrNull() }

    /** Downloads fresh rates; on any failure keeps the old ones and says why. */
    suspend fun refresh(): Result<Rates> = withContext(Dispatchers.IO) {
        runCatching {
            val connection = URL(URL_RATES).openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = 10_000; connection.readTimeout = 10_000
                connection.setRequestProperty("Accept", "application/json")
                check(connection.responseCode == 200) { "HTTP ${connection.responseCode}" }
                val body = connection.inputStream.bufferedReader().use { it.readText().take(MAX_BYTES) }
                Rates.parse(body).also { prefs.edit { putString("json", body) } }
            } finally { connection.disconnect() }
        }
    }

    /** The currencies the user had on screen. */
    var chosen: List<String>
        get() = prefs.getString("chosen", null)?.split(',')?.filter { it.length == 3 }.orEmpty()
        set(value) = prefs.edit { putString("chosen", value.joinToString(",")) }

    companion object {
        const val URL_RATES = "https://open.er-api.com/v6/latest/USD"
        private const val MAX_BYTES = 200_000
    }
}
