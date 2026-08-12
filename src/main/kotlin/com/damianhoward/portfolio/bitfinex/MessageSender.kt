package com.damianhoward.portfolio.bitfinex

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.io.IOException

class MessageSender(
    private val signaturePayload: SignaturePayload = SignaturePayload(),
    private val hmacSigner: HmacSigner = HmacSigner(),
    private val httpMessageSender: HttpMessageSender = HttpMessageSender(),
    private val mapper: ObjectMapper = jacksonObjectMapper(),
    private val credentials: ApiCredentials = ApiCredentials(),
    private val nonceSupplier: () -> String = MonotonicNonce()::next,
) {
    fun sendMessage(apiPath: String, message: Any): String {
        val body = createJson(message)
        return sendMessageInternal(apiPath, body)
    }

    fun sendMessage(apiPath: String): String = sendMessageInternal(apiPath, "{}")

    private fun sendMessageInternal(apiPath: String, jsonBody: String): String {
        val apiKey = credentials.apiKey()
        val apiSecret = credentials.apiSecret()
        val nonce = nonceSupplier()
        val signature = signaturePayload.of(jsonBody, nonce, apiPath)
        val signedSignature = hmacSigner.sign(signature, apiSecret)
        val uri = "https://api.bitfinex.com/$apiPath"
        return try {
            httpMessageSender.sendPostMessage(uri, nonce, apiKey, signedSignature, jsonBody)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    private fun createJson(message: Any): String = try {
        mapper.writeValueAsString(message)
    } catch (e: JsonProcessingException) {
        throw RuntimeException(e)
    }
}
