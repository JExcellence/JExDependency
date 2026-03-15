package de.jexcellence.economy.type;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Enum representing different severity levels for currency log entries.
 *
 * <p>This enum indicates the importance and urgency of log entries,
 * helping with filtering, alerting, and monitoring.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public enum ELogLevel {

    /**
     * Debug information for development and troubleshooting.
     */
    DEBUG(10, "Debug"),

    /**
     * General information about normal operations.
     */
    INFO(20, "Info"),

    /**
     * Warning about potential issues or unusual conditions.
     */
    WARNING(30, "Warning"),

    /**
     * Error conditions that need attention but don't stop operation.
     */
    ERROR(40, "Error"),

    /**
     * Critical errors that may affect system stability.
     */
    CRITICAL(50, "Critical");

    private static final Map<Integer, ELogLevel> LEVELS_BY_SEVERITY = Collections.unmodifiableMap(
            Arrays.stream(values()).collect(Collectors.toMap(ELogLevel::getSeverity, Function.identity()))
    );

    private static final Map<String, ELogLevel> LEVELS_BY_DISPLAY_NAME = Collections.unmodifiableMap(
            Arrays.stream(values())
                    .collect(Collectors.toMap(level -> level.displayName.toLowerCase(Locale.ROOT), Function.identity()))
    );

    private final int severity;
    private final String displayName;

    ELogLevel(final int severity, final @NotNull String displayName) {
        this.severity = severity;
        this.displayName = displayName;
    }

    /**
     * Retrieves the numeric severity associated with the log level.
     *
     * @return the severity value where higher numbers indicate greater urgency
     */
    public int getSeverity() {
        return this.severity;
    }

    /**
     * Retrieves the human-readable display name for the log level.
     *
     * @return the display name to present in user interfaces
     */
    public @NotNull String getDisplayName() {
        return this.displayName;
    }

    /**
     * Attempts to resolve a log level from its numeric severity.
     *
     * @param severity the severity to look up
     * @return an optional containing the matching level or empty when no level is defined
     */
    public static @NotNull Optional<ELogLevel> fromSeverity(final int severity) {
        return Optional.ofNullable(LEVELS_BY_SEVERITY.get(severity));
    }

    /**
     * Attempts to resolve a log level from a display name, ignoring case and surrounding whitespace.
     *
     * @param displayName the display name to look up
     * @return an optional containing the matching level or empty when the input cannot be matched
     */
    public static @NotNull Optional<ELogLevel> fromDisplayName(final @Nullable String displayName) {
        if (displayName == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(LEVELS_BY_DISPLAY_NAME.get(displayName.trim().toLowerCase(Locale.ROOT)));
    }

    /**
     * Attempts to resolve a log level from an arbitrary string, ignoring case and surrounding whitespace.
     *
     * @param name the potential enum name
     * @return an optional containing the matching level or empty when the input cannot be parsed
     */
    public static @NotNull Optional<ELogLevel> fromName(final @Nullable String name) {
        if (name == null) {
            return Optional.empty();
        }

        final String normalized = name.trim();
        if (normalized.isEmpty()) {
            return Optional.empty();
        }

        try {
            return Optional.of(ELogLevel.valueOf(normalized.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
