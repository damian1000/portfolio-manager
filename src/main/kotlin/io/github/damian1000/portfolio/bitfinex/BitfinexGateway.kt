package io.github.damian1000.portfolio.bitfinex

import java.util.UUID

class BitfinexGateway(
    private val messageSender: MessageSender = MessageSender(),
    private val bitfinexMovementMapper: BitfinexMovementMapper = BitfinexMovementMapper(),
    private val paymentIdSupplier: () -> String = { UUID.randomUUID().toString() },
) {
    fun retrieveMovementHistory(currency: String): List<Movement> {
        val apiPath = "v2/auth/r/movements/$currency/hist"
        val result = messageSender.sendMessage(apiPath)
        return bitfinexMovementMapper.mapMovement(result)
    }

    fun retrieveWallets(): String = messageSender.sendMessage("v2/auth/r/wallets")

    fun retrieveSettingsForKey(key: String): String = messageSender.sendMessage(
        "v2/auth/r/settings",
        BitfinexReadSettingKeys(listOf("api:$key")),
    )

    fun submitWithdrawalRequest(currency: Currency, amount: String, destinationAddress: String): String {
        val withdrawalRequestDto =
            BitfinexWithdrawalRequest(
                BitfinexWallet.exchange,
                currency.code,
                amount,
                destinationAddress,
                paymentIdSupplier(),
            )
        return messageSender.sendMessage("v2/auth/w/withdraw", withdrawalRequestDto)
    }
}
