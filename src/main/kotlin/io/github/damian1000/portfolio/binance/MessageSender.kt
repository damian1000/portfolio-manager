package io.github.damian1000.portfolio.binance

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.hc.core5.http.message.BasicNameValuePair
import org.apache.hc.core5.net.WWWFormCodec
import java.io.IOException
import java.nio.charset.Charset

class MessageSender {
    private val signingHelper = SigningHelper()
    private val messageSender = HttpMessageSender()
    private val mapper = ObjectMapper()
    private val propertyHelper = PropertyHelper()

    fun sendGetMessage(apiPath: String): String {
        return sendGetMessage(apiPath, null)
    }

    fun sendGetMessage(apiPath: String, dataClass: Any?): String {
        val allQueryParameters = if (dataClass != null) {
            val parameterMap = mapper.convertValue(dataClass, object : TypeReference<Map<String, Any>>() {})

            val nameValuePairs: List<BasicNameValuePair> = parameterMap
                .map { entry -> BasicNameValuePair(entry.key, entry.value.toString()) }

            val apiSecret = propertyHelper.getApiSecret();
            val queryParameters = WWWFormCodec.format(nameValuePairs, Charset.forName("UTF-8"))
            val signature = signingHelper.sign(queryParameters, apiSecret)

            val allParameters = nameValuePairs.plus(BasicNameValuePair("signature", signature))
            "?${WWWFormCodec.format(allParameters, Charset.forName("UTF-8"))}"
        } else {
            ""
        }

        val apiKey = propertyHelper.getApiKey();
        val uri = "https://api.binance.com$apiPath$allQueryParameters"
        println("Sending $uri")

        return try {
            messageSender.sendGetMessage(uri, apiKey)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    fun sendPostMessage(apiPath: String, message: Any): String {
//        val body = createJson(message)
//        return sendPostMessageInternal(apiPath, body)
        throw UnsupportedOperationException("Binance POST is not implemented for $apiPath")
    }

//    private fun sendPostMessageInternal(apiPath: String, jsonBody: String): String {
//        val apiKey = propertyHelper.getApiKey()
//        val apiSecret = propertyHelper.getApiSecret();
//        val nonce = (System.currentTimeMillis() * 1000).toString()
//        val signature = signatureHelper.createSignatureHelper(jsonBody, nonce, apiPath)
//        val signedSignature = signingHelper.sign(signature, apiSecret)
//        val uri = String.format("https://api.binance.com/%s", apiPath)
//        println("Sending $jsonBody to $apiPath")
//        return try {
//            messageSender.sendPostMessage(uri, nonce, apiKey, signedSignature, jsonBody)
//        } catch (e: IOException) {
//            throw RuntimeException(e)
//        }
//    }

}
