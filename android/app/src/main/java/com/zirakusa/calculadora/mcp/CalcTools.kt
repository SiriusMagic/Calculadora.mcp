package com.zirakusa.calculadora.mcp

import com.zirakusa.calculadora.data.Entry
import com.zirakusa.calculadora.engine.CalcError
import com.zirakusa.calculadora.engine.Calculator
import com.zirakusa.calculadora.engine.Category
import com.zirakusa.calculadora.engine.Rates
import com.zirakusa.calculadora.engine.Units
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.time.Instant

/** What the tools read and change; the app gives the real one, tests a fake. */
interface CalcData {
    fun history(): List<Entry>
    fun clearHistory()
    /** The saved rates; refreshed from the provider first when [fresh] and they are older than a day. */
    suspend fun rates(fresh: Boolean): Rates?
    /** Writes [expression] on the calculator's screen; with [equals] presses «=» (saved in the history like a user's). */
    fun type(expression: String, equals: Boolean): String?
}

/** An expected failure the AI can read (structuredContent.error.code), never a crash. */
class ToolFailure(val code: String, message: String) : Exception(message)

/**
 * The calculator's MCP tools. Every calculation goes through engine/Calculator (never eval): the simple operations
 * build an expression only from validated numbers. READ tokens calculate, convert and read the history; WRITE tokens
 * can also type on the screen and clear the history.
 */
object CalcTools {
    class Tool(val name: String, val description: String, val write: Boolean, val schema: JSONObject)

    private val NUMBER = Regex("^-?\\d{1,30}(\\.\\d{1,30})?$")
    private fun schema(vararg props: Pair<String, JSONObject>, required: List<String> = emptyList()) = JSONObject()
        .put("type", "object").put("properties", JSONObject().apply { props.forEach { (k, v) -> put(k, v) } })
        .put("required", JSONArray(required)).put("additionalProperties", false)
    private fun str(d: String) = JSONObject().put("type", "string").put("description", d)
    private fun num(d: String) = JSONObject().put("type", listOf("number", "string").let(::JSONArray)).put("description", "$d (use . for decimals)")
    private fun nums(d: String) = JSONObject().put("type", "array").put("minItems", 2).put("maxItems", 50).put("items", num("A number")).put("description", d)

    val all = listOf(
        Tool("calc_evaluate", "Solve an expression exactly: + - * / (or × ÷ −), %, parentheses, ^, !, √, sin cos tan (degrees), ln, log, π, e. " +
            "'100+10%' is 110 like a pocket calculator. Returns the result or an error code.", false,
            schema("expression" to str("e.g. (12.5*4)+3^2 or 2×sin(30)"), required = listOf("expression"))),
        Tool("calc_add", "Add numbers.", false, schema("numbers" to nums("Numbers to add"), required = listOf("numbers"))),
        Tool("calc_subtract", "a − b.", false, schema("a" to num("Minuend"), "b" to num("Subtrahend"), required = listOf("a", "b"))),
        Tool("calc_multiply", "Multiply numbers.", false, schema("numbers" to nums("Numbers to multiply"), required = listOf("numbers"))),
        Tool("calc_divide", "a ÷ b (exact, up to 34 digits).", false, schema("a" to num("Dividend"), "b" to num("Divisor"), required = listOf("a", "b"))),
        Tool("calc_percent", "percent % of a number (e.g. 15 % of 200 = 30).", false,
            schema("percent" to num("The percent"), "of" to num("The number"), required = listOf("percent", "of"))),
        Tool("calc_power", "base ^ exponent.", false, schema("base" to num("Base"), "exponent" to num("Exponent"), required = listOf("base", "exponent"))),
        Tool("calc_sqrt", "Square root.", false, schema("x" to num("Number (0 or more)"), required = listOf("x"))),
        Tool("calc_history", "The calculator's history, newest first: what the user (or an AI) calculated.", false,
            schema("query" to str("Only entries containing this text"), "limit" to JSONObject().put("type", "integer").put("minimum", 1).put("maximum", 100))),
        Tool("calc_convert_currency", "Convert money with the latest saved exchange rates (refreshed if older than a day).", false,
            schema("amount" to num("Amount"), "from" to str("ISO code, e.g. USD"), "to" to str("ISO code, e.g. EUR"), required = listOf("amount", "from", "to"))),
        Tool("calc_currencies", "Currency codes available for calc_convert_currency and when the rates were updated.", false, schema()),
        Tool("calc_convert_units", "Convert units: LENGTH, MASS, TEMPERATURE, AREA, VOLUME, SPEED, DATA, TIME (see calc_units).", false,
            schema("value" to num("Value"), "category" to str("e.g. LENGTH"), "from" to str("Unit symbol, e.g. km"), "to" to str("Unit symbol, e.g. mi"),
                required = listOf("value", "category", "from", "to"))),
        Tool("calc_units", "The unit categories and the symbols of each.", false, schema()),
        Tool("calc_type", "Write an expression on the calculator's screen, as if typed; with equals=true also press «=» (saved in the history).",
            true, schema("expression" to str("What to type"), "equals" to JSONObject().put("type", "boolean"), required = listOf("expression"))),
        Tool("calc_history_clear", "Erase the calculator's history.", true, schema()),
    )

    fun available(access: Access) = all.filter { !it.write || access == Access.WRITE }

