package com.damianhoward.portfolio.binance

import org.slf4j.LoggerFactory
import kotlin.system.exitProcess

private val log = LoggerFactory.getLogger("com.damianhoward.portfolio.binance.Main")

fun main(args: Array<String>) {
    exitProcess(run(args))
}

/**
 * Reads Binance system status and wallet balances. Takes no arguments.
 *
 * It used to demand a currency, an amount and a destination address, and then use none of them:
 * the currency was parsed and dropped, and the other two reached nothing but a log line — which
 * also put a full destination address on disk. There is no Binance withdrawal path to implement
 * them against, so a signature that asks for one advertises something this side cannot do.
 */
internal fun run(args: Array<String>, binanceGateway: BinanceGateway = BinanceGateway()): Int {
    if (args.isNotEmpty()) {
        log.error("Usage: binance (no arguments). This reads system status and wallet balances; it cannot withdraw.")
        return 64
    }
    return try {
        log.info("system status: {}", binanceGateway.retrieveSystem())
        log.info("wallets: {}", binanceGateway.retrieveWallets())
        0
    } catch (e: Exception) {
        log.error("Binance read failed", e)
        1
    }
}
