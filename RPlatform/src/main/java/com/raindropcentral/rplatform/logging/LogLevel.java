package com.raindropcentral.rplatform.logging;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.logging.Level;

/**
 * Friendly log levels used by our logger and mapping to java.util.logging.Level.
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

    public @NotNull Level toJavaLevel() {
        return javaLevel;
    }

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