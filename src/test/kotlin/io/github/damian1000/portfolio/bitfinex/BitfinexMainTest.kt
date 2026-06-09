package io.github.damian1000.portfolio.bitfinex

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.ByteArrayOutputStream

class BitfinexMainTest {

    private fun stubGateway(): BitfinexGateway {
        val gateway = mock<BitfinexGateway>()
        whenever(gateway.retrieveWallets()).thenReturn("[]")
        whenever(gateway.retrieveMovementHistory(any())).thenReturn(emptyList())
        whenever(gateway.retrieveSettingsForKey(any())).thenReturn("[]")
        return gateway
    }

    private fun stubPropertyHelper(): PropertyHelper {
        val helper = mock<PropertyHelper>()
        whenever(helper.getApiKey()).thenReturn("test-key")
        return helper
    }

    @Test
    fun `bad arguments exit 64`() {
        val audit = ByteArrayOutputStream()
        val exit = run(
            args = arrayOf("--withdraw", "BTC", "0"),
            bitfinexGateway = stubGateway(),
            propertyHelper = stubPropertyHelper(),
            auditLogFactory = { WithdrawalAuditLog(audit) },
        )
        assertEquals(64, exit)
        assertEquals(0, audit.size(), "no audit record for usage errors")
    }

    @Test
    fun `dry run does not submit withdrawal and exits 0`() {
        val gateway = stubGateway()
        val audit = ByteArrayOutputStream()
        val exit = run(
            args = arrayOf("--withdraw", "BTC", "0.10", "bc1qexampleaddress"),
            bitfinexGateway = gateway,
            propertyHelper = stubPropertyHelper(),
            auditLogFactory = { WithdrawalAuditLog(audit) },
        )
        assertEquals(0, exit)
        verify(gateway, never()).submitWithdrawalRequest(any(), any(), any())
        val text = audit.toString().trim()
        assertTrue(text.contains("DRY_RUN"))
        assertTrue(text.contains("bc1q***ss"), "address must be redacted in audit: $text")
    }

    @Test
    fun `confirmed withdrawal submits and audits LIVE record`() {
        val gateway = stubGateway()
        whenever(gateway.submitWithdrawalRequest(eq(Currency.BTC), eq("0.10"), eq("bc1qexampleaddress")))
            .thenReturn("submitted-ok")
        val audit = ByteArrayOutputStream()

        val exit = run(
            args = arrayOf("--withdraw", "BTC", "0.10", "bc1qexampleaddress", "--confirm-withdrawal"),
            bitfinexGateway = gateway,
            propertyHelper = stubPropertyHelper(),
            auditLogFactory = { WithdrawalAuditLog(audit) },
        )

        assertEquals(0, exit)
        verify(gateway).submitWithdrawalRequest(Currency.BTC, "0.10", "bc1qexampleaddress")
        val text = audit.toString()
        assertTrue(text.contains("LIVE"))
        assertTrue(text.contains("submitted-ok"))
    }

    @Test
    fun `confirmed withdrawal exits non-zero when gateway throws`() {
        val gateway = stubGateway()
        whenever(gateway.submitWithdrawalRequest(any(), any(), any()))
            .thenThrow(RuntimeException("network down"))
        val audit = ByteArrayOutputStream()

        val exit = run(
            args = arrayOf("--withdraw", "BTC", "0.10", "bc1qexampleaddress", "--confirm-withdrawal"),
            bitfinexGateway = gateway,
            propertyHelper = stubPropertyHelper(),
            auditLogFactory = { WithdrawalAuditLog(audit) },
        )

        assertEquals(1, exit)
        val text = audit.toString()
        assertTrue(text.contains("LIVE"))
        assertTrue(text.contains("failed:"))
        assertTrue(text.contains("network down"))
    }

    @Test
    fun `no withdrawal flag runs read-only and exits 0`() {
        val gateway = stubGateway()
        val exit = run(
            args = emptyArray(),
            bitfinexGateway = gateway,
            propertyHelper = stubPropertyHelper(),
            auditLogFactory = { error("audit should not open without withdrawal request") },
        )
        assertEquals(0, exit)
        verify(gateway, never()).submitWithdrawalRequest(any(), any(), any())
    }

    @Test
    fun `read failure produces exit 1 with no withdrawal flag`() {
        val gateway = mock<BitfinexGateway>()
        whenever(gateway.retrieveWallets()).thenThrow(RuntimeException("boom"))
        val exit = run(
            args = emptyArray(),
            bitfinexGateway = gateway,
            propertyHelper = stubPropertyHelper(),
            auditLogFactory = { error("audit should not open without withdrawal request") },
        )
        assertEquals(1, exit)
    }
}
