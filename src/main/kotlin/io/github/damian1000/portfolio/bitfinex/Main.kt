package io.github.damian1000.portfolio.bitfinex

import org.slf4j.LoggerFactory
import kotlin.system.exitProcess

private val log = LoggerFactory.getLogger("io.github.damian1000.portfolio.bitfinex.Main")

fun main(args: Array<String>) {
    exitProcess(run(args))
}

internal fun run(
    args: Array<String>,
    bitfinexGateway: BitfinexGateway = BitfinexGateway(),
    propertyHelper: PropertyHelper = PropertyHelper(),
    auditLogFactory: () -> WithdrawalAuditLog = WithdrawalAuditLog::openDefault,
): Int {
    val cliResult =
        try {
            WithdrawalCli.parse(args)
        } catch (e: CliUsageException) {
            log.error("Invalid arguments: {}", e.message)
            return 64
        }

    val readCurrency =
        when (cliResult) {
            is CliResult.NotRequested -> Currency.BTC
            is CliResult.DryRun -> cliResult.request.currency
            is CliResult.Confirmed -> cliResult.request.currency
        }
    val readOk = readPortfolio(bitfinexGateway, propertyHelper, readCurrency)

    return when (cliResult) {
        is CliResult.NotRequested -> if (readOk) 0 else 1
        is CliResult.DryRun -> {
            logDryRun(cliResult.request)
            auditLogFactory().use { it.record("DRY_RUN", cliResult.request, "not submitted") }
            if (readOk) 0 else 1
        }
        is CliResult.Confirmed -> {
            val withdrawExitCode =
                submitWithdrawal(
                    bitfinexGateway,
                    propertyHelper,
                    cliResult.request,
                    auditLogFactory,
                )
            when {
                withdrawExitCode != 0 -> withdrawExitCode
                readOk -> 0
                else -> 1
            }
        }
    }
}

private fun logDryRun(request: WithdrawalRequest) {
    log.info(
        "[DRY-RUN] would submit withdrawal: currency={} amount={} destination={} (pass --confirm-withdrawal to send)",
        request.currency,
        request.amount,
        WithdrawalCli.redactAddress(request.destinationAddress),
    )
}

private fun readPortfolio(gateway: BitfinexGateway, propertyHelper: PropertyHelper, currency: Currency): Boolean = try {
    log.info("wallets: {}", gateway.retrieveWallets())
    gateway.retrieveMovementHistory(currency.name).forEach { log.info("movement: {}", it) }
    log.info("settings: {}", gateway.retrieveSettingsForKey(propertyHelper.getApiKey()))
    true
} catch (e: Exception) {
    log.error("Bitfinex read failed", e)
    false
}

private fun submitWithdrawal(
    gateway: BitfinexGateway,
    propertyHelper: PropertyHelper,
    request: WithdrawalRequest,
    auditLogFactory: () -> WithdrawalAuditLog,
): Int {
    log.warn(
        "[LIVE] submitting withdrawal: currency={} amount={} destination={}",
        request.currency,
        request.amount,
        WithdrawalCli.redactAddress(request.destinationAddress),
    )
    return try {
        val response =
            gateway.submitWithdrawalRequest(
                request.currency,
                request.amount,
                request.destinationAddress,
            )
        log.info("withdrawal response: {}", response)
        auditLogFactory().use { it.record("LIVE", request, "submitted: $response") }
        readPortfolio(gateway, propertyHelper, request.currency)
        0
    } catch (e: Exception) {
        log.error("Withdrawal failed", e)
        auditLogFactory().use { it.record("LIVE", request, "failed: ${e.message}") }
        1
    }
}
