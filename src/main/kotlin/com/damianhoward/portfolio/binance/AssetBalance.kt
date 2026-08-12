package com.damianhoward.portfolio.binance

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.math.BigDecimal

@JsonIgnoreProperties(ignoreUnknown = true)
data class AssetBalance(val asset: String, val free: BigDecimal, val locked: BigDecimal)
