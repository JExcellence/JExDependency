package com.raindropcentral.rplatform.config;

import com.raindropcentral.rplatform.testutil.ReflectionTestUtils;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests defaulting and override behavior in {@link LoggerSection}.
 *
 * @author ItsRainingHP
 * @since 2.0.0
 * @version 1.0.0
 */
class LoggerSectionTest {

    @Test
    void exposesDefaultsWhenNoExplicitValuesAreSet() {
        final LoggerSection section = new LoggerSection(new EvaluationEnvironmentBuilder());

        assertEquals("INFO", section.getDefaultLevel());
        assertFalse(section.isDebugMode());
        assertFalse(section.isConsoleLogging());
        assertEquals("ALL", section.getLoggers().get("com.raindropcentral"));
    }

    @Test
    void defaultLoggerMapIsReturnedAsIndependentCopies() {
        final LoggerSection section = new LoggerSection(new EvaluationEnvironmentBuilder());

        final Map<String, String> first = section.getLoggers();
        first.put("custom.logger", "DEBUG");

        final Map<String, String> second = section.getLoggers();
        assertFalse(second.containsKey("custom.logger"));
    }

    @Test
    void usesConfiguredOverridesWhenProvided() {
        final LoggerSection section = new LoggerSection(new EvaluationEnvironmentBuilder());
        final Map<String, String> configuredLoggers = new HashMap<>(Map.of("com.example", "TRACE"));

        ReflectionTestUtils.setField(section, "defaultLevel", "DEBUG");
        ReflectionTestUtils.setField(section, "loggers", configuredLoggers);
        ReflectionTestUtils.setField(section, "debugMode", true);
        ReflectionTestUtils.setField(section, "consoleLogging", true);

        assertEquals("DEBUG", section.getDefaultLevel());
        assertEquals(configuredLoggers, section.getLoggers());
        assertTrue(section.isDebugMode());
        assertTrue(section.isConsoleLogging());
    }
}
