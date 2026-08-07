package io.github.damian1000.portfolio.binance

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.matchesRegex
import org.junit.jupiter.api.Test

class HmacSignerTest {
    private val signer = HmacSigner()

    @Test
    fun `produces stable HMAC-SHA256 hex for known input`() {
        // Vector verified against multiple implementations:
        // HMAC-SHA256(key="secret", msg="timestamp=1700000000") = 64-char hex
        val signed = signer.sign("timestamp=1700000000", "secret")

        assertThat(
            signed,
            equalTo("64e07dc8254f55df9a8b73101668ca102e119c2b712ca363d46ad635e1fa2c65"),
        )
        assertThat(signed.length, equalTo(64))
        assertThat(signed, matchesRegex("[0-9a-f]{64}"))
        // Deterministic
        assertThat(signer.sign("timestamp=1700000000", "secret"), equalTo(signed))
    }

    @Test
    fun `different secrets yield different signatures`() {
        val message = "symbol=BTCUSDT&side=BUY"
        val a = signer.sign(message, "secret-a")
        val b = signer.sign(message, "secret-b")
        assertThat(a, org.hamcrest.Matchers.not(equalTo(b)))
    }

    @Test
    fun `an empty secret fails with the JCE cause rather than a wrapper`() {
        // This used to catch Exception and rethrow RuntimeException("Unable to sign message."),
        // which replaced every distinct cause with one string. The signer now catches nothing, so
        // the reason survives. A blank secret is rejected earlier by ApiCredentials, where the
        // environment variable's name is still known — that is the message an operator needs.
        val ex =
            org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException::class.java) {
                signer.sign("anything", "")
            }
        assertThat(ex.message, equalTo("Empty key"))
    }
}
