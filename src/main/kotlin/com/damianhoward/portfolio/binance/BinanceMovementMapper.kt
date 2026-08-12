package com.damianhoward.portfolio.binance

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class BinanceMovementMapper {
    private val mapper = jacksonObjectMapper()

    fun mapMovement(json: String): List<Movement> {
        val transfers = mutableListOf<Movement>()
        try {
            val history: List<List<Any?>> = mapper.readValue(json)
            for (binanceTransfer in history) {
                val transfer = Movement()
                transfers.add(transfer)
                transfer.id = binanceTransfer.required(ID_INDEX).toString().toLong()
                transfer.currencyCode = binanceTransfer.optionalString(CURRENCY_CODE_INDEX)
                transfer.currencyValue = binanceTransfer.optionalString(CURRENCY_VALUE_INDEX)
                transfer.createdTimestamp =
                    LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(binanceTransfer.required(CREATED_TIMESTAMP_INDEX).toString().toLong()),
                        ZoneId.of("UTC"),
                    )
                transfer.updatedTimestamp =
                    LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(binanceTransfer.required(UPDATED_TIMESTAMP_INDEX).toString().toLong()),
                        ZoneId.of("UTC"),
                    )
                transfer.status = binanceTransfer.optionalString(STATUS_INDEX)
                transfer.amount = binanceTransfer.optionalBigDecimal(AMOUNT_INDEX)
                transfer.fees = binanceTransfer.optionalBigDecimal(FEES_INDEX)
                transfer.destinationAddress = binanceTransfer.optionalString(DESTINATION_ADDRESS_INDEX)
                transfer.transactionId = binanceTransfer.optionalString(TRANSACTION_ID_INDEX)
                transfer.withdrawTransactionNote = binanceTransfer.optionalString(NOTE_INDEX)
            }
        } catch (e: JsonProcessingException) {
            throw RuntimeException(e)
        }
        return transfers
    }

    companion object {
        private const val ID_INDEX = 0
        private const val CURRENCY_CODE_INDEX = 1
        private const val CURRENCY_VALUE_INDEX = 2
        private const val CREATED_TIMESTAMP_INDEX = 5
        private const val UPDATED_TIMESTAMP_INDEX = 6
        private const val STATUS_INDEX = 9
        private const val AMOUNT_INDEX = 12
        private const val FEES_INDEX = 13
        private const val DESTINATION_ADDRESS_INDEX = 16
        private const val TRANSACTION_ID_INDEX = 20
        private const val NOTE_INDEX = 21

        private fun List<Any?>.required(index: Int): Any = getOrNull(index) ?: throw IllegalArgumentException("Binance movement field $index is missing")

        private fun List<Any?>.optionalString(index: Int): String? = getOrNull(index)?.toString()

        private fun List<Any?>.optionalBigDecimal(index: Int): BigDecimal? = getOrNull(index)?.toString()?.let(::BigDecimal)
    }
}
