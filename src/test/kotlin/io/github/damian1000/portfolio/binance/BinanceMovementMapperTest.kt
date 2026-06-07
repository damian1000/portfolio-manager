package io.github.damian1000.portfolio.binance

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneOffset

class BinanceMovementMapperTest {

    private val mapper = BinanceMovementMapper()

    @Test
    fun `maps a single transfer with all fields populated`() {
        // 22 columns wide to cover NOTE_INDEX (21). The numeric / nullable
        // optionals are placed at the indexes the mapper actually reads from.
        // 1640995200000ms = 2022-01-01T00:00:00Z
        // 1641081600000ms = 2022-01-02T00:00:00Z
        val json = """
            [[
              42, "USDC", "USDC-erc20",
              null, null,
              1640995200000, 1641081600000,
              null, null,
              "COMPLETED",
              null, null,
              "100.50", "0.25",
              null, null,
              "0xDestAddr",
              null, null, null,
              "tx-abc", "Withdrawal note"
            ]]
        """.trimIndent()

        val movements = mapper.mapMovement(json)
        assertEquals(1, movements.size)
        val m = movements[0]
        assertEquals(42L, m.id)
        assertEquals("USDC", m.currencyCode)
        assertEquals("USDC-erc20", m.currencyValue)
        assertEquals(LocalDateTime.ofEpochSecond(1640995200, 0, ZoneOffset.UTC), m.createdTimestamp)
        assertEquals(LocalDateTime.ofEpochSecond(1641081600, 0, ZoneOffset.UTC), m.updatedTimestamp)
        assertEquals("COMPLETED", m.status)
        assertEquals(BigDecimal("100.50"), m.amount)
        assertEquals(BigDecimal("0.25"), m.fees)
        assertEquals("0xDestAddr", m.destinationAddress)
        assertEquals("tx-abc", m.transactionId)
        assertEquals("Withdrawal note", m.withdrawTransactionNote)
    }

    @Test
    fun `null optional fields parse as null`() {
        val json = """
            [[
              99, null, null,
              null, null,
              1640995200000, 1640995200000,
              null, null,
              null,
              null, null,
              null, null,
              null, null,
              null,
              null, null, null,
              null, null
            ]]
        """.trimIndent()

        val movements = mapper.mapMovement(json)
        val m = movements.single()
        assertEquals(99L, m.id)
        assertNull(m.currencyCode)
        assertNull(m.amount)
        assertNull(m.destinationAddress)
        assertNull(m.transactionId)
    }

    @Test
    fun `empty json array yields empty list`() {
        assertEquals(emptyList<Movement>(), mapper.mapMovement("[]"))
    }

    @Test
    fun `multiple movements parse independently`() {
        val json = """
            [
              [1, "A", "A", null, null, 1640995200000, 1640995200000, null, null, "OK", null, null, "1.00", null, null, null, null, null, null, null, null, null],
              [2, "B", "B", null, null, 1640995200000, 1640995200000, null, null, "OK", null, null, "2.00", null, null, null, null, null, null, null, null, null]
            ]
        """.trimIndent()
        val movements = mapper.mapMovement(json)
        assertEquals(2, movements.size)
        assertEquals(1L, movements[0].id)
        assertEquals(2L, movements[1].id)
    }

    @Test
    fun `malformed json throws RuntimeException`() {
        assertThrows(RuntimeException::class.java) { mapper.mapMovement("{not json") }
    }

    @Test
    fun `short array missing required field throws`() {
        // Missing required CREATED_TIMESTAMP_INDEX (5) — array has only 3 elements.
        val json = """[[1, "USDC", "USDC"]]"""
        assertThrows(IllegalArgumentException::class.java) { mapper.mapMovement(json) }
    }
}
