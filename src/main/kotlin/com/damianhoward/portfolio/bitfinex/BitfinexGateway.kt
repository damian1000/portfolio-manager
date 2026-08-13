package com.damianhoward.portfolio.bitfinex

import com.damianhoward.portfolio.net.RequestThrottle
import com.damianhoward.portfolio.net.RetryPolicy
import java.util.UUID

class BitfinexGateway(
    private val messageSender: MessageSender = MessageSender(),
    private val bitfinexMovementMapper: BitfinexMovementMapper = BitfinexMovementMapper(),
    private val bitfinexWalletMapper: BitfinexWalletMapper = BitfinexWalletMapper(),
    private val paymentIdSupplier: () -> String = { UUID.randomUUID().toString() },
    private val throttle: RequestThrottle = BitfinexRetries.throttle(),
    private val retries: RetryPolicy = RetryPolicy(),
) {
    /**
     * Reads are throttled and retried; the withdrawal below is neither, deliberately.
     *
     * A retried read costs a duplicate response. A retried withdrawal risks a duplicate payment,
     * and Bitfinex offers no idempotency key that would make one safe -- which is why
     * [WithdrawalReconciler] exists. That path stays a single attempt whose uncertain outcome is
     * recorded as UNKNOWN and settled against movement history.
     */
    private fun <T> read(call: () -> T): T = retries.execute(BitfinexRetries::classify) { throttle.throttle(call) }
    fun retrieveMovementHistory(currency: String): List<Movement> {
        val apiPath = "v2/auth/r/movements/$currency/hist"
        val result = read { messageSender.sendMessage(apiPath) }
        return bitfinexMovementMapper.mapMovement(result)
    }

    fun retrieveWallets(): List<Wallet> = bitfinexWalletMapper.mapWallets(read { messageSender.sendMessage("v2/auth/r/wallets") })

    fun retrieveSettingsForKey(key: String): String = read {
        messageSender.sendMessage("v2/auth/r/settings", BitfinexReadSettingKeys(listOf("api:$key")))
    }

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
