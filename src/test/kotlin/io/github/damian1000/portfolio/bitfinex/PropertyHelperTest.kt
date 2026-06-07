package io.github.damian1000.portfolio.bitfinex

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PropertyHelperTest {

    @Test
    fun `returns env values when present`() {
        val helper = PropertyHelper(env = mapOf(
            "BITFINEX_API_KEY" to "bf-key",
            "BITFINEX_API_SECRET" to "bf-secret",
        )::get)
        assertEquals("bf-key", helper.getApiKey())
        assertEquals("bf-secret", helper.getApiSecret())
    }

    @Test
    fun `throws when api key missing`() {
        val ex = assertThrows(RuntimeException::class.java) { PropertyHelper(env = { null }).getApiKey() }
        assertTrue(ex.message!!.contains("BITFINEX_API_KEY"))
    }

    @Test
    fun `throws when api secret missing`() {
        val ex = assertThrows(RuntimeException::class.java) { PropertyHelper(env = { null }).getApiSecret() }
        assertTrue(ex.message!!.contains("BITFINEX_API_SECRET"))
    }
}
