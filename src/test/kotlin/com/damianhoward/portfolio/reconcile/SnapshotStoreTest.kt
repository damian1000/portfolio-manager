package com.damianhoward.portfolio.reconcile

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.math.BigDecimal
import java.nio.file.Path

class SnapshotStoreTest {
    @TempDir
    lateinit var dir: Path

    private fun store() = SnapshotStore(dir.resolve("nested").resolve("balance-snapshots.jsonl"))

    private fun snapshot(venue: String, at: Long, amount: String) = BalanceSnapshot(venue, at, listOf(CurrencyBalance("BTC", BigDecimal(amount))))

    @Test
    fun `a snapshot round-trips with its exact decimals`() {
        val store = store()
        store.append(snapshot("bitfinex", 1_000L, "0.50000000"))

        val read = store.snapshots().single()
        assertEquals("bitfinex", read.venue)
        assertEquals(BigDecimal("0.50000000"), read.balances.single().amount)
    }

    @Test
    fun `appending keeps every snapshot, because the question is what changed between two`() {
        val store = store()
        store.append(snapshot("bitfinex", 1_000L, "1.0"))
        store.append(snapshot("bitfinex", 2_000L, "1.5"))

        assertEquals(2, store.snapshots().size)
    }

    @Test
    fun `the latest for a venue is the most recent by its own timestamp, not by write order`() {
        // Order on disk is arrival order, which is not the same claim as chronological order.
        val store = store()
        store.append(snapshot("bitfinex", 2_000L, "1.5"))
        store.append(snapshot("bitfinex", 1_000L, "1.0"))

        assertEquals(2_000L, store.latestFor("bitfinex")!!.takenAtEpochMilli)
    }

    @Test
    fun `venues do not see each other's snapshots`() {
        val store = store()
        store.append(snapshot("bitfinex", 2_000L, "1.5"))
        store.append(snapshot("binance", 3_000L, "9.9"))

        assertEquals(BigDecimal("1.5"), store.latestFor("bitfinex")!!.balances.single().amount)
        assertEquals(3_000L, store.latestFor("binance")!!.takenAtEpochMilli)
    }

    @Test
    fun `a first run has nothing to measure from, and that is not a fault`() {
        assertNull(store().latestFor("bitfinex"))
        assertTrue(store().snapshots().isEmpty(), "a missing file reads as no history, not an error")
    }

    @Test
    fun `the directory is created rather than assumed`() {
        // openDefault() and this both write under a directory that may not exist yet; a first run
        // on a new machine must not fail on that.
        val store = store()
        store.append(snapshot("bitfinex", 1_000L, "1.0"))

        assertEquals(1, store.snapshots().size)
    }

    @Test
    fun `an unknown currency reads as zero, so an arriving balance is a change rather than an error`() {
        val previous = BalanceSnapshot("bitfinex", 1_000L, emptyList())

        assertEquals(BigDecimal.ZERO, previous.amountOf("BTC"))
    }
}
