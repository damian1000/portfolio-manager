package com.damianhoward.portfolio.reconcile

import java.math.BigDecimal

/**
 * One currency's verdict: did the balance move by what the venue's own movement records account
 * for?
 *
 * The property is conservation, the same one `trading-system` asserts over its fill ledger — a
 * derived quantity must equal the signed sum of the events behind it. Here the derived quantity is
 * the venue's balance and the events are its deposits and withdrawals.
 */
sealed interface CurrencyVerdict {
    val currency: String

    /** The change is fully accounted for by movements in the window. */
    data class Accounted(override val currency: String, val delta: BigDecimal) : CurrencyVerdict

    /**
     * The balance moved by more or less than the movements explain. [unexplained] is the balance
     * change minus what the movements account for, signed from the balance's side: positive means
     * money arrived that no movement records.
     *
     * This is not automatically a fault. Trading moves a balance without producing a *movement* —
     * a movement is a transfer in or out of the venue, not a fill — so on an account that trades,
     * an unexplained delta is expected and this check is a statement about custody rather than
     * about correctness. It earns its place on an account used for holding and transferring, where
     * the expected answer is zero and anything else wants a human.
     */
    data class Unexplained(override val currency: String, val delta: BigDecimal, val accountedFor: BigDecimal) : CurrencyVerdict {
        val unexplained: BigDecimal get() = delta - accountedFor
    }

    /**
     * Not judged, and deliberately so. [reason] says which of the two cases applies.
     *
     * A verdict withheld is worth more than a wrong one. The venue's movement history is paged and
     * time-ordered by its own clock, so a movement can sit just outside the window this process
     * measured, or arrive in the history after the balance that already reflects it. Reporting
     * either as unexplained would produce a drift report that cries wolf, and a report nobody
     * trusts is one nobody reads — the same reasoning that makes an in-flight projection
     * inconclusive rather than divergent in `trading-system`.
     */
    data class Inconclusive(override val currency: String, val reason: String) : CurrencyVerdict
}

/**
 * The drift report for one venue over one interval: what changed, what the venue's own records
 * account for, and what is left over.
 *
 * Structured rather than printed prose, so a caller can act on it. [needsAttention] is the single
 * question an operator asks, and it is deliberately false for an inconclusive currency: not
 * knowing is not the same as knowing something is wrong.
 */
data class BalanceReconciliation(val venue: String, val fromEpochMilli: Long, val toEpochMilli: Long, val verdicts: List<CurrencyVerdict>) {
    val unexplained: List<CurrencyVerdict.Unexplained>
        get() = verdicts.filterIsInstance<CurrencyVerdict.Unexplained>()

    val inconclusive: List<CurrencyVerdict.Inconclusive>
        get() = verdicts.filterIsInstance<CurrencyVerdict.Inconclusive>()

    val needsAttention: Boolean get() = unexplained.isNotEmpty()
}
