package io.github.damian1000.portfolio.bitfinex

import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class SigningHelper {

    fun sign(message: String, apiSecret: String): String = try {
        val mac = Mac.getInstance(HMAC_SHA384_ALGORITHM)
        mac.init(SecretKeySpec(apiSecret.toByteArray(Charsets.UTF_8), HMAC_SHA384_ALGORITHM))
        bytesToHex(mac.doFinal(message.toByteArray(Charsets.UTF_8)))
    } catch (e: InvalidKeyException) {
        throw RuntimeException(e)
    } catch (e: NoSuchAlgorithmException) {
        throw RuntimeException(e)
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

    private companion object {
        const val HMAC_SHA384_ALGORITHM = "HmacSHA384"
    }
}