    suspend fun call(name: String, args: JSONObject, access: Access, data: CalcData): JSONObject {
        val tool = all.firstOrNull { it.name == name } ?: throw ToolFailure("UNKNOWN_TOOL", "No tool called $name.")
        if (tool.write && access != Access.WRITE) throw ToolFailure("READ_ONLY", "This token is read-only.")
        return when (name) {
            "calc_evaluate" -> solve(args.optString("expression").take(Calculator.MAX_LENGTH).ifBlank { throw bad("expression is required.") })
            "calc_add" -> solve(list(args).joinToString("+") { wrap(it) })
            "calc_multiply" -> solve(list(args).joinToString("*") { wrap(it) })
            "calc_subtract" -> solve("${wrap(n(args, "a"))}-${wrap(n(args, "b"))}")
            "calc_divide" -> solve("${wrap(n(args, "a"))}/${wrap(n(args, "b"))}")
            "calc_percent" -> solve("${wrap(n(args, "of"))}*${wrap(n(args, "percent"))}/100")
            "calc_power" -> solve("${wrap(n(args, "base"))}^${wrap(n(args, "exponent"))}")
            "calc_sqrt" -> solve("√${wrap(n(args, "x"))}")
            "calc_history" -> {
                val query = args.optString("query").trim()
                val found = data.history().filter { query.isEmpty() || it.expression.contains(query) || it.result.contains(query) }
                JSONObject().put("total", found.size).put("entries", JSONArray(found.take(args.optInt("limit", 20).coerceIn(1, 100)).map {
                    JSONObject().put("expression", it.expression).put("result", it.result).put("at", Instant.ofEpochMilli(it.at).toString())
                }))
            }
            "calc_convert_currency" -> {
                val rates = data.rates(fresh = true) ?: throw ToolFailure("NO_RATES", "No exchange rates yet: the phone needs internet once.")
                val from = code(args, "from"); val to = code(args, "to")
                val value = rates.convert(BigDecimal(n(args, "amount")), from, to)
                    ?: throw ToolFailure("UNKNOWN_CURRENCY", "Unknown currency (see calc_currencies).")
                JSONObject().put("amount", n(args, "amount")).put("from", from).put("to", to).put("result", Calculator.format(value))
                    .put("ratesUpdated", Instant.ofEpochSecond(rates.updatedAt).toString())
            }
            "calc_currencies" -> {
                val rates = data.rates(fresh = false) ?: throw ToolFailure("NO_RATES", "No exchange rates yet: the phone needs internet once.")
                JSONObject().put("base", rates.base).put("codes", JSONArray((listOf(rates.base) + rates.rates.keys).distinct().sorted()))
                    .put("updated", Instant.ofEpochSecond(rates.updatedAt).toString())
            }
            "calc_convert_units" -> {
                val category = runCatching { Category.valueOf(args.optString("category").trim().uppercase()) }
                    .getOrElse { throw bad("category is one of ${Category.entries.joinToString()}.") }
                val from = category.units.firstOrNull { it.symbol == args.optString("from").trim() } ?: throw bad("Unknown unit (see calc_units).")
                val to = category.units.firstOrNull { it.symbol == args.optString("to").trim() } ?: throw bad("Unknown unit (see calc_units).")
                JSONObject().put("value", n(args, "value")).put("from", from.symbol).put("to", to.symbol)
                    .put("result", Calculator.format(Units.convert(BigDecimal(n(args, "value")), from, to)))
            }
            "calc_units" -> JSONObject().apply { Category.entries.forEach { c -> put(c.name, JSONArray(c.units.map { it.symbol })) } }
            "calc_type" -> {
                val text = args.optString("expression").trim().take(Calculator.MAX_LENGTH)
                if (text.isEmpty() || !text.all { it.isDigit() || it in ALLOWED }) throw bad("Only numbers and calculator symbols can be typed.")
                JSONObject().put("typed", text).apply { data.type(text, args.optBoolean("equals"))?.let { put("result", it) } }
            }
            else -> { data.clearHistory(); JSONObject().put("cleared", true) } // calc_history_clear
        }
    }

    private const val ALLOWED = "+-−*×/÷%^!√().,πe sincotalgq"

    private fun solve(expression: String): JSONObject = try {
        JSONObject().put("expression", expression).put("result", Calculator.format(Calculator.evaluate(expression)))
    } catch (e: CalcError) { throw ToolFailure(e.code, e.message ?: e.code) }

    private fun bad(message: String) = ToolFailure("INVALID_ARGUMENT", message)

    /** A number given as JSON number or string; anything else is refused (it never reaches the parser as text). */
    private fun n(args: JSONObject, key: String): String = number(args.opt(key)) ?: throw bad("$key must be a number.")
    private fun number(value: Any?): String? = when (value) {
        is Number -> BigDecimal(value.toString()).toPlainString()
        is String -> value.trim().replace(',', '.').takeIf(NUMBER::matches)
        else -> null
    }?.takeIf(NUMBER::matches)
    private fun list(args: JSONObject): List<String> {
        val array = args.optJSONArray("numbers") ?: throw bad("numbers must be a list of 2 to 50 numbers.")
        if (array.length() !in 2..50) throw bad("numbers must be a list of 2 to 50 numbers.")
        return (0 until array.length()).map { number(array.opt(it)) ?: throw bad("numbers must be a list of 2 to 50 numbers.") }
    }
    private fun wrap(x: String) = if (x.startsWith("-")) "($x)" else x
    private fun code(args: JSONObject, key: String) = args.optString(key).trim().uppercase().takeIf { Regex("^[A-Z]{3}$").matches(it) }
        ?: throw bad("$key must be a 3-letter currency code.")
}
