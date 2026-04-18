package de.jexcellence.economy.type;

/**
 * Category of a transaction log entry.
 *
 * @author JExcellence
 * @since 3.0.0
 */
public enum LogType {

    /** Balance changes (deposits, withdrawals, transfers). */
    TRANSACTION,

    /** Currency lifecycle (creation, deletion, edits). */
    MANAGEMENT,

    /** Plugin system events. */
    SYSTEM,

    /** Failed operations. */
    ERROR
}
