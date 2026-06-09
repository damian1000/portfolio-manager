package io.github.damian1000.portfolio.bitfinex

import java.io.Closeable
import java.io.OutputStream
import java.io.PrintWriter
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.Instant

class WithdrawalAuditLog(outputStream: OutputStream) : Closeable {

    private val writer = PrintWriter(outputStream.writer(StandardCharsets.UTF_8).buffered(), true)

    fun record(mode: String, request: WithdrawalRequest, outcome: String) {
        val line = listOf(
            Instant.now().toString(),
            mode,
            request.currency.name,
            request.amount,
            WithdrawalCli.redactAddress(request.destinationAddress),
            outcome.replace("\n", " "),
        ).joinToString(separator = "\t")
        writer.println(line)
    }

    override fun close() {
        writer.close()
    }

    companion object {
        fun openDefault(): WithdrawalAuditLog {
            val dir = System.getenv("PORTFOLIO_AUDIT_DIR")
                ?.let { Path.of(it) }
                ?: Path.of(System.getProperty("user.home"), ".portfolio-manager")
            Files.createDirectories(dir)
            val file = dir.resolve("bitfinex-withdrawals.log")
            return WithdrawalAuditLog(
                Files.newOutputStream(file, StandardOpenOption.CREATE, StandardOpenOption.APPEND)
            )
        }
    }
}
