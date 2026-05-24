package bitfinex

import java.util.*

class BitfinexGateway {

    private val messageSender = MessageSender()
    private val bitfinexMovementMapper = BitfinexMovementMapper()

    fun retrieveMovementHistory(currency: String): List<Movement> {
        val apiPath = String.format("v2/auth/r/movements/%s/hist", currency)
        val result = messageSender.sendMessage(apiPath);
        return bitfinexMovementMapper.mapMovement(result)
    }

    fun retrieveWallets(): String {
        val apiPath = "v2/auth/r/wallets"
        return messageSender.sendMessage(apiPath)
    }

    fun retrieveSettingsForKey(key: String): String {
        val apiPath = "v2/auth/r/settings"
        val readSettingsDto = BitfinexReadSettingKeys(listOf("api:$key"))
        return messageSender.sendMessage(apiPath, readSettingsDto)
    }

    fun submitWithdrawalRequest(currency: Currency, amount: String, destinationAddress: String): String {
        val apiPath = "v2/auth/w/withdraw"
        val paymentId = UUID.randomUUID().toString()
        val withdrawalRequestDto = BitfinexWithdrawalRequest(BitfinexWallet.exchange, currency.code, amount,
            destinationAddress, paymentId)
        return messageSender.sendMessage(apiPath, withdrawalRequestDto)
    }

}