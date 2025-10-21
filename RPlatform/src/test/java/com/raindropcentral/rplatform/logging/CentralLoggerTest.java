package com.raindropcentral.rplatform.logging;

import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class CentralLoggerTest {

    @TempDir
    Path tempDir;

    private JavaPlugin plugin;
    private PrintStream originalOut;
    private PrintStream originalErr;

    @BeforeEach
    void setUp() {
        CentralLogger.shutdown();
        originalOut = System.out;
        originalErr = System.err;
        plugin = Mockito.mock(JavaPlugin.class);
        Mockito.when(plugin.getDataFolder()).thenReturn(tempDir.toFile());
        Mockito.when(plugin.getName()).thenReturn("TestPlugin");
        Mockito.when(plugin.getLogger()).thenReturn(Logger.getLogger("TestPlugin"));
    }

    @AfterEach
    void tearDown() {
        CentralLogger.shutdown();
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    @Test
    void initializeRegistersHandlersAndEnhancesRecords() throws Exception {
        final LoggerConfig config = mockConfig(true);
        initialize(config);

        final CapturingConsoleHandler consoleHandler = new CapturingConsoleHandler();
        consoleHandler.setLevel(Level.ALL);
        replaceConsoleHandler(consoleHandler);

        final Logger logger = CentralLogger.getLogger(CentralLoggerTest.class);
        logger.info("formatted message");
        CentralLogger.flush();

        assertFalse(consoleHandler.getRecords().isEmpty(), "Expected console handler to capture a record");
        final LogRecord record = consoleHandler.getRecords().getFirst();
        assertEquals("CentralLoggerTest", record.getSourceClassName());
        assertEquals("formatted message", record.getMessage());
        assertTrue(CentralLogger.isWorking());
    }

    @Test
    void getLoggerByClassUsesParentHandlers() {
        final LoggerConfig config = mockConfig(true);
        initialize(config);

        final Logger classLogger = CentralLogger.getLogger(CentralLoggerTest.class);
        final Logger nameLogger = CentralLogger.getLogger(CentralLoggerTest.class.getName());

        assertSame(classLogger, nameLogger);
        assertTrue(classLogger.getUseParentHandlers(), "Logger should delegate to parent handlers");

        final RecordingHandler recordingHandler = new RecordingHandler();
        final Logger root = LogManager.getLogManager().getLogger("");
        root.addHandler(recordingHandler);
        try {
            classLogger.info("parent-routing");
            assertFalse(recordingHandler.getRecords().isEmpty());
            assertEquals("parent-routing", recordingHandler.getRecords().getFirst().getMessage());
        } finally {
            root.removeHandler(recordingHandler);
        }
    }

    @Test
    void setConsoleLoggingEnabledTogglesHandlersAndLogsStateChanges() throws Exception {
        final LoggerConfig config = mockConfig(true);
        initialize(config);

        final CapturingConsoleHandler consoleHandler = new CapturingConsoleHandler();
        consoleHandler.setLevel(Level.ALL);
        replaceConsoleHandler(consoleHandler);

        final RecordingHandler audit = new RecordingHandler();
        final Logger centralLogger = CentralLogger.getLogger("CentralLogger");
        centralLogger.addHandler(audit);

        try {
            final Logger managed = CentralLogger.getLogger("com.raindropcentral.test");
            final ConsoleHandler manual = new ConsoleHandler();
            managed.addHandler(manual);
            assertTrue(Arrays.asList(managed.getHandlers()).contains(manual));

            CentralLogger.setConsoleLoggingEnabled(false);
            assertFalse(CentralLogger.isConsoleLoggingEnabled());
            assertFalse(Arrays.asList(managed.getHandlers()).contains(manual), "Manual console handler should be removed");
            assertTrue(audit.getRecords().stream().anyMatch(r -> r.getMessage().contains("DISABLED")));

            CentralLogger.setConsoleLoggingEnabled(true);
            assertTrue(CentralLogger.isConsoleLoggingEnabled());
            assertTrue(audit.getRecords().stream().anyMatch(r -> r.getMessage().contains("ENABLED")));
        } finally {
            centralLogger.removeHandler(audit);
        }
    }

    @Test
    void getLogFilePathReflectsPluginDataDirectory() {
        final LoggerConfig config = mockConfig(true);
        initialize(config);

        final String path = CentralLogger.getLogFilePath();
        final String expectedDate = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(LocalDate.now());
        assertTrue(path.endsWith("logs" + File.separator + "testplugin-" + expectedDate + ".log"));
    }

    @Test
    void getLogFilePathBeforeInitializationReturnsMessage() {
        CentralLogger.shutdown();
        assertEquals("Plugin not loaded", CentralLogger.getLogFilePath());
    }

    @Test
    void flushInvokesHandlerFlush() throws Exception {
        final LoggerConfig config = mockConfig(true);
        initialize(config);

        final CapturingConsoleHandler consoleHandler = new CapturingConsoleHandler();
        consoleHandler.setLevel(Level.ALL);
        replaceConsoleHandler(consoleHandler);

        CentralLogger.flush();
        assertTrue(consoleHandler.wasFlushed());
    }

    @Test
    void shutdownRestoresSystemStreamsAndClosesHandlers() throws Exception {
        final LoggerConfig config = mockConfig(true);
        initialize(config);

        final CapturingConsoleHandler consoleHandler = new CapturingConsoleHandler();
        consoleHandler.setLevel(Level.ALL);
        replaceConsoleHandler(consoleHandler);

        final PrintStream outBeforeShutdown = System.out;
        final PrintStream errBeforeShutdown = System.err;
        assertNotSame(originalOut, outBeforeShutdown);
        assertNotSame(originalErr, errBeforeShutdown);

        CentralLogger.shutdown();

        assertSame(originalOut, System.out);
        assertSame(originalErr, System.err);
        assertTrue(consoleHandler.wasClosed());
        assertFalse(CentralLogger.isWorking());
    }

    private LoggerConfig mockConfig(final boolean consoleEnabled) {
        final LoggerConfig config = Mockito.mock(LoggerConfig.class);
        Mockito.when(config.isConsoleEnabled()).thenReturn(consoleEnabled);
        Mockito.when(config.isDebugEnabled()).thenReturn(true);
        Mockito.when(config.getConsoleLevel()).thenReturn(LogLevel.INFO);
        Mockito.when(config.getFileLevel()).thenReturn(LogLevel.INFO);
        Mockito.when(config.getLevelForPackage(Mockito.anyString())).thenReturn(LogLevel.INFO);
        Mockito.when(config.getLevelForPackage(Mockito.isNull())).thenReturn(LogLevel.INFO);
        return config;
    }

    private void initialize(final LoggerConfig config) {
        try (MockedStatic<LoggerConfig> mocked = Mockito.mockStatic(LoggerConfig.class)) {
            mocked.when(() -> LoggerConfig.load(plugin)).thenReturn(config);
            CentralLogger.initialize(plugin);
        }
    }

    private void replaceConsoleHandler(final ConsoleHandler handler) throws Exception {
        final Field field = CentralLogger.class.getDeclaredField("CONSOLE_HANDLER");
        field.setAccessible(true);
        field.set(null, handler);
    }

    private static final class CapturingConsoleHandler extends ConsoleHandler {

        private final List<LogRecord> records = new ArrayList<>();
        private boolean flushed;
        private boolean closed;

        @Override
        public synchronized void publish(final LogRecord record) {
            if (record != null) {
                records.add(record);
            }
        }

        @Override
        public synchronized void flush() {
            flushed = true;
        }

        @Override
        public synchronized void close() {
            closed = true;
        }

        List<LogRecord> getRecords() {
            return records;
        }

        boolean wasFlushed() {
            return flushed;
        }

        boolean wasClosed() {
            return closed;
        }
    }

    private static final class RecordingHandler extends Handler {

        private final List<LogRecord> records = new ArrayList<>();

        @Override
        public synchronized void publish(final LogRecord record) {
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
}
