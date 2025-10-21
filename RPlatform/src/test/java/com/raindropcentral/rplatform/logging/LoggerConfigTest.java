package com.raindropcentral.rplatform.logging;

import com.raindropcentral.rplatform.config.LoggerSection;
import de.jexcellence.evaluable.ConfigKeeper;
import de.jexcellence.evaluable.ConfigManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LoggerConfigTest {

    @TempDir
    Path tempDir;

    private JavaPlugin plugin;
    private File dataFolder;

    @BeforeEach
    void setUp() {
        this.dataFolder = tempDir.resolve("plugin").toFile();
        assertTrue(dataFolder.mkdirs() || dataFolder.isDirectory());

        this.plugin = Mockito.mock(JavaPlugin.class);
        Mockito.when(plugin.getDataFolder()).thenReturn(dataFolder);
        Mockito.when(plugin.getName()).thenReturn("TestPlugin");
        Mockito.when(plugin.getResource(Mockito.anyString())).thenReturn(null);
    }

    @Test
    void loadAppliesSectionValuesAndOverrides() {
        final LoggerSection section = Mockito.mock(LoggerSection.class);
        Mockito.when(section.isConsoleLogging()).thenReturn(false);
        Mockito.when(section.isDebugMode()).thenReturn(true);
        Mockito.when(section.getDefaultLevel()).thenReturn("TRACE");

        final Map<String, String> overrides = new LinkedHashMap<>();
        overrides.put("com.example", "DEBUG");
        overrides.put("com.example.deep", "FINEST");
        overrides.put("org.bukkit", "ERROR");
        Mockito.when(section.getLoggers()).thenReturn(overrides);

        try (MockedConstruction<ConfigManager> ignoredManager = Mockito.mockConstruction(ConfigManager.class);
             MockedConstruction<ConfigKeeper> keeper = Mockito.mockConstruction(ConfigKeeper.class,
                     (mock, context) -> injectRootSection(mock, section))) {
            final LoggerConfig config = LoggerConfig.load(plugin);

            assertFalse(config.isConsoleEnabled());
            assertTrue(config.isDebugEnabled());
            assertEquals(LogLevel.TRACE, config.getConsoleLevel());
            assertEquals(LogLevel.TRACE, config.getFileLevel());

            assertEquals(LogLevel.FINEST, config.getLevelForPackage("com.example.deep.feature"));
            assertEquals(LogLevel.DEBUG, config.getLevelForPackage("com.example.service"));
            assertEquals(LogLevel.ERROR, config.getLevelForPackage("org.bukkit.command"));
            assertEquals(LogLevel.TRACE, config.getLevelForPackage("net.unknown"));
        }
    }

    @Test
    void loadCreatesDefaultsWhenSectionMissing() throws Exception {
        final Path logFile = dataFolder.toPath().resolve("logs").resolve("logging.yml");
        assertFalse(Files.exists(logFile));

        try (MockedConstruction<ConfigManager> ignoredManager = Mockito.mockConstruction(ConfigManager.class);
             MockedConstruction<ConfigKeeper> keeper = Mockito.mockConstruction(ConfigKeeper.class,
                     (mock, context) -> injectRootSection(mock, null))) {
            final LoggerConfig config = LoggerConfig.load(plugin);

            assertTrue(config.isConsoleEnabled());
            assertFalse(config.isDebugEnabled());
            assertEquals(LogLevel.INFO, config.getConsoleLevel());
            assertEquals(LogLevel.INFO, config.getFileLevel());
            assertEquals(LogLevel.WARNING, config.getLevelForPackage("com.raindropcentral.anything"));
            assertEquals(LogLevel.INFO, config.getLevelForPackage(null));
        }

        assertTrue(Files.exists(logFile), "Expected default logging.yml to be created");
    }

    @Test
    void loadFallsBackToDefaultsWhenConfigConstructionFails() throws Exception {
        final Path logFile = dataFolder.toPath().resolve("logs").resolve("logging.yml");
        assertFalse(Files.exists(logFile));

        try (MockedConstruction<ConfigManager> ignoredManager = Mockito.mockConstruction(ConfigManager.class);
             MockedConstruction<ConfigKeeper> keeper = Mockito.mockConstruction(ConfigKeeper.class,
                     (mock, context) -> { throw new IllegalStateException("boom"); })) {
            final LoggerConfig config = LoggerConfig.load(plugin);

            assertTrue(config.isConsoleEnabled());
            assertFalse(config.isDebugEnabled());
            assertEquals(LogLevel.INFO, config.getConsoleLevel());
            assertEquals(LogLevel.INFO, config.getFileLevel());
            assertEquals(LogLevel.WARNING, config.getLevelForPackage("com.raindropcentral.core"));
        }

        assertTrue(Files.exists(logFile), "Expected defaults to be persisted when load fails");
    }

    @Test
    void loadRequiresPluginInstance() {
        assertThrows(NullPointerException.class, () -> LoggerConfig.load(null));
    }

    private void injectRootSection(final ConfigKeeper<?> keeper, final LoggerSection section) {
        try {
            final Field field = ConfigKeeper.class.getDeclaredField("rootSection");
            field.setAccessible(true);
            field.set(keeper, section);
        } catch (ReflectiveOperationException exception) {
            fail("Unable to inject rootSection", exception);
        }
    }
}
