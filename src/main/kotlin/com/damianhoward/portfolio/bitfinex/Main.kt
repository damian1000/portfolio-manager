package com.damianhoward.portfolio.bitfinex

import com.damianhoward.portfolio.reconcile.BalanceReconciler
import com.damianhoward.portfolio.reconcile.BalanceSnapshot
import com.damianhoward.portfolio.reconcile.CurrencyBalance
import com.damianhoward.portfolio.reconcile.LedgerMovement
import com.damianhoward.portfolio.reconcile.SnapshotStore
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlin.system.exitProcess

private val log = LoggerFactory.getLogger("com.damianhoward.portfolio.bitfinex.Main")

private const val EXIT_OK = 0
private const val EXIT_FAILED = 1
private const val EXIT_USAGE = 64
private const val EXIT_UNRESOLVED = 75

/** The venue name snapshots are keyed by; one store holds both venues. */
private const val VENUE = "bitfinex"

fun main(args: Array<String>) {
    exitProcess(run(args))
}

internal fun run(
    args: Array<String>,
    bitfinexGateway: BitfinexGateway = BitfinexGateway(),
    credentials: ApiCredentials = ApiCredentials(),
    journalFactory: () -> WithdrawalJournal = WithdrawalJournal::openDefault,
    reconcilerFactory: (BitfinexGateway) -> WithdrawalReconciler = { WithdrawalReconciler(it) },
    withdrawalIdSupplier: () -> String = { UUID.randomUUID().toString() },
    snapshotFactory: () -> SnapshotStore = SnapshotStore::openDefault,
    balanceReconciler: BalanceReconciler = BalanceReconciler(),
    clock: Clock = Clock.systemUTC(),
): Int {
    val cliResult =
        try {
            WithdrawalCli.parse(args)
        } catch (e: CliUsageException) {
            log.error("Invalid arguments: {}", e.message)
            return EXIT_USAGE
        }

    val journal = journalFactory()

    // Before anything else, and regardless of what was asked for: settle any withdrawal an earlier
    // run lost track of. A run that leaves an unresolved record behind and submits a new withdrawal
    // anyway is the duplicate-payment bug.
    val stillUnresolved = reconcileOutstanding(journal, reconcilerFactory(bitfinexGateway))
    if (stillUnresolved.isNotEmpty()) {
        stillUnresolved.forEach {
            log.error(
                "withdrawal {} is unresolved (state={} currency={} amount={} destination={})",
                it.withdrawalId,
                it.state,
                it.currency,
                it.amount,
                it.destination,
            )
        }
        log.error(
            "Refusing to act while {} withdrawal(s) remain unresolved. Re-run once the venue has published them, " +
                "or settle them by hand against the movement history.",
            stillUnresolved.size,
        )
        return EXIT_UNRESOLVED
    }

    val readCurrency =
        when (cliResult) {
            is CliResult.NotRequested -> Currency.BTC
            is CliResult.DryRun -> cliResult.request.currency
            is CliResult.Confirmed -> cliResult.request.currency
        }
    val readOk = readPortfolio(bitfinexGateway, credentials, readCurrency, snapshotFactory(), balanceReconciler, clock)

    return when (cliResult) {
        is CliResult.NotRequested -> if (readOk) EXIT_OK else EXIT_FAILED
        is CliResult.DryRun -> {
            logDryRun(cliResult.request)
            // A dry run never reaches the withdraw endpoint, so its outcome is never in doubt:
            // record it terminal rather than leaving something for the next run to reconcile.
            journal.record(
                withdrawalIdSupplier(),
                WithdrawalState.FAILED,
                "DRY_RUN",
                cliResult.request,
                "not submitted",
            )
            if (readOk) EXIT_OK else EXIT_FAILED
        }
        is CliResult.Confirmed -> {
            // The read doubles as a pre-flight check: if the venue can't even be read (auth,
            // connectivity), don't attempt to move money — abort loudly and leave an audit trail.
            if (!readOk) {
                log.error("Aborting withdrawal: the pre-flight portfolio read failed; nothing was submitted")
                journal.record(
                    withdrawalIdSupplier(),
                    WithdrawalState.FAILED,
                    "LIVE",
                    cliResult.request,
                    "aborted: pre-flight read failed",
                )
                EXIT_FAILED
            } else {
                submitWithdrawal(
                    bitfinexGateway,
                    credentials,
                    cliResult.request,
                    journal,
                    withdrawalIdSupplier(),
                    snapshotFactory(),
                    balanceReconciler,
                    clock,
                )
            }
        }
    }
}

/**
 * Asks the venue about every withdrawal the journal has not seen through to a terminal state, and
 * returns those still in doubt afterwards.
 */
