package com.damianhoward.portfolio.bitfinex

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/**
 * The mapping contract against `v2/auth/r/wallets`, which answers with positional arrays.
 *
 * The fixtures are built from the venue's documented response rather than captured from a live
 * call — this repository's tests reach no network and hold no credentials. They prove the mapping
 * matches the shape as documented, not that the venue still sends it; only a live read proves the
 * second.
 */
class BitfinexWalletMapperTest {
    private val mapper = BitfinexWalletMapper()

    // [WALLET_TYPE, CURRENCY, BALANCE, UNSETTLED_INTEREST, AVAILABLE_BALANCE, LAST_CHANGE, ...]
    private val walletsJson =
        """
        [
          ["exchange","BTC",0.6,0,0.5,null,null],
          ["exchange","ETH",1.25,0,1.25,null,null],
          ["margin","BTC",0.1,0,null,null,null]
        ]
        """.trimIndent()

    @Test
    fun `each wallet type is its own row, because a currency can sit in more than one`() {
        val wallets = mapper.mapWallets(walletsJson)

        assertEquals(3, wallets.size)
        assertEquals(listOf("exchange", "exchange", "margin"), wallets.map { it.type })
        assertEquals(BigDecimal("0.6"), wallets[0].balance)
        assertEquals(BigDecimal("0.5"), wallets[0].availableBalance)
    }

    @Test
    fun `an available balance the venue has not calculated stays null rather than becoming zero`() {
        // Zero says "nothing can be moved", null says "the venue has not said yet". A withdrawal
        // decision reads that field, so collapsing them would turn "unknown" into a hard refusal.
        val margin = mapper.mapWallets(walletsJson).single { it.type == "margin" }

        assertNull(margin.availableBalance)
    }

    @Test
    fun `trailing fields the venue adds are ignored`() {
        val extended = """[["exchange","BTC",0.6,0,0.5,null,null,"something new",42]]"""

        assertEquals(BigDecimal("0.6"), mapper.mapWallets(extended).single().balance)
    }

    @Test
    fun `a row missing a required field fails rather than mapping to a plausible wrong number`() {
        // The failure mode positional arrays actually have: a field removed from the middle shifts
        // every index after it, so the balance silently becomes whatever now sits at index 2.
        val truncated = """[["exchange","BTC"]]"""

        val e = assertThrows(IllegalArgumentException::class.java) { mapper.mapWallets(truncated) }
        assertEquals("Bitfinex wallet field 2 is missing", e.message)
    }

    @Test
    fun `a response that is not the documented shape fails without repeating the payload`() {
        val e = assertThrows(IllegalArgumentException::class.java) { mapper.mapWallets("""{"error":"nope"}""") }

        assertEquals("Bitfinex wallets response did not parse", e.message)
        assertTrue(e.message!!.none { it.isDigit() })
    }

    @Test
    fun `an empty account maps to no wallets rather than failing`() {
        assertTrue(mapper.mapWallets("[]").isEmpty())
    }
}
