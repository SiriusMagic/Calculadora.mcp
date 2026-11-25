package com.zirakusa.calculadora.mcp

import java.security.SecureRandom
import java.util.Base64

/** READ: calculates, converts and reads the history. WRITE: also types on the calculator's screen and clears the history. */
enum class Access(val key: String) { READ("read"), WRITE("write") }

/**
 * A token the handler accepts. The app serves only Arka's own (WRITE): Arka turns the Modo MCP on or off, and the hub
 * decides what each AI may do. READ exists so the rules for read-only tokens stay tested.
 */
class Token(val secret: String, val access: Access = Access.WRITE) {
    companion object {
        /** "cal_" + 256 random bits: the prefix tells the hub (and secret scanners) which app it belongs to. */
        fun secret(): String = "cal_" + Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(32).also(SecureRandom()::nextBytes))
        fun id(): String = ByteArray(6).also(SecureRandom()::nextBytes).joinToString("") { "%02x".format(it) }
    }
}
