package com.damianhoward.portfolio.binance

import com.damianhoward.portfolio.net.Attempt
import com.damianhoward.portfolio.net.RequestThrottle
import java.io.IOException
import java.time.Duration

/**
 * How Binance's failures map onto retry decisions.
 *
 * The interesting difference from Bitfinex, and the reason classification is venue-local rather
 * than shared: Binance distinguishes **418 from 429**. A 429 means slow down; a 418 means an
 * earlier 429 was ignored and the IP is now banned, for minutes to days.
 *
 * So 418 is fatal here even though it is the same family as the retryable one. Retrying into a ban
 * extends it, which is the single worst thing a client can do at that moment — the failure mode
 * that makes an unbounded retry expensive rather than merely rude.
 */
object BinanceRetries {
    fun classify(e: Exception): Attempt = when {
        e is HttpRequestFailed && e.status == IM_A_TEAPOT -> Attempt.Fatal
        e is HttpRequestFailed && e.status == TOO_MANY_REQUESTS -> Attempt.Retryable()
        e is HttpRequestFailed && e.status >= INTERNAL_SERVER_ERROR -> Attempt.Retryable()
        e is HttpRequestFailed -> Attempt.Fatal
        e is IOException -> Attempt.Retryable()
        else -> Attempt.Fatal
    }

    /**
     * Binance's limits are weight-based rather than a simple request count, and the account
     * endpoint is one of the heavier reads. This run makes two calls, so spacing them is enough;
     * anything that starts polling would need the weight budget modelled rather than a fixed gap.
     */
    fun throttle(): RequestThrottle = RequestThrottle(Duration.ofMillis(250))

    private const val TOO_MANY_REQUESTS = 429

    /** Binance's own choice of code for "you were told to slow down and did not". */
    private const val IM_A_TEAPOT = 418
    private const val INTERNAL_SERVER_ERROR = 500
}
