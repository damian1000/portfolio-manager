package com.damianhoward.portfolio.bitfinex

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class MonotonicNonceTest {
    @Test
    fun `a frozen clock still yields a strictly increasing sequence`() {
        // The defect exactly: every call inside one millisecond read the same clock value, so
        // Bitfinex saw a repeated nonce and rejected everything after the first.
        val nonce = MonotonicNonce(clock = { 1_700_000_000_000L })

        val issued = (1..5).map { nonce.next().toLong() }

        assertEquals(issued.sorted(), issued, "issued in increasing order")
        assertEquals(issued.toSet().size, issued.size, "no value repeats")
        assertEquals(1_700_000_000_000_000L, issued.first(), "the first still tracks the clock")
    }

    @Test
    fun `it tracks the clock when time actually moves`() {
        var millis = 1_700_000_000_000L
        val nonce = MonotonicNonce(clock = { millis })

        val first = nonce.next().toLong()
        millis += 5
        val second = nonce.next().toLong()

        assertEquals(1_700_000_000_000_000L, first)
        assertEquals(1_700_000_000_005_000L, second, "a real advance is followed, not just +1")
    }

    @Test
    fun `a clock stepped backwards cannot produce a nonce the exchange would reject`() {
        var millis = 1_700_000_000_000L
        val nonce = MonotonicNonce(clock = { millis })

        val before = nonce.next().toLong()
        millis -= 60_000 // an NTP correction, or a leap adjustment
        val after = nonce.next().toLong()

        assertTrue(after > before, "must keep climbing across a backwards clock, got $after after $before")
    }

    @Test
    fun `concurrent senders never collide`() {
        val nonce = MonotonicNonce(clock = { 1_700_000_000_000L })
        val threads = 8
        val perThread = 500
        val pool = Executors.newFixedThreadPool(threads)

        val issued =
            try {
                pool
                    .invokeAll((1..threads).map { Callable { (1..perThread).map { nonce.next() } } })
                    .flatMap { it.get() }
            } finally {
                pool.shutdown()
                pool.awaitTermination(10, TimeUnit.SECONDS)
            }

        assertEquals(threads * perThread, issued.size)
        assertEquals(issued.size, issued.toSet().size, "every nonce across all threads is distinct")
    }
}
