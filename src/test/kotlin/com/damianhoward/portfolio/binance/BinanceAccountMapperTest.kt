package com.damianhoward.portfolio.binance

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/**
 * The mapping contract against Binance's `/api/v3/account` shape.
 *
 * The fixtures are built from the venue's documented response rather than captured from a live
 * call — this repository's tests reach no network and hold no credentials. That is a real
 * limitation and worth naming: they prove the mapping matches the shape as documented, not that
 * the venue still sends that shape. Only a live read proves the second, and the first is what
 * catches a field rename on our side, which is what the previous constructor round-trips could
 * never do.
 */
class BinanceAccountMapperTest {
    private val mapper = BinanceAccountMapper()

    private val accountJson =
        """
        {
          "makerCommission": 15,
          "takerCommission": 15,
          "canTrade": true,
          "canWithdraw": true,
          "accountType": "SPOT",
          "balances": [
            {"asset":"BTC","free":"0.50000000","locked":"0.10000000"},
            {"asset":"ETH","free":"1.25000000","locked":"0.00000000"},
            {"asset":"XRP","free":"0.00000000","locked":"0.00000000"}
          ],
          "permissions": ["SPOT"]
        }
        """.trimIndent()

    @Test
    fun `balances map with their exact decimals`() {
        val balances = mapper.mapAccount(accountJson).balances!!

        assertEquals(3, balances.size)
        assertEquals(AssetBalance("BTC", BigDecimal("0.50000000"), BigDecimal("0.10000000")), balances[0])
        assertEquals(BigDecimal("1.25000000"), balances[1].free)
    }

    @Test
    fun `total is free plus locked, because a committed balance is still held`() {
        val btc = mapper.mapAccount(accountJson).balances!!.first { it.asset == "BTC" }

        assertEquals(BigDecimal("0.60000000"), btc.total)
    }

    @Test
    fun `an asset with nothing in it is not reported as a holding`() {
        // Binance returns a row per listed asset, so the response is mostly zeroes.
        val held = mapper.mapAccount(accountJson).nonZeroBalances()

        assertEquals(listOf("BTC", "ETH"), held.map { it.asset })
    }

    @Test
    fun `a field the venue adds is ignored rather than fatal`() {
        // The account document grows over time and none of it is ours to track.
        val withNewField = accountJson.replace(""""accountType": "SPOT",""", """"accountType":"SPOT","brokered":false,""")

        assertEquals(3, mapper.mapAccount(withNewField).balances!!.size)
    }

    @Test
    fun `a renamed balance field fails the mapping rather than reading as zero`() {
        // The case the old constructor round-trips could not catch: they never mapped any JSON, so
        // a rename broke production and left the test suite green.
        val renamed = accountJson.replace(""""free":""", """"available":""")

        val e = assertThrows(IllegalArgumentException::class.java) { mapper.mapAccount(renamed) }
        assertEquals("Binance account response did not parse", e.message)
    }

    @Test
    fun `the failure carries no payload, because the payload is account balances`() {
        val e = assertThrows(IllegalArgumentException::class.java) { mapper.mapAccount("""{"balances":[{"asset":1}]}""") }

        assertTrue(e.message!!.none { it.isDigit() }, "a balance must not reach the message: ${e.message}")
    }

    @Test
    fun `an account with no balances field is not an account with no balances`() {
        // Absent and empty are different facts; collapsing them would let a shape change read as a
        // flat account, which is the one wrong answer nobody would question.
        val account = mapper.mapAccount("""{"accountType":"SPOT"}""")

        assertNull(account.balances)
        assertTrue(account.nonZeroBalances().isEmpty())
    }
}
