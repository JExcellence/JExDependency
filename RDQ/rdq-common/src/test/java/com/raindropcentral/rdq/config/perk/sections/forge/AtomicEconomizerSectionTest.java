package com.raindropcentral.rdq.config.perk.sections.forge;

import com.raindropcentral.rdq.config.perk.PluginCurrencySection;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class AtomicEconomizerSectionTest {

    @Test
    void itDoesNotInstantiateCurrencySectionsByDefault() {
        final AtomicEconomizerSection economizerSection = new AtomicEconomizerSection(new EvaluationEnvironmentBuilder());

        assertNull(economizerSection.getCurrencySections(),
            "getCurrencySections should return null when no currencies were configured");
    }

    @Test
    void itReturnsConfiguredCurrencySectionsFromGetter() throws Exception {
        final AtomicEconomizerSection economizerSection = new AtomicEconomizerSection(new EvaluationEnvironmentBuilder());
        final Map<String, PluginCurrencySection> currencySections = Map.of(
            "test-currency",
            new PluginCurrencySection(new EvaluationEnvironmentBuilder())
        );

        setField(economizerSection, "currencySections", currencySections);

        assertSame(currencySections, economizerSection.getCurrencySections(),
            "getCurrencySections should expose the configured currency section map");
    }

    private static void setField(final Object target, final String name, final Object value) throws Exception {
        final Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
