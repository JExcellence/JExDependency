package de.jexcellence.economy.type;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Optional;

/**
 * Severity level for transaction log entries.
 *
 * @author JExcellence
 * @since 3.0.0
 */
public enum LogLevel {

    /** Development diagnostics. */
    DEBUG(10),

    /** Normal operations. */
    INFO(20),

    /** Potential issues. */
    WARNING(30),

    /** Errors needing attention. */
    ERROR(40),

    /** Critical system failures. */
    CRITICAL(50);

    private final int severity;

    LogLevel(int severity) {
        this.severity = severity;
    }

    /**
     * Returns the numeric severity (higher = more urgent).
     *
     * @return the severity value
     */
    public int severity() {
        return severity;
    }

    /**
     * Resolves a log level by name (case-insensitive).
     *
     * @param name the name to match
     * @return the matching level, or empty
     */
    public static @NotNull Optional<LogLevel> fromName(@NotNull String name) {
        try {
            return Optional.of(valueOf(name.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
