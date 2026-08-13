package com.damianhoward.portfolio.net

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class RequestThrottleTest {
    private class SteppingClock(private var now: Instant = Instant.parse("2026-08-13T12:00:00Z")) : Clock() {
        override fun instant(): Instant = now

        override fun getZone(): ZoneOffset = ZoneOffset.UTC

        override fun withZone(zone: ZoneId): Clock = this

        fun advance(duration: Duration) {
            now = now.plus(duration)
        }
    }

    private val clock = SteppingClock()
    private val slept = mutableListOf<Duration>()

    /** Waiting advances the clock, as a real sleep would; otherwise the spacing never elapses. */
    private fun throttle(interval: Duration = Duration.ofMillis(250)) = RequestThrottle(interval, clock) {
        slept += it
        clock.advance(it)
    }

    @Test
    fun `the first call is not delayed`() {
        val result = throttle().throttle { "first" }

        assertEquals("first", result)
        assertTrue(slept.isEmpty(), "nothing precedes the first call to space it from")
    }

    @Test
    fun `a call arriving too soon waits out the remainder`() {
        val throttle = throttle()
        throttle.throttle { }

        throttle.throttle { }

        assertEquals(listOf(Duration.ofMillis(250)), slept)
    }

    @Test
    fun `a call arriving after the interval is not delayed at all`() {
        val throttle = throttle()
        throttle.throttle { }
        clock.advance(Duration.ofSeconds(1))

        throttle.throttle { }

        assertTrue(slept.isEmpty(), "the interval had already passed")
    }

    @Test
    fun `only the remainder is waited, not the whole interval`() {
        val throttle = throttle()
        throttle.throttle { }
        clock.advance(Duration.ofMillis(200))

        throttle.throttle { }

        assertEquals(listOf(Duration.ofMillis(50)), slept)
    }

    @Test
    fun `spacing is measured between starts, so a slow response does not push the next call further out`() {
        // Measured from the end of the previous call instead, an already-slow run would be made
        // slower still by its own latency rather than by the venue's limit.
        val throttle = throttle()
        throttle.throttle { clock.advance(Duration.ofMillis(400)) }

        throttle.throttle { }

        assertTrue(slept.isEmpty(), "the call itself outlasted the interval, so nothing is owed")
    }

    @Test
    fun `the call's result is returned unchanged`() {
        assertEquals(42, throttle().throttle { 42 })
    }
}
