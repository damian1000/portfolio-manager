package com.damianhoward.portfolio.binance

class BinanceGateway(private val messageSender: MessageSender = MessageSender()) {
    fun retrieveWallets(): String {
        val apiPath = "/api/v3/account"
        val accountRequest = BinanceAccountRequest(60_000, System.currentTimeMillis())
        return messageSender.sendGetMessage(apiPath, accountRequest)
    }

    fun retrieveSystem(): String {
        val apiPath = "/sapi/v1/system/status"
        return messageSender.sendGetMessage(apiPath)
    }
}
