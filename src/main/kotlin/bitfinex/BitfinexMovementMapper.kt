package bitfinex

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class BitfinexMovementMapper {

    private val mapper = jacksonObjectMapper()

    fun mapMovement(json: String): List<Movement> {
        val transfers = mutableListOf<Movement>()
        try {
            val history:List<List<String>> = mapper.readValue(json);
            for (bitfinexTransfer in history) {
                val transfer = Movement()
                transfers.add(transfer)
                transfer.id = java.lang.Long.valueOf(bitfinexTransfer[ID_INDEX])
                transfer.currencyCode = bitfinexTransfer[CURRENCY_CODE_INDEX]
                transfer.currencyValue = bitfinexTransfer[CURRENCY_VALUE_INDEX]
                transfer.createdTimestamp = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(bitfinexTransfer[CREATED_TIMESTAMP_INDEX].toLong()), ZoneId.of("UTC"));
                transfer.updatedTimestamp = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(bitfinexTransfer[UPDATED_TIMESTAMP_INDEX].toLong()), ZoneId.of("UTC"));
                transfer.status = bitfinexTransfer[STATUS_INDEX]
                transfer.amount = BigDecimal(bitfinexTransfer[AMOUNT_INDEX])
                transfer.fees = BigDecimal(bitfinexTransfer[FEES_INDEX])
                transfer.destinationAddress = bitfinexTransfer[DESTINATION_ADDRESS_INDEX]
                transfer.transactionId = bitfinexTransfer[TRANSACTION_ID_INDEX]
                transfer.withdrawTransactionNote = bitfinexTransfer[NOTE_INDEX]
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
    }
}