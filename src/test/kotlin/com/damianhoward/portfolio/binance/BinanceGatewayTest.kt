package com.damianhoward.portfolio.binance

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class BinanceGatewayTest {
    private val messageSender = mock<MessageSender>()
    private val gateway = BinanceGateway(messageSender)

    @Test
    fun `retrieveWallets sends a signed account request`() {
        val captor = argumentCaptor<Any>()
        whenever(messageSender.sendGetMessage(eq("/api/v3/account"), captor.capture())).thenReturn("acc")

        assertEquals("acc", gateway.retrieveWallets())
        val req = captor.firstValue as BinanceAccountRequest
        assertEquals(60_000L, req.recvWindow)
        assertTrue(req.timestamp > 0L)
    }

    @Test
    fun `retrieveSystem hits the public system status endpoint without params`() {
        whenever(messageSender.sendGetMessage("/sapi/v1/system/status")).thenReturn("up")
        assertEquals("up", gateway.retrieveSystem())
        verify(messageSender).sendGetMessage("/sapi/v1/system/status")
    }
}
