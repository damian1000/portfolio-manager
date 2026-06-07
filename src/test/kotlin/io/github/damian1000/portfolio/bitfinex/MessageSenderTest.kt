package io.github.damian1000.portfolio.bitfinex

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.IOException

class MessageSenderTest {

    private val propertyHelper = PropertyHelper(env = mapOf(
        "BITFINEX_API_KEY" to "bk", "BITFINEX_API_SECRET" to "bs",
    )::get)
    private val http = mock<HttpMessageSender>()
    private val sender = MessageSender(
        signatureHelper = SignatureHelper(),
        signingHelper = SigningHelper(),
        httpMessageSender = http,
        mapper = jacksonObjectMapper(),
        propertyHelper = propertyHelper,
        nonceSupplier = { "1700000000000000" },
    )

    @Test
    fun `bare path POSTs with stable nonce and api headers`() {
        val urlCaptor = argumentCaptor<String>()
        val nonceCaptor = argumentCaptor<String>()
        val keyCaptor = argumentCaptor<String>()
        val sigCaptor = argumentCaptor<String>()
        val bodyCaptor = argumentCaptor<String>()
        whenever(http.sendPostMessage(urlCaptor.capture(), nonceCaptor.capture(), keyCaptor.capture(), sigCaptor.capture(), bodyCaptor.capture()))
            .thenReturn("[]")

        assertEquals("[]", sender.sendMessage("v2/auth/r/wallets"))
        assertEquals("https://api.bitfinex.com/v2/auth/r/wallets", urlCaptor.firstValue)
        assertEquals("1700000000000000", nonceCaptor.firstValue)
        assertEquals("bk", keyCaptor.firstValue)
        assertEquals("{}", bodyCaptor.firstValue)
    }

    @Test
    fun `message with body serializes via jackson`() {
        val bodyCaptor = argumentCaptor<String>()
        whenever(http.sendPostMessage(any(), any(), any(), any(), bodyCaptor.capture())).thenReturn("ok")

        sender.sendMessage("v2/auth/r/settings", BitfinexReadSettingKeys(listOf("api:foo")))

        val body = bodyCaptor.firstValue
        assertTrue(body.contains("\"keys\""))
        assertTrue(body.contains("api:foo"))
    }

    @Test
    fun `default nonceSupplier produces a microsecond-precision timestamp`() {
        val nonceCaptor = argumentCaptor<String>()
        val defaultSender = MessageSender(
            signatureHelper = SignatureHelper(),
            signingHelper = SigningHelper(),
            httpMessageSender = http,
            propertyHelper = propertyHelper,
        )
        whenever(http.sendPostMessage(any(), nonceCaptor.capture(), any(), any(), any())).thenReturn("[]")

        defaultSender.sendMessage("v2/auth/r/wallets")

        val nonce = nonceCaptor.firstValue
        assertTrue(nonce.length >= 13, "default supplier returns micros so it should be ~16 digits")
        assertTrue(nonce.all { it.isDigit() })
    }

    @Test
    fun `IOException from http layer becomes RuntimeException`() {
        whenever(http.sendPostMessage(any(), any(), any(), any(), any()))
            .thenAnswer { throw IOException("network") }
        assertThrows(RuntimeException::class.java) { sender.sendMessage("v2/auth/r/wallets") }
    }
}
