package com.damianhoward.portfolio.bitfinex

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class BitfinexGatewayTest {
    private val messageSender = mock<MessageSender>()
    private val mapper = BitfinexMovementMapper()
    private val gateway =
        BitfinexGateway(
            messageSender = messageSender,
            bitfinexMovementMapper = mapper,
            paymentIdSupplier = { "fixed-payment-id" },
        )

    @Test
    fun `retrieveWallets hits the wallets endpoint`() {
        whenever(messageSender.sendMessage("v2/auth/r/wallets")).thenReturn("wallets-json")
        assertEquals("wallets-json", gateway.retrieveWallets())
    }

    @Test
    fun `retrieveMovementHistory threads currency into apiPath and parses result`() {
        whenever(messageSender.sendMessage("v2/auth/r/movements/BTC/hist")).thenReturn(
            """[[1,"BTC","BTC",null,null,1640995200000,1640995200000,null,null,"OK",null,null,"0.5",null,null,null,null,null,null,null,null,null]]""",
        )
        val movements = gateway.retrieveMovementHistory("BTC")
        assertEquals(1, movements.size)
        assertEquals(1L, movements[0].id)
    }

    @Test
    fun `retrieveSettingsForKey wraps the api key`() {
        val captor = argumentCaptor<BitfinexReadSettingKeys>()
        whenever(messageSender.sendMessage(eq("v2/auth/r/settings"), captor.capture())).thenReturn("settings")

        gateway.retrieveSettingsForKey("my-api-key")
        assertEquals(listOf("api:my-api-key"), captor.firstValue.keys)
    }

    @Test
    fun `default paymentIdSupplier produces a UUID per call`() {
        val captor = argumentCaptor<BitfinexWithdrawalRequest>()
        val defaultGateway = BitfinexGateway(messageSender = messageSender, bitfinexMovementMapper = mapper)
        whenever(messageSender.sendMessage(eq("v2/auth/w/withdraw"), captor.capture())).thenReturn("ok")

        defaultGateway.submitWithdrawalRequest(Currency.BTC, "0.10", "bc1qaddress")
        defaultGateway.submitWithdrawalRequest(Currency.BTC, "0.10", "bc1qaddress")

        val first = captor.firstValue.paymentId
        val second = captor.secondValue.paymentId
        assertEquals(36, first.length, "default supplier produces UUIDs")
        org.junit.jupiter.api.Assertions
            .assertNotEquals(first, second, "each call produces a fresh id")
    }

    @Test
    fun `submitWithdrawalRequest builds the withdrawal DTO from arguments`() {
        val captor = argumentCaptor<BitfinexWithdrawalRequest>()
        whenever(messageSender.sendMessage(eq("v2/auth/w/withdraw"), captor.capture())).thenReturn("submitted")

        val response = gateway.submitWithdrawalRequest(Currency.BTC, "0.10", "bc1qaddress")

        assertEquals("submitted", response)
        val req = captor.firstValue
        assertEquals("BITCOIN", req.method, "Bitfinex uses the descriptive currency code, not the ticker")
        assertEquals("0.10", req.amount)
        assertEquals("bc1qaddress", req.address)
        assertEquals("fixed-payment-id", req.paymentId)
        assertEquals(BitfinexWallet.exchange, req.wallet)
    }
}
