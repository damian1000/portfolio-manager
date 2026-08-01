package io.github.damian1000.portfolio.bitfinex

/**
 * Where a withdrawal has got to. The venue is the authority on whether money moved; these states
 * record what this process is entitled to believe, which is not the same thing.
 *
 * Only [CONFIRMED] and [FAILED] are terminal. Everything else means "the venue may be holding a
 * request we do not know the outcome of", and the only way out is to ask the venue.
 */
enum class WithdrawalState {
    /**
     * Written, and flushed to disk, *before* the venue is contacted. If the process dies here we
     * cannot tell whether the request left the machine, so this is deliberately not terminal: a
     * crash between the journal write and the socket write is indistinguishable from a crash
     * during it.
     */
    INTENT,

    /** The venue returned a response. Whether it settles is still the venue's business. */
    SUBMITTED,

    /** Reconciled against the venue's movement history. Money moved. Terminal. */
    CONFIRMED,

    /**
     * The venue is known not to be holding this request — either it rejected it outright, or
     * reconciliation found no matching movement after submission. Terminal.
     */
    FAILED,

    /**
     * The outcome is genuinely unknown: typically a timeout after the venue may already have
     * accepted. Recording this as [FAILED] is what invites a duplicate withdrawal, so it never
     * resolves itself — only reconciliation against movement history can move it on.
     */
    UNKNOWN,
    ;

    val isTerminal: Boolean get() = this == CONFIRMED || this == FAILED
}

/**
 * One line of the journal. [withdrawalId] is stable across every state a withdrawal passes
 * through and is also sent to the venue as the payment id, so a retry is recognisably the same
 * request rather than a second one.
 *
 * [destination] is always redacted — the journal is an audit artifact, and a full destination
 * address in a file that outlives the process is a liability, not evidence.
 */
data class WithdrawalRecord(
    val timestamp: String,
    val withdrawalId: String,
    val state: WithdrawalState,
    val mode: String,
    val currency: String,
    val amount: String,
    val destination: String,
    val detail: String = "",
)
