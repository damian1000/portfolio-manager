package com.damianhoward.portfolio.binance

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.hc.core5.http.message.BasicNameValuePair
import org.apache.hc.core5.net.WWWFormCodec
import java.io.IOException
import java.nio.charset.Charset

class MessageSender(
    private val hmacSigner: HmacSigner = HmacSigner(),
    private val httpMessageSender: HttpMessageSender = HttpMessageSender(),
    private val mapper: ObjectMapper = ObjectMapper(),
    private val credentials: ApiCredentials = ApiCredentials(),
) {
    fun sendGetMessage(apiPath: String): String = sendGetMessage(apiPath, null)

    fun sendGetMessage(apiPath: String, dataClass: Any?): String {
        val allQueryParameters =
            if (dataClass != null) {
                val parameterMap = mapper.convertValue(dataClass, object : TypeReference<Map<String, Any>>() {})

                val nameValuePairs: List<BasicNameValuePair> =
                    parameterMap
                        .map { entry -> BasicNameValuePair(entry.key, entry.value.toString()) }

                val apiSecret = credentials.apiSecret()
                val queryParameters = WWWFormCodec.format(nameValuePairs, Charset.forName("UTF-8"))
                val signature = hmacSigner.sign(queryParameters, apiSecret)

                val allParameters = nameValuePairs.plus(BasicNameValuePair("signature", signature))
                "?${WWWFormCodec.format(allParameters, Charset.forName("UTF-8"))}"
            } else {
                ""
            }

        val apiKey = credentials.apiKey()
        val uri = "https://api.binance.com$apiPath$allQueryParameters"

        return try {
            httpMessageSender.sendGetMessage(uri, apiKey)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }
}
