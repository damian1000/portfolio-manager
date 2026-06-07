package io.github.damian1000.portfolio.binance

class PropertyHelper {

    fun getApiKey(): String {
        return System.getenv()["BINANCE_API_KEY"]
            ?: throw RuntimeException("Please specify BINANCE_API_KEY in env")
    }

    fun getApiSecret(): String {
        return System.getenv()["BINANCE_API_SECRET"]
            ?: throw RuntimeException("Please specify BINANCE_API_SECRET in env")
    }
}