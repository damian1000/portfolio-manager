package io.github.damian1000.portfolio.bitfinex

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.io.IOException

class MessageSender(
    private val signatureHelper: SignatureHelper = SignatureHelper(),
    private val signingHelper: SigningHelper = SigningHelper(),
    private val httpMessageSender: HttpMessageSender = HttpMessageSender(),
    private val mapper: ObjectMapper = jacksonObjectMapper(),
    private val propertyHelper: PropertyHelper = PropertyHelper(),
    private val nonceSupplier: () -> String = { (System.currentTimeMillis() * 1000).toString() },
) {

    fun sendMessage(apiPath: String, message: Any): String {
        val body = createJson(message)
        return sendMessageInternal(apiPath, body)
    }

    fun sendMessage(apiPath: String): String {
        return sendMessageInternal(apiPath, "{}")
    }

    private fun sendMessageInternal(apiPath: String, jsonBody: String): String {
        val apiKey = propertyHelper.getApiKey()
        val apiSecret = propertyHelper.getApiSecret()
        val nonce = nonceSupplier()
        val signature = signatureHelper.createSignatureHelper(jsonBody, nonce, apiPath)
        val signedSignature = signingHelper.sign(signature, apiSecret)
        val uri = "https://api.bitfinex.com/$apiPath"
        return try {
            httpMessageSender.sendPostMessage(uri, nonce, apiKey, signedSignature, jsonBody)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    private fun createJson(message: Any): String {
        return try {
            mapper.writeValueAsString(message)
        } catch (e: JsonProcessingException) {
            throw RuntimeException(e)
        }
    }
}
