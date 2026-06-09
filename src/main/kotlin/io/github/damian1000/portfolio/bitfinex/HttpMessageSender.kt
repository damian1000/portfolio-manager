package io.github.damian1000.portfolio.bitfinex

import org.apache.hc.client5.http.classic.methods.HttpPost
import org.apache.hc.client5.http.config.RequestConfig
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.core5.http.ContentType
import org.apache.hc.core5.http.io.HttpClientResponseHandler
import org.apache.hc.core5.http.io.entity.EntityUtils
import org.apache.hc.core5.http.io.entity.StringEntity
import org.apache.hc.core5.util.Timeout
import java.net.URI

class HttpMessageSender(
    private val client: CloseableHttpClient = defaultClient(),
) : AutoCloseable {

    fun sendPostMessage(
        url: String,
        nonce: String,
        apiKey: String,
        signedSignature: String,
        body: String,
    ): String {
        val request = HttpPost(url)
        request.addHeader("Content-Type", "application/json")
        request.addHeader("bfx-nonce", nonce)
        request.addHeader("bfx-apikey", apiKey)
        request.addHeader("bfx-signature", signedSignature)
        request.entity = StringEntity(body, ContentType.APPLICATION_JSON)
        return client.execute(request, HttpClientResponseHandler { response ->
            val responseBody = EntityUtils.toString(response.entity)
            if (response.code !in 200..299) {
                throw HttpRequestFailed(safeEndpoint(url), response.code, response.reasonPhrase)
            }
            responseBody
        })
    }

    override fun close() = client.close()

    private fun safeEndpoint(url: String): String =
        runCatching { URI(url).let { "${it.host}${it.path}" } }.getOrDefault("<unparseable>")

    companion object {
        private fun defaultClient(): CloseableHttpClient =
            HttpClients.custom()
                .setDefaultRequestConfig(
                    RequestConfig.custom()
                        .setConnectTimeout(Timeout.ofSeconds(2))
                        .setResponseTimeout(Timeout.ofSeconds(5))
                        .setConnectionRequestTimeout(Timeout.ofSeconds(1))
                        .build()
                )
                .build()
    }
}

class HttpRequestFailed(
    val endpoint: String,
    val status: Int,
    val reason: String?,
) : RuntimeException("HTTP $status ($reason) at $endpoint")
