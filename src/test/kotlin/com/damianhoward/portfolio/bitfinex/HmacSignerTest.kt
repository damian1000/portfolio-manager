package com.damianhoward.portfolio.bitfinex

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.matchesRegex
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.Test

class HmacSignerTest {
    private val signer = HmacSigner()

    @Test
    fun `produces stable HMAC-SHA384 hex for known input`() {
        // Bitfinex uses HMAC-SHA384 -> 96 hex chars
        val signed = signer.sign("/api/v2/auth/r/wallets1700000000{}", "secret")

        assertThat(
            signed,
            equalTo("511d7b472aa44e498639936680038a5361e7b665c46f8977d3f1c554d9ef929f9f6458e7432cef165e04e1efb0ce1cf2"),
        )
        assertThat(signed.length, equalTo(96))
        assertThat(signed, matchesRegex("[0-9a-f]{96}"))
    }

    @Test
    fun `different secrets yield different signatures`() {
        val message = "/api/v2/auth/r/wallets1700000000{}"
        assertThat(signer.sign(message, "secret-a"), not(equalTo(signer.sign(message, "secret-b"))))
    }
}
