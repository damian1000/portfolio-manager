package io.github.damian1000.portfolio.bitfinex

import org.apache.commons.codec.binary.Hex
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * HMAC-SHA384 over a request's [SignaturePayload], hex-encoded — the value Bitfinex expects in the
 * `bfx-signature` header. SHA384 is the venue's choice; Binance's signer is the same shape at
 * SHA256, and the two stay separate because they are two venues' contracts rather than one
 * concept with a parameter.
 *
 * Nothing is caught here deliberately. `Mac.getInstance` and `Mac.init` declare checked exceptions
 * that Kotlin does not require handling, and neither is reachable through this method: HmacSHA384
 * is a standard algorithm every JVM provides, and `InvalidKeyException` needs a key `SecretKeySpec`
 * would have rejected first. Catching them would claim a failure mode is handled that cannot
 * occur, and wrapping them in `RuntimeException` — as this did — discards the type and message
 * that would explain a genuinely broken JVM. A blank secret is a configuration error and is
 * rejected by [ApiCredentials], where the environment variable's name is still known.
 */
class HmacSigner {
    fun sign(message: String, apiSecret: String): String {
        val mac = Mac.getInstance(HMAC_SHA384_ALGORITHM)
        mac.init(SecretKeySpec(apiSecret.toByteArray(Charsets.UTF_8), HMAC_SHA384_ALGORITHM))
        return Hex.encodeHexString(mac.doFinal(message.toByteArray(Charsets.UTF_8)))
    }

    private companion object {
        const val HMAC_SHA384_ALGORITHM = "HmacSHA384"
    }
}
