package com.raindropcentral.rplatform.logging;

import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

/**
 * PlatformLogger is a lightweight facade over {@link java.util.logging.Logger} that automatically
 * routes output through {@link CentralLogger}'s handlers. Each instance leverages
 * {@link LoggerConfig} to align severity filtering with the central configuration while exposing
 * semantic helpers for common Raindrop severity levels.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public class PlatformLogger {

    /**
     * Underlying JUL logger connected to {@link CentralLogger}'s universal handler.
     */
    private final Logger logger;

    /**
     * Configuration snapshot used to honor per-package level overrides and debug toggles.
     */
    private final LoggerConfig config;

    private PlatformLogger(final @NotNull JavaPlugin plugin) {
        // Ensure CentralLogger is initialized in your plugin's onEnable before creating this
        this.logger = CentralLogger.getLogger(plugin.getName());
        this.config = LoggerConfig.load(plugin);

        // Respect package-level config immediately for this logger
        final LogLevel lvl = config.getLevelForPackage(plugin.getClass().getPackage().getName());
        this.logger.setLevel(lvl.toJavaLevel());
        this.logger.setUseParentHandlers(true);
    }

    /**
     * Factory method that creates a plugin-scoped logger wired to {@link CentralLogger}.
     *
     * @param plugin plugin requesting a logger; used for naming and configuration lookup
     * @return a configured {@link PlatformLogger}
     */
    public static @NotNull PlatformLogger create(final @NotNull JavaPlugin plugin) {
        return new PlatformLogger(plugin);
    }

    /**
     * Logs an unrecoverable condition using the CRITICAL level.
     *
     * @param message description of the failure
     */
    public void critical(final @NotNull String message) {
        logger.log(LogLevel.CRITICAL.toJavaLevel(), message);
    }

    /**
     * Logs an unrecoverable condition together with the originating exception.
     *
     * @param message   description of the failure
     * @param throwable root cause to attach to the log record
     */
    public void critical(final @NotNull String message, final @NotNull Throwable throwable) {
        logger.log(LogLevel.CRITICAL.toJavaLevel(), message, throwable);
    }

    /**
     * Records an error-level message.
     *
     * @param message description of the error condition
     */
    public void error(final @NotNull String message) {
        logger.log(LogLevel.ERROR.toJavaLevel(), message);
    }

    /**
     * Records an error-level message with an associated exception.
     *
     * @param message   description of the error condition
     * @param throwable exception to include with the record
     */
    public void error(final @NotNull String message, final @NotNull Throwable throwable) {
        logger.log(LogLevel.ERROR.toJavaLevel(), message, throwable);
    }

    /**
     * Emits a warning describing a recoverable issue or configuration concern.
     *
     * @param message description of the warning condition
     */
    public void warn(final @NotNull String message) {
        logger.log(LogLevel.WARNING.toJavaLevel(), message);
    }

    /**
     * Writes an informational message to the shared logging pipeline.
     *
     * @param message description of the event
     */
    public void info(final @NotNull String message) {
        logger.log(LogLevel.INFO.toJavaLevel(), message);
    }

    /**
     * Emits a debug-level message if debug mode is enabled in {@link LoggerConfig}.
     *
     * @param message description of the diagnostic entry
     */
    public void debug(final @NotNull String message) {
        if (config.isDebugEnabled()) {
            logger.log(LogLevel.DEBUG.toJavaLevel(), message);
        }
    }

    /**
     * Emits a trace-level message when debug mode is active.
     *
     * @param message detailed diagnostic content
     */
    public void trace(final @NotNull String message) {
        if (config.isDebugEnabled()) {
            logger.log(LogLevel.TRACE.toJavaLevel(), message);
        }
    }

    /**
     * Toggles console mirroring through {@link CentralLogger} for all managed loggers.
     *
     * @param enabled {@code true} to mirror to console, {@code false} to restrict to files
     */
    public void setConsoleEnabled(final boolean enabled) {
        CentralLogger.setConsoleLoggingEnabled(enabled);
    }

    /**
     * Reports whether console mirroring is currently active.
     *
     * @return {@code true} if the console handler is installed
     */
    public boolean isConsoleEnabled() {
        return CentralLogger.isConsoleLoggingEnabled();
    }

    /**
     * Exposes the underlying JUL logger when advanced customization is required.
     *
     * @return the backing {@link Logger}
     */
    public @NotNull Logger getJavaLogger() {
        return logger;
    }

    /**
     * No-op placeholder maintained for API symmetry with closable resources.
     */
    public void close() {
        // no-op
    }
}