package com.damianhoward.portfolio.net

import java.time.Duration
import kotlin.random.Random

/** What a failed attempt should cause. Venue-specific classification produces one of these. */
sealed interface Attempt {
    /** Retry after [waitHint] if the venue supplied one, otherwise after the policy's own backoff. */
    data class Retryable(val waitHint: Duration? = null) : Attempt

    /** Never retry: the request was wrong, or the venue has said stop. */
    data object Fatal : Attempt
}

/** Raised when every attempt was used and the last one still failed. Carries the cause. */
class RetriesExhausted(val attempts: Int, cause: Throwable) : RuntimeException("gave up after $attempts attempt(s)", cause)

/**
 * Bounded retry with exponential backoff, for calls a venue may refuse for a moment.
 *
 * This is shared between the venue packages while the gateways deliberately are not, because it is
 * the one thing here that protects an invariant rather than saving typing: retry only bounded, only
 * on transient failure, with an exhaustion path a caller can see. Getting that wrong has a specific
 * and expensive shape — an unbounded retry against a rate limiter is how a rate limit becomes a
 * ban, which costs the account rather than the request.
 *
 * What is *not* shared is which failure is transient. Binance and Bitfinex disagree about status
 * codes and about what a ban looks like, so classification is a function each venue supplies and
 * this class never inspects an exception itself.
 *
 * Jitter is not decoration. Without it every retry in a run lines up on the same schedule, so a
 * venue that refused the first call refuses the retries together — the pattern that turns one
 * limit into a burst against the same window.
 *
 * A server-supplied wait hint always wins over the computed backoff. The venue knows when it will
 * accept traffic again and a client guessing shorter is how the next refusal is earned.
 */
class RetryPolicy(
    private val maxAttempts: Int = DEFAULT_MAX_ATTEMPTS,
    private val initialBackoff: Duration = DEFAULT_INITIAL_BACKOFF,
    private val maxBackoff: Duration = DEFAULT_MAX_BACKOFF,
    private val sleeper: (Duration) -> Unit = { Thread.sleep(it.toMillis()) },
    private val random: Random = Random.Default,
) {
    init {
        require(maxAttempts >= 1) { "maxAttempts must be at least 1, got $maxAttempts" }
    }

    /**
     * Runs [call], retrying while [classify] says the failure is transient and attempts remain.
     *
     * A fatal classification rethrows the original exception rather than wrapping it: the caller
     * asked for one request and the venue answered it, so there is nothing about retrying to
     * report. Exhaustion wraps, because "we tried five times over eight seconds" is a different
     * fact from the last failure alone and the caller cannot infer it.
     */
    fun <T> execute(classify: (Exception) -> Attempt, call: () -> T): T {
        var backoff = initialBackoff
        var attempt = 1
        while (true) {
            try {
                return call()
            } catch (e: Exception) {
                val decision = classify(e)
                if (decision is Attempt.Fatal) throw e
                if (attempt >= maxAttempts) throw RetriesExhausted(attempt, e)

                val hint = (decision as Attempt.Retryable).waitHint
                sleeper(hint ?: jittered(backoff))
                backoff = minOf(backoff.multipliedBy(2), maxBackoff)
                attempt++
            }
        }
    }

    /**
     * Full jitter: a uniform draw from zero to the current backoff, rather than the backoff with a
     * small wobble. It spreads retries across the whole window instead of clustering them near its
     * end, which is the property that matters when several calls back off together.
     */
    private fun jittered(backoff: Duration): Duration = Duration.ofMillis(random.nextLong(backoff.toMillis() + 1))

    companion object {
        /**
         * Five attempts over roughly eight seconds of backoff. This runs in a CLI a human is
         * waiting on, so the bound is about how long a person will sit there, not about how long
         * an outage lasts — a venue still refusing after eight seconds is one to come back to.
         */
        const val DEFAULT_MAX_ATTEMPTS = 5
        val DEFAULT_INITIAL_BACKOFF: Duration = Duration.ofMillis(500)
        val DEFAULT_MAX_BACKOFF: Duration = Duration.ofSeconds(4)
    }
}
