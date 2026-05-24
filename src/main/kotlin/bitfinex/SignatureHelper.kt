package bitfinex

class SignatureHelper {

    fun createSignatureHelper(body: String, nonce: String, path: String): String {
        return String.format("/api/%s%s%s", path, nonce, body)
    }
}