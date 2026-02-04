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
 * CentralLogger provides file-based logging for Raindrop Central plugins with minimal console spam.
 * Logs are written to plugin-specific files with rotation, while console output is kept clean.
 */
public class CentralLogger {

    private static final int MAX_FILE_SIZE = 10 * 1024 * 1024;
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

    private static volatile boolean CONSOLE_LOGGING_ENABLED = false;
    private static volatile boolean DEBUG_MODE = false;
    private static volatile Level CONSOLE_LEVEL = Level.WARNING;
    private static volatile Level FILE_LEVEL = Level.ALL;

    private static final Map<String, Logger> MANAGED_LOGGERS = new ConcurrentHashMap<>();

    private static final long DUP_WINDOW_MS = 750L;
    private static final Map<String, Long> LAST_SEEN = new ConcurrentHashMap<>();

    private CentralLogger() {}

    public static synchronized void initialize(@NotNull final JavaPlugin plugin) {
        if (INITIALIZED) return;

        LOADED_PLUGIN = plugin;
        try {
            ORIGINAL_OUT = System.out;
            ORIGINAL_ERR = System.err;

            final LoggerConfig cfg = LoggerConfig.load(plugin);
            CONSOLE_LOGGING_ENABLED = cfg.isConsoleEnabled();
            DEBUG_MODE = cfg.isDebugEnabled();
            CONSOLE_LEVEL = cfg.getConsoleLevel().toJavaLevel();
            FILE_LEVEL = cfg.getFileLevel().toJavaLevel();

            setupFileHandler(plugin);
            if (CONSOLE_LOGGING_ENABLED) {
                setupConsoleHandler();
            }

            setupUniversalRootCapture(cfg);
            redirectSystemStreams();

            INITIALIZED = true;

        } catch (final Exception e) {
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
        FILE_HANDLER.setFormatter(new PlatformLogFormatter(false));
        FILE_HANDLER.setLevel(FILE_LEVEL);
        try {
            FILE_HANDLER.setEncoding("UTF-8");
        } catch (UnsupportedEncodingException ignored) {}
    }

    private static void setupConsoleHandler() {
        CONSOLE_HANDLER = new PlatformConsoleHandler(ORIGINAL_OUT);
        CONSOLE_HANDLER.setLevel(CONSOLE_LEVEL);
        CONSOLE_HANDLER.setFormatter(new EnhancedLogFormatter(true));
        try {
            CONSOLE_HANDLER.setEncoding("UTF-8");
        } catch (UnsupportedEncodingException ignored) {}
    }

    private static void setupUniversalRootCapture(final LoggerConfig cfg) {
        final LogManager lm = LogManager.getLogManager();
        final Logger root = lm.getLogger("");

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

        UNIVERSAL_HANDLER = new UniversalLogHandler(cfg);
        root.addHandler(UNIVERSAL_HANDLER);
        root.setUseParentHandlers(false);
        root.setLevel(Level.ALL);

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

    public static Logger getLogger(final Class<?> clazz) {
        return getLogger(clazz.getName());
    }

    public static Logger getLogger(final String name) {
        final Logger l = Logger.getLogger(Objects.requireNonNullElse(name, ""));
        l.setUseParentHandlers(true);
        stripConsoleHandlers(l);
        MANAGED_LOGGERS.put(name, l);
        return l;
    }

    public static synchronized void setConsoleLoggingEnabled(final boolean enabled) {
        CONSOLE_LOGGING_ENABLED = enabled;
        if (enabled && CONSOLE_HANDLER == null) {
            setupConsoleHandler();
        }
        for (final Logger l : MANAGED_LOGGERS.values()) {
            stripConsoleHandlers(l);
        }
        final Logger logger = getLogger("CentralLogger");
        logger.log(Level.INFO, "Console logging " + (enabled ? "ENABLED" : "DISABLED"));
    }

    public static boolean isConsoleLoggingEnabled() {
        return CONSOLE_LOGGING_ENABLED;
    }

    public static boolean isWorking() {
        return INITIALIZED && FILE_HANDLER != null && !EMERGENCY_MODE.get();
    }

    public static String getLogFilePath() {
        if (LOADED_PLUGIN == null) return "Plugin not loaded";
        final String timestamp = LocalDateTime.now().format(FILE_DATE_FORMAT);
        return new File(LOADED_PLUGIN.getDataFolder(), "logs/" + LOADED_PLUGIN.getName().toLowerCase() + "-" + timestamp + ".log").getAbsolutePath();
    }

    public static void flush() {
        try {
            if (UNIVERSAL_HANDLER != null) UNIVERSAL_HANDLER.flush();
            if (FILE_HANDLER != null) FILE_HANDLER.flush();
            if (CONSOLE_HANDLER != null) CONSOLE_HANDLER.flush();
        } catch (final Exception e) {
            safeErr("[LOGGER ERROR] Failed to flush: " + e.getMessage());
        }
    }

    public static synchronized void shutdown() {
        if (!INITIALIZED) return;

        try {
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

    private static final class UniversalLogHandler extends Handler {

        private final LoggerConfig cfg;

        UniversalLogHandler(final LoggerConfig cfg) {
            this.cfg = cfg;
            setLevel(Level.ALL);
            setFilter(record -> {
                if (record == null) return false;

                final String msg = record.getMessage();
                if (msg != null && msg.contains("Nag author") && msg.contains("System.out/err")) {
                    return false;
                }

                if (!DEBUG_MODE) {
                    final Level lvl = record.getLevel();
                    if (lvl == Level.FINE || lvl == Level.FINER || lvl == Level.FINEST) return false;
                }

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

                if (isDuplicate(enhanced)) {
                    return;
                }

                if (FILE_HANDLER != null && FILE_HANDLER.isLoggable(enhanced)) {
                    FILE_HANDLER.publish(enhanced);
                }

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
            return name.startsWith("com.raindropcentral.rplatform.logging")
                    || "System.out".equals(name)
                    || "System.err".equals(name)
                    || name.contains("LoggingPrintStream")
                    || name.contains("CentralLogger");
        }

        private boolean isNagMessage(final LogRecord r) {
            final String msg = r.getMessage();
            if (msg == null) return false;
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
        }
    }
}
