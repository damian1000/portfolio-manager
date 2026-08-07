package io.github.damian1000.portfolio.binance

import org.apache.commons.codec.binary.Hex
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * HMAC-SHA256 over a request's query string, hex-encoded — the `signature` parameter Binance
 * appends to an authenticated request. Bitfinex's signer is the same shape at SHA384, and the two
 * stay separate because they are two venues' contracts rather than one concept with a parameter.
 *
 * Nothing is caught here deliberately. `Mac.getInstance` and `Mac.init` declare checked exceptions
 * that Kotlin does not require handling, and neither is reachable through this method: HmacSHA256
 * is a standard algorithm every JVM provides, and `InvalidKeyException` needs a key `SecretKeySpec`
 * would have rejected first. This previously caught `Exception` and rethrew `RuntimeException`,
 * which turned any failure into one message and discarded the cause's type. A blank secret is a
 * configuration error and is rejected by [ApiCredentials], where the variable's name is known.
 */
class HmacSigner {
    fun sign(message: String, secret: String): String {
        val mac = Mac.getInstance(HMAC_SHA256_ALGORITHM)
        mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), HMAC_SHA256_ALGORITHM))
        return Hex.encodeHexString(mac.doFinal(message.toByteArray(Charsets.UTF_8)))
    }

    private companion object {
        const val HMAC_SHA256_ALGORITHM = "HmacSHA256"
    }
}
