package com.raindropcentral.rplatform.logging;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.logging.Level;

/**
 * LogLevel enumerates the severity values supported by {@link CentralLogger} and
 * {@link PlatformLogger}, providing conversions to {@link Level} so JUL handlers can enforce
 * consistent thresholds.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public enum LogLevel {
    CRITICAL(Level.SEVERE),
    ERROR(Level.SEVERE),
    WARN(Level.WARNING),
    WARNING(Level.WARNING),
    INFO(Level.INFO),
    CONFIG(Level.CONFIG),
    DEBUG(Level.FINE),
    TRACE(Level.FINER),
    FINE(Level.FINE),
    FINER(Level.FINER),
    FINEST(Level.FINEST);

    private final Level javaLevel;

    LogLevel(final Level javaLevel) {
        this.javaLevel = javaLevel;
    }

    /**
     * Translates this level into the corresponding {@link Level} understood by JUL.
     *
     * @return JUL level equivalent
     */
    public @NotNull Level toJavaLevel() {
        return javaLevel;
    }

    /**
     * Parses user-provided text into a {@link LogLevel}, defaulting to {@link #INFO} when the text
     * is blank or unknown.
     *
     * @param s textual representation of a severity level
     * @return matching {@link LogLevel}
     */
    public static @NotNull LogLevel fromString(final String s) {
        if (s == null || s.isEmpty()) return INFO;
        final String k = s.trim().toUpperCase(Locale.ROOT);
        switch (k) {
            case "CRITICAL": return CRITICAL;
            case "ERROR": return ERROR;
            case "WARN": return WARN;
            case "WARNING": return WARNING;
            case "INFO": return INFO;
            case "CONFIG": return CONFIG;
            case "DEBUG": return DEBUG;
            case "TRACE": return TRACE;
            case "FINE": return FINE;
            case "FINER": return FINER;
            case "FINEST": return FINEST;
            // Fallback: try JDK Level names
            case "SEVERE": return ERROR;
            default: return INFO;
        }
    }
}