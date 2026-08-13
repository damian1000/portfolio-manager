package com.damianhoward.portfolio.net

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.IOException
import java.time.Duration
import kotlin.random.Random

class RetryPolicyTest {
    private val slept = mutableListOf<Duration>()

    /** Records what would have been waited instead of waiting, so the suite costs nothing. */
    private fun policy(maxAttempts: Int = 5, random: Random = Random(1)) = RetryPolicy(
        maxAttempts = maxAttempts,
        initialBackoff = Duration.ofMillis(100),
        maxBackoff = Duration.ofMillis(800),
        sleeper = { slept += it },
        random = random,
    )

    private val alwaysRetryable: (Exception) -> Attempt = { Attempt.Retryable() }
    private val alwaysFatal: (Exception) -> Attempt = { Attempt.Fatal }

    @Test
    fun `a call that succeeds first time neither sleeps nor retries`() {
        var calls = 0

        val result = policy().execute(alwaysRetryable) {
            calls++
            "ok"
        }

        assertEquals("ok", result)
        assertEquals(1, calls)
        assertTrue(slept.isEmpty(), "a successful call must not wait")
    }

    @Test
    fun `a transient failure is retried and the eventual result returned`() {
        var calls = 0

        val result =
            policy().execute(alwaysRetryable) {
                calls++
                if (calls < 3) throw IOException("connection reset")
                "ok"
            }

        assertEquals("ok", result)
        assertEquals(3, calls)
        assertEquals(2, slept.size, "one wait per retry, none after the success")
    }

    @Test
    fun `retries are bounded, which is the whole point`() {
        // Unbounded retry against a rate limiter is how a rate limit becomes a ban. The bound is
        // the invariant this class exists to hold.
        var calls = 0

        assertThrows(RetriesExhausted::class.java) {
            policy(maxAttempts = 4).execute(alwaysRetryable) {
                calls++
                throw IOException("still refusing")
            }
        }

        assertEquals(4, calls)
        assertEquals(3, slept.size, "no wait after the final attempt — nothing follows it")
    }

    @Test
    fun `exhaustion carries the attempt count and the last cause`() {
        // "we tried four times" is a different fact from the last failure, and the caller cannot
        // infer it from the exception alone.
        val last = IOException("still refusing")

        val e =
            assertThrows(RetriesExhausted::class.java) {
                policy(maxAttempts = 4).execute(alwaysRetryable) { throw last }
            }

        assertEquals(4, e.attempts)
        assertSame(last, e.cause)
    }

    @Test
    fun `a fatal failure is not retried and is rethrown unwrapped`() {
        // The caller asked for one request and the venue answered it; there is nothing about
        // retrying to report, so wrapping would only hide the real exception behind indirection.
        var calls = 0
        val original = IllegalStateException("bad signature")

        val thrown =
            assertThrows(IllegalStateException::class.java) {
                policy().execute(alwaysFatal) {
                    calls++
                    throw original
                }
            }

        assertSame(original, thrown)
        assertEquals(1, calls)
        assertTrue(slept.isEmpty())
    }

    @Test
    fun `backoff grows and then stops at the ceiling`() {
        // Asserted against a fixed seed with full jitter, so each wait is bounded by the backoff
        // of its round rather than equal to it.
        assertThrows(RetriesExhausted::class.java) {
            policy(maxAttempts = 6).execute(alwaysRetryable) { throw IOException("no") }
        }

        val ceilings = listOf(100L, 200L, 400L, 800L, 800L)
        slept.forEachIndexed { i, wait ->
            assertTrue(wait.toMillis() <= ceilings[i], "wait ${i + 1} was ${wait.toMillis()}ms, over ${ceilings[i]}ms")
        }
        assertEquals(5, slept.size)
    }

    @Test
    fun `a venue's own wait hint wins over the computed backoff`() {
        // The venue knows when it will accept traffic again; a client guessing shorter is how the
        // next refusal is earned.
        val hinted: (Exception) -> Attempt = { Attempt.Retryable(Duration.ofSeconds(30)) }

        assertThrows(RetriesExhausted::class.java) {
            policy(maxAttempts = 2).execute(hinted) { throw IOException("429") }
        }

        assertEquals(listOf(Duration.ofSeconds(30)), slept)
    }

    @Test
    fun `jitter spreads waits rather than repeating one value`() {
        // Without it every retry in a run lines up on the same schedule, so calls that backed off
        // together return together — one limit becoming a burst against the same window.
        assertThrows(RetriesExhausted::class.java) {
            policy(maxAttempts = 6, random = Random(7)).execute(alwaysRetryable) { throw IOException("no") }
        }

        assertTrue(slept.distinct().size > 1, "every wait was identical: $slept")
    }

    @Test
    fun `a policy that could never attempt anything is rejected at construction`() {
        assertThrows(IllegalArgumentException::class.java) { RetryPolicy(maxAttempts = 0) }
    }

    @Test
    fun `a single-attempt policy runs once and wraps the failure`() {
        var calls = 0

        assertThrows(RetriesExhausted::class.java) {
            policy(maxAttempts = 1).execute(alwaysRetryable) {
                calls++
                throw IOException("no")
            }
        }

        assertEquals(1, calls)
        assertTrue(slept.isEmpty())
    }
}
