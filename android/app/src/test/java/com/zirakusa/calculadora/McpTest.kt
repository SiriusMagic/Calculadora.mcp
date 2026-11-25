package com.zirakusa.calculadora

import com.zirakusa.calculadora.data.Entry
import com.zirakusa.calculadora.engine.Rates
import com.zirakusa.calculadora.mcp.Access
import com.zirakusa.calculadora.mcp.CalcData
import com.zirakusa.calculadora.mcp.HubLink
import com.zirakusa.calculadora.mcp.McpHandler
import com.zirakusa.calculadora.mcp.McpMode
import com.zirakusa.calculadora.mcp.Token
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.math.BigDecimal

/** The calculator's MCP: tools list, good calls, bad arguments, not found, read-only, and the hub link. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class McpTest {
    private val data = object : CalcData {
        var history = listOf(Entry("720+100", "820", 0), Entry("60×12", "720", 0))
        var typed: Pair<String, Boolean>? = null
        override fun history() = history
        override fun clearHistory() { history = emptyList() }
        override suspend fun rates(fresh: Boolean) = Rates("USD", mapOf("EUR" to BigDecimal("0.9"), "MXN" to BigDecimal("18")), 1_763_596_800)
        override fun type(expression: String, equals: Boolean): String? { typed = expression to equals; return if (equals) "42" else null }
    }
    private val reader = Token("cal_" + "r".repeat(40), Access.READ)
    private val writer = Token("cal_" + "w".repeat(40))
    private val handler = McpHandler({ listOf(reader, writer) }, data, "0.3.0")

    private fun rpc(token: Token, method: String, params: JSONObject = JSONObject()) = runBlocking {
        JSONObject(handler.handle("POST", "/mcp", mapOf("Authorization" to "Bearer ${token.secret}"), emptyMap(),
            JSONObject().put("jsonrpc", "2.0").put("id", 1).put("method", method).put("params", params).toString()).body).getJSONObject("result")
    }
    private fun tool(token: Token, name: String, args: JSONObject) = rpc(token, "tools/call", JSONObject().put("name", name).put("arguments", args))
    private fun ok(token: Token, name: String, args: JSONObject) = tool(token, name, args).also { assertFalse(it.toString(), it.optBoolean("isError")) }.getJSONObject("structuredContent")
    private fun code(token: Token, name: String, args: JSONObject) = tool(token, name, args).also { assertTrue(it.optBoolean("isError")) }
        .getJSONObject("structuredContent").getJSONObject("error").getString("code")

    @Test fun toolsFollowTheAccess() {
        val read = rpc(reader, "tools/list").getJSONArray("tools").toString()
        assertTrue(read.contains("calc_multiply")); assertTrue(read.contains("calc_convert_currency")); assertFalse(read.contains("calc_type"))
        assertTrue(rpc(writer, "tools/list").getJSONArray("tools").toString().contains("calc_history_clear"))
        assertEquals("redirect", rpc(reader, "initialize").getJSONObject("_meta").getJSONObject(McpHandler.TOKEN_META).getString("kind"))
    }

    @Test fun operationsAreExactAndGoThroughTheEngine() {
        assertEquals("0.3", ok(reader, "calc_add", JSONObject().put("numbers", JSONArray(listOf("0.1", 0.2)))).getString("result"))
        assertEquals("-7", ok(reader, "calc_subtract", JSONObject().put("a", 3).put("b", 10)).getString("result"))
        assertEquals("150", ok(reader, "calc_multiply", JSONObject().put("numbers", JSONArray(listOf(2, 3, 25)))).getString("result"))
        assertEquals("0.333333333333", ok(reader, "calc_divide", JSONObject().put("a", 1).put("b", 3)).getString("result"))
        assertEquals("30", ok(reader, "calc_percent", JSONObject().put("percent", 15).put("of", 200)).getString("result"))
        assertEquals("1024", ok(reader, "calc_power", JSONObject().put("base", 2).put("exponent", 10)).getString("result"))
        assertEquals("110", ok(reader, "calc_evaluate", JSONObject().put("expression", "100+10%")).getString("result"))
        assertEquals("1", ok(reader, "calc_evaluate", JSONObject().put("expression", "2×sin(30)")).getString("result"))
    }

    @Test fun badInputIsAnErrorCodeNeverCode() {
        assertEquals("DIVISION_BY_ZERO", code(reader, "calc_divide", JSONObject().put("a", 1).put("b", 0)))
        assertEquals("INVALID_ARGUMENT", code(reader, "calc_add", JSONObject().put("numbers", JSONArray(listOf("1", "2);x")))))
        assertEquals("INVALID_ARGUMENT", code(reader, "calc_add", JSONObject().put("numbers", JSONArray(listOf(1)))))
        assertEquals("INVALID_ARGUMENT", code(reader, "calc_subtract", JSONObject().put("a", "uno").put("b", 1)))
        assertEquals("DOMAIN", code(reader, "calc_sqrt", JSONObject().put("x", -4)))
        assertEquals("SYNTAX", code(reader, "calc_evaluate", JSONObject().put("expression", "Runtime.exec()")))
        assertEquals("UNKNOWN_TOOL", code(reader, "calc_hack", JSONObject()))
    }

    @Test fun historyCurrenciesAndUnits() {
        assertEquals(1, ok(reader, "calc_history", JSONObject().put("query", "720+")).getInt("total"))
        assertEquals("90", ok(reader, "calc_convert_currency", JSONObject().put("amount", 100).put("from", "usd").put("to", "EUR")).getString("result"))
        assertEquals("UNKNOWN_CURRENCY", code(reader, "calc_convert_currency", JSONObject().put("amount", 1).put("from", "USD").put("to", "ZZZ")))
        assertEquals("1.609344", ok(reader, "calc_convert_units", JSONObject().put("value", 1).put("category", "length").put("from", "mi").put("to", "km")).getString("result"))
        assertEquals("INVALID_ARGUMENT", code(reader, "calc_convert_units", JSONObject().put("value", 1).put("category", "LENGTH").put("from", "mi").put("to", "kg")))
        assertTrue(ok(reader, "calc_units", JSONObject()).has("TEMPERATURE"))
    }

    @Test fun typingAndClearingNeedAWriteToken() {
        assertEquals("READ_ONLY", code(reader, "calc_type", JSONObject().put("expression", "6*7")))
        assertEquals("42", ok(writer, "calc_type", JSONObject().put("expression", "6*7").put("equals", true)).getString("result"))
        assertEquals("6*7" to true, data.typed)
        assertEquals("INVALID_ARGUMENT", code(writer, "calc_type", JSONObject().put("expression", "<script>")))
        ok(writer, "calc_history_clear", JSONObject())
        assertTrue(data.history.isEmpty())
    }

    @Test fun arkaTurnsThePublicLinkOnAndOff() {
        McpMode.load(androidx.test.core.app.ApplicationProvider.getApplicationContext())
        McpMode.turnRemote(false)
        assertEquals("REDIRECT_OFF", HubLink.answer(true).getString("error"))
        // Only the same developer (Arka) may connect, disconnect, read the status or run a tool.
        listOf(HubLink.connect(false), HubLink.disconnect(false), HubLink.status(false), HubLink.tool(false, "{}"))
            .forEach { assertEquals("NOT_SAME_DEVELOPER", it.getString("error")) }
        assertTrue(HubLink.connect(true).getString("link")!!.contains("/mcp?token=cal_"))
        assertTrue(HubLink.status(true).getBoolean("remote"))
        assertFalse(HubLink.disconnect(true).getBoolean("remote"))
        assertEquals("REDIRECT_OFF", HubLink.answer(true).getString("error"))
    }

    @Test fun theOldHubTokenIsKept() {
        val old = """[{"id":"a","secret":"cal_${"l".repeat(40)}","kind":"llm"},{"id":"b","secret":"cal_${"h".repeat(40)}","kind":"redirect"}]"""
        assertEquals("cal_" + "h".repeat(40), com.zirakusa.calculadora.mcp.Legacy.redirectSecret(old))
        assertEquals(null, com.zirakusa.calculadora.mcp.Legacy.redirectSecret("[]"))
        assertEquals(null, com.zirakusa.calculadora.mcp.Legacy.redirectSecret(""))
    }

    /** Lucía runs a tool on the phone through the same handler, with or without the public link. */
    @Test fun luciaRunsToolsOnThePhone() {
        val relay = McpMode.relayUrl
        McpMode.relayUrl = ""                                             // no relay in tests
        McpMode.load(org.robolectric.RuntimeEnvironment.getApplication())
        McpMode.turnRemote(false)
        val list = JSONObject(McpMode.local("""{"jsonrpc":"2.0","id":1,"method":"tools/list"}""")!!)
        assertTrue(list.getJSONObject("result").getJSONArray("tools").length() > 0)
        val viaProvider = JSONObject(HubLink.tool(true, """{"jsonrpc":"2.0","id":2,"method":"tools/list"}""").getString("response"))
        assertEquals(2, viaProvider.getInt("id"))
        assertEquals("BAD_REQUEST", HubLink.tool(true, "").getString("error"))
        McpMode.relayUrl = relay                                          // as the other tests expect
    }
}
