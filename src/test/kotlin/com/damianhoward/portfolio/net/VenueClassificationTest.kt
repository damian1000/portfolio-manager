package com.damianhoward.portfolio.net

import com.damianhoward.portfolio.binance.BinanceRetries
import com.damianhoward.portfolio.bitfinex.BitfinexRetries
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.IOException
import com.damianhoward.portfolio.binance.HttpRequestFailed as BinanceFailure
import com.damianhoward.portfolio.bitfinex.HttpRequestFailed as BitfinexFailure

/**
 * The venue-specific half of the retry decision, which is why classification is not shared.
 *
 * The policy owns the rule — bounded, backed off, with an exhaustion path. What each venue owns is
 * which failure is transient, and the two genuinely disagree.
 */
class VenueClassificationTest {
    @Test
    fun `both venues retry a rate limit`() {
        assertEquals(Attempt.Retryable(), BinanceRetries.classify(BinanceFailure("api", 429, "Too Many Requests")))
        assertEquals(Attempt.Retryable(), BitfinexRetries.classify(BitfinexFailure("api", 429, "Too Many Requests")))
    }

    @Test
    fun `Binance treats 418 as fatal, and that is the difference worth having`() {
        // 429 means slow down; 418 means an earlier 429 was ignored and the IP is banned. Retrying
        // into a ban extends it, which is the single worst thing to do at that moment — and it is
        // the reason a shared classifier keyed on status alone would be wrong.
        assertEquals(Attempt.Fatal, BinanceRetries.classify(BinanceFailure("api", 418, "I'm a teapot")))
    }

    @Test
    fun `both venues retry a server failure, which is the venue failing rather than refusing`() {
        assertEquals(Attempt.Retryable(), BinanceRetries.classify(BinanceFailure("api", 503, "Service Unavailable")))
        assertEquals(Attempt.Retryable(), BitfinexRetries.classify(BitfinexFailure("api", 500, "Server Error")))
    }

    @Test
    fun `neither venue retries a client error`() {
        // A bad signature or an expired key does not become valid by being asked again, and
        // retrying burns the rate budget a genuine 429 needs.
        assertEquals(Attempt.Fatal, BinanceRetries.classify(BinanceFailure("api", 401, "Unauthorized")))
        assertEquals(Attempt.Fatal, BitfinexRetries.classify(BitfinexFailure("api", 400, "Bad Request")))
    }

    @Test
    fun `both venues retry a transport failure`() {
        assertEquals(Attempt.Retryable(), BinanceRetries.classify(IOException("connection reset")))
        assertEquals(Attempt.Retryable(), BitfinexRetries.classify(IOException("connection reset")))
    }

    @Test
    fun `an unrecognised failure is fatal rather than retried hopefully`() {
        assertEquals(Attempt.Fatal, BinanceRetries.classify(IllegalStateException("something else")))
        assertEquals(Attempt.Fatal, BitfinexRetries.classify(IllegalStateException("something else")))
    }
}
