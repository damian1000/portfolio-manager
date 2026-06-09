package io.github.damian1000.portfolio

import io.github.damian1000.portfolio.binance.Account
import io.github.damian1000.portfolio.binance.AssetBalance
import io.github.damian1000.portfolio.binance.BinanceAccountRequest
import io.github.damian1000.portfolio.bitfinex.BitfinexReadSettingKeys
import io.github.damian1000.portfolio.bitfinex.BitfinexWallet
import io.github.damian1000.portfolio.bitfinex.BitfinexWithdrawalRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDateTime
import io.github.damian1000.portfolio.binance.Currency as BinanceCurrency
import io.github.damian1000.portfolio.binance.Movement as BinanceMovement
import io.github.damian1000.portfolio.bitfinex.Currency as BitfinexCurrency
import io.github.damian1000.portfolio.bitfinex.Movement as BitfinexMovement

/**
 * Round-trips the wire-shape models. These are largely Jackson-bound DTOs;
 * exercising the constructors and accessors here makes sure a field rename
 * shows up as a clear test failure rather than as a silent JSON-mapping break.
 */
class PojoTest {
    @Test
    fun `binance account holds asset balances`() {
        val balances = listOf(AssetBalance("BTC", BigDecimal("0.5"), BigDecimal.ZERO))
        val account = Account(balances)
        assertEquals(balances, account.balances)
        assertNotNull(Account(null))
    }

    @Test
    fun `binance asset balance round trip`() {
        val a = AssetBalance("ETH", BigDecimal("1.25"), BigDecimal("0.10"))
        assertEquals("ETH", a.asset)
        assertEquals(BigDecimal("1.25"), a.free)
        assertEquals(BigDecimal("0.10"), a.locked)
    }

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
    fun `binance currency enum has codes`() {
        // Just iterate to keep the enum live in coverage.
        for (c in BinanceCurrency.entries) assertNotNull(c.code)
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
