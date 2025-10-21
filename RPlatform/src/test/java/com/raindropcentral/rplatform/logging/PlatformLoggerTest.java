package com.raindropcentral.rplatform.logging;

import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class PlatformLoggerTest {

    @TempDir
    Path tempDir;

    private JavaPlugin plugin;

    @BeforeEach
    void setUp() {
        plugin = Mockito.mock(JavaPlugin.class);
        Mockito.when(plugin.getName()).thenReturn("UnitTestPlugin");
        Mockito.when(plugin.getDataFolder()).thenReturn(tempDir.toFile());
        Mockito.when(plugin.getLogger()).thenReturn(Logger.getLogger("UnitTestPlugin"));
    }

    @Test
    void createConfiguresLoggerUsingPluginMetadata() {
        final LoggerConfig config = Mockito.mock(LoggerConfig.class);
        final String packageName = plugin.getClass().getPackage().getName();
        Mockito.when(config.getLevelForPackage(Mockito.anyString())).then(invocation -> {
            final String name = invocation.getArgument(0, String.class);
            if (name != null && name.startsWith(packageName)) {
                return LogLevel.ERROR;
            }
            return LogLevel.INFO;
        });
        Mockito.when(config.isDebugEnabled()).thenReturn(false);

        final Logger julLogger = freshLogger();

        try (MockedStatic<LoggerConfig> configMock = Mockito.mockStatic(LoggerConfig.class);
             MockedStatic<CentralLogger> centralMock = Mockito.mockStatic(CentralLogger.class)) {
            configMock.when(() -> LoggerConfig.load(plugin)).thenReturn(config);
            centralMock.when(() -> CentralLogger.getLogger("UnitTestPlugin")).thenReturn(julLogger);

            final PlatformLogger platformLogger = PlatformLogger.create(plugin);

            configMock.verify(() -> LoggerConfig.load(plugin));
            centralMock.verify(() -> CentralLogger.getLogger("UnitTestPlugin"));

            assertSame(julLogger, platformLogger.getJavaLogger());
            assertEquals(LogLevel.ERROR.toJavaLevel(), julLogger.getLevel());
            assertTrue(julLogger.getUseParentHandlers());
        }
    }

    @Test
    void severityMethodsDelegateToJulLogger() {
        final LoggerConfig config = Mockito.mock(LoggerConfig.class);
        Mockito.when(config.getLevelForPackage(Mockito.anyString())).thenReturn(LogLevel.TRACE);
        Mockito.when(config.isDebugEnabled()).thenReturn(true);

        final Logger julLogger = freshLogger();

        try (MockedStatic<LoggerConfig> configMock = Mockito.mockStatic(LoggerConfig.class);
             MockedStatic<CentralLogger> centralMock = Mockito.mockStatic(CentralLogger.class)) {
            configMock.when(() -> LoggerConfig.load(plugin)).thenReturn(config);
            centralMock.when(() -> CentralLogger.getLogger("UnitTestPlugin")).thenReturn(julLogger);

            final PlatformLogger platformLogger = PlatformLogger.create(plugin);

            final RecordingHandler handler = new RecordingHandler();
            final Logger javaLogger = platformLogger.getJavaLogger();
            javaLogger.setUseParentHandlers(false);
            javaLogger.addHandler(handler);

            final Throwable criticalCause = new IllegalStateException("critical");
            final Throwable errorCause = new RuntimeException("error");

            platformLogger.critical("critical-message");
            platformLogger.critical("critical-ex", criticalCause);
            platformLogger.error("error-message");
            platformLogger.error("error-ex", errorCause);
            platformLogger.warn("warn-message");
            platformLogger.info("info-message");
            platformLogger.debug("debug-message");
            platformLogger.trace("trace-message");

            final List<LogRecord> records = handler.getRecords();
            assertEquals(8, records.size());

            assertEquals(LogLevel.CRITICAL.toJavaLevel(), records.getFirst().getLevel());
            assertEquals("critical-message", records.getFirst().getMessage());

            assertEquals(LogLevel.CRITICAL.toJavaLevel(), records.get(1).getLevel());
            assertEquals("critical-ex", records.get(1).getMessage());
            assertSame(criticalCause, records.get(1).getThrown());

            assertEquals(LogLevel.ERROR.toJavaLevel(), records.get(2).getLevel());
            assertEquals("error-message", records.get(2).getMessage());

            assertEquals(LogLevel.ERROR.toJavaLevel(), records.get(3).getLevel());
            assertEquals("error-ex", records.get(3).getMessage());
            assertSame(errorCause, records.get(3).getThrown());

            assertEquals(LogLevel.WARNING.toJavaLevel(), records.get(4).getLevel());
            assertEquals("warn-message", records.get(4).getMessage());

            assertEquals(LogLevel.INFO.toJavaLevel(), records.get(5).getLevel());
            assertEquals("info-message", records.get(5).getMessage());

            assertEquals(LogLevel.DEBUG.toJavaLevel(), records.get(6).getLevel());
            assertEquals("debug-message", records.get(6).getMessage());

            assertEquals(LogLevel.TRACE.toJavaLevel(), records.get(7).getLevel());
            assertEquals("trace-message", records.get(7).getMessage());
        }
    }

    @Test
    void debugAndTraceSuppressedWhenDebugDisabled() {
        final LoggerConfig config = Mockito.mock(LoggerConfig.class);
        Mockito.when(config.getLevelForPackage(Mockito.anyString())).thenReturn(LogLevel.TRACE);
        Mockito.when(config.isDebugEnabled()).thenReturn(false);

        final Logger julLogger = freshLogger();

        try (MockedStatic<LoggerConfig> configMock = Mockito.mockStatic(LoggerConfig.class);
             MockedStatic<CentralLogger> centralMock = Mockito.mockStatic(CentralLogger.class)) {
            configMock.when(() -> LoggerConfig.load(plugin)).thenReturn(config);
            centralMock.when(() -> CentralLogger.getLogger("UnitTestPlugin")).thenReturn(julLogger);

            final PlatformLogger platformLogger = PlatformLogger.create(plugin);

            final RecordingHandler handler = new RecordingHandler();
            final Logger javaLogger = platformLogger.getJavaLogger();
            javaLogger.setUseParentHandlers(false);
            javaLogger.addHandler(handler);

            platformLogger.debug("debug-message");
            platformLogger.trace("trace-message");
            platformLogger.info("info-message");

            final List<LogRecord> records = handler.getRecords();
            assertEquals(1, records.size());
            assertEquals("info-message", records.getFirst().getMessage());
            assertEquals(LogLevel.INFO.toJavaLevel(), records.getFirst().getLevel());
        }
    }

    @Test
    void consoleUtilitiesDelegateToCentralLogger() {
        final LoggerConfig config = Mockito.mock(LoggerConfig.class);
        Mockito.when(config.getLevelForPackage(Mockito.anyString())).thenReturn(LogLevel.INFO);
        Mockito.when(config.isDebugEnabled()).thenReturn(false);

        final Logger julLogger = freshLogger();
        final AtomicBoolean consoleEnabled = new AtomicBoolean(true);

        try (MockedStatic<LoggerConfig> configMock = Mockito.mockStatic(LoggerConfig.class);
             MockedStatic<CentralLogger> centralMock = Mockito.mockStatic(CentralLogger.class)) {
            configMock.when(() -> LoggerConfig.load(plugin)).thenReturn(config);
            centralMock.when(() -> CentralLogger.getLogger("UnitTestPlugin")).thenReturn(julLogger);
            centralMock.when(CentralLogger::isConsoleLoggingEnabled).then(invocation -> consoleEnabled.get());
            centralMock.when(() -> CentralLogger.setConsoleLoggingEnabled(true)).then(invocation -> {
                consoleEnabled.set(true);
                return null;
            });
            centralMock.when(() -> CentralLogger.setConsoleLoggingEnabled(false)).then(invocation -> {
                consoleEnabled.set(false);
                return null;
            });

            final PlatformLogger platformLogger = PlatformLogger.create(plugin);

            assertTrue(platformLogger.isConsoleEnabled());

            platformLogger.setConsoleEnabled(false);
            assertFalse(platformLogger.isConsoleEnabled());

            platformLogger.setConsoleEnabled(true);
            assertTrue(platformLogger.isConsoleEnabled());

            centralMock.verify(() -> CentralLogger.setConsoleLoggingEnabled(false));
            centralMock.verify(() -> CentralLogger.setConsoleLoggingEnabled(true));
        }
    }

    @Test
    void closeRemovesHandlersAndClosesThem() {
        final LoggerConfig config = Mockito.mock(LoggerConfig.class);
        Mockito.when(config.getLevelForPackage(Mockito.anyString())).thenReturn(LogLevel.INFO);
        Mockito.when(config.isDebugEnabled()).thenReturn(false);

        final Logger julLogger = freshLogger();

        try (MockedStatic<LoggerConfig> configMock = Mockito.mockStatic(LoggerConfig.class);
             MockedStatic<CentralLogger> centralMock = Mockito.mockStatic(CentralLogger.class)) {
            configMock.when(() -> LoggerConfig.load(plugin)).thenReturn(config);
            centralMock.when(() -> CentralLogger.getLogger("UnitTestPlugin")).thenReturn(julLogger);

            final PlatformLogger platformLogger = PlatformLogger.create(plugin);

            final TrackingHandler handler = new TrackingHandler();
            julLogger.addHandler(handler);
            assertEquals(1, julLogger.getHandlers().length);

            platformLogger.close();

            assertEquals(0, julLogger.getHandlers().length);
            assertTrue(handler.flushed);
            assertTrue(handler.closed);
        }
    }

    private Logger freshLogger() {
        final Logger logger = Logger.getLogger("platform-logger-" + UUID.randomUUID());
        logger.setLevel(Level.ALL);
        logger.setUseParentHandlers(false);
        for (final Handler handler : logger.getHandlers()) {
            logger.removeHandler(handler);
        }
        return logger;
    }

    private static final class RecordingHandler extends Handler {

        private final List<LogRecord> records = new ArrayList<>();

        @Override
        public void publish(final LogRecord record) {
            if (record != null) {
                records.add(record);
            }
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }

        List<LogRecord> getRecords() {
            return records;
        }
    }

    private static final class TrackingHandler extends Handler {

        private boolean flushed;
        private boolean closed;

        @Override
        public void publish(final LogRecord record) {
        }

        @Override
        public void flush() {
            flushed = true;
        }

        @Override
        public void close() {
            closed = true;
        }
    }
}

