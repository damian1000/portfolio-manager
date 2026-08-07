package io.github.damian1000.portfolio.bitfinex

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * The concatenation Bitfinex rebuilds server-side before comparing HMACs. It had no test, which
 * for an authentication contract means a wrong order would have surfaced as a venue rejecting
 * every request rather than as a failing build.
 */
class SignaturePayloadTest {
    private val payload = SignaturePayload()

    @Test
    fun `concatenates the api prefix, path, nonce and body in that order`() {
        assertEquals(
            "/api/v2/auth/r/wallets1700000000000{\"key\":\"value\"}",
            payload.of(body = "{\"key\":\"value\"}", nonce = "1700000000000", path = "v2/auth/r/wallets"),
        )
    }

    @Test
    fun `an empty body still produces the prefix, path and nonce`() {
        // MessageSender sends "{}" rather than "" for a bodyless call, but the builder itself must
        // not quietly depend on that.
        assertEquals("/api/v2/auth/r/wallets1700000000000", payload.of(body = "", nonce = "1700000000000", path = "v2/auth/r/wallets"))
    }

    @Test
    fun `nothing separates the parts`() {
        // A separator would authenticate a different string than the venue signs, so this pins the
        // absence rather than leaving it to the reader of a format string.
        assertEquals("/api/PNB", payload.of(body = "B", nonce = "N", path = "P"))
    }

    @Test
    fun `a different nonce yields a different payload`() {
        val first = payload.of(body = "{}", nonce = "1", path = "v2/auth/r/wallets")
        val second = payload.of(body = "{}", nonce = "2", path = "v2/auth/r/wallets")
        assertEquals(false, first == second, "the nonce must reach the signed string, or replay protection is decorative")
    }
}
