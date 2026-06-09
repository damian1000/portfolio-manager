package io.github.damian1000.portfolio.bitfinex

import java.math.BigDecimal
import java.time.LocalDateTime

data class Movement(
    var id: Long? = null,
    var currencyCode: String? = null,
    var currencyValue: String? = null,
    var createdTimestamp: LocalDateTime? = null,
    var updatedTimestamp: LocalDateTime? = null,
    var status: String? = null,
    var amount: BigDecimal? = null,
    var fees: BigDecimal? = null,
    var destinationAddress: String? = null,
    var transactionId: String? = null,
    var withdrawTransactionNote: String? = null,
)
