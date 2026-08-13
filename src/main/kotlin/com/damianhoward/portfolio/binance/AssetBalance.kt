package com.damianhoward.portfolio.binance

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.math.BigDecimal

/**
 * One asset's holding: [free] is available to trade or withdraw, [locked] is committed to open
 * orders. Both are quantities of [asset], not values.
 *
 * Binance sends them as JSON strings rather than numbers, and they are kept as [BigDecimal] for
 * the same reason: a satoshi-scale balance through a double is a balance that no longer sums.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class AssetBalance(val asset: String, val free: BigDecimal, val locked: BigDecimal) {
    /** What the account holds of this asset. Reconciling against movements needs both parts. */
    val total: BigDecimal get() = free + locked
}
