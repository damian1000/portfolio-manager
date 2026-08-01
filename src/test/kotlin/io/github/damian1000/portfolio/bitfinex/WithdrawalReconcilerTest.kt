package io.github.damian1000.portfolio.bitfinex

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

class WithdrawalReconcilerTest {
    private val recordedAt: Instant = Instant.parse("2026-08-01T12:00:00Z")

    private fun record(state: WithdrawalState = WithdrawalState.UNKNOWN) = WithdrawalRecord(
        timestamp = recordedAt.toString(),
        withdrawalId = "id-1",
        state = state,
        mode = "LIVE",
        currency = "BTC",
        amount = "0.10",
        destination = "bc1q***x9",
    )

    private fun movement(
        id: Long = 1L,
        status: String? = "COMPLETED",
        amount: String = "-0.10",
        address: String = "bc1qexampleaddressx9",
        createdAt: Instant = recordedAt.plusSeconds(30),
    ) = Movement(
        id = id,
        status = status,
        amount = BigDecimal(amount),
        destinationAddress = address,
        createdTimestamp = LocalDateTime.ofInstant(createdAt, ZoneOffset.UTC),
    )

    private fun reconciler(movements: List<Movement>, now: Instant = recordedAt.plusSeconds(60)): WithdrawalReconciler {
        val gateway = mock<BitfinexGateway>()
        whenever(gateway.retrieveMovementHistory(any())).thenReturn(movements)
        return WithdrawalReconciler(gateway, Clock.fixed(now, ZoneId.of("UTC")))
    }

    @Test
    fun `a single completed movement confirms the withdrawal`() {
        val resolution = reconciler(listOf(movement())).resolve(record())

        assertEquals(WithdrawalState.CONFIRMED, (resolution as Resolution.Resolved).state)
    }

    @Test
    fun `a cancelled movement fails the withdrawal`() {
        val resolution = reconciler(listOf(movement(status = "CANCELED"))).resolve(record())

        assertEquals(WithdrawalState.FAILED, (resolution as Resolution.Resolved).state)
    }

    @Test
    fun `a pending movement stays unresolved because neither terminal state is honest yet`() {
        val resolution = reconciler(listOf(movement(status = "PENDING"))).resolve(record())

        assertTrue(resolution is Resolution.Unresolved, "PENDING is not an outcome")
    }

    @Test
    fun `an unrecognised status stays unresolved rather than being guessed`() {
        val resolution = reconciler(listOf(movement(status = "SOMETHING_NEW"))).resolve(record())

        assertTrue(resolution is Resolution.Unresolved)
    }

    @Test
    fun `absence inside the settling window is not evidence of failure`() {
        val resolution = reconciler(emptyList(), now = recordedAt.plusSeconds(60)).resolve(record())

        assertTrue(resolution is Resolution.Unresolved, "the venue may simply not have published it yet")
    }

    @Test
    fun `absence after the settling window means the venue never accepted it`() {
        val resolution =
            reconciler(emptyList(), now = recordedAt.plus(Duration.ofMinutes(11))).resolve(record())

        assertEquals(WithdrawalState.FAILED, (resolution as Resolution.Resolved).state)
    }

    @Test
    fun `two matching movements stay unresolved rather than picking one`() {
        val resolution = reconciler(listOf(movement(id = 1L), movement(id = 2L))).resolve(record())

        assertTrue(resolution is Resolution.Unresolved)
        assertTrue((resolution as Resolution.Unresolved).detail.contains("2 movements match"))
    }

    @Test
    fun `a movement to a different destination does not match`() {
        val resolution =
            reconciler(listOf(movement(address = "bc1qsomeotheraddressZZ")), now = recordedAt.plus(Duration.ofMinutes(11)))
                .resolve(record())

        assertEquals(WithdrawalState.FAILED, (resolution as Resolution.Resolved).state, "no match means no match")
    }

    @Test
    fun `amount is compared by value so trailing zeroes do not break matching`() {
        val resolution = reconciler(listOf(movement(amount = "-0.1"))).resolve(record())

        assertEquals(WithdrawalState.CONFIRMED, (resolution as Resolution.Resolved).state)
    }

    @Test
    fun `a movement predating the intent is not this withdrawal`() {
        val old = movement(createdAt = recordedAt.minus(Duration.ofHours(2)))
        val resolution = reconciler(listOf(old), now = recordedAt.plus(Duration.ofMinutes(11))).resolve(record())

        assertEquals(WithdrawalState.FAILED, (resolution as Resolution.Resolved).state)
    }

    @Test
    fun `an unreadable movement history stays unresolved rather than assuming failure`() {
        val gateway = mock<BitfinexGateway>()
        whenever(gateway.retrieveMovementHistory(any())).thenThrow(RuntimeException("venue unreachable"))

        val resolution =
            WithdrawalReconciler(gateway, Clock.fixed(recordedAt.plus(Duration.ofHours(1)), ZoneId.of("UTC")))
                .resolve(record())

        assertTrue(resolution is Resolution.Unresolved, "we cannot conclude anything without the venue")
        assertTrue((resolution as Resolution.Unresolved).detail.contains("venue unreachable"))
    }
}
