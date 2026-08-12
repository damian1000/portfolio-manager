package com.damianhoward.portfolio.bitfinex

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

    /**
     * [withdrawalId] is the journal's id for this withdrawal, sent as the venue's `payment_id` and
     * stable across retries, so a resubmission carries the same value the first attempt did rather
     * than a fresh random one.
     *
     * It is not a true idempotency key and must not be relied on as one: Bitfinex documents
     * `payment_id` as a destination memo for currencies that need one, and does not promise to
     * deduplicate on it. Reconciling against movement history is what actually prevents a duplicate
     * withdrawal; this only makes the two attempts recognisably related to a human reading the
     * venue's records.
     */
    fun submitWithdrawalRequest(currency: Currency, amount: String, destinationAddress: String, withdrawalId: String = paymentIdSupplier()): String {
        val withdrawalRequestDto =
            BitfinexWithdrawalRequest(
                BitfinexWallet.exchange,
                currency.code,
                amount,
                destinationAddress,
                withdrawalId,
            )
        return messageSender.sendMessage("v2/auth/w/withdraw", withdrawalRequestDto)
    }
}
