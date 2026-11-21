package com.zirakusa.calculadora.data

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

/** One finished calculation; [auto] when it was kept without pressing «=» (so it is never lost). */
data class Entry(val expression: String, val result: String, val at: Long, val auto: Boolean = false)

/** The last [MAX] calculations, newest first, kept only on this phone (no cloud backup). */
interface HistoryStore {
    fun read(): List<Entry>
    fun write(entries: List<Entry>)

    companion object {
        const val MAX = 100
        fun serialize(entries: List<Entry>): String = JSONArray(entries.take(MAX).map {
            JSONObject().put("e", it.expression).put("r", it.result).put("t", it.at).apply { if (it.auto) put("a", true) }
        }).toString()
        fun parse(text: String?): List<Entry> = runCatching {
            val array = JSONArray(text ?: "[]")
            (0 until array.length()).map { array.getJSONObject(it) }.map { Entry(it.getString("e"), it.getString("r"), it.optLong("t"), it.optBoolean("a")) }
        }.getOrDefault(emptyList())
    }
}

class PrefsHistory(context: Context) : HistoryStore {
    private val prefs = context.getSharedPreferences("calculadora", Context.MODE_PRIVATE)
    override fun read() = HistoryStore.parse(prefs.getString("history", null))
    override fun write(entries: List<Entry>) = prefs.edit { putString("history", HistoryStore.serialize(entries)) }
}
