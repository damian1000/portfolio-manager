package com.damianhoward.portfolio.bitfinex

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class WithdrawalCliTest {
    @Test
    fun `no --withdraw flag returns NotRequested`() {
        val result = WithdrawalCli.parse(arrayOf("BTC"))
        assertInstanceOf(CliResult.NotRequested::class.java, result)
    }

    @Test
    fun `empty args returns NotRequested`() {
        val result = WithdrawalCli.parse(emptyArray())
        assertInstanceOf(CliResult.NotRequested::class.java, result)
    }

    @Test
    fun `--withdraw without --confirm-withdrawal returns DryRun`() {
        val result =
            WithdrawalCli.parse(
                arrayOf("--withdraw", "BTC", "0.10", "bc1qexampleaddress"),
            )
        val dryRun = assertInstanceOf(CliResult.DryRun::class.java, result)
        assertEquals(Currency.BTC, dryRun.request.currency)
        assertEquals("0.10", dryRun.request.amount)
        assertEquals("bc1qexampleaddress", dryRun.request.destinationAddress)
    }

    @Test
    fun `--withdraw with --confirm-withdrawal returns Confirmed`() {
        val result =
            WithdrawalCli.parse(
                arrayOf("--withdraw", "ETH", "1.5", "0xabc", "--confirm-withdrawal"),
            )
        val confirmed = assertInstanceOf(CliResult.Confirmed::class.java, result)
        assertEquals(Currency.ETH, confirmed.request.currency)
        assertEquals("1.5", confirmed.request.amount)
        assertEquals("0xabc", confirmed.request.destinationAddress)
    }

    @Test
    fun `flag order is independent of position`() {
        val result =
            WithdrawalCli.parse(
                arrayOf("--confirm-withdrawal", "--withdraw", "BTC", "0.10", "bc1q"),
            )
        assertInstanceOf(CliResult.Confirmed::class.java, result)
    }

    @Test
    fun `--withdraw with wrong arg count throws`() {
        val ex =
            assertThrows(CliUsageException::class.java) {
                WithdrawalCli.parse(arrayOf("--withdraw", "BTC", "0.10"))
            }
        assert(ex.message!!.contains("Usage"))
    }

    @Test
    fun `unknown currency throws with allowed list`() {
        val ex =
            assertThrows(CliUsageException::class.java) {
                WithdrawalCli.parse(arrayOf("--withdraw", "DOGE", "0.10", "dogeaddress"))
            }
        assert(ex.message!!.contains("DOGE"))
        assert(ex.message!!.contains("BTC"))
    }

    @Test
    fun `non-numeric amount throws`() {
        val ex =
            assertThrows(CliUsageException::class.java) {
                WithdrawalCli.parse(arrayOf("--withdraw", "BTC", "lots", "bc1qaddress"))
            }
        assert(ex.message!!.contains("not a valid number"))
    }

    @Test
    fun `zero amount throws`() {
        val ex =
            assertThrows(CliUsageException::class.java) {
                WithdrawalCli.parse(arrayOf("--withdraw", "BTC", "0", "bc1qaddress"))
            }
        assert(ex.message!!.contains("must be positive"))
    }

    @Test
    fun `negative amount throws`() {
        val ex =
            assertThrows(CliUsageException::class.java) {
                WithdrawalCli.parse(arrayOf("--withdraw", "BTC", "-0.5", "bc1qaddress"))
            }
        assert(ex.message!!.contains("must be positive"))
    }

    @Test
    fun `blank destination address throws`() {
        val ex =
            assertThrows(CliUsageException::class.java) {
                WithdrawalCli.parse(arrayOf("--withdraw", "BTC", "0.10", "   "))
            }
        assert(ex.message!!.contains("must not be blank"))
    }

    @Test
    fun `redactAddress short address fully masked`() {
        assertEquals("***", WithdrawalCli.redactAddress("abc"))
        assertEquals("***", WithdrawalCli.redactAddress("abcdef"))
    }

    @Test
    fun `redactAddress long address keeps prefix and suffix`() {
        assertEquals("bc1q***x9", WithdrawalCli.redactAddress("bc1qexampleaddressx9"))
    }
}
