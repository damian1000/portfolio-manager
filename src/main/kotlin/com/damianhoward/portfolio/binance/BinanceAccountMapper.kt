package com.damianhoward.portfolio.binance

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

/**
 * Maps Binance's `/api/v3/account` response into [Account].
 *
 * The models existed before this did, and nothing mapped into them: the gateway returned the raw
 * JSON string and the only reference to [Account] was a test constructing one. That is the shape
 * of an unused abstraction — and it also meant the caller logged the entire account document,
 * commission schedule and permissions included, to say what the balances were.
 *
 * [Account] is annotated `@JsonIgnoreProperties(ignoreUnknown = true)`, so a field the venue adds
 * is ignored rather than fatal. A field it *removes* or renames is the case worth catching, and
 * that surfaces here as a mapping failure rather than at whatever later point read a null.
 */
class BinanceAccountMapper {
    private val mapper = jacksonObjectMapper()

    /** @throws IllegalArgumentException when the response is not the documented account shape */
    fun mapAccount(json: String): Account = try {
        mapper.readValue<Account>(json)
    } catch (e: JsonProcessingException) {
        // The message carries the offending payload, and a wallet response is account data —
        // so it is deliberately not propagated. The caller logs the type, not the content.
        throw IllegalArgumentException("Binance account response did not parse", e)
    }
}
