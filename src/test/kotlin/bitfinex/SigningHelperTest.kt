package bitfinex

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.matchesRegex
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.Test

class SigningHelperTest {

    private val signer = SigningHelper()

    @Test
    fun `produces stable HMAC-SHA384 hex for known input`() {
        // Bitfinex uses HMAC-SHA384 -> 96 hex chars
        val signed = signer.sign("/api/v2/auth/r/wallets1700000000{}", "secret")

        assertThat(signed.length, equalTo(96))
        assertThat(signed, matchesRegex("[0-9a-f]{96}"))
    }

    @Test
    fun `different secrets yield different signatures`() {
        val message = "/api/v2/auth/r/wallets1700000000{}"
        assertThat(signer.sign(message, "secret-a"), not(equalTo(signer.sign(message, "secret-b"))))
    }
}
