package com.damianhoward.portfolio.bitfinex

import java.math.BigDecimal

/**
 * One wallet's holding of one currency, as `v2/auth/r/wallets` reports it.
 *
 * Bitfinex splits a balance across wallet types — [BitfinexWallet.exchange], `margin`, `funding` —
 * so a currency appears once per type it is held in, and the account's holding of that currency is
 * the sum. [availableBalance] is what can be moved now; [balance] includes what is committed to
 * open orders or positions. The venue sends it as null while it is still calculating, which is a
 * different fact from zero and is kept as one.
 *
 * Values are [BigDecimal] because they are quantities of a currency, and a crypto balance through
 * a double stops summing at the precision that matters.
 */
data class Wallet(val type: String, val currency: String, val balance: BigDecimal, val availableBalance: BigDecimal?)
