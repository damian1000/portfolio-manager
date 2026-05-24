package bitfinex

import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class SigningHelper {

    private val HMAC_SHA1_ALGORITHM = "HmacSHA384"

    fun sign(message: String, apiSecret: String): String {
        return try {
            val mac = Mac.getInstance(HMAC_SHA1_ALGORITHM)
            mac.init(SecretKeySpec(apiSecret.toByteArray(), HMAC_SHA1_ALGORITHM))
            bytesToHex(mac.doFinal(message.toByteArray()))
        } catch (e: InvalidKeyException) {
            throw RuntimeException(e)
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException(e)
        }
    }

    private fun bytesToHex(hash: ByteArray): String {
        val hexString = StringBuffer()
        for (b in hash) {
            val hex = Integer.toHexString(0xff and b.toInt())
            if (hex.length == 1) {
                hexString.append('0')
            }
            hexString.append(hex)
        }
        return hexString.toString()
    }

}