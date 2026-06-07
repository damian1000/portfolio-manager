package io.github.damian1000.portfolio.binance

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * Account information.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class Account(val balances: List<AssetBalance>?)
