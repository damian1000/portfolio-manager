package com.damianhoward.portfolio.bitfinex

/**
 * The Bitfinex API key and secret, read from the environment.
 *
 * Both are required and blank is treated as absent, which is the point of validating here rather
 * than in [HmacSigner]. A blank secret still produces a well-formed HMAC, so it would be sent, and
 * the venue would answer with an authentication error that says nothing about which of two
 * variables was wrong. Failing at the boundary keeps the variable's name in the message.
 *
 * The values are never logged and never held in a field, so they exist only for the length of the
 * call that signs a request.
 */
class ApiCredentials(private val env: (String) -> String? = System::getenv) {
    fun apiKey(): String = required("BITFINEX_API_KEY")

    fun apiSecret(): String = required("BITFINEX_API_SECRET")

    private fun required(name: String): String {
        val value = env(name)
        require(!value.isNullOrBlank()) { "$name is not set in the environment" }
        return value
    }
}
