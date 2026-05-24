package bitfinex

fun main(args: Array<String>) {
    val bitfinexGateway = BitfinexGateway()
    val propertyHelper = PropertyHelper()
    try {
        val currency = Currency.valueOf(args[0])
        val amount = args[1]
        val destinationaddress = args[2]
        println("currency:$currency:amount:$amount:destinationaddress:$destinationaddress")
        println("wallets: " + bitfinexGateway.retrieveWallets())
        val history = bitfinexGateway.retrieveMovementHistory(currency.name)
        for (tranfer in history) println(tranfer)

        println("settings: "+bitfinexGateway.retrieveSettingsForKey(propertyHelper.getApiKey()));
        println("submitWithdrawalRequest: "+bitfinexGateway.submitWithdrawalRequest(currency, amount, destinationaddress));

        println("wallets: " + bitfinexGateway.retrieveWallets())
        val historyAfter = bitfinexGateway.retrieveMovementHistory(currency.name)
        for (tranfer in historyAfter) println(tranfer)

    } catch (e: Exception) {
        e.printStackTrace()
    }
}