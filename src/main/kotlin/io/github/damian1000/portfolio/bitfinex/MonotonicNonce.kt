package io.github.damian1000.portfolio.bitfinex

import java.util.concurrent.atomic.AtomicLong

/**
 * Strictly increasing nonces for Bitfinex's replay guard.
 *
 * The exchange rejects any request whose nonce is not greater than the last one it saw for that
 * API key ("nonce: small"), and it is the wall clock that cannot deliver that: `currentTimeMillis`
 * has millisecond resolution, so two calls inside the same millisecond — a burst, a retry, two
 * threads — produced identical nonces and the second request failed. Scaling to microseconds does
 * not fix it either; it only widens the numbering, leaving the same repeated value.
 *
 * So the counter, not the clock, is the source of the guarantee: each nonce is the later of the
 * current microsecond reading and one past the previous nonce. It tracks real time while time
 * moves, and steps by one when it does not, which also carries it over a clock adjusted backwards.
 * `updateAndGet` makes that read-decide-write atomic, so concurrent senders cannot collide.
 *
 * One instance must serve one API key: the guarantee is per key, and two instances would issue
 * overlapping sequences.
 */
class MonotonicNonce(private val clock: () -> Long = System::currentTimeMillis) {
    private val last = AtomicLong(0)

    fun next(): String = last.updateAndGet { previous -> maxOf(clock() * MICROS_PER_MILLI, previous + 1) }.toString()

    private companion object {
        const val MICROS_PER_MILLI = 1_000L
    }
}