private fun reconcileOutstanding(journal: WithdrawalJournal, reconciler: WithdrawalReconciler): List<WithdrawalRecord> {
    val outstanding = journal.unresolved()
    if (outstanding.isEmpty()) return emptyList()

    log.warn(
        "{} withdrawal(s) left unresolved by an earlier run; reconciling against movement history",
        outstanding.size,
    )
    return outstanding.filter { record ->
        when (val resolution = reconciler.resolve(record)) {
            is Resolution.Resolved -> {
                log.warn("withdrawal {} resolved to {}: {}", record.withdrawalId, resolution.state, resolution.detail)
                journal.append(
                    record.copy(
                        timestamp = Instant.now().toString(),
                        state = resolution.state,
                        detail = "reconciled: ${resolution.detail}",
                    ),
                )
                false
            }
            is Resolution.Unresolved -> {
                log.error("withdrawal {} still unresolved: {}", record.withdrawalId, resolution.detail)
                true
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

private fun readPortfolio(
    gateway: BitfinexGateway,
    credentials: ApiCredentials,
    currency: Currency,
    snapshots: SnapshotStore,
    reconciler: BalanceReconciler,
    clock: Clock,
): Boolean = try {
    // One line per wallet rather than the raw response. The venue returns a row per wallet type
    // per currency, so the document is mostly structure; what an operator reads this for is which
    // currency sits where, and how much of it can actually be moved.
    val wallets = gateway.retrieveWallets()
    wallets.forEach {
        log.info(
            "wallet: type={} currency={} balance={} available={}",
            it.type,
            it.currency,
            it.balance,
            it.availableBalance ?: "calculating",
        )
    }
    val movements = gateway.retrieveMovementHistory(currency.name)
    movements.forEach { log.info("movement: {}", it) }
    log.info("settings: {}", gateway.retrieveSettingsForKey(credentials.apiKey()))
    reconcileBalances(wallets, movements, snapshots, reconciler, clock)
    true
} catch (e: Exception) {
    log.error("Bitfinex read failed", e)
    false
}

/**
 * Takes a balance snapshot, and reports what changed since the last one that the venue's own
 * movement records do not account for.
 *
 * The snapshot is recorded whatever the verdict, including on the first run when there is nothing
 * to compare against. A run that reports drift and forgets to leave an anchor makes the *next* run
 * unable to say anything, which is the failure that turns a check into a one-off.
 *
 * A balance is summed across wallet types. Bitfinex splits a currency over exchange, margin and
 * funding, and an internal transfer between them is not a movement — reconciling per wallet type
 * would report every such transfer as unexplained on both sides.
 */
private fun reconcileBalances(wallets: List<Wallet>, movements: List<Movement>, snapshots: SnapshotStore, reconciler: BalanceReconciler, clock: Clock) {
    val balances =
        wallets
            .groupBy { it.currency }
            .map { (currency, rows) -> CurrencyBalance(currency, rows.fold(BigDecimal.ZERO) { sum, w -> sum + w.balance }) }
    val current = BalanceSnapshot(VENUE, clock.millis(), balances)

    val previous = snapshots.latestFor(VENUE)
    if (previous == null) {
        snapshots.append(current)
        log.info("balance reconciliation: first snapshot recorded; nothing to measure a change from yet")
        return
    }

    val ledger =
        movements.mapNotNull { m ->
            val code = m.currencyCode ?: return@mapNotNull null
            val amount = m.amount ?: return@mapNotNull null
            val settledAt = m.updatedTimestamp ?: m.createdTimestamp ?: return@mapNotNull null
            LedgerMovement(code, amount, settledAt.toInstant(ZoneOffset.UTC).toEpochMilli())
        }

    val result = reconciler.reconcile(previous, current, ledger)
    snapshots.append(current)

    result.inconclusive.forEach { log.info("balance reconciliation: {} not judged — {}", it.currency, it.reason) }
    if (result.needsAttention) {
        result.unexplained.forEach {
            log.warn(
                "balance reconciliation: {} moved by {} with {} accounted for; {} is unexplained",
                it.currency,
                it.delta,
                it.accountedFor,
                it.unexplained,
            )
        }
    } else {
        log.info("balance reconciliation: every judged balance is accounted for by its movements")
    }
}

private fun submitWithdrawal(
    gateway: BitfinexGateway,
    credentials: ApiCredentials,
    request: WithdrawalRequest,
    journal: WithdrawalJournal,
    withdrawalId: String,
    snapshots: SnapshotStore,
    balanceReconciler: BalanceReconciler,
    clock: Clock,
): Int {
    log.warn(
        "[LIVE] submitting withdrawal {}: currency={} amount={} destination={}",
        withdrawalId,
        request.currency,
        request.amount,
        WithdrawalCli.redactAddress(request.destinationAddress),
    )

    // Durable before the venue is contacted. Everything after this point can fail in a way that
    // leaves money moving, and this record is what lets the next run find out that it might have.
    journal.record(withdrawalId, WithdrawalState.INTENT, "LIVE", request, "about to submit")

    val response =
        try {
            gateway.submitWithdrawalRequest(
                request.currency,
                request.amount,
                request.destinationAddress,
                withdrawalId,
            )
        } catch (e: Exception) {
            // Not FAILED. A timeout or a dropped connection says nothing about whether the venue
            // accepted the request, and recording it as a failure is what invites someone to retry
            // into a second withdrawal. Only the movement history can settle this.
            log.error("Withdrawal {} outcome is unknown", withdrawalId, e)
            journal.record(withdrawalId, WithdrawalState.UNKNOWN, "LIVE", request, "no response: ${e.message}")
            log.error(
                "Withdrawal {} may or may not have been accepted. Nothing will be resubmitted; " +
                    "re-run to reconcile it against the venue's movement history.",
                withdrawalId,
            )
            return EXIT_UNRESOLVED
        }

    log.info("withdrawal response: {}", response)
    journal.record(withdrawalId, WithdrawalState.SUBMITTED, "LIVE", request, "venue responded: $response")

    // SUBMITTED is not terminal: the venue has taken it, but only the movement history says whether
    // it settled. The next run picks it up and reconciles it.
    readPortfolio(gateway, credentials, request.currency, snapshots, balanceReconciler, clock)
    return EXIT_OK
}
