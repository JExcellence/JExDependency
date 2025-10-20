package com.raindropcentral.rplatform.config;

import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LoggerSectionTest {

    private LoggerSection section;

    @BeforeEach
    void setUp() {
        section = new LoggerSection(Mockito.mock(EvaluationEnvironmentBuilder.class));
    }

    @Test
    void defaultLevelReturnsConfiguredValue() {
        setField("defaultLevel", "WARNING");

        assertEquals("WARNING", section.getDefaultLevel());
    }

    @Test
    void defaultLevelFallsBackToInfoWhenUnset() {
        assertEquals("INFO", section.getDefaultLevel());
    }

    @Test
    void getLoggersReturnsConfiguredOverrides() {
        Map<String, String> overrides = new HashMap<>();
        overrides.put("com.example", "FINE");
        overrides.put("org.bukkit", "SEVERE");
        setField("loggers", overrides);

        assertSame(overrides, section.getLoggers());
    }

    @Test
    void getLoggersProvidesDefaultOverridesWhenMissing() {
        Map<String, String> defaults = section.getLoggers();

        assertEquals(6, defaults.size());
        assertEquals("ALL", defaults.get("com.raindropcentral"));
        assertEquals("INFO", defaults.get("de.jexcellence"));
        assertEquals("WARNING", defaults.get("me.devnatan.inventoryframework"));
        assertEquals("INFO", defaults.get("org.bukkit"));
        assertEquals("WARNING", defaults.get("org.hibernate"));
        assertEquals("WARNING", defaults.get("net.minecraft"));
    }

    @Test
    void debugModeReflectsConfigurationToggle() {
        assertFalse(section.isDebugMode());

        setField("debugMode", Boolean.TRUE);
        assertTrue(section.isDebugMode());

        setField("debugMode", Boolean.FALSE);
        assertFalse(section.isDebugMode());
    }

    @Test
    void consoleLoggingReflectsConfigurationToggle() {
        assertFalse(section.isConsoleLogging());

        setField("consoleLogging", Boolean.TRUE);
        assertTrue(section.isConsoleLogging());

        setField("consoleLogging", Boolean.FALSE);
        assertFalse(section.isConsoleLogging());
    }

    private void setField(final String fieldName, final Object value) {
        try {
            Field field = LoggerSection.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(section, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to set field " + fieldName, e);
        }
    }
}
