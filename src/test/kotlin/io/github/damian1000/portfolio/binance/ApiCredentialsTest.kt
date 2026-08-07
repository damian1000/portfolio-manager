package io.github.damian1000.portfolio.binance

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ApiCredentialsTest {
    @Test
    fun `returns env values when present`() {
        val helper =
            ApiCredentials(
                env = mapOf(
                    "BINANCE_API_KEY" to "k-123",
                    "BINANCE_API_SECRET" to "s-abc",
                )::get,
            )
        assertEquals("k-123", helper.apiKey())
        assertEquals("s-abc", helper.apiSecret())
    }

    @Test
    fun `throws when api key missing`() {
        val helper = ApiCredentials(env = { null })
        val ex = assertThrows(RuntimeException::class.java) { helper.apiKey() }
        org.junit.jupiter.api.Assertions
            .assertTrue(ex.message!!.contains("BINANCE_API_KEY"))
    }

    @Test
    fun `throws when api secret missing`() {
        val helper = ApiCredentials(env = { null })
        val ex = assertThrows(RuntimeException::class.java) { helper.apiSecret() }
        org.junit.jupiter.api.Assertions
            .assertTrue(ex.message!!.contains("BINANCE_API_SECRET"))
    }

    @Test
    fun `a blank secret is treated as absent`() {
        // A blank secret still produces a well-formed HMAC, so without this it would be sent and
        // the venue would answer with an authentication error naming neither variable.
        val helper = ApiCredentials(env = { "   " })
        val ex = assertThrows(RuntimeException::class.java) { helper.apiSecret() }
        org.junit.jupiter.api.Assertions
            .assertTrue(ex.message!!.contains("BINANCE_API_SECRET"))
    }
}
