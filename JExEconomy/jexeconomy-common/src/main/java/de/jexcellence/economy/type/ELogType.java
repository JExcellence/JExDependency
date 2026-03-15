package de.jexcellence.economy.type;

/**
 * Enum representing different types of currency log entries.
 *
 * <p>This enum categorizes log entries based on the nature of the operation
 * or event being logged, allowing for efficient filtering and analysis.
 */
public enum ELogType {
    
    /**
     * Transaction-related logs (deposits, withdrawals, transfers, balance changes).
     */
    TRANSACTION,
    
    /**
     * Currency management logs (creation, deletion, configuration changes).
     */
    MANAGEMENT,
    
    /**
     * System operation logs (plugin events, cache operations, database operations).
     */
    SYSTEM,
    
    /**
     * Error and exception logs (failed operations, validation errors).
     */
    ERROR,
    
    /**
     * Audit and security logs (administrative actions, permission checks).
     */
    AUDIT,
    
    /**
     * Debug and development logs (detailed operation information).
     */
    DEBUG
}
