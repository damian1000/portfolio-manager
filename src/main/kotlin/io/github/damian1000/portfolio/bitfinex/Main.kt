package io.github.damian1000.portfolio.bitfinex

import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("io.github.damian1000.portfolio.bitfinex.Main")

fun main(args: Array<String>) {
    val bitfinexGateway = BitfinexGateway()
    val propertyHelper = PropertyHelper()
    try {
        val withdrawRequested = args.firstOrNull() == "--withdraw"
        val withdrawalArgs = if (withdrawRequested) args.drop(1) else emptyList()
        if (withdrawRequested) {
            require(withdrawalArgs.size == 3) {
                "Usage for withdrawal: bitfinex.MainKt --withdraw <currency> <amount> <destinationAddress>"
            }
        }
        val currency = Currency.valueOf(if (withdrawRequested) withdrawalArgs[0] else args.getOrElse(0) { "BTC" })

        log.info("wallets: {}", bitfinexGateway.retrieveWallets())
        bitfinexGateway.retrieveMovementHistory(currency.name).forEach { log.info("movement: {}", it) }

        log.info("settings: {}", bitfinexGateway.retrieveSettingsForKey(propertyHelper.getApiKey()))

        if (withdrawRequested) {
            val amount = withdrawalArgs[1]
            val destinationAddress = withdrawalArgs[2]
            log.info("submitWithdrawalRequest: {}",
                bitfinexGateway.submitWithdrawalRequest(currency, amount, destinationAddress))

            log.info("wallets: {}", bitfinexGateway.retrieveWallets())
            bitfinexGateway.retrieveMovementHistory(currency.name).forEach { log.info("movement: {}", it) }
        }

    } catch (e: Exception) {
        log.error("Bitfinex CLI failed", e)
    }
}
