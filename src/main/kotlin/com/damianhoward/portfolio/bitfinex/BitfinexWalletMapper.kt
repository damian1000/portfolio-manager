package com.damianhoward.portfolio.bitfinex

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.math.BigDecimal

/**
 * Maps `v2/auth/r/wallets` into [Wallet].
 *
 * Bitfinex v2 answers with positional arrays rather than objects, so a field is identified by its
 * index and the indices are named here for the same reason [BitfinexMovementMapper] names its own:
 * a bare `row[4]` at a call site is unreadable and silently wrong the day the venue inserts a
 * field. Trailing elements the venue adds are ignored, which is what the index approach gives for
 * free; a field removed from the middle would shift everything after it, and that surfaces as a
 * parse failure on the required indices rather than as a plausible wrong number.
 */
class BitfinexWalletMapper {
    private val mapper = jacksonObjectMapper()

    /** @throws IllegalArgumentException when the response is not the documented wallets shape */
    fun mapWallets(json: String): List<Wallet> {
        val rows: List<List<Any?>> =
            try {
                mapper.readValue(json)
            } catch (e: JsonProcessingException) {
                // Deliberately without the cause's message, which carries the payload — and the
                // payload here is account balances.
                throw IllegalArgumentException("Bitfinex wallets response did not parse", e)
            }
        return rows.map { row ->
            Wallet(
                type = row.required(TYPE_INDEX).toString(),
                currency = row.required(CURRENCY_INDEX).toString(),
                balance = BigDecimal(row.required(BALANCE_INDEX).toString()),
                // Null while the venue is still calculating it, which is not zero.
                availableBalance = row.getOrNull(AVAILABLE_BALANCE_INDEX)?.toString()?.let(::BigDecimal),
            )
        }
    }

    companion object {
        private const val TYPE_INDEX = 0
        private const val CURRENCY_INDEX = 1
        private const val BALANCE_INDEX = 2
        private const val AVAILABLE_BALANCE_INDEX = 4

        private fun List<Any?>.required(index: Int): Any = getOrNull(index) ?: throw IllegalArgumentException("Bitfinex wallet field $index is missing")
    }
}
