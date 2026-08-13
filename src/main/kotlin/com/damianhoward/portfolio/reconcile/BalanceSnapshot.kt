package com.damianhoward.portfolio.reconcile

import java.math.BigDecimal

/**
 * What a venue said one currency's balance was, at one moment.
 *
 * The unit is a quantity of [currency], never a value: this repository holds no prices and
 * converting here would invent one. [BigDecimal] because a crypto balance through a double stops
 * summing at the precision that matters.
 */
data class CurrencyBalance(val currency: String, val amount: BigDecimal)

/**
 * Every balance a venue reported at [takenAtEpochMilli], which is the anchor a later reconciliation
 * measures from.
 *
 * The timestamp is the venue's read time as this process saw it, not the venue's own clock — the
 * two APIs report movement times in their own terms and there is no shared clock to reconcile
 * against. That inexactness is why [BalanceReconciliation] treats a movement near the boundary as
 * a reason to withhold judgement rather than as evidence of drift.
 */
data class BalanceSnapshot(val venue: String, val takenAtEpochMilli: Long, val balances: List<CurrencyBalance>) {
    fun amountOf(currency: String): BigDecimal = balances.firstOrNull { it.currency == currency }?.amount ?: BigDecimal.ZERO

    /** Every currency either snapshot knows about, so one appearing or disappearing is still compared. */
    fun currenciesWith(other: BalanceSnapshot): List<String> = (balances.map { it.currency } + other.balances.map { it.currency }).distinct().sorted()
}
