package io.github.damian1000.portfolio.bitfinex

class PropertyHelper(private val env: (String) -> String? = System::getenv) {
    fun getApiKey(): String = env("BITFINEX_API_KEY")
        ?: throw RuntimeException("Please specify BITFINEX_API_KEY in env")

    fun getApiSecret(): String = env("BITFINEX_API_SECRET")
        ?: throw RuntimeException("Please specify BITFINEX_API_SECRET in env")
}
