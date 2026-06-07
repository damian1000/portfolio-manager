package io.github.damian1000.portfolio.binance

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class PropertyHelperTest {

    @Test
    fun `returns env values when present`() {
        val helper = PropertyHelper(env = mapOf(
            "BINANCE_API_KEY" to "k-123",
            "BINANCE_API_SECRET" to "s-abc",
        )::get)
        assertEquals("k-123", helper.getApiKey())
        assertEquals("s-abc", helper.getApiSecret())
    }

    @Test
    fun `throws when api key missing`() {
        val helper = PropertyHelper(env = { null })
        val ex = assertThrows(RuntimeException::class.java) { helper.getApiKey() }
        org.junit.jupiter.api.Assertions.assertTrue(ex.message!!.contains("BINANCE_API_KEY"))
    }

    @Test
    fun `throws when api secret missing`() {
        val helper = PropertyHelper(env = { null })
        val ex = assertThrows(RuntimeException::class.java) { helper.getApiSecret() }
        org.junit.jupiter.api.Assertions.assertTrue(ex.message!!.contains("BINANCE_API_SECRET"))
    }
}
