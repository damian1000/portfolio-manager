package io.github.damian1000.portfolio.bitfinex

class SignatureHelper {
    fun createSignatureHelper(body: String, nonce: String, path: String): String = String.format("/api/%s%s%s", path, nonce, body)
}
