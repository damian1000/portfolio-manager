package com.damianhoward.portfolio.bitfinex

/**
 * Builds the string Bitfinex requires a request to be authenticated over: the API path prefixed
 * with `/api/`, then the nonce, then the JSON body, concatenated with no separators.
 *
 * The concatenation order is the contract and is not arbitrary — the venue rebuilds this same
 * string server-side and compares HMACs, so a wrong order fails authentication rather than
 * producing a wrong result. It is kept apart from [HmacSigner] because this half is the venue's
 * message format and the other half is a standard MAC over whatever it is handed.
 */
class SignaturePayload {
    fun of(body: String, nonce: String, path: String): String = "/api/$path$nonce$body"
}
