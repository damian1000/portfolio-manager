package binance

import org.apache.commons.codec.binary.Hex
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class SigningHelper {

    fun sign(message: String, secret: String): String {
        return try {
            val sha256Hmac = Mac.getInstance("HmacSHA256")
            val secretKeySpec = SecretKeySpec(secret.toByteArray(), "HmacSHA256")
            sha256Hmac.init(secretKeySpec)
            String(Hex.encodeHex(sha256Hmac.doFinal(message.toByteArray())))
        } catch (e: Exception) {
            throw RuntimeException("Unable to sign message.", e)
        }
    }

}