package de.jexcellence.jexplatform.logging;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SLF4J-style logger with {@code {}} placeholder substitution and zero verbosity.
 *
 * <p>Placeholder arguments are only evaluated when the message's level is enabled,
 * avoiding unnecessary string concatenation on hot paths. Output routes to
 * {@link System#out} (white text in Minecraft consoles, never red stderr) and
 * optionally to a rotating log file.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * var log = JExLogger.of("JExEconomy");
 * log.info("Loaded {} currencies in {}ms", count, elapsed);
 * log.warn("Currency not found: {}", id);
 * log.error("Migration failed", exception);
 * }</pre>
 *
 * @author JExcellence
 * @since 1.0.0
 */
public final class JExLogger {

    private static final Map<String, JExLogger> REGISTRY = new ConcurrentHashMap<>();

    private final Logger julLogger;
    private final ConsoleAppender consoleAppender;
    private final @Nullable FileAppender fileAppender;

    private JExLogger(String name, LogLevel consoleLevel, @Nullable FileAppender fileAppender) {
        this.julLogger = Logger.getLogger("JExPlatform." + name);
        this.julLogger.setUseParentHandlers(false);
        this.julLogger.setLevel(Level.ALL);

        // Remove any inherited handlers
        for (var h : julLogger.getHandlers()) {
            julLogger.removeHandler(h);
        }

        this.consoleAppender = new ConsoleAppender(name, consoleLevel);
        this.julLogger.addHandler(consoleAppender);

        this.fileAppender = fileAppender;
        if (fileAppender != null) {
            julLogger.addHandler(fileAppender);
        }
    }

    // ── Factories ───────────────────────────────────────────────────────────────

    /**
     * Returns a console-only logger at {@link LogLevel#INFO}.
     *
     * <p>Loggers are cached by name — calling this twice with the same name
     * returns the same instance.
     *
     * @param name the logger name, typically the plugin name
     * @return the logger instance
     */
    public static @NotNull JExLogger of(@NotNull String name) {
        return REGISTRY.computeIfAbsent(name, n -> new JExLogger(n, LogLevel.INFO, null));
    }

    /**
     * Returns a logger with custom console level and optional file output.
     *
     * @param name            the logger name
     * @param consoleLevel    minimum level for console output
     * @param enableFile      whether to write to a rotating log file
     * @param dataFolder      plugin data folder for log files (required if enableFile is true)
     * @return the logger instance
     */
    public static @NotNull JExLogger of(
            @NotNull String name,
            @NotNull LogLevel consoleLevel,
            boolean enableFile,
            @Nullable Path dataFolder
    ) {
        return REGISTRY.computeIfAbsent(name, n -> {
            FileAppender fa = null;
            if (enableFile && dataFolder != null) {
                try {
                    fa = new FileAppender(n, dataFolder.resolve("logs"), LogLevel.DEBUG);
                } catch (IOException e) {
                    System.err.println("[JExPlatform] Failed to create file logger for " + n + ": " + e.getMessage());
                }
            }
            return new JExLogger(n, consoleLevel, fa);
        });
    }

    // ── Logging methods ─────────────────────────────────────────────────────────

    /**
     * Logs at DEBUG level.
     *
     * @param message the message (may contain {@code {}} placeholders)
     * @param args    placeholder values
     */
    public void debug(String message, Object... args) {
        if (julLogger.isLoggable(Level.FINE)) {
            julLogger.log(Level.FINE, substitute(message, args));
        }
    }

    /**
     * Logs at INFO level.
     *
     * @param message the message (may contain {@code {}} placeholders)
     * @param args    placeholder values
     */
    public void info(String message, Object... args) {
        if (julLogger.isLoggable(Level.INFO)) {
            julLogger.log(Level.INFO, substitute(message, args));
        }
    }

    /**
     * Logs at WARN level.
     *
     * @param message the message (may contain {@code {}} placeholders)
     * @param args    placeholder values
     */
    public void warn(String message, Object... args) {
        if (julLogger.isLoggable(Level.WARNING)) {
            julLogger.log(Level.WARNING, substitute(message, args));
        }
    }

    /**
     * Logs an exception at WARN level.
     *
     * @param message the message
     * @param cause   the exception
     */
    public void warn(String message, Throwable cause) {
        julLogger.log(Level.WARNING, message, cause);
    }

    /**
     * Logs at ERROR level.
     *
     * @param message the message (may contain {@code {}} placeholders)
     * @param args    placeholder values
     */
    public void error(String message, Object... args) {
        if (julLogger.isLoggable(Level.SEVERE)) {
            julLogger.log(Level.SEVERE, substitute(message, args));
        }
    }

    /**
     * Logs an exception at ERROR level.
     *
     * @param message the message
     * @param cause   the exception
     */
    public void error(String message, Throwable cause) {
        julLogger.log(Level.SEVERE, message, cause);
    }

    // ── Configuration ───────────────────────────────────────────────────────────

    /**
     * Changes the minimum console output level at runtime.
     *
     * @param level the new minimum level
     */
    public void setConsoleLevel(@NotNull LogLevel level) {
        consoleAppender.setConsoleLevel(level);
    }

    /**
     * Changes the minimum file output level at runtime.
     *
     * @param level the new minimum level
     */
    public void setFileLevel(@NotNull LogLevel level) {
        if (fileAppender != null) {
            fileAppender.setFileLevel(level);
        }
    }

    /**
     * Flushes and closes all handlers. After this call, the logger is removed
     * from the registry and further logging is a no-op.
     */
    public void close() {
        for (var h : julLogger.getHandlers()) {
            h.flush();
            h.close();
            julLogger.removeHandler(h);
        }
        REGISTRY.remove(julLogger.getName().replace("JExPlatform.", ""));
    }

    /**
     * Closes all registered loggers. Called during platform shutdown.
     */
    public static void closeAll() {
        REGISTRY.values().forEach(JExLogger::close);
        REGISTRY.clear();
    }

    // ── Internal ────────────────────────────────────────────────────────────────

    /**
     * Replaces {@code {}} placeholders with argument values, left to right.
     * Extra arguments are ignored; missing arguments leave the placeholder as-is.
     */
    private static String substitute(String template, Object... args) {
        if (args == null || args.length == 0 || template == null) return template;

        var sb = new StringBuilder(template.length() + 32);
        var argIndex = 0;
        var i = 0;

        while (i < template.length()) {
            if (i + 1 < template.length() && template.charAt(i) == '{' && template.charAt(i + 1) == '}') {
                sb.append(argIndex < args.length ? String.valueOf(args[argIndex++]) : "{}");
                i += 2;
            } else {
                sb.append(template.charAt(i++));
            }
        }

        return sb.toString();
    }
}
