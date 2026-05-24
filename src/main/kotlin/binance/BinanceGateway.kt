package binance

class BinanceGateway {

    private val messageSender = MessageSender()
    private val binanceMovementMapper = BinanceMovementMapper()

//    fun retrieveMovementHistory(currency: String): List<Movement> {
//        val apiPath = String.format("v2/auth/r/movements/%s/hist", currency)
//        val result = messageSender.sendPostMessage(apiPath);
//        return bitfinexMovementMapper.mapMovement(result)
//    }

    fun retrieveWallets(): String {
        val apiPath = "/api/v3/account"
        val accountRequest = BinanceAccountRequest(60000, System.currentTimeMillis())
        return messageSender.sendGetMessage(apiPath, accountRequest)
    }

    fun retrieveSystem(): String {
        val apiPath = "/sapi/v1/system/status"
        return messageSender.sendGetMessage(apiPath)
    }

//    fun retrieveSettingsForKey(key: String): String {
//        val apiPath = "v2/auth/r/settings"
//        val readSettingsDto = BitfinexReadSettingKeys(listOf("api:$key"))
//        return messageSender.sendMessage(apiPath, readSettingsDto)
//    }

//    fun submitWithdrawalRequest(currency: Currency, amount: String, destinationAddress: String): String {
//        val apiPath = "v2/auth/w/withdraw"
//        val paymentId = UUID.randomUUID().toString()
//        val withdrawalRequestDto = BitfinexWithdrawalRequest(BitfinexWallet.exchange, currency.code, amount,
//            destinationAddress, paymentId)
//        return messageSender.sendMessage(apiPath, withdrawalRequestDto)
//    }

}