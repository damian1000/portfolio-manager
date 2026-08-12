package com.damianhoward.portfolio.bitfinex

// Lowercase entries match Bitfinex's API wallet keys (`exchange`, `margin`, `funding`).
@Suppress("EnumEntryName")
enum class BitfinexWallet {
    exchange,
    margin,
    funding,
}
