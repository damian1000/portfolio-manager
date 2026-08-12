package com.damianhoward.portfolio.bitfinex

import java.math.BigDecimal

data class WithdrawalRequest(val currency: Currency, val amount: String, val destinationAddress: String)

sealed class CliResult {
    object NotRequested : CliResult()

    data class DryRun(val request: WithdrawalRequest) : CliResult()

    data class Confirmed(val request: WithdrawalRequest) : CliResult()
}

class CliUsageException(message: String) : RuntimeException(message)

object WithdrawalCli {
    private const val WITHDRAW_FLAG = "--withdraw"
    private const val CONFIRM_FLAG = "--confirm-withdrawal"

    fun parse(args: Array<String>): CliResult {
        if (!args.contains(WITHDRAW_FLAG)) return CliResult.NotRequested

        val confirmed = args.contains(CONFIRM_FLAG)
        val positional = args.filter { it != WITHDRAW_FLAG && it != CONFIRM_FLAG }
        if (positional.size != 3) {
            throw CliUsageException(
                "Usage: bitfinex.MainKt --withdraw <currency> <amount> <destinationAddress> [--confirm-withdrawal]",
            )
        }

        val currency =
            try {
                Currency.valueOf(positional[0])
            } catch (e: IllegalArgumentException) {
                throw CliUsageException(
                    "Unsupported currency '${positional[0]}'. Allowed: ${Currency.entries.joinToString { it.name }}",
                )
            }

        val amount = positional[1]
        validateAmount(amount)

        val destinationAddress = positional[2]
        if (destinationAddress.isBlank()) {
            throw CliUsageException("Destination address must not be blank")
        }

        val request = WithdrawalRequest(currency, amount, destinationAddress)
        return if (confirmed) CliResult.Confirmed(request) else CliResult.DryRun(request)
    }

    private fun validateAmount(amount: String) {
        val parsed =
            try {
                BigDecimal(amount)
            } catch (e: NumberFormatException) {
                throw CliUsageException("Amount '$amount' is not a valid number")
            }
        if (parsed.signum() <= 0) {
            throw CliUsageException("Amount must be positive, got: $amount")
        }
    }

    fun redactAddress(address: String): String {
        if (address.length <= 6) return "***"
        return "${address.take(4)}***${address.takeLast(2)}"
    }
}
