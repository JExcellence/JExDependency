package com.raindropcentral.rplatform.logging;

import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Enumeration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.*;

/**
 * CentralLogger wires Raindrop Central plugins into a shared {@link java.util.logging.LogManager}
 * configuration so that {@link PlatformLogger} instances and the redirected stdout/stderr streams
 * write through the same handlers. The class installs {@link PlatformConsoleHandler},
 * {@link PlatformLogFormatter}, and the universal JUL handler that respects {@link LoggerConfig}
 * overrides before {@link System#out} and {@link System#err} are pointed at
 * {@link LoggingPrintStream}.
 *
 * <p>The utility is designed to be initialized once per JVM via
 * {@link #initialize(JavaPlugin)} and then consumed by plugin-specific
 * {@link PlatformLogger#create(JavaPlugin)} instances.</p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public class CentralLogger {

    private static final int MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final int MAX_FILE_COUNT = 5;
    private static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static volatile boolean INITIALIZED = false;
    private static JavaPlugin LOADED_PLUGIN;

    private static PrintStream ORIGINAL_OUT;
    private static PrintStream ORIGINAL_ERR;

    private static FileHandler FILE_HANDLER;
    private static ConsoleHandler CONSOLE_HANDLER;
    private static UniversalLogHandler UNIVERSAL_HANDLER;

    private static final AtomicBoolean EMERGENCY_MODE = new AtomicBoolean(false);
    private static final ThreadLocal<Integer> RECURSION_DEPTH = ThreadLocal.withInitial(() -> 0);
    private static final int MAX_RECURSION_DEPTH = 3;

    // Config-driven flags
    private static volatile boolean CONSOLE_LOGGING_ENABLED = true;
    private static volatile boolean DEBUG_MODE = false;
    private static volatile Level CONSOLE_LEVEL = Level.INFO;
    private static volatile Level FILE_LEVEL = Level.ALL;

    // Logger registry for visibility/tweaks at runtime
    private static final Map<String, Logger> MANAGED_LOGGERS = new ConcurrentHashMap<>();

    // Duplicate suppression window (ms)
    private static final long DUP_WINDOW_MS = 750L;
    private static final Map<String, Long> LAST_SEEN = new ConcurrentHashMap<>();

    private CentralLogger() {}

    /**
     * Initializes the logging pipeline for the provided plugin, attaching handlers, loading
     * {@link LoggerConfig}, and redirecting stdout/stderr into the {@link CentralLogger} routing.
     * This should be called once during {@code onEnable} before creating any
     * {@link PlatformLogger} instances.
     *
     * @param plugin the plugin requesting initialization; used to resolve data folders and
     *               configuration
     */
    public static synchronized void initialize(@NotNull final JavaPlugin plugin) {
        if (INITIALIZED) return;

        LOADED_PLUGIN = plugin;
        try {
            ORIGINAL_OUT = System.out;
            ORIGINAL_ERR = System.err;

            // Load config
            final LoggerConfig cfg = LoggerConfig.load(plugin);
            CONSOLE_LOGGING_ENABLED = cfg.isConsoleEnabled();
            DEBUG_MODE = cfg.isDebugEnabled();
            CONSOLE_LEVEL = cfg.getConsoleLevel().toJavaLevel();
            FILE_LEVEL = cfg.getFileLevel().toJavaLevel();

            // Handlers
            setupFileHandler(plugin);
            if (CONSOLE_LOGGING_ENABLED) {
                setupConsoleHandler(); // writes to ORIGINAL_OUT to avoid recursion
            }

            // Universal capture
            setupUniversalRootCapture(cfg);

            // Redirect stdout/stderr after handlers are ready
            redirectSystemStreams();

            INITIALIZED = true;

            // Optional startup line (goes straight to original to avoid loops)
            ORIGINAL_OUT.println("[INIT] CentralLogger initialized - Console: " + CONSOLE_LOGGING_ENABLED
                    + ", ConsoleLevel: " + CONSOLE_LEVEL + ", FileLevel: " + FILE_LEVEL
                    + ", Debug: " + DEBUG_MODE);

        } catch (final Exception e) {
            // Fail safe: print to original streams to avoid loops
            if (ORIGINAL_ERR != null) {
                e.printStackTrace(ORIGINAL_ERR);
            } else {
                e.printStackTrace();
            }
        }
    }

    private static void setupFileHandler(final JavaPlugin plugin) throws Exception {
        final File logDir = new File(plugin.getDataFolder(), "logs");
        if (!logDir.exists() && !logDir.mkdirs()) {
            throw new Exception("Could not create log directory: " + logDir.getAbsolutePath());
        }

        final String timestamp = LocalDateTime.now().format(FILE_DATE_FORMAT);
        final File logFile = new File(logDir, plugin.getName().toLowerCase() + "-" + timestamp + ".log");

        FILE_HANDLER = new FileHandler(logFile.getAbsolutePath(), MAX_FILE_SIZE, MAX_FILE_COUNT, true);
        FILE_HANDLER.setFormatter(new PlatformLogFormatter(false)); // keep rich file format without ANSI
        FILE_HANDLER.setLevel(FILE_LEVEL);
        try {
            FILE_HANDLER.setEncoding("UTF-8");
        } catch (UnsupportedEncodingException ignored) {}
    }

    private static void setupConsoleHandler() {
        // IMPORTANT: write to ORIGINAL_OUT to avoid recursion with redirected System.out
        CONSOLE_HANDLER = new PlatformConsoleHandler(ORIGINAL_OUT);
        CONSOLE_HANDLER.setLevel(CONSOLE_LEVEL);
        CONSOLE_HANDLER.setFormatter(new EnhancedLogFormatter(true)); // cleaner console formatting
        try {
            CONSOLE_HANDLER.setEncoding("UTF-8");
        } catch (UnsupportedEncodingException ignored) {}
    }

    private static void setupUniversalRootCapture(final LoggerConfig cfg) {
        final LogManager lm = LogManager.getLogManager();
        final Logger root = lm.getLogger("");

        // Remove default console handlers everywhere to prevent duplicate console prints
        stripConsoleHandlers(root);
        final Enumeration<String> names = lm.getLoggerNames();
        while (names.hasMoreElements()) {
            final String name = names.nextElement();
            if (name == null || name.isEmpty()) continue;
            final Logger l = lm.getLogger(name);
            if (l != null) {
                stripConsoleHandlers(l);
                applyNagFilter(l);
                MANAGED_LOGGERS.put(name, l);
            }
        }

        // Install our universal root handler
        UNIVERSAL_HANDLER = new UniversalLogHandler(cfg);
        root.addHandler(UNIVERSAL_HANDLER);
        root.setUseParentHandlers(false);
        root.setLevel(Level.ALL);

        // Apply package-level overrides from config
        applyPackageLevels(cfg);
    }

    private static void applyNagFilter(final Logger logger) {
        if (logger == null) return;
        final Filter existing = logger.getFilter();
        logger.setFilter(record -> {
            if (record == null) return false;
            final String msg = record.getMessage();
            if (msg != null && msg.contains("Nag author") && msg.contains("System.out/err")) {
                return false;
            }
            return existing == null || existing.isLoggable(record);
        });
    }

    private static void applyPackageLevels(final LoggerConfig cfg) {
        // Apply current overrides
        // We rely on LoggerConfig.getLevelForPackage(..) to determine default per package when logging occurs.
        // For existing loggers, set best-effort levels now:
        final LogManager lm = LogManager.getLogManager();
        final Enumeration<String> names = lm.getLoggerNames();
        while (names.hasMoreElements()) {
            final String n = names.nextElement();
            if (n == null || n.isEmpty()) continue;
            final Logger l = lm.getLogger(n);
            if (l != null) {
                final LogLevel target = cfg.getLevelForPackage(n);
                l.setLevel(target.toJavaLevel());
                l.setUseParentHandlers(true);
                stripConsoleHandlers(l);
                MANAGED_LOGGERS.put(n, l);
            }
        }
    }

    private static void redirectSystemStreams() {
        final Logger outLogger = Logger.getLogger("System.out");
        final Logger errLogger = Logger.getLogger("System.err");

        // Make sure they propagate to root (we already control root handlers)
        outLogger.setUseParentHandlers(true);
        errLogger.setUseParentHandlers(true);
        stripConsoleHandlers(outLogger);
        stripConsoleHandlers(errLogger);

        System.setOut(new LoggingPrintStream(outLogger, Level.INFO, ORIGINAL_OUT));
        System.setErr(new LoggingPrintStream(errLogger, Level.SEVERE, ORIGINAL_ERR));
    }

    private static void stripConsoleHandlers(final Logger logger) {
        if (logger == null) return;
        for (final Handler h : logger.getHandlers()) {
            if (h instanceof ConsoleHandler) {
                try {
                    logger.removeHandler(h);
                    h.flush();
                    h.close();
                } catch (final Exception ignored) {
                }
            }
        }
    }

    /**
     * Obtains a JUL logger for the provided class that is registered with the centralized handler
     * registry so that configuration updates can be applied dynamically.
     *
     * @param clazz the class whose name should back the logger
     * @return a logger whose parent handlers are managed by {@link CentralLogger}
     */
    public static Logger getLogger(final Class<?> clazz) {
        return getLogger(clazz.getName());
    }

    /**
     * Obtains (and registers) a JUL logger using the supplied name. The logger is configured to use
     * parent handlers so that the universal handler can apply {@link LoggerConfig} overrides.
     *
     * @param name the desired logger name
     * @return the JUL logger managed by {@link CentralLogger}
     */
    public static Logger getLogger(final String name) {
        final Logger l = Logger.getLogger(Objects.requireNonNullElse(name, ""));
        l.setUseParentHandlers(true);
        stripConsoleHandlers(l);
        MANAGED_LOGGERS.put(name, l);
        return l;
    }

    /**
     * Enables or disables console mirroring for all managed loggers. When enabled a
     * {@link PlatformConsoleHandler} is attached so records propagate to the console stream supplied
     * during initialization.
     *
     * @param enabled {@code true} to enable console logging, {@code false} to suppress it
     */
    public static synchronized void setConsoleLoggingEnabled(final boolean enabled) {
        CONSOLE_LOGGING_ENABLED = enabled;
        if (enabled && CONSOLE_HANDLER == null) {
            setupConsoleHandler();
        }
        // Clean direct handlers from all managed loggers
        for (final Logger l : MANAGED_LOGGERS.values()) {
            stripConsoleHandlers(l);
        }
        // Signal in logs
        final Logger logger = getLogger("CentralLogger");
        logger.log(Level.INFO, "Console logging " + (enabled ? "ENABLED" : "DISABLED"));
    }

    /**
     * Indicates whether console logging is currently enabled for the centralized handler.
     *
     * @return {@code true} when console mirroring is active
     */
    public static boolean isConsoleLoggingEnabled() {
        return CONSOLE_LOGGING_ENABLED;
    }

    /**
     * Reports whether the logger has finished initialization and is not in emergency mode.
     *
     * @return {@code true} if handlers were installed successfully and no recursion guard is active
     */
    public static boolean isWorking() {
        return INITIALIZED && FILE_HANDLER != null && !EMERGENCY_MODE.get();
    }

    /**
     * Resolves the current log file path based on the active plugin and rotation settings.
     *
     * @return the absolute path to today's rolling log file, or an explanatory message when the
     *         plugin has not been initialized
     */
    public static String getLogFilePath() {
        if (LOADED_PLUGIN == null) return "Plugin not loaded";
        final String timestamp = LocalDateTime.now().format(FILE_DATE_FORMAT);
        return new File(LOADED_PLUGIN.getDataFolder(), "logs/" + LOADED_PLUGIN.getName().toLowerCase() + "-" + timestamp + ".log").getAbsolutePath();
    }

    /**
     * Flushes all active handlers to ensure buffered records reach their destinations.
     */
    public static void flush() {
        try {
            if (UNIVERSAL_HANDLER != null) UNIVERSAL_HANDLER.flush();
            if (FILE_HANDLER != null) FILE_HANDLER.flush();
            if (CONSOLE_HANDLER != null) CONSOLE_HANDLER.flush();
        } catch (final Exception e) {
            safeErr("[LOGGER ERROR] Failed to flush: " + e.getMessage());
        }
    }

    /**
     * Restores the original system streams and closes all installed handlers. Invoke this during a
     * plugin shutdown to gracefully release file descriptors and console hooks.
     */
    public static synchronized void shutdown() {
        if (!INITIALIZED) return;

        try {
            // Restore streams before closing handlers
            if (ORIGINAL_OUT != null) System.setOut(ORIGINAL_OUT);
            if (ORIGINAL_ERR != null) System.setErr(ORIGINAL_ERR);

            if (UNIVERSAL_HANDLER != null) {
                UNIVERSAL_HANDLER.flush();
                UNIVERSAL_HANDLER.close();
            }
            if (FILE_HANDLER != null) {
                FILE_HANDLER.flush();
                FILE_HANDLER.close();
            }
            if (CONSOLE_HANDLER != null) {
                CONSOLE_HANDLER.flush();
                CONSOLE_HANDLER.close();
            }
        } catch (final Exception e) {
            safeErr("[LOGGER ERROR] Error during shutdown: " + e.getMessage());
        } finally {
            INITIALIZED = false;
        }
    }

    private static void safeErr(final String line) {
        try {
            if (ORIGINAL_ERR != null) {
                ORIGINAL_ERR.println(line);
            } else {
                System.err.println(line);
            }
        } catch (final Exception ignored) {
        }
    }

    // Root universal handler — all JVM loggers pass here once installed
    /**
     * UniversalLogHandler is the shared JUL handler installed on the root logger. It enforces
     * {@link LoggerConfig} policies for every record before delegating to the configured console and
     * file handlers.
     *
     * @author JExcellence
     * @since 1.0.0
     * @version 1.0.1
     */
    private static final class UniversalLogHandler extends Handler {

        /**
         * Active configuration snapshot controlling package level overrides and debug flags.
         */
        private final LoggerConfig cfg;

        UniversalLogHandler(final LoggerConfig cfg) {
            this.cfg = cfg;
            setLevel(Level.ALL);
            setFilter(record -> {
                if (record == null) return false;

                // Filter out Paper/Spigot nag messages early
                final String msg = record.getMessage();
                if (msg != null && msg.contains("Nag author") && msg.contains("System.out/err")) {
                    return false;
                }

                // Honor debug flag
                if (!DEBUG_MODE) {
                    final Level lvl = record.getLevel();
                    // Hide very low levels
                    if (lvl == Level.FINE || lvl == Level.FINER || lvl == Level.FINEST) return false;
                }

                // Package/level policy
                final String loggerName = record.getLoggerName() != null ? record.getLoggerName() : "";
                final LogLevel target = cfg.getLevelForPackage(loggerName);
                return record.getLevel().intValue() >= target.toJavaLevel().intValue();
            });
        }

        @Override
        public void publish(final LogRecord record) {
            if (record == null || !isLoggable(record)) return;
            if (EMERGENCY_MODE.get()) return;

            final int depth = RECURSION_DEPTH.get();
            if (depth >= MAX_RECURSION_DEPTH) {
                EMERGENCY_MODE.set(true);
                safeErr("[EMERGENCY] Max recursion depth reached, entering emergency mode");
                return;
            }

            RECURSION_DEPTH.set(depth + 1);
            try {
                final LogRecord enhanced = enhance(record);

                // Duplicate suppression across a small window
                if (isDuplicate(enhanced)) {
                    return;
                }

                if (FILE_HANDLER != null && FILE_HANDLER.isLoggable(enhanced)) {
                    FILE_HANDLER.publish(enhanced);
                }

                // Avoid console loops, suppress internal logs, and filter nag messages
                if (CONSOLE_LOGGING_ENABLED && CONSOLE_HANDLER != null && CONSOLE_HANDLER.isLoggable(enhanced)) {
                    if (!isFromLoggingSystem(enhanced) && !isNagMessage(enhanced)) {
                        CONSOLE_HANDLER.publish(enhanced);
                    }
                }
            } catch (final Exception e) {
                safeErr("[LOGGER ERROR] Failed to publish: " + e.getMessage());
            } finally {
                RECURSION_DEPTH.set(depth);
                if (depth == 0) {
                    EMERGENCY_MODE.set(false);
                }
            }
        }

        private boolean isDuplicate(final LogRecord r) {
            final String loggerName = r.getLoggerName() != null ? r.getLoggerName() : "";
            final String msg = r.getMessage() != null ? r.getMessage() : "";
            final String key = r.getLevel() + "|" + normalize(loggerName) + "|" + normalize(msg);
            final long now = System.currentTimeMillis();
            final Long prev = LAST_SEEN.put(key, now);
            return prev != null && (now - prev) <= DUP_WINDOW_MS;
        }

        private String normalize(final String s) {
            return s.replaceAll("\\s+", " ").trim();
        }

        private boolean isFromLoggingSystem(final LogRecord r) {
            final String name = r.getLoggerName();
            if (name == null) return false;
            // Hide our own internal cycles and the redirected streams
            return name.startsWith("com.raindropcentral.rplatform.logging")
                    || "System.out".equals(name)
                    || "System.err".equals(name)
                    || name.contains("LoggingPrintStream")
                    || name.contains("CentralLogger");
        }

        private boolean isNagMessage(final LogRecord r) {
            final String msg = r.getMessage();
            if (msg == null) return false;
            // Filter out Paper/Spigot nag messages about System.out/err usage
            return msg.contains("Nag author") && msg.contains("System.out/err");
        }

        private LogRecord enhance(final LogRecord original) {
            final LogRecord out = new LogRecord(original.getLevel(), original.getMessage());
            out.setLoggerName(original.getLoggerName());
            out.setInstant(Instant.ofEpochMilli(original.getMillis()));
            out.setThrown(original.getThrown());
            out.setParameters(original.getParameters());
            out.setLongThreadID(original.getLongThreadID());

            try {
                if (original.getSourceClassName() != null) {
                    final String fqcn = original.getSourceClassName();
                    out.setSourceClassName(shortClassName(fqcn));
                } else if (original.getLoggerName() != null) {
                    out.setSourceClassName(shortClassName(original.getLoggerName()));
                }
                if (original.getSourceMethodName() != null) {
                    out.setSourceMethodName(original.getSourceMethodName());
                }
            } catch (final Exception ignored) {
            }
            return out;
        }

        private String shortClassName(final String fq) {
            final int idx = fq.lastIndexOf('.');
            return idx >= 0 ? fq.substring(idx + 1) : fq;
        }

        @Override
        public void flush() {
            try {
                if (FILE_HANDLER != null) FILE_HANDLER.flush();
                if (CONSOLE_HANDLER != null) CONSOLE_HANDLER.flush();
            } catch (final Exception e) {
                safeErr("[LOGGER ERROR] Failed to flush: " + e.getMessage());
            }
        }

        @Override
        public void close() throws SecurityException {
            // Root handler lifecycle is controlled by CentralLogger
        }
    }
}