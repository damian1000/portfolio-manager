package io.github.damian1000.portfolio.binance

import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.math.BigInteger

private val log = LoggerFactory.getLogger("io.github.damian1000.portfolio.binance.Main")

fun main(args: Array<String>) {

    val rawQuantity = BigDecimal.valueOf(BigInteger("10000000").toLong())
    val quantity = rawQuantity.divide(BigDecimal("100000000"))
    log.info("quantity: {}", quantity)

    val binanceGateway = BinanceGateway()
    try {
        val currency = Currency.valueOf(args[0])
        val amount = args[1]
        val destinationAddress = args[2]

        log.info("currency:{}:amount:{}:destinationAddress:{}", currency, amount, destinationAddress)
        log.info("system status: {}", binanceGateway.retrieveSystem())
        log.info("wallets: {}", binanceGateway.retrieveWallets())
    } catch (e: Exception) {
        log.error("Binance CLI failed", e)
    }
}