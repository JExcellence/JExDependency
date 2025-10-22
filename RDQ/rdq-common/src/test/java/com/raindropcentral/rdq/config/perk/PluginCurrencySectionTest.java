package com.raindropcentral.rdq.config.perk;

import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PluginCurrencySectionTest {

    @Test
    void itReturnsSafeDefaultsWhenFieldsAreUnset() {
        final PluginCurrencySection section = new PluginCurrencySection(new EvaluationEnvironmentBuilder());

        assertEquals("", section.getTargetPluginId(),
            "getTargetPluginId should return an empty string when the field is unset");
        assertEquals("", section.getCurrencyTypeId(),
            "getCurrencyTypeId should return an empty string when the field is unset");
        assertEquals(0.0, section.getAmount(),
            "getAmount should return 0.0 when the field is unset");
    }

    @Test
    void itReturnsConfiguredValuesFromGetters() throws Exception {
        final PluginCurrencySection section = new PluginCurrencySection(new EvaluationEnvironmentBuilder());

        setField(section, "targetPluginId", "example-plugin");
        setField(section, "currencyTypeId", "example-currency");
        setField(section, "amount", 42.5);

        assertEquals("example-plugin", section.getTargetPluginId(),
            "getTargetPluginId should expose the configured value");
        assertEquals("example-currency", section.getCurrencyTypeId(),
            "getCurrencyTypeId should expose the configured value");
        assertEquals(42.5, section.getAmount(),
            "getAmount should expose the configured value");
    }

    private static void setField(final Object target, final String name, final Object value) throws Exception {
        final Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
