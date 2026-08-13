package com.damianhoward.portfolio.reconcile

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Duration

class BalanceReconcilerTest {
    private val reconciler = BalanceReconciler()

    private val t0 = 1_700_000_000_000L
    private val t1 = t0 + Duration.ofHours(6).toMillis()

    private fun snapshot(at: Long, vararg balances: Pair<String, String>) =
        BalanceSnapshot("bitfinex", at, balances.map { CurrencyBalance(it.first, BigDecimal(it.second)) })

    private fun movement(currency: String, amount: String, at: Long) = LedgerMovement(currency, BigDecimal(amount), at)

    /** Mid-window, comfortably clear of either boundary. */
    private val midWindow = t0 + Duration.ofHours(3).toMillis()

    @Test
    fun `a balance that moved by exactly its movements is accounted for`() {
        val result =
            reconciler.reconcile(
                snapshot(t0, "BTC" to "1.0"),
                snapshot(t1, "BTC" to "1.5"),
                listOf(movement("BTC", "0.5", midWindow)),
            )

        assertEquals(CurrencyVerdict.Accounted("BTC", BigDecimal("0.5")), result.verdicts.single())
        assertFalse(result.needsAttention)
    }

    @Test
    fun `a withdrawal is a negative movement and reduces the balance`() {
        val result =
            reconciler.reconcile(
                snapshot(t0, "BTC" to "1.0"),
                snapshot(t1, "BTC" to "0.4"),
                listOf(movement("BTC", "-0.6", midWindow)),
            )

        assertTrue(result.verdicts.single() is CurrencyVerdict.Accounted)
    }

    @Test
    fun `money that arrived with no movement behind it is unexplained, and signed from the balance's side`() {
        val result =
            reconciler.reconcile(
                snapshot(t0, "BTC" to "1.0"),
                snapshot(t1, "BTC" to "1.5"),
                emptyList(),
            )

        val verdict = result.unexplained.single()
        assertEquals(BigDecimal("0.5"), verdict.unexplained)
        assertTrue(result.needsAttention)
    }

    @Test
    fun `money that left with no movement behind it is unexplained the other way`() {
        val result =
            reconciler.reconcile(
                snapshot(t0, "BTC" to "1.0"),
                snapshot(t1, "BTC" to "0.7"),
                emptyList(),
            )

        assertEquals(BigDecimal("-0.3"), result.unexplained.single().unexplained)
    }

    @Test
    fun `a partial explanation is still unexplained, and carries both halves`() {
        // The number an operator needs is not the gap alone: 0.5 arrived, 0.2 is documented, so
        // the question is where 0.3 came from — and knowing 0.2 was legitimate is part of asking it.
        val result =
            reconciler.reconcile(
                snapshot(t0, "BTC" to "1.0"),
                snapshot(t1, "BTC" to "1.5"),
                listOf(movement("BTC", "0.2", midWindow)),
            )

        val verdict = result.unexplained.single()
        assertEquals(BigDecimal("0.5"), verdict.delta)
        assertEquals(BigDecimal("0.2"), verdict.accountedFor)
        assertEquals(BigDecimal("0.3"), verdict.unexplained)
    }

    @Test
    fun `trailing zeroes do not manufacture a difference`() {
        // BigDecimal.equals is false for 1.0 against 1.00, and venues are not consistent about
        // scale. Comparing with equals here would report drift on every quiet interval.
        val result =
            reconciler.reconcile(
                snapshot(t0, "BTC" to "1.0"),
                snapshot(t1, "BTC" to "1.50"),
                listOf(movement("BTC", "0.500", midWindow)),
            )

        assertTrue(result.verdicts.single() is CurrencyVerdict.Accounted, result.verdicts.toString())
    }

    @Test
    fun `a movement outside the window does not explain a change inside it`() {
        val result =
            reconciler.reconcile(
                snapshot(t0, "BTC" to "1.0"),
                snapshot(t1, "BTC" to "1.5"),
                listOf(movement("BTC", "0.5", t1 + Duration.ofHours(2).toMillis())),
            )

        assertEquals(BigDecimal("0.5"), result.unexplained.single().unexplained)
    }

