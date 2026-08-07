package io.github.damian1000.portfolio.bitfinex

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ApiCredentialsTest {
    @Test
    fun `returns env values when present`() {
        val helper =
            ApiCredentials(
                env = mapOf(
                    "BITFINEX_API_KEY" to "bf-key",
                    "BITFINEX_API_SECRET" to "bf-secret",
                )::get,
            )
        assertEquals("bf-key", helper.apiKey())
        assertEquals("bf-secret", helper.apiSecret())
    }

    @Test
    fun `throws when api key missing`() {
        val ex = assertThrows(RuntimeException::class.java) { ApiCredentials(env = { null }).apiKey() }
        assertTrue(ex.message!!.contains("BITFINEX_API_KEY"))
    }

    @Test
    fun `throws when api secret missing`() {
        val ex = assertThrows(RuntimeException::class.java) { ApiCredentials(env = { null }).apiSecret() }
        assertTrue(ex.message!!.contains("BITFINEX_API_SECRET"))
    }

    @Test
    fun `a blank secret is treated as absent`() {
        // A blank secret still produces a well-formed HMAC, so without this it would be sent and
        // the venue would answer with an authentication error naming neither variable.
        val helper = ApiCredentials(env = { "   " })
        val ex = assertThrows(RuntimeException::class.java) { helper.apiSecret() }
        org.junit.jupiter.api.Assertions
            .assertTrue(ex.message!!.contains("BITFINEX_API_SECRET"))
    }
}
