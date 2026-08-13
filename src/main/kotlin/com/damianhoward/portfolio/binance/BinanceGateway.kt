package com.damianhoward.portfolio.binance

import com.damianhoward.portfolio.net.RequestThrottle
import com.damianhoward.portfolio.net.RetryPolicy

class BinanceGateway(
    private val messageSender: MessageSender = MessageSender(),
    private val accountMapper: BinanceAccountMapper = BinanceAccountMapper(),
    private val throttle: RequestThrottle = BinanceRetries.throttle(),
    private val retries: RetryPolicy = RetryPolicy(),
) {
    /** Both endpoints here are reads, so both are spaced and retried. */
    private fun <T> read(call: () -> T): T = retries.execute(BinanceRetries::classify) { throttle.throttle(call) }

    fun retrieveWallets(): Account {
        val apiPath = "/api/v3/account"
        val accountRequest = BinanceAccountRequest(60_000, System.currentTimeMillis())
        return accountMapper.mapAccount(read { messageSender.sendGetMessage(apiPath, accountRequest) })
    }

    fun retrieveSystem(): String {
        val apiPath = "/sapi/v1/system/status"
        return read { messageSender.sendGetMessage(apiPath) }
    }
}
