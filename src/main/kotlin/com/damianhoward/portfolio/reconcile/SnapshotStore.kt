package com.damianhoward.portfolio.reconcile

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

/**
 * Append-only history of balance snapshots, one JSON object per line, in the same shape and with
 * the same durability discipline as `WithdrawalJournal`.
 *
 * Append-only rather than last-value-wins because the interesting question is not "what is the
 * balance" — the venue answers that on demand — but "what did it do between two points". Keeping
 * every snapshot means a reconciliation can be re-run later against the same pair, which a file
 * holding only the latest could never support.
 *
 * Each write opens, writes, `fsync`s and closes. That is the wrong design for a hot path and the
 * right one for a handful of records that a money-movement decision is made against: a snapshot
 * that reached the page cache and not the disk would leave the next run measuring from a point
 * that no longer exists.
 */
class SnapshotStore(private val file: Path) {
    private val mapper: ObjectMapper = jacksonObjectMapper()

    /** Appends one snapshot and does not return until it is durable. */
    fun append(snapshot: BalanceSnapshot) {
        val line = mapper.writeValueAsString(snapshot).replace("\n", " ") + "\n"
        file.parent?.let { Files.createDirectories(it) }
        FileOutputStream(file.toFile(), true).use { out ->
            out.write(line.toByteArray(StandardCharsets.UTF_8))
            out.flush()
            out.fd.sync()
        }
    }

    fun snapshots(): List<BalanceSnapshot> {
        if (!Files.exists(file)) return emptyList()
        return Files
            .readAllLines(file, StandardCharsets.UTF_8)
            .filter { it.isNotBlank() }
            .map { mapper.readValue<BalanceSnapshot>(it) }
    }

    /**
     * The most recent snapshot for [venue], or null when this is the first run against it.
     *
     * Null is a first run, not a fault: there is nothing to measure a change from yet, and the
     * caller records the anchor rather than reporting drift it cannot know about.
     */
    fun latestFor(venue: String): BalanceSnapshot? = snapshots().filter { it.venue == venue }.maxByOrNull { it.takenAtEpochMilli }

    companion object {
        private const val FILE_NAME = "balance-snapshots.jsonl"

        /** Beside the withdrawal journal, so one directory holds everything this tool remembers. */
        fun openDefault(): SnapshotStore {
            val dir =
                System
                    .getenv("PORTFOLIO_AUDIT_DIR")
                    ?.let { Path.of(it) }
                    ?: Path.of(System.getProperty("user.home"), ".portfolio-manager")
            Files.createDirectories(dir)
            return SnapshotStore(dir.resolve(FILE_NAME))
        }
    }
}
