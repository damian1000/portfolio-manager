package com.damianhoward.portfolio.bitfinex

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class WithdrawalJournalTest {
    private fun request(address: String = "bc1qexampleaddressx9") = WithdrawalRequest(Currency.BTC, "0.10", address)

    @Test
    fun `record writes one json line per call and redacts the destination`(@TempDir tmp: Path) {
        val journal = WithdrawalJournal(tmp.resolve("j.jsonl"))

        journal.record("id-1", WithdrawalState.INTENT, "LIVE", request(), "about to submit")
        journal.record("id-1", WithdrawalState.SUBMITTED, "LIVE", request(), "venue responded")

        val records = journal.records()
        assertEquals(2, records.size)
        assertEquals(WithdrawalState.INTENT, records[0].state)
        assertEquals(WithdrawalState.SUBMITTED, records[1].state)
        assertEquals("bc1q***x9", records[0].destination)

        val text = Files.readString(tmp.resolve("j.jsonl"))
        assertFalse(text.contains("bc1qexampleaddressx9"), "the full destination must never reach the journal")
    }

    @Test
    fun `records survive being written by separate journal instances`(@TempDir tmp: Path) {
        val file = tmp.resolve("j.jsonl")
        WithdrawalJournal(file).record("id-1", WithdrawalState.INTENT, "LIVE", request())
        WithdrawalJournal(file).record("id-2", WithdrawalState.INTENT, "LIVE", request())

        assertEquals(2, WithdrawalJournal(file).records().size, "each append must be durable and additive")
    }

    @Test
    fun `unresolved reports the latest state per withdrawal and drops terminal ones`(@TempDir tmp: Path) {
        val journal = WithdrawalJournal(tmp.resolve("j.jsonl"))

        journal.record("settled", WithdrawalState.INTENT, "LIVE", request())
        journal.record("settled", WithdrawalState.SUBMITTED, "LIVE", request())
        journal.record("settled", WithdrawalState.CONFIRMED, "LIVE", request())
        journal.record("in-doubt", WithdrawalState.INTENT, "LIVE", request())
        journal.record("in-doubt", WithdrawalState.UNKNOWN, "LIVE", request())
        journal.record("rejected", WithdrawalState.FAILED, "LIVE", request())

        val unresolved = journal.unresolved()

        assertEquals(listOf("in-doubt"), unresolved.map { it.withdrawalId })
        assertEquals(WithdrawalState.UNKNOWN, unresolved.single().state)
    }

    @Test
    fun `an intent with no later record counts as unresolved`(@TempDir tmp: Path) {
        // A crash between the journal write and the venue call is indistinguishable from a crash
        // during it, so a bare INTENT must not be assumed to have gone nowhere.
        val journal = WithdrawalJournal(tmp.resolve("j.jsonl"))
        journal.record("abandoned", WithdrawalState.INTENT, "LIVE", request())

        assertEquals(listOf("abandoned"), journal.unresolved().map { it.withdrawalId })
    }

    @Test
    fun `a missing journal file reads as empty rather than throwing`(@TempDir tmp: Path) {
        val journal = WithdrawalJournal(tmp.resolve("does-not-exist.jsonl"))

        assertTrue(journal.records().isEmpty())
        assertTrue(journal.unresolved().isEmpty())
    }

    @Test
    fun `embedded newlines in detail cannot forge a second record`(@TempDir tmp: Path) {
        val journal = WithdrawalJournal(tmp.resolve("j.jsonl"))

        journal.record("id-1", WithdrawalState.UNKNOWN, "LIVE", request(), "no response: line one\nline two")

        assertEquals(1, journal.records().size, "a newline in detail must not split into two records")
    }

    @Test
    fun `openDefault honours PORTFOLIO_AUDIT_DIR falling back to user home`(@TempDir tmp: Path) {
        val originalHome = System.getProperty("user.home")
        System.setProperty("user.home", tmp.toString())
        try {
            WithdrawalJournal.openDefault().record("id-1", WithdrawalState.INTENT, "LIVE", request())
            val file = tmp.resolve(".portfolio-manager").resolve(WithdrawalJournal.FILE_NAME)
            assertTrue(Files.exists(file), "journal should be created under the home directory")
        } finally {
            System.setProperty("user.home", originalHome)
        }
    }
}
