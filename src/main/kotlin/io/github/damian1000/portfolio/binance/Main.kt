package io.github.damian1000.portfolio.binance

import java.math.BigDecimal
import java.math.BigInteger

fun main(args: Array<String>) {

    val rawQuantity = BigDecimal.valueOf(BigInteger("10000000").toLong())
    val quantity = rawQuantity.divide(BigDecimal("100000000"))
    System.out.println(quantity)

    val binanceGateway = BinanceGateway()
    val propertyHelper = PropertyHelper()
    try {
        val currency = Currency.valueOf(args[0])
        val amount = args[1]
        val destinationaddress = args[2]

        println("currency:$currency:amount:$amount:destinationaddress:$destinationaddress")

        println("system status: " + binanceGateway.retrieveSystem())

        println("wallets: " + binanceGateway.retrieveWallets())

//        val history = bitfinexGateway.retrieveMovementHistory(currency.name)
//        for (tranfer in history) println(tranfer)

//        println("settings: "+bitfinexGateway.retrieveSettingsForKey(propertyHelper.getApiKey()));
//        println("submitWithdrawalRequest: "+bitfinexGateway.submitWithdrawalRequest(currency, amount, destinationaddress));

//        println("wallets: " + bitfinexGateway.retrieveWallets())
//        val historyAfter = bitfinexGateway.retrieveMovementHistory(currency.name)
//        for (tranfer in historyAfter) println(tranfer)

    } catch (e: Exception) {
        e.printStackTrace()
    }
}