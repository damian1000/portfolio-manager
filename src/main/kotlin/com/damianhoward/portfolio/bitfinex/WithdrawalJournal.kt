package com.damianhoward.portfolio.bitfinex

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant

/**
 * Append-only record of every withdrawal state transition, one JSON object per line.
 *
 * Replaces the tab-separated audit log. The properties that made that log worth having are kept —
 * append-only, destination always redacted — and the two that were missing are added: the file can
 * be read back, and a write is on disk before the caller continues.
 *
 * Durability is the point of this class. The previous log wrote through an auto-flushing
 * `PrintWriter`, which reaches the operating system but not the platter; a machine that loses power
 * between the flush and the venue call would come back with no evidence the withdrawal existed.
 * Each [append] opens, writes, `fsync`s and closes. That is a poor design for a hot path and the
 * right one for a handful of records guarding money movement.
 */
class WithdrawalJournal(private val file: Path) {
    private val mapper: ObjectMapper = jacksonObjectMapper()

    /** Appends one record and does not return until it is durable. */
    fun append(record: WithdrawalRecord) {
        val line = mapper.writeValueAsString(record).replace("\n", " ") + "\n"
        file.parent?.let { Files.createDirectories(it) }
        FileOutputStream(file.toFile(), true).use { out ->
            out.write(line.toByteArray(StandardCharsets.UTF_8))
            out.flush()
            out.fd.sync()
        }
    }

    fun record(withdrawalId: String, state: WithdrawalState, mode: String, request: WithdrawalRequest, detail: String = "") {
        append(
            WithdrawalRecord(
                timestamp = Instant.now().toString(),
                withdrawalId = withdrawalId,
                state = state,
                mode = mode,
                currency = request.currency.name,
                amount = request.amount,
                destination = WithdrawalCli.redactAddress(request.destinationAddress),
                detail = detail.replace("\n", " "),
            ),
        )
    }

    fun records(): List<WithdrawalRecord> {
        if (!Files.exists(file)) return emptyList()
        return Files
            .readAllLines(file, StandardCharsets.UTF_8)
            .filter { it.isNotBlank() }
            .map { mapper.readValue<WithdrawalRecord>(it) }
    }

    /**
     * The latest state of every withdrawal that has not reached a terminal one.
     *
     * A withdrawal is identified by [WithdrawalRecord.withdrawalId], and the last line wins, so
     * replaying the file gives the current position without any separate state file to keep in
     * step. Ordering is the file's own: appends are serial, and a CLI run writes its records in
     * the order they happened.
     */
    fun unresolved(): List<WithdrawalRecord> = records()
        .groupBy { it.withdrawalId }
        .values
        .map { it.last() }
        .filterNot { it.state.isTerminal }
        .sortedBy { it.timestamp }

    companion object {
        /**
         * A new filename rather than the old `.log`. The format changed, and appending JSON lines
         * to a file that already holds tab-separated ones would leave a journal that cannot be
         * read back — which is the whole reason this class exists. Any existing `.log` stays where
         * it is as a historical record.
         */
        const val FILE_NAME = "bitfinex-withdrawals.jsonl"

        fun openDefault(): WithdrawalJournal {
            val dir =
                System
                    .getenv("PORTFOLIO_AUDIT_DIR")
                    ?.let { Path.of(it) }
                    ?: Path.of(System.getProperty("user.home"), ".portfolio-manager")
            Files.createDirectories(dir)
            return WithdrawalJournal(dir.resolve(FILE_NAME))
        }
    }
}
