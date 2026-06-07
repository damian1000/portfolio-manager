package io.github.damian1000.portfolio.bitfinex

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

        println("wallets: " + bitfinexGateway.retrieveWallets())
        val history = bitfinexGateway.retrieveMovementHistory(currency.name)
        for (tranfer in history) println(tranfer)

        println("settings: "+bitfinexGateway.retrieveSettingsForKey(propertyHelper.getApiKey()));

        if (withdrawRequested) {
            val amount = withdrawalArgs[1]
            val destinationAddress = withdrawalArgs[2]
            println("submitWithdrawalRequest: " +
                bitfinexGateway.submitWithdrawalRequest(currency, amount, destinationAddress))

            println("wallets: " + bitfinexGateway.retrieveWallets())
            val historyAfter = bitfinexGateway.retrieveMovementHistory(currency.name)
            for (tranfer in historyAfter) println(tranfer)
        }

    } catch (e: Exception) {
        e.printStackTrace()
    }
}
