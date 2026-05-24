package binance

import org.apache.hc.client5.http.classic.methods.HttpGet
import org.apache.hc.client5.http.classic.methods.HttpPost
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.core5.http.ContentType
import org.apache.hc.core5.http.io.HttpClientResponseHandler
import org.apache.hc.core5.http.io.entity.EntityUtils
import org.apache.hc.core5.http.io.entity.StringEntity

class HttpMessageSender {

    fun sendGetMessage(url: String, apiKey: String): String {
        val request = HttpGet(url)
        request.addHeader("X-MBX-APIKEY", apiKey)
        return HttpClients.createDefault().use { client ->
            client.execute(request, asString(url))
        }
    }

    fun sendPostMessage(
        url: String,
        nonce: String,
        apiKey: String,
        signedSignature: String,
        body: String
    ): String {
        val request = HttpPost(url)
        request.addHeader("Content-Type", ContentType.APPLICATION_JSON.mimeType)
        request.addHeader("bfx-nonce", nonce)
        request.addHeader("bfx-apikey", apiKey)
        request.addHeader("bfx-signature", signedSignature)
        request.entity = StringEntity(body, ContentType.APPLICATION_JSON)
        return HttpClients.createDefault().use { client ->
            client.execute(request, asString(url))
        }
    }

    private fun asString(url: String): HttpClientResponseHandler<String> =
        HttpClientResponseHandler { response ->
            val body = EntityUtils.toString(response.entity)
            if (response.code !in 200..299) {
                throw RuntimeException("HTTP $url failed: ${response.code} ${response.reasonPhrase} - $body")
            }
            body
        }
}
