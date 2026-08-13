package com.damianhoward.portfolio

import com.damianhoward.portfolio.binance.BinanceAccountRequest
import com.damianhoward.portfolio.bitfinex.BitfinexReadSettingKeys
import com.damianhoward.portfolio.bitfinex.BitfinexWallet
import com.damianhoward.portfolio.bitfinex.BitfinexWithdrawalRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDateTime
import com.damianhoward.portfolio.binance.Movement as BinanceMovement
import com.damianhoward.portfolio.bitfinex.Currency as BitfinexCurrency
import com.damianhoward.portfolio.bitfinex.Movement as BitfinexMovement

/**
 * Request-shape models that are serialised outward, where the constructor and its accessors are
 * the whole contract.
 *
 * The wallet models used to be here too, as constructor round-trips, on the stated grounds that
 * they would make a field rename fail. They could not: nothing in them mapped any JSON, so a
 * rename broke the venue mapping and left this suite green. Those live in
 * [com.damianhoward.portfolio.binance.BinanceAccountMapperTest] and
 * [com.damianhoward.portfolio.bitfinex.BitfinexWalletMapperTest] now, against real response
 * shapes, which is what actually catches it.
 */
class PojoTest {
    @Test
    fun `binance account request carries recvWindow and timestamp`() {
        val req = BinanceAccountRequest(60_000L, 1_700_000_000_000L)
        assertEquals(60_000L, req.recvWindow)
        assertEquals(1_700_000_000_000L, req.timestamp)
    }

    @Test
    fun `binance movement defaults nullable, can be set`() {
        val m = BinanceMovement()
        assertEquals(null, m.id)
        m.id = 1L
        m.amount = BigDecimal("100")
        m.createdTimestamp = LocalDateTime.now()
        assertEquals(1L, m.id)
        assertEquals(BigDecimal("100"), m.amount)
        assertNotNull(m.createdTimestamp)
    }

    @Test
    fun `bitfinex movement and wallet and withdrawal request round trip`() {
        val m = BitfinexMovement()
        m.currencyCode = "BTC"
        assertEquals("BTC", m.currencyCode)

        val wallet = BitfinexWallet.exchange
        assertNotNull(wallet)

        val withdrawal =
            BitfinexWithdrawalRequest(
                wallet = wallet,
                method = "BTC",
                amount = "0.10",
                address = "bc1qaddress",
                paymentId = "pid-1",
            )
        assertEquals("BTC", withdrawal.method)
        assertEquals(wallet, withdrawal.wallet)
    }

    @Test
    fun `bitfinex currency enum and read-setting-keys carry data`() {
        for (c in BitfinexCurrency.entries) assertNotNull(c.code)
        val keys = BitfinexReadSettingKeys(listOf("api:read"))
        assertEquals(listOf("api:read"), keys.keys)
    }
}
