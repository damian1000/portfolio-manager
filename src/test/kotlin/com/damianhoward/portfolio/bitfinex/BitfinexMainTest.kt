package com.damianhoward.portfolio.bitfinex

import com.damianhoward.portfolio.reconcile.SnapshotStore
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.nio.file.Files
import java.nio.file.Path

class BitfinexMainTest {
    private fun stubGateway(): BitfinexGateway {
        val gateway = mock<BitfinexGateway>()
        whenever(gateway.retrieveWallets()).thenReturn(emptyList())
        whenever(gateway.retrieveMovementHistory(any())).thenReturn(emptyList())
        whenever(gateway.retrieveSettingsForKey(any())).thenReturn("[]")
        return gateway
    }

    private fun stubApiCredentials(): ApiCredentials {
        val helper = mock<ApiCredentials>()
        whenever(helper.apiKey()).thenReturn("test-key")
        return helper
    }

    private fun runWith(
        args: Array<String>,
        gateway: BitfinexGateway,
        journal: WithdrawalJournal,
        reconciler: WithdrawalReconciler = mock(),
        withdrawalId: String = "test-id",
        snapshots: SnapshotStore = SnapshotStore(Files.createTempDirectory("snap").resolve("s.jsonl")),
    ) = run(
        args = args,
        bitfinexGateway = gateway,
        credentials = stubApiCredentials(),
        journalFactory = { journal },
        reconcilerFactory = { reconciler },
        withdrawalIdSupplier = { withdrawalId },
        // Injected, not defaulted. The default resolves under the developer's home directory, so a
        // test that let it stand would write balance snapshots there and read back whatever an
        // earlier run left -- shared state between the suite and the machine it runs on.
        snapshotFactory = { snapshots },
    )

    private fun journal(tmp: Path) = WithdrawalJournal(tmp.resolve("j.jsonl"))

    @Test
    fun `bad arguments exit 64 without journalling anything`(@TempDir tmp: Path) {
        val journal = journal(tmp)

        val exit = runWith(arrayOf("--withdraw", "BTC", "0"), stubGateway(), journal)

        assertEquals(64, exit)
        assertTrue(journal.records().isEmpty(), "a usage error is not a withdrawal")
    }

    @Test
    fun `dry run does not submit and leaves nothing to reconcile`(@TempDir tmp: Path) {
        val gateway = stubGateway()
        val journal = journal(tmp)

        val exit = runWith(arrayOf("--withdraw", "BTC", "0.10", "bc1qexampleaddress"), gateway, journal)

        assertEquals(0, exit)
        verify(gateway, never()).submitWithdrawalRequest(any(), any(), any(), any())
        assertEquals("DRY_RUN", journal.records().single().mode)
        assertTrue(journal.unresolved().isEmpty(), "a dry run was never in doubt")
        assertFalse(Files.readString(tmp.resolve("j.jsonl")).contains("bc1qexampleaddress"))
    }

    @Test
    fun `a confirmed withdrawal journals INTENT before contacting the venue`(@TempDir tmp: Path) {
        val gateway = stubGateway()
        val journal = journal(tmp)
        // Prove the ordering rather than assert it after the fact: at the moment the venue is
        // called, the durable intent must already be on disk.
        whenever(gateway.submitWithdrawalRequest(eq(Currency.BTC), eq("0.10"), eq("bc1qexampleaddress"), eq("test-id")))
            .thenAnswer {
                assertEquals(
                    listOf(WithdrawalState.INTENT),
                    journal.records().map { r -> r.state },
                    "INTENT must be durable before the withdraw call is made",
                )
                "submitted-ok"
            }

        val exit =
            runWith(arrayOf("--withdraw", "BTC", "0.10", "bc1qexampleaddress", "--confirm-withdrawal"), gateway, journal)

        assertEquals(0, exit)
        assertEquals(
            listOf(WithdrawalState.INTENT, WithdrawalState.SUBMITTED),
            journal.records().map { it.state },
        )
    }

