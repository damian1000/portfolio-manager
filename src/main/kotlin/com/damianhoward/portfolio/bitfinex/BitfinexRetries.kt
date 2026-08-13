package com.damianhoward.portfolio.bitfinex

import com.damianhoward.portfolio.net.Attempt
import com.damianhoward.portfolio.net.RequestThrottle
import java.io.IOException
import java.time.Duration

/**
 * How Bitfinex's failures map onto retry decisions.
 *
 * Venue-local on purpose. The shared [com.damianhoward.portfolio.net.RetryPolicy] owns the rule —
 * bounded, backed off, with a visible exhaustion path — and knows nothing about status codes,
 * because the two venues disagree about them.
 */
object BitfinexRetries {
    /**
     * 429 is the rate limiter and is the case this exists for. 5xx is the venue failing rather
     * than refusing, which is worth one more try. A transport failure is retryable for a *read*
     * and would not be for a withdrawal, which is why this is applied to reads only — see
     * [throttle]'s note.
     *
     * Everything else is fatal, 4xx above all: a bad signature or an expired key does not become
     * valid by being asked again, and retrying it burns the rate budget that a genuine 429 needs.
     */
    fun classify(e: Exception): Attempt = when {
        e is HttpRequestFailed && e.status == TOO_MANY_REQUESTS -> Attempt.Retryable()
        e is HttpRequestFailed && e.status >= INTERNAL_SERVER_ERROR -> Attempt.Retryable()
        e is HttpRequestFailed -> Attempt.Fatal
        e is IOException -> Attempt.Retryable()
        else -> Attempt.Fatal
    }

    /**
     * Bitfinex's documented limit on authenticated reads is well above one per second; this spaces
     * requests conservatively because the whole run makes a handful of calls and the cost of being
     * polite is under a second.
     *
     * **Reads only.** This must never wrap a withdrawal. A retried read costs a duplicate response;
     * a retried withdrawal risks a duplicate payment, and the venue offers no idempotency key that
     * would make it safe — which is the reason `WithdrawalReconciler` exists at all. The withdrawal
     * path stays a single attempt whose uncertain outcome is recorded as `UNKNOWN` and settled
     * against movement history.
     */
    fun throttle(): RequestThrottle = RequestThrottle(Duration.ofMillis(250))

    private const val TOO_MANY_REQUESTS = 429
    private const val INTERNAL_SERVER_ERROR = 500
}
