package io.github.damian1000.portfolio.binance

import org.slf4j.LoggerFactory
import kotlin.system.exitProcess

private val log = LoggerFactory.getLogger("io.github.damian1000.portfolio.binance.Main")

fun main(args: Array<String>) {
    exitProcess(run(args))
}

internal fun run(args: Array<String>, binanceGateway: BinanceGateway = BinanceGateway()): Int {
    if (args.size < 3) {
        log.error("Usage: binance <currency> <amount> <destinationAddress>")
        return 64
    }
    val currency =
        try {
            Currency.valueOf(args[0])
        } catch (e: IllegalArgumentException) {
            log.error("Unknown currency '{}'", args[0])
            return 64
        }
    val amount = args[1]
    val destinationAddress = args[2]

    log.info("currency:{}:amount:{}:destinationAddress:{}", currency, amount, destinationAddress)
    return try {
        log.info("system status: {}", binanceGateway.retrieveSystem())
        log.info("wallets: {}", binanceGateway.retrieveWallets())
        0
    } catch (e: Exception) {
        log.error("Binance read failed", e)
        1
    }
}
