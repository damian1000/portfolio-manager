package com.damianhoward.portfolio.net

import java.time.Clock
import java.time.Duration

/**
 * A minimum interval between calls to one venue, applied before the request rather than after the
 * refusal.
 *
 * Retry handles being told no; this exists so it is asked less often. The two are not
 * interchangeable — a client that only backs off after a 429 has already spent the request that
 * earned it, and on a venue that counts refusals toward a ban the cheapest call is the one not
 * made.
 *
 * Deliberately a spacing rule rather than a token bucket. A bucket lets a burst through and then
 * stalls, which is the right shape for a server protecting itself and the wrong one for a client
 * with a handful of calls per run: a burst is exactly what a venue's limiter notices. Even spacing
 * over a few requests costs a second or two and never presents a spike.
 *
 * One instance per venue. Sharing one across venues would make Binance wait for Bitfinex, which
 * are separate limits with no relationship.
 */
class RequestThrottle(
    private val minimumInterval: Duration,
    private val clock: Clock = Clock.systemUTC(),
    private val sleeper: (Duration) -> Unit = { Thread.sleep(it.toMillis()) },
) {
    private var lastCallMillis: Long? = null

    /** Waits until at least [minimumInterval] has passed since the previous call, then runs [call]. */
    fun <T> throttle(call: () -> T): T {
        // Recorded before the call rather than after it, so the interval spaces the *starts* of
        // requests. Measured from the end instead, a slow response would push the next request
        // further out than the limit requires and make an already-slow run slower still.
        lastCallMillis?.let { previous ->
            val elapsed = clock.millis() - previous
            val remaining = minimumInterval.toMillis() - elapsed
            if (remaining > 0) sleeper(Duration.ofMillis(remaining))
        }
        lastCallMillis = clock.millis()
        return call()
    }
}
