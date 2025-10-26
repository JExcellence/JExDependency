package com.raindropcentral.rdq;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import de.jexcellence.dependency.JEDependency;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

class RDQPremiumTest {

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
    void lifecycleHappyPathDelegatesToConstructedImplementation() {
        try (MockedStatic<JEDependency> dependencyMock = Mockito.mockStatic(JEDependency.class);
             MockedConstruction<RDQPremiumImpl> implConstruction = Mockito.mockConstruction(RDQPremiumImpl.class, (mock, context) -> {
                 doNothing().when(mock).onLoad();
                 doNothing().when(mock).onEnable();
                 doNothing().when(mock).onDisable();
             })) {

            RDQPremium plugin = MockBukkit.load(RDQPremium.class);

            dependencyMock.verify(() -> JEDependency.initializeWithRemapping(plugin, RDQPremium.class));

            List<RDQPremiumImpl> constructed = implConstruction.constructed();
            assertEquals(1, constructed.size(), "Plugin should construct exactly one delegate");

            RDQPremiumImpl delegate = constructed.getFirst();

            verify(delegate).onLoad();
            assertSame(delegate, plugin.getImpl(), "getImpl should expose the constructed delegate");

            reset(delegate);

            plugin.onEnable();
            verify(delegate).onEnable();

            reset(delegate);

            plugin.onDisable();
            verify(delegate).onDisable();
        }
    }

    @Test
    void onLoadFailureLogsAndDisablesPlugin() {
        Logger rootLogger = Logger.getLogger("");
        RecordingHandler handler = new RecordingHandler();
        rootLogger.addHandler(handler);

        try (MockedStatic<JEDependency> dependencyMock = Mockito.mockStatic(JEDependency.class);
             MockedConstruction<RDQPremiumImpl> implConstruction = Mockito.mockConstruction(RDQPremiumImpl.class, (mock, context) ->
                     doThrow(new IllegalStateException("boom")).when(mock).onLoad())) {

            RDQPremium plugin = MockBukkit.load(RDQPremium.class);

            dependencyMock.verify(() -> JEDependency.initializeWithRemapping(plugin, RDQPremium.class));

            List<RDQPremiumImpl> constructed = implConstruction.constructed();
            assertEquals(1, constructed.size(), "Constructor should run even when onLoad fails");

            assertNull(plugin.getImpl(), "Delegate should be cleared when onLoad throws");

            boolean logged = handler.getRecords().stream()
                    .anyMatch(record -> record.getLevel() == Level.SEVERE
                            && record.getMessage().contains("Failed to load RDQ Premium"));
            assertTrue(logged, "onLoad should log the failure at SEVERE level");

            assertFalse(this.server.getPluginManager().isPluginEnabled(plugin),
                    "Plugin must be disabled when the delegate fails to load");
        } finally {
            rootLogger.removeHandler(handler);
        }
    }

    private static final class RecordingHandler extends Handler {

        private final List<LogRecord> records = new ArrayList<>();

        @Override
        public void publish(final LogRecord record) {
            this.records.add(record);
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }

        public List<LogRecord> getRecords() {
            return this.records;
        }
    }
}
