package io.github.damian1000.portfolio.binance

class PropertyHelper(private val env: (String) -> String? = System::getenv) {
    fun getApiKey(): String = env("BINANCE_API_KEY")
        ?: throw RuntimeException("Please specify BINANCE_API_KEY in env")

    fun getApiSecret(): String = env("BINANCE_API_SECRET")
        ?: throw RuntimeException("Please specify BINANCE_API_SECRET in env")
}