    @Test
    fun `a movement near a boundary withholds the verdict rather than guessing`() {
        // The two clocks are not shared: the snapshot carries ours, the movement carries the
        // venue's. A movement on the edge may land inside one side and outside the other, and a
        // difference produced that way is measurement, not drift.
        val result =
            reconciler.reconcile(
                snapshot(t0, "BTC" to "1.0"),
                snapshot(t1, "BTC" to "1.5"),
                listOf(movement("BTC", "0.5", t1 - Duration.ofMinutes(1).toMillis())),
            )

        val verdict = result.inconclusive.single()
        assertTrue(verdict.reason.contains("boundary"), verdict.reason)
        assertFalse(result.needsAttention, "not knowing is not the same as knowing something is wrong")
    }

    @Test
    fun `a boundary movement is inconclusive even when the arithmetic happens to agree`() {
        // Checked before the comparison on purpose. A coincidental match is not evidence the
        // window was clean, and reporting it as accounted would hide the one case worth a look.
        val result =
            reconciler.reconcile(
                snapshot(t0, "BTC" to "1.0"),
                snapshot(t1, "BTC" to "1.5"),
                listOf(movement("BTC", "0.5", t0 + Duration.ofMinutes(2).toMillis())),
            )

        assertTrue(result.verdicts.single() is CurrencyVerdict.Inconclusive)
    }

    @Test
    fun `a currency that only appears in one snapshot is still judged`() {
        // A currency arriving is the interesting case, not an edge to drop: it means a balance
        // exists now that did not before, and something must account for it.
        val result =
            reconciler.reconcile(
                snapshot(t0, "BTC" to "1.0"),
                snapshot(t1, "BTC" to "1.0", "ETH" to "2.0"),
                emptyList(),
            )

        assertEquals(listOf("BTC", "ETH"), result.verdicts.map { it.currency })
        assertEquals(BigDecimal("2.0"), result.unexplained.single().unexplained)
    }

    @Test
    fun `each currency is judged on its own`() {
        val result =
            reconciler.reconcile(
                snapshot(t0, "BTC" to "1.0", "ETH" to "5.0"),
                snapshot(t1, "BTC" to "1.5", "ETH" to "5.0"),
                listOf(movement("BTC", "0.9", midWindow)),
            )

        assertEquals(listOf("BTC"), result.unexplained.map { it.currency })
        assertTrue(result.verdicts.first { it.currency == "ETH" } is CurrencyVerdict.Accounted)
    }

    @Test
    fun `a movement in another currency never explains this one`() {
        val result =
            reconciler.reconcile(
                snapshot(t0, "BTC" to "1.0"),
                snapshot(t1, "BTC" to "1.5"),
                listOf(movement("ETH", "0.5", midWindow)),
            )

        assertEquals(BigDecimal("0.5"), result.unexplained.single().unexplained)
    }

    @Test
    fun `reconciling two different venues is a programming error, not a drift report`() {
        val e =
            assertThrows(IllegalArgumentException::class.java) {
                reconciler.reconcile(
                    BalanceSnapshot("binance", t0, emptyList()),
                    BalanceSnapshot("bitfinex", t1, emptyList()),
                    emptyList(),
                )
            }
        assertTrue(e.message!!.contains("binance"), e.message)
    }

    @Test
    fun `snapshots in the wrong order are rejected rather than silently inverted`() {
        assertThrows(IllegalArgumentException::class.java) {
            reconciler.reconcile(snapshot(t1, "BTC" to "1.0"), snapshot(t0, "BTC" to "1.0"), emptyList())
        }
    }

    @Test
    fun `an untouched account over a quiet interval reports nothing to look at`() {
        val result =
            reconciler.reconcile(
                snapshot(t0, "BTC" to "1.0", "ETH" to "5.0"),
                snapshot(t1, "BTC" to "1.0", "ETH" to "5.0"),
                emptyList(),
            )

        assertFalse(result.needsAttention)
        assertTrue(result.verdicts.all { it is CurrencyVerdict.Accounted })
    }
}
