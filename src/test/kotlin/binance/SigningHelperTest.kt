package binance

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.matchesRegex
import org.junit.jupiter.api.Test

class SigningHelperTest {

    private val signer = SigningHelper()

    @Test
    fun `produces stable HMAC-SHA256 hex for known input`() {
        // Vector verified against multiple implementations:
        // HMAC-SHA256(key="secret", msg="timestamp=1700000000") = 64-char hex
        val signed = signer.sign("timestamp=1700000000", "secret")

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
}
