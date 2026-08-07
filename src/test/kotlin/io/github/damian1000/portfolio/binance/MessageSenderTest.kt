package io.github.damian1000.portfolio.binance

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.IOException

class MessageSenderTest {
    private val credentials =
        ApiCredentials(
            env = mapOf(
                "BINANCE_API_KEY" to "test-key",
                "BINANCE_API_SECRET" to "test-secret",
            )::get,
        )
    private val httpSender = mock<HttpMessageSender>()
    private val sender =
        MessageSender(
            hmacSigner = HmacSigner(),
            httpMessageSender = httpSender,
            mapper = ObjectMapper(),
            credentials = credentials,
        )

    @Test
    fun `GET without params hits the bare api path`() {
        whenever(httpSender.sendGetMessage(eq("https://api.binance.com/sapi/v1/system/status"), eq("test-key")))
            .thenReturn("{\"status\":0}")

        assertEquals("{\"status\":0}", sender.sendGetMessage("/sapi/v1/system/status"))
    }

    @Test
    fun `GET with params signs the query string and appends signature`() {
        val urlCaptor = argumentCaptor<String>()
        whenever(httpSender.sendGetMessage(urlCaptor.capture(), eq("test-key"))).thenReturn("ok")

        sender.sendGetMessage("/api/v3/account", BinanceAccountRequest(60_000, 1_700_000_000_000L))

        val url = urlCaptor.firstValue
        assertTrue(url.startsWith("https://api.binance.com/api/v3/account?"))
        assertTrue(url.contains("recvWindow=60000"))
        assertTrue(url.contains("timestamp=1700000000000"))
        assertTrue(url.contains("signature="))
    }

    @Test
    fun `IOException from http layer becomes RuntimeException`() {
        whenever(httpSender.sendGetMessage(any(), any())).thenAnswer { throw IOException("boom") }
        assertThrows(RuntimeException::class.java) { sender.sendGetMessage("/sapi/v1/system/status") }
    }

    @Test
    fun `api key header is sent on every request`() {
        whenever(httpSender.sendGetMessage(any(), any())).thenReturn("")
        sender.sendGetMessage("/sapi/v1/system/status")
        verify(httpSender).sendGetMessage(any(), eq("test-key"))
    }
}
