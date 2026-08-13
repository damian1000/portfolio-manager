package com.damianhoward.portfolio.binance

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * A Binance account, as `/api/v3/account` reports it. Only the balances are modelled; the
 * commission schedule, permissions and account flags are ignored because nothing here reads them,
 * and a model that carries fields no caller uses is a maintenance cost with no reader.
 *
 * [balances] is nullable because the venue's absence of the field and an empty account are
 * different facts, and collapsing them would let a failed mapping read as a flat account.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class Account(val balances: List<AssetBalance>?) {
    /**
     * Balances with something in them. Binance returns a row for every asset it lists, so the
     * response is hundreds of zeroes around the handful that matter.
     */
    fun nonZeroBalances(): List<AssetBalance> = balances.orEmpty().filter { it.total.signum() > 0 }
}
