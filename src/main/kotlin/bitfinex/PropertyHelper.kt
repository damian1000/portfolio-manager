package bitfinex

class PropertyHelper {

    fun getApiKey(): String {
        return System.getenv()["BITFINEX_API_KEY"]
            ?: throw RuntimeException("Please specify BITFINEX_API_KEY in env")
    }

    fun getApiSecret(): String {
        return System.getenv()["BITFINEX_API_SECRET"]
            ?: throw RuntimeException("Please specify BITFINEX_API_SECRET in env")
    }
}