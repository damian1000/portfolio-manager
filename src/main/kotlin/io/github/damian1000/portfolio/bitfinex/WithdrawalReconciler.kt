package io.github.damian1000.portfolio.bitfinex

import java.math.BigDecimal
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

/**
 * The outcome of asking the venue what happened to a withdrawal we lost track of.
 *
 * [Unresolved] is a first-class answer, not a failure of this class. Guessing here is what produces
 * a duplicate withdrawal, so every path that lacks evidence returns it.
 */
sealed class Resolution {
    data class Resolved(val state: WithdrawalState, val detail: String) : Resolution()

    data class Unresolved(val detail: String) : Resolution()
}

/**
 * Decides what a non-terminal journal record actually came to, using the venue's movement history
 * as the authority.
 *
 * Bitfinex's withdraw endpoint has no client-supplied idempotency key that comes back on the
 * movement — `payment_id` is a destination memo for currencies that need one, not an order id — so
 * a record is matched on currency, amount and destination within a time window. That is a
 * heuristic, and it is treated as one: a single unambiguous match resolves, and everything else
 * stays [Resolution.Unresolved] for a human to look at.
 *
 * The asymmetry in [settlingWindow] is deliberate. A movement that is absent immediately after a
 * timeout proves nothing — the venue may simply not have published it yet — so absence only counts
 * as evidence of failure once a withdrawal has had time to appear.
 */
class WithdrawalReconciler(
    private val gateway: BitfinexGateway,
    private val clock: Clock = Clock.systemUTC(),
    private val settlingWindow: Duration = Duration.ofMinutes(10),
) {
    fun resolve(record: WithdrawalRecord): Resolution {
        val movements =
            try {
                gateway.retrieveMovementHistory(record.currency)
            } catch (e: Exception) {
                return Resolution.Unresolved("could not read movement history: ${e.message}")
            }

        val recordedAt = Instant.parse(record.timestamp)
        val candidates = movements.filter { it.matches(record, recordedAt) }

        return when (candidates.size) {
            1 -> classify(candidates.single())
            0 ->
                if (Duration.between(recordedAt, clock.instant()) >= settlingWindow) {
                    Resolution.Resolved(
                        WithdrawalState.FAILED,
                        "no matching movement after ${settlingWindow.toMinutes()}m; the venue never accepted it",
                    )
                } else {
                    Resolution.Unresolved(
                        "no matching movement yet, and less than ${settlingWindow.toMinutes()}m has passed; re-run to check again",
                    )
                }
            else ->
                Resolution.Unresolved(
                    "${candidates.size} movements match this withdrawal (ids ${candidates.mapNotNull { it.id }.joinToString()}); " +
                        "resolve by hand rather than guess which",
                )
        }
    }

    private fun classify(movement: Movement): Resolution {
        val id = movement.id?.toString() ?: "unknown"
        return when (movement.status?.uppercase()) {
            "COMPLETED" -> Resolution.Resolved(WithdrawalState.CONFIRMED, "movement $id COMPLETED")
            "CANCELED", "CANCELLED", "REJECTED" ->
                Resolution.Resolved(WithdrawalState.FAILED, "movement $id ${movement.status}")
            // PENDING, PROCESSING, or a status this code has not seen: the venue is holding it, so
            // it did not fail, but it has not settled either. Neither terminal state is honest yet.
            else -> Resolution.Unresolved("movement $id is ${movement.status ?: "in an unreported state"}")
        }
    }

    private fun Movement.matches(record: WithdrawalRecord, recordedAt: Instant): Boolean {
        val sameDestination =
            destinationAddress?.let { WithdrawalCli.redactAddress(it) } == record.destination
        // Withdrawals leave the account, so the venue reports a negative amount; compare magnitude.
        // BigDecimal.compareTo rather than equals so "0.10" and "0.1" are the same amount.
        val sameAmount =
            amount?.abs()?.compareTo(BigDecimal(record.amount).abs()) == 0
        // A movement that predates the intent cannot be this withdrawal. The venue timestamps in
        // UTC, which is how BitfinexMovementMapper builds them.
        val notBefore =
            createdTimestamp?.toInstant(ZoneOffset.UTC)?.isBefore(recordedAt.minusSeconds(60)) != true
        return sameDestination && sameAmount && notBefore
    }
}
