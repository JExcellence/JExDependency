package com.raindropcentral.core;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import de.jexcellence.dependency.JEDependency;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RCorePremiumTest {

    private ServerMock server;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void onLoadInitializesDependenciesAndExposesDelegate() {
        try (MockedStatic<JEDependency> dependency = Mockito.mockStatic(JEDependency.class);
             MockedConstruction<RCorePremiumImpl> construction = Mockito.mockConstruction(RCorePremiumImpl.class,
                 (mock, context) -> {
                     Mockito.doNothing().when(mock).onLoad();
                     Mockito.doNothing().when(mock).onEnable();
                     Mockito.doNothing().when(mock).onDisable();
                 })) {
            RCorePremium plugin = MockBukkit.load(RCorePremium.class);

            dependency.verify(() -> JEDependency.initializeWithRemapping(plugin, RCorePremium.class));

            List<RCorePremiumImpl> delegates = construction.constructed();
            assertEquals(1, delegates.size(), "Expected premium delegate to be constructed once");

            RCorePremiumImpl delegate = delegates.getFirst();
            assertSame(delegate, plugin.getImpl(), "Delegate should be stored for later phases");

            Mockito.verify(delegate).onLoad();
            Mockito.verify(delegate).onEnable();

            plugin.onDisable();
            Mockito.verify(delegate, Mockito.atLeastOnce()).onDisable();
        }
    }

    @Test
    void onEnableWithoutDelegateLogsAndDisablesPlugin() {
        RecordingHandler handler = new RecordingHandler();
        Logger rootLogger = Logger.getLogger("");
        Level original = rootLogger.getLevel();
        rootLogger.setLevel(Level.ALL);
        rootLogger.addHandler(handler);

        try (MockedStatic<JEDependency> dependency = Mockito.mockStatic(JEDependency.class)) {
            dependency.when(() -> JEDependency.initializeWithRemapping(Mockito.any(), Mockito.eq(RCorePremium.class)))
                .thenThrow(new RuntimeException("boom"));

            RCorePremium plugin = MockBukkit.load(RCorePremium.class);

            dependency.verify(() -> JEDependency.initializeWithRemapping(plugin, RCorePremium.class));

            assertNull(plugin.getImpl(), "Failed bootstrap should leave delegate unset");
            assertFalse(plugin.isEnabled(), "Plugin must be disabled when delegate missing");
            assertFalse(this.server.getPluginManager().isPluginEnabled(plugin),
                "Plugin manager should mark RCore as disabled");

            boolean logged = handler.records.stream()
                .anyMatch(record -> record.getMessage().contains("[RCore] Cannot enable - RCore failed to load"));
            assertTrue(logged, "Missing delegate should be logged at severe level");
        } finally {
            rootLogger.removeHandler(handler);
            rootLogger.setLevel(original);
        }
    }

    private static final class RecordingHandler extends Handler {

        private final List<LogRecord> records = new CopyOnWriteArrayList<>();

        @Override
        public void publish(LogRecord record) {
            if (record != null) {
                this.records.add(record);
            }
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    }
}
