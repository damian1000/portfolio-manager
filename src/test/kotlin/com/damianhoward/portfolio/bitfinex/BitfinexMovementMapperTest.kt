package com.damianhoward.portfolio.bitfinex

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneOffset

class BitfinexMovementMapperTest {
    private val mapper = BitfinexMovementMapper()

    @Test
    fun `maps single movement with full row`() {
        val json =
            """
            [[
              7, "BTC", "BTC",
              null, null,
              1640995200000, 1641081600000,
              null, null,
              "COMPLETED",
              null, null,
              "0.50", "0.0005",
              null, null,
              "bc1qaddress",
              null, null, null,
              "tx-xyz", "memo"
            ]]
            """.trimIndent()

        val m = mapper.mapMovement(json).single()
        assertEquals(7L, m.id)
        assertEquals("BTC", m.currencyCode)
        assertEquals(LocalDateTime.ofEpochSecond(1640995200, 0, ZoneOffset.UTC), m.createdTimestamp)
        assertEquals(LocalDateTime.ofEpochSecond(1641081600, 0, ZoneOffset.UTC), m.updatedTimestamp)
        assertEquals(BigDecimal("0.50"), m.amount)
        assertEquals(BigDecimal("0.0005"), m.fees)
        assertEquals("bc1qaddress", m.destinationAddress)
        assertEquals("tx-xyz", m.transactionId)
    }

    @Test
    fun `empty json array yields empty list`() {
        assertEquals(emptyList<Movement>(), mapper.mapMovement("[]"))
    }

    @Test
    fun `malformed json throws RuntimeException`() {
        assertThrows(RuntimeException::class.java) { mapper.mapMovement("not-json-at-all") }
    }

    @Test
    fun `short array missing required field throws`() {
        assertThrows(IllegalArgumentException::class.java) {
            mapper.mapMovement("""[[1, "BTC", "BTC"]]""")
        }
    }
}
