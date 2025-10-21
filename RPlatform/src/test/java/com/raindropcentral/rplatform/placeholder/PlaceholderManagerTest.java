package com.raindropcentral.rplatform.placeholder;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlaceholderManagerTest {

    private static final String IDENTIFIER = "test";

    private JavaPlugin plugin;
    private PluginManager pluginManager;
    private MockedStatic<Bukkit> bukkit;
    private Logger logger;
    private TestLogHandler logHandler;

    @BeforeEach
    void setUp() {
        plugin = Mockito.mock(JavaPlugin.class);
        pluginManager = Mockito.mock(PluginManager.class);

        logger = Logger.getLogger("PlaceholderManagerTest");
        logger.setUseParentHandlers(false);
        for (Handler handler : logger.getHandlers()) {
            logger.removeHandler(handler);
        }

        logHandler = new TestLogHandler();
        logger.addHandler(logHandler);

        Mockito.when(plugin.getLogger()).thenReturn(logger);
        Mockito.when(plugin.getName()).thenReturn("TestPlugin");

        PAPIHook.reset();

        bukkit = Mockito.mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getPluginManager).thenReturn(pluginManager);
    }

    @AfterEach
    void tearDown() {
        if (bukkit != null) {
            bukkit.close();
        }

        logger.removeHandler(logHandler);
        PAPIHook.reset();
    }

    @Test
    void registerSkipsWhenPlaceholderApiMissing() {
        Mockito.when(pluginManager.getPlugin("PlaceholderAPI")).thenReturn(null);

        PlaceholderManager manager = new PlaceholderManager(plugin, IDENTIFIER);
        manager.register();

        assertFalse(manager.isRegistered(), "Manager should remain unregistered when PlaceholderAPI is absent");
        assertEquals(0, PAPIHook.getRegisterCalls(), "Register should not be invoked without PlaceholderAPI");
        assertTrue(PAPIHook.getCallSequence().isEmpty(), "No placeholder lifecycle calls should be recorded");
        assertFalse(PAPIHook.isIdentifierActive(IDENTIFIER), "Identifier should not be tracked as active");
    }

    @Test
    void registerInvokesExpansionOnceWhenAvailable() {
        Plugin placeholderApi = Mockito.mock(Plugin.class);
        Mockito.when(pluginManager.getPlugin("PlaceholderAPI")).thenReturn(placeholderApi);

        PlaceholderManager manager = new PlaceholderManager(plugin, IDENTIFIER);
        manager.register();

        assertTrue(manager.isRegistered(), "Manager should mark the expansion as registered");
        assertEquals(1, PAPIHook.getRegisterCalls(), "Register should be invoked exactly once");
        assertEquals(List.of("register:" + IDENTIFIER), PAPIHook.getCallSequence(),
                "Identifier should be recorded in the registration sequence");
        assertTrue(PAPIHook.isIdentifierActive(IDENTIFIER), "Identifier should be tracked as active after registration");

        manager.register();

        assertEquals(1, PAPIHook.getRegisterCalls(), "Duplicate register calls must be ignored");
        assertEquals(List.of("register:" + IDENTIFIER), PAPIHook.getCallSequence(),
                "Call sequence should remain unchanged after duplicate registration attempts");
    }

    @Test
    void unregisterInvokesExpansionOnceAndClearsState() {
        Plugin placeholderApi = Mockito.mock(Plugin.class);
        Mockito.when(pluginManager.getPlugin("PlaceholderAPI")).thenReturn(placeholderApi);

        PlaceholderManager manager = new PlaceholderManager(plugin, IDENTIFIER);
        manager.register();
        manager.unregister();

        assertFalse(manager.isRegistered(), "Manager should clear registered flag after unregister");
        assertEquals(1, PAPIHook.getRegisterCalls(), "Register should have been invoked once");
        assertEquals(1, PAPIHook.getUnregisterCalls(), "Unregister should be invoked exactly once");
        assertEquals(List.of("register:" + IDENTIFIER, "unregister:" + IDENTIFIER), PAPIHook.getCallSequence(),
                "Call sequence should capture register and unregister operations in order");
        assertFalse(PAPIHook.isIdentifierActive(IDENTIFIER), "Identifier should be removed from the active registry");

        manager.unregister();

        assertEquals(1, PAPIHook.getUnregisterCalls(), "Duplicate unregister calls must be ignored");
        assertEquals(List.of("register:" + IDENTIFIER, "unregister:" + IDENTIFIER), PAPIHook.getCallSequence(),
                "Call sequence should remain unchanged after duplicate unregister attempts");
    }

    @Test
    void registerLogsWarningWhenIdentifierInvalid() {
        Plugin placeholderApi = Mockito.mock(Plugin.class);
        Mockito.when(pluginManager.getPlugin("PlaceholderAPI")).thenReturn(placeholderApi);

        PlaceholderManager manager = new PlaceholderManager(plugin, "Invalid Identifier!");
        logHandler.clear();
        manager.register();

        assertFalse(manager.isRegistered(), "Manager should remain unregistered when identifier validation fails");
        assertEquals(0, PAPIHook.getRegisterCalls(), "Invalid identifiers must prevent registration");
        assertTrue(PAPIHook.getCallSequence().isEmpty(), "No calls should be recorded for invalid identifiers");
        assertTrue(logHandler.containsMessage("Failed to register PlaceholderAPI expansion"),
                "A warning should be logged when registration fails");
    }

    @Test
    void duplicateRegistrationLogsWarningAndPreservesOriginalRegistration() {
        Plugin placeholderApi = Mockito.mock(Plugin.class);
        Mockito.when(pluginManager.getPlugin("PlaceholderAPI")).thenReturn(placeholderApi);

        PlaceholderManager firstManager = new PlaceholderManager(plugin, IDENTIFIER);
        firstManager.register();
        assertTrue(firstManager.isRegistered(), "Initial registration should succeed");
        assertEquals(1, PAPIHook.getRegisterCalls(), "First registration should increment call count");

        logHandler.clear();

        PlaceholderManager secondManager = new PlaceholderManager(plugin, IDENTIFIER);
        secondManager.register();

        assertFalse(secondManager.isRegistered(), "Second manager should not report registration after failure");
        assertEquals(1, PAPIHook.getRegisterCalls(), "Duplicate registration must not increment call count");
        assertEquals(List.of("register:" + IDENTIFIER), PAPIHook.getCallSequence(),
                "Only the initial registration should be recorded in the sequence");
        assertTrue(logHandler.containsMessage("Failed to register PlaceholderAPI expansion"),
                "Warning log should be emitted when duplicate registration occurs");

        firstManager.unregister();
        assertEquals(1, PAPIHook.getUnregisterCalls(), "Unregister should still succeed for the original manager");
        assertEquals(List.of("register:" + IDENTIFIER, "unregister:" + IDENTIFIER), PAPIHook.getCallSequence(),
                "Call sequence should include unregister from the original manager");
    }

    private static final class TestLogHandler extends Handler {

        private final List<String> messages = new ArrayList<>();

        @Override
        public void publish(final LogRecord record) {
            if (record.getLevel().intValue() >= Level.INFO.intValue()) {
                messages.add(record.getMessage());
            }
        }

        @Override
        public void flush() {
            // No buffered output
        }

        @Override
        public void close() {
            messages.clear();
        }

        void clear() {
            messages.clear();
        }

        boolean containsMessage(final String fragment) {
            return messages.stream().anyMatch(message -> message.contains(fragment));
        }
    }
}
