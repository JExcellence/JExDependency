package com.raindropcentral.rdq.config.perk.sections.forge;

import com.raindropcentral.rdq.config.perk.PluginCurrencySection;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class AmplificationForgeSectionTest {

    @Test
    void itDefaultsPrimitiveFieldsToZero() {
        final AmplificationForgeSection forgeSection = new AmplificationForgeSection(new EvaluationEnvironmentBuilder());

        assertEquals(0.0D, forgeSection.getChance(),
            "getChance should default to 0.0 before configuration");
        assertEquals(0.0D, forgeSection.getRate(),
            "getRate should default to 0.0 before configuration");
        assertEquals(0, forgeSection.getDistance(),
            "getDistance should default to 0 before configuration");
        assertNull(forgeSection.getCost(),
            "getCost should return null before any currency section is configured");
    }

    @Test
    void itReturnsConfiguredValuesFromGetters() throws Exception {
        final AmplificationForgeSection forgeSection = new AmplificationForgeSection(new EvaluationEnvironmentBuilder());
        final Map<String, PluginCurrencySection> cost = Map.of(
            "amplify",
            new PluginCurrencySection(new EvaluationEnvironmentBuilder())
        );

        setField(forgeSection, "chance", 0.45D);
        setField(forgeSection, "rate", 0.85D);
        setField(forgeSection, "distance", 12);
        setField(forgeSection, "cost", cost);

        assertEquals(0.45D, forgeSection.getChance(),
            "getChance should expose the configured amplification chance");
        assertEquals(0.85D, forgeSection.getRate(),
            "getRate should expose the configured amplification rate");
        assertEquals(12, forgeSection.getDistance(),
            "getDistance should expose the configured amplification radius");
        assertSame(cost, forgeSection.getCost(),
            "getCost should expose the configured currency section map");
    }

    private static void setField(final Object target, final String name, final Object value) throws Exception {
        final Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