    @Test
    fun `a timeout after submission is recorded UNKNOWN, never FAILED`(@TempDir tmp: Path) {
        val gateway = stubGateway()
        whenever(gateway.submitWithdrawalRequest(any(), any(), any(), any()))
            .thenThrow(RuntimeException("read timed out"))
        val journal = journal(tmp)

        val exit =
            runWith(arrayOf("--withdraw", "BTC", "0.10", "bc1qexampleaddress", "--confirm-withdrawal"), gateway, journal)

        assertEquals(75, exit)
        assertEquals(listOf(WithdrawalState.INTENT, WithdrawalState.UNKNOWN), journal.records().map { it.state })
        assertEquals(
            listOf(WithdrawalState.UNKNOWN),
            journal.unresolved().map { it.state },
            "an unknown outcome must survive as work for the next run",
        )
    }

    @Test
    fun `an unresolved withdrawal blocks a new one from being submitted`(@TempDir tmp: Path) {
        val gateway = stubGateway()
        val journal = journal(tmp)
        journal.record("stuck", WithdrawalState.UNKNOWN, "LIVE", WithdrawalRequest(Currency.BTC, "0.10", "bc1qold"))
        val reconciler = mock<WithdrawalReconciler>()
        whenever(reconciler.resolve(any())).thenReturn(Resolution.Unresolved("venue still silent"))

        val exit =
            runWith(
                arrayOf("--withdraw", "BTC", "0.50", "bc1qnewaddress", "--confirm-withdrawal"),
                gateway,
                journal,
                reconciler,
            )

        assertEquals(75, exit)
        verify(gateway, never()).submitWithdrawalRequest(any(), any(), any(), any())
    }

    @Test
    fun `reconciliation settles an outstanding withdrawal and then lets the run proceed`(@TempDir tmp: Path) {
        val gateway = stubGateway()
        val journal = journal(tmp)
        journal.record("stuck", WithdrawalState.UNKNOWN, "LIVE", WithdrawalRequest(Currency.BTC, "0.10", "bc1qold"))
        val reconciler = mock<WithdrawalReconciler>()
        whenever(reconciler.resolve(any()))
            .thenReturn(Resolution.Resolved(WithdrawalState.CONFIRMED, "movement 42 COMPLETED"))

        val exit = runWith(emptyArray(), gateway, journal, reconciler)

        assertEquals(0, exit)
        assertTrue(journal.unresolved().isEmpty(), "the outstanding withdrawal should now be settled")
        assertEquals(WithdrawalState.CONFIRMED, journal.records().last().state)
    }

    @Test
    fun `reconciliation runs even on a read-only invocation`(@TempDir tmp: Path) {
        val journal = journal(tmp)
        journal.record("stuck", WithdrawalState.INTENT, "LIVE", WithdrawalRequest(Currency.BTC, "0.10", "bc1qold"))
        val reconciler = mock<WithdrawalReconciler>()
        whenever(reconciler.resolve(any())).thenReturn(Resolution.Unresolved("still silent"))

        val exit = runWith(emptyArray(), stubGateway(), journal, reconciler)

        assertEquals(75, exit, "money left in doubt is worth reporting even when nothing was asked for")
        verify(reconciler).resolve(any())
    }

    @Test
    fun `confirmed withdrawal aborts when the pre-flight read fails`(@TempDir tmp: Path) {
        val gateway = mock<BitfinexGateway>()
        whenever(gateway.retrieveWallets()).thenThrow(RuntimeException("auth rejected"))
        val journal = journal(tmp)

        val exit =
            runWith(arrayOf("--withdraw", "BTC", "0.10", "bc1qexampleaddress", "--confirm-withdrawal"), gateway, journal)

        assertEquals(1, exit)
        verify(gateway, never()).submitWithdrawalRequest(any(), any(), any(), any())
        assertEquals(WithdrawalState.FAILED, journal.records().single().state)
        assertTrue(journal.unresolved().isEmpty(), "nothing was sent, so nothing is in doubt")
    }

    @Test
    fun `no withdrawal flag runs read-only and exits 0`(@TempDir tmp: Path) {
        val gateway = stubGateway()

        val exit = runWith(emptyArray(), gateway, journal(tmp))

        assertEquals(0, exit)
        verify(gateway, never()).submitWithdrawalRequest(any(), any(), any(), any())
    }

    @Test
    fun `read failure produces exit 1 with no withdrawal flag`(@TempDir tmp: Path) {
        val gateway = mock<BitfinexGateway>()
        whenever(gateway.retrieveWallets()).thenThrow(RuntimeException("boom"))

        val exit = runWith(emptyArray(), gateway, journal(tmp))

        assertEquals(1, exit)
    }
}
