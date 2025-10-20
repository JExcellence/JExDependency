package com.raindropcentral.core;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.plugin.PluginManagerMock;
import de.jexcellence.dependency.JEDependency;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.lang.reflect.Field;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

class RCoreFreeTest {

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
    void lifecycleDelegatesToImplementationWhenLoadSucceeds() {
        try (MockedStatic<JEDependency> dependencyMock = Mockito.mockStatic(JEDependency.class);
             MockedConstruction<RCoreFreeImpl> implConstruction = Mockito.mockConstruction(RCoreFreeImpl.class, (mock, context) -> {
                 doNothing().when(mock).onLoad();
                 doNothing().when(mock).onEnable();
                 doNothing().when(mock).onDisable();
             })) {

            RCoreFree plugin = MockBukkit.load(RCoreFree.class);

            dependencyMock.verify(() -> JEDependency.initializeWithRemapping(plugin, RCoreFree.class));

            List<RCoreFreeImpl> constructed = implConstruction.constructed();
            assertEquals(1, constructed.size(), "Plugin should construct a single delegate instance");
            RCoreFreeImpl delegate = constructed.getFirst();

            verify(delegate).onLoad();
            verify(delegate).onEnable();
            assertSame(delegate, plugin.getImpl(), "getImpl should expose the constructed delegate");

            plugin.onDisable();
            verify(delegate).onDisable();
        }
    }

    @Test
    void loadFailureLogsAndDisablesPluginDuringEnable() throws Exception {
        PluginManagerMock pluginManagerSpy = spyPluginManager();
        Logger rootLogger = Logger.getLogger("");
        Level previousLevel = rootLogger.getLevel();
        List<LogRecord> records = new CopyOnWriteArrayList<>();
        Handler handler = new Handler() {
            @Override
            public void publish(final LogRecord record) {
                records.add(record);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        handler.setLevel(Level.ALL);
        rootLogger.addHandler(handler);
        rootLogger.setLevel(Level.ALL);

        try (MockedStatic<JEDependency> dependencyMock = Mockito.mockStatic(JEDependency.class);
             MockedConstruction<RCoreFreeImpl> implConstruction = Mockito.mockConstruction(RCoreFreeImpl.class, (mock, context) ->
                     doThrow(new IllegalStateException("boom")).when(mock).onLoad())) {

            RCoreFree plugin = MockBukkit.load(RCoreFree.class);

            dependencyMock.verify(() -> JEDependency.initializeWithRemapping(plugin, RCoreFree.class));

            assertEquals(1, implConstruction.constructed().size(), "Constructor should still run prior to failure");
            assertNull(plugin.getImpl(), "Delegate should be cleared when load fails");

            assertTrue(records.stream()
                    .anyMatch(record -> record.getLevel().intValue() >= Level.SEVERE.intValue()
                            && record.getMessage().contains("Failed to load RCore")),
                    "Failure should be logged at SEVERE level");

            verify(pluginManagerSpy).disablePlugin(plugin);
            assertFalse(plugin.isEnabled(), "Plugin should be disabled after failing to initialize");
        } finally {
            rootLogger.removeHandler(handler);
            rootLogger.setLevel(previousLevel);
        }
    }

    private PluginManagerMock spyPluginManager() throws Exception {
        PluginManagerMock pluginManager = Mockito.spy(this.server.getPluginManager());
        Field field = ServerMock.class.getDeclaredField("pluginManager");
        field.setAccessible(true);
        field.set(this.server, pluginManager);
        return pluginManager;
    }
}
