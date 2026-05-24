package binance

import okhttp3.*
import java.io.IOException
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class BinanceApiClient {
    private val baseUrl = "https://api.binance.com/api/v3"
    private val client = OkHttpClient()

    fun getWalletBalance(apiKey: String, secretKey: String) {
        val url = "$baseUrl/account"

        val timestamp = System.currentTimeMillis()
        val signature = generateSignature(secretKey, "timestamp=$timestamp")

        val request = Request.Builder()
            .url(url)
            .addHeader("X-MBX-APIKEY", apiKey)
            .addHeader("Content-Type", "application/json")
            .addHeader("timestamp", timestamp.toString())
            .addHeader("signature", signature)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val jsonResponse = response.body.toString()
                println(jsonResponse)
            }

            override fun onFailure(call: Call, e: IOException) {
                println("Failed to retrieve wallet balance")
            }
        })
    }

    private fun generateSignature(secretKey: String, queryString: String): String {
        val hmac = Mac.getInstance("HmacSHA256")
        val secretKeySpec = SecretKeySpec(secretKey.toByteArray(), "HmacSHA256")
        hmac.init(secretKeySpec)
        return hmac.doFinal(queryString.toByteArray()).toHexString()
    }
}

fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }

fun main() {
    val binanceApiClient = BinanceApiClient()
    val apiKey = "YOUR_API_KEY"
    val secretKey = "YOUR_SECRET_KEY"
    binanceApiClient.getWalletBalance(apiKey, secretKey)
}
