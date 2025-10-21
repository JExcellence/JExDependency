package de.jexcellence.economy.type;

/**
 * Enum representing different severity levels for currency log entries.
 * <p>
 * This enum indicates the importance and urgency of log entries,
 * helping with filtering, alerting, and monitoring.
 * </p>
 */
public enum ELogLevel {
    
    /**
     * Debug information for development and troubleshooting.
     */
    DEBUG,
    
    /**
     * General information about normal operations.
     */
    INFO,
    
    /**
     * Warning about potential issues or unusual conditions.
     */
    WARNING,
    
    /**
     * Error conditions that need attention but don't stop operation.
     */
    ERROR,
    
    /**
     * Critical errors that may affect system stability.
     */
    CRITICAL
}