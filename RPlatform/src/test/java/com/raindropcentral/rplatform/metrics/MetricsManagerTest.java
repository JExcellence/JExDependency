package com.raindropcentral.rplatform.metrics;

import com.raindropcentral.rplatform.api.PlatformType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class MetricsManagerTest {

    private Logger logger;
    private TestLogHandler logHandler;

    @BeforeEach
    void setUp() {
        MetricsTestRegistry.reset();
        logger = Logger.getLogger("MetricsManagerTest-" + System.nanoTime());
        logger.setUseParentHandlers(false);
        logger.setLevel(Level.ALL);
        logHandler = new TestLogHandler();
        logger.addHandler(logHandler);
    }

    @AfterEach
    void tearDown() {
        logger.removeHandler(logHandler);
    }

    @Test
    void initializesMetricsWhenPluginEnabled() {
        JavaPlugin plugin = createPluginMock(true);

        new MetricsManager(plugin, 42, PlatformType.PAPER);

        assertSame(plugin, MetricsTestRegistry.getLastPlugin(), "Plugin should be forwarded to the metrics implementation");
        assertEquals(42, MetricsTestRegistry.getLastServiceId(), "Service ID should be passed to the metrics implementation");
        assertFalse(MetricsTestRegistry.isLastFoliaFlag(), "Paper platform should flag folia=false");
        assertTrue(MetricsTestRegistry.wasPluginEnabledAtConstruction(), "Enabled plugins should be recorded as enabled");
        assertEquals(1, MetricsTestRegistry.getInitializationCount(), "Successful initialization should be counted once");
        assertEquals(1, MetricsTestRegistry.getAttemptCount(), "Exactly one construction attempt should occur");

        MetricsTestRegistry.BStatsMetricsStub bStatsMetrics = MetricsTestRegistry.getLastBStatsMetrics();
        assertNotNull(bStatsMetrics, "BStats metrics stub should be created on success");
        assertSame(plugin, bStatsMetrics.getPlugin(), "BStats stub should receive the plugin instance");
        assertEquals(42, bStatsMetrics.getServiceId(), "BStats stub should record the service ID");
        assertFalse(bStatsMetrics.isFolia(), "Paper runs should not be marked as Folia");
        assertFalse(bStatsMetrics.isShutdown(), "BStats stub should start in an active state");
        assertFalse(logHandler.containsMessage("Failed to initialize metrics"), "No warnings should be logged on success");
    }

    @Test
    void logsWarningWhenPluginDisabled() {
        JavaPlugin plugin = createPluginMock(false);
        MetricsTestRegistry.setFailWhenPluginDisabled(true);

        new MetricsManager(plugin, 21, PlatformType.PAPER);

        assertEquals(1, MetricsTestRegistry.getAttemptCount(), "Construction should be attempted once");
        assertEquals(0, MetricsTestRegistry.getInitializationCount(), "Disabled plugins should not initialize metrics");
        assertNull(MetricsTestRegistry.getLastBStatsMetrics(), "No BStats stub should be created when initialization fails");
        assertTrue(logHandler.containsMessage("Failed to initialize metrics"), "Failure should be logged to the plugin logger");
    }

    @Test
    void logsWarningWhenServiceIdInvalid() {
        JavaPlugin plugin = createPluginMock(true);
        MetricsTestRegistry.setFailOnInvalidServiceId(true);

        new MetricsManager(plugin, 0, PlatformType.PAPER);

        assertEquals(1, MetricsTestRegistry.getAttemptCount(), "Invalid service IDs should still attempt construction");
        assertEquals(0, MetricsTestRegistry.getInitializationCount(), "Invalid service IDs should not initialize metrics");
        assertNull(MetricsTestRegistry.getLastBStatsMetrics(), "No BStats stub should be created for invalid IDs");
        assertTrue(logHandler.containsMessage("Failed to initialize metrics"), "Invalid IDs should log a warning");
    }

    @Test
    void repeatedInitializationsCreateIndependentMetricsInstances() {
        JavaPlugin plugin = createPluginMock(true);

        new MetricsManager(plugin, 7, PlatformType.PAPER);
        new MetricsManager(plugin, 8, PlatformType.FOLIA);

        assertEquals(2, MetricsTestRegistry.getAttemptCount(), "Each constructor call should attempt initialization");
        assertEquals(2, MetricsTestRegistry.getInitializationCount(), "Both initializations should succeed");
        assertEquals(8, MetricsTestRegistry.getLastServiceId(), "The most recent service ID should be tracked");
        assertTrue(MetricsTestRegistry.isLastFoliaFlag(), "Folia platform should mark folia=true");

        MetricsTestRegistry.BStatsMetricsStub bStatsMetrics = MetricsTestRegistry.getLastBStatsMetrics();
        assertNotNull(bStatsMetrics, "The latest BStats stub should be available");
        assertSame(plugin, bStatsMetrics.getPlugin(), "Plugin should propagate to the BStats stub");
        assertEquals(8, bStatsMetrics.getServiceId(), "BStats stub should capture the latest service ID");
        assertTrue(bStatsMetrics.isFolia(), "Folia runs should be marked as such on the stub");
    }

    private @NotNull JavaPlugin createPluginMock(final boolean enabled) {
        JavaPlugin plugin = Mockito.mock(JavaPlugin.class);
        Mockito.when(plugin.getLogger()).thenReturn(logger);
        Mockito.when(plugin.getName()).thenReturn("MetricsTestPlugin");
        Mockito.when(plugin.isEnabled()).thenReturn(enabled);
        return plugin;
    }

    private static final class TestLogHandler extends Handler {

        private final List<String> messages = new ArrayList<>();

        @Override
        public void publish(final LogRecord record) {
            if (record != null && record.getMessage() != null) {
                messages.add(record.getMessage());
            }
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }

        boolean containsMessage(final String text) {
            return messages.stream().anyMatch(message -> message.contains(text));
        }
    }
}
