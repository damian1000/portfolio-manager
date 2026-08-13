package com.damianhoward.portfolio.reconcile

import java.math.BigDecimal
import java.time.Duration

/**
 * One transfer in or out of a venue, reduced to what reconciliation needs: which currency, how
 * much, signed, and when the venue says it settled.
 *
 * A venue-neutral shape on purpose. Binance and Bitfinex describe a movement differently and this
 * check does not care — mapping into it is each venue's job, which keeps the arithmetic here
 * testable without either API. It is not a shared gateway abstraction: the gateways stay separate,
 * as the README says they do.
 */
data class LedgerMovement(val currency: String, val signedAmount: BigDecimal, val settledAtEpochMilli: Long)

/**
 * Asserts that a venue's balance moved by exactly what its own movement records account for.
 *
 * Between two snapshots, a currency's balance change must equal the signed sum of the movements
 * the venue reports in that window. The write path here belongs to the venue, not to us, which is
 * what makes the check meaningful: there is no shared code that could make both sides agree by
 * construction, so agreement is evidence rather than tautology.
 *
 * What it cannot do is share a clock with the venue. Movements carry the venue's settlement time
 * and snapshots carry ours, so the two boundaries do not line up exactly. Rather than pretend they
 * do, a movement settling within [boundaryTolerance] of either edge makes that currency
 * [CurrencyVerdict.Inconclusive]: it may be counted in one side and not the other, and a difference
 * produced by that is an artefact of measurement rather than a fact about the account.
 */
class BalanceReconciler(private val boundaryTolerance: Duration = DEFAULT_BOUNDARY_TOLERANCE) {
    fun reconcile(previous: BalanceSnapshot, current: BalanceSnapshot, movements: List<LedgerMovement>): BalanceReconciliation {
        require(previous.venue == current.venue) {
            "cannot reconcile ${previous.venue} against ${current.venue}"
        }
        require(current.takenAtEpochMilli >= previous.takenAtEpochMilli) {
            "the current snapshot predates the previous one"
        }

        val from = previous.takenAtEpochMilli
        val to = current.takenAtEpochMilli
        val toleranceMillis = boundaryTolerance.toMillis()

        val verdicts =
            previous.currenciesWith(current).map { currency ->
                val inWindow = movements.filter { it.currency == currency && it.settledAtEpochMilli in from..to }
                val nearBoundary =
                    movements.any {
                        it.currency == currency &&
                            (
                                kotlin.math.abs(it.settledAtEpochMilli - from) <= toleranceMillis ||
                                    kotlin.math.abs(it.settledAtEpochMilli - to) <= toleranceMillis
                                )
                    }

                val delta = current.amountOf(currency) - previous.amountOf(currency)
                val accountedFor = inWindow.fold(BigDecimal.ZERO) { sum, m -> sum + m.signedAmount }

                when {
                    // Checked before the comparison, not after: a movement on the edge makes the
                    // arithmetic untrustworthy whichever way it happens to come out, and a
                    // coincidental match is not a reason to claim the window was clean.
                    nearBoundary ->
                        CurrencyVerdict.Inconclusive(
                            currency,
                            "a movement settled within ${boundaryTolerance.toMinutes()}m of a snapshot boundary",
                        )
                    // compareTo, not equals: BigDecimal.equals is false for 1.0 against 1.00, and
                    // venues are not consistent about trailing zeroes.
                    delta.compareTo(accountedFor) == 0 -> CurrencyVerdict.Accounted(currency, delta)
                    else -> CurrencyVerdict.Unexplained(currency, delta, accountedFor)
                }
            }

        return BalanceReconciliation(current.venue, from, to, verdicts)
    }

    companion object {
        /**
         * Five minutes either side. Wide enough to cover the gap between a venue settling a
         * movement and publishing it, narrow enough that an ordinary interval is still judged —
         * on an account whose movements are occasional, almost every window has nothing near an
         * edge and is decided normally.
         */
        val DEFAULT_BOUNDARY_TOLERANCE: Duration = Duration.ofMinutes(5)
    }
}
