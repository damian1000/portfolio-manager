package io.github.damian1000.portfolio.bitfinex

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

class WithdrawalAuditLogTest {

    @Test
    fun `record writes a tab-separated line with redacted address`() {
        val sink = ByteArrayOutputStream()
        WithdrawalAuditLog(sink).use {
            it.record(
                mode = "DRY_RUN",
                request = WithdrawalRequest(
                    Currency.BTC, "0.10", "bc1qexampleaddressx9",
                ),
                outcome = "not submitted",
            )
        }

        val line = sink.toString(StandardCharsets.UTF_8).trim()
        val fields = line.split('\t')
        assertEquals(6, fields.size, "expected 6 tab-separated fields")
        assertTrue(fields[0].matches(Regex("""\d{4}-\d{2}-\d{2}T.*Z""")), "first field is an ISO instant")
        assertEquals("DRY_RUN", fields[1])
        assertEquals("BTC", fields[2])
        assertEquals("0.10", fields[3])
        assertEquals("bc1q***x9", fields[4])
        assertEquals("not submitted", fields[5])
    }

    @Test
    fun `record flattens embedded newlines in outcome`() {
        val sink = ByteArrayOutputStream()
        WithdrawalAuditLog(sink).use {
            it.record(
                mode = "LIVE",
                request = WithdrawalRequest(Currency.BTC, "0.10", "bc1qaddress"),
                outcome = "failed: line one\nline two",
            )
        }

        val text = sink.toString(StandardCharsets.UTF_8)
        val nonTrailingNewlines = text.trimEnd('\n').count { it == '\n' }
        assertEquals(0, nonTrailingNewlines, "embedded newlines should be replaced")
        assertTrue(text.contains("line one line two"))
    }
}
