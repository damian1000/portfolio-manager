package io.github.damian1000.portfolio.bitfinex

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.io.IOException

class MessageSender {
    private val signatureHelper = SignatureHelper()
    private val signingHelper = SigningHelper()
    private val messageSender = HttpMessageSender()
    private val mapper = jacksonObjectMapper()
    private val propertyHelper = PropertyHelper()

    fun sendMessage(apiPath: String, message: Any): String {
        val body = createJson(message)
        return sendMessageInternal(apiPath, body)
    }

    fun sendMessage(apiPath: String): String {
        val body = "{}"
        return sendMessageInternal(apiPath, body)
    }

    private fun sendMessageInternal(apiPath: String, jsonBody: String): String {
        val apiKey = propertyHelper.getApiKey()
        val apiSecret = propertyHelper.getApiSecret();
        val nonce = (System.currentTimeMillis() * 1000).toString()
        val signature = signatureHelper.createSignatureHelper(jsonBody, nonce, apiPath)
        val signedSignature = signingHelper.sign(signature, apiSecret)
        val uri = String.format("https://api.bitfinex.com/%s", apiPath)
        System.out.printf("Sending %s to %s%n", jsonBody, apiPath)
        return try {
            messageSender.sendPostMessage(uri, nonce, apiKey, signedSignature, jsonBody)
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